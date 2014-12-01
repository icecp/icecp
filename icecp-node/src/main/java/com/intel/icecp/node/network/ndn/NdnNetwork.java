/*
 * ******************************************************************************
 *
 * INTEL CONFIDENTIAL
 *
 * Copyright 2013 - 2016 Intel Corporation All Rights Reserved.
 *
 * The source code contained or described herein and all documents related to
 * the source code ("Material") are owned by Intel Corporation or its suppliers
 * or licensors. Title to the Material remains with Intel Corporation or its
 * suppliers and licensors. The Material contains trade secrets and proprietary
 * and confidential information of Intel or its suppliers and licensors. The
 * Material is protected by worldwide copyright and trade secret laws and treaty
 * provisions. No part of the Material may be used, copied, reproduced,
 * modified, published, uploaded, posted, transmitted, distributed, or disclosed
 * in any way without Intel's prior express written permission.
 *
 * No license under any patent, copyright, trade secret or other intellectual
 * property right is granted to or conferred upon you by disclosure or delivery
 * of the Materials, either expressly, by implication, inducement, estoppel or
 * otherwise. Any license under such intellectual property rights must be
 * express and approved by Intel in writing.
 *
 * Unless otherwise agreed by Intel in writing, you may not remove or alter this
 * notice or any other notice embedded in Materials by Intel or Intel's
 * suppliers or licensors in any way.
 *
 * ******************************************************************************
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
