/*
 * Copyright (c) 2017 Intel Corporation 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intel.icecp.node.network.ndn;

import com.intel.icecp.core.Network;
import com.intel.jndn.management.ManagementException;
import com.intel.jndn.management.Nfdc;
import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Represent an NDN network
 *
 */
public class NdnNetwork implements Network<NdnNetworkEndpoint> {

    private static final org.apache.logging.log4j.Logger logger = LogManager.getLogger();
    private final URI nfdHostname;
    private final Name globalPrefix;
    private final Name identityPrefix;
    private final Face face;
    private final NdnNetworkEndpoint localEndpoint;
    private final NdnDiscoveryService discoveryService;
    private ScheduledExecutorService pool;
    private List<NdnNetworkEndpoint> endpoints = new ArrayList<>();

    /**
     *
     * @param nfdHostname e.g. "tcp4://nfd.server.org
     * @param globalPrefix the broadcast prefix all nodes subscribe to
     * @param identityPrefix the identity prefix of this node
     * @param face the face on which this node communicates
     * @param pool the thread pool
     */
    public NdnNetwork(URI nfdHostname, Name globalPrefix, Name identityPrefix, Face face, ScheduledExecutorService pool) {
        this.nfdHostname = nfdHostname;
        this.globalPrefix = globalPrefix;
        this.identityPrefix = identityPrefix;
        this.face = face;
        this.pool = pool;

        this.localEndpoint = buildLocalEndpoint(identityPrefix);
        this.discoveryService = new NdnDiscoveryService(globalPrefix, localEndpoint, face);
    }

    private NdnNetworkEndpoint buildLocalEndpoint(Name identityPrefix) {
        try {
            return new NdnNetworkEndpoint(identityPrefix, InetAddress.getLocalHost().getCanonicalHostName());
        } catch (UnknownHostException ex) {
            throw new Error("Could not determine local address, aborting.", ex);
        }
    }

    @Override
    public String scheme() {
        return "ndn";
    }

    @Override
    public NdnNetworkEndpoint local() {
        return localEndpoint;
    }

    @Override
    public NdnNetworkEndpoint[] list() {
        return endpoints.toArray(new NdnNetworkEndpoint[endpoints.size()]);
    }

    @Override
    public CompletableFuture<Void> connect(NdnNetworkEndpoint endpoint) {
        return addNode(face, endpoint).thenAccept((t) -> endpoints.add(endpoint));
    }

    private CompletableFuture<Void> addNode(final Face face, final NdnNetworkEndpoint endpoint) {
        return rtt(endpoint).thenAcceptAsync((rtt) -> {
            try {
                String canonicalForm = endpoint.toCanonicalForm();
                Nfdc.register(face, canonicalForm, globalPrefix, rtt);
                Nfdc.register(face, canonicalForm, endpoint.prefix, rtt);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }, pool);
    }

    @Override
    public CompletableFuture<Void> disconnect(NdnNetworkEndpoint endpoint) {
        return CompletableFuture.runAsync(() -> {
            try {
                String canonicalForm = endpoint.toCanonicalForm();
                Nfdc.unregister(face, globalPrefix, canonicalForm);
                Nfdc.unregister(face, endpoint.prefix, canonicalForm);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }, pool).thenAccept((t) -> endpoints.remove(endpoint));
    }

    @Override
    public CompletableFuture<NdnNetworkEndpoint[]> discover() {
        return CompletableFuture.supplyAsync(() -> {
            List<NdnNetworkEndpoint> list = discoveryService.list(face);
            return list.toArray(new NdnNetworkEndpoint[list.size()]);
        }, pool);
    }

    @Override
    public CompletableFuture<Void> advertise() throws IOException {
        return CompletableFuture.runAsync(() -> {
            try {
                setupBroadcastStrategy(face, globalPrefix);
                discoveryService.start().get();
            } catch (IOException | InterruptedException | ExecutionException ex) {
                logger.error("Failed to advertise.", ex);
                throw new RuntimeException(ex);
            }
        }, pool);
    }

    /**
     * Set the strategy of the global mesh prefix to broadcast.
     *
     * @param localFace a {@link Face} to a local NFD
     * @param prefix the global mesh prefix
     * @throws IOException if NFD management fails
     */
    private void setupBroadcastStrategy(Face localFace, Name prefix) throws IOException {
        try {
            Nfdc.setStrategy(localFace, prefix, new Name("/localhost/nfd/strategy/broadcast"));
        } catch (ManagementException ex) {
            throw new IOException(ex);
        }
    }

    public void stopAdvertising() {
        discoveryService.stop();
    }

    @Override
    public CompletableFuture<Integer> rtt(NdnNetworkEndpoint endpoint) {
        CompletableFuture<Integer> promise = new CompletableFuture<>();

        pool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Long startTime = System.currentTimeMillis();
                    AsynchronousSocketChannel sc = AsynchronousSocketChannel.open();
                    sc.connect(endpoint.toAddress(), startTime, new CompletionHandler<Void, Long>() {
                        @Override
                        public void completed(Void v, Long startTime) {
                            long endTime = System.currentTimeMillis();
                            long totalTime = endTime - startTime;
                            promise.complete((int) totalTime);
                        }

                        @Override
                        public void failed(Throwable thrwbl, Long startTime) {
                            promise.completeExceptionally(thrwbl);
                        }
                    }
                    );
                } catch (IOException ex) {
                    promise.completeExceptionally(ex);
                }
            }
        });
        return promise;
    }

}
