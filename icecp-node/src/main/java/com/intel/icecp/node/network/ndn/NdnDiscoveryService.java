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

import com.intel.icecp.core.Node;
import com.intel.jndn.utils.client.impl.SimpleClient;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.named_data.jndn.Data;
import net.named_data.jndn.Exclude;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnRegisterSuccess;
import net.named_data.jndn.encoding.EncodingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Run a discovery service for advertising and discovering NDN endpoints; in the
 * future this may use UDP multicast but for now it relies on the discovery
 * prefix to have a broadcast strategy to pass on discoverys packets.
 *
 */
public class NdnDiscoveryService {

    private static final Logger logger = LogManager.getLogger();
    private static final String DISCOVERY_SUFFIX = "discovery";
    private static final long DISCOVERY_PACKET_LIFETIME_MS = 10000;
    private long prefixId = -1;
    private final Name globalPrefix;
    private final NdnNetworkEndpoint localEndpoint;
    private final Face face;
    private final Name discoveryPrefix;

    public NdnDiscoveryService(Name globalPrefix, NdnNetworkEndpoint localEndpoint, Face face) {
        this.globalPrefix = globalPrefix;
        this.discoveryPrefix = new Name(globalPrefix).append(DISCOVERY_SUFFIX);
        this.localEndpoint = localEndpoint;
        this.face = face;
    }

    /**
     * Stope responding to discovery requests
     */
    public void stop() {
        if (prefixId != -1) {
            face.removeRegisteredPrefix(prefixId);
        }
    }

    /**
     * Respond to discovery requests with the encoded endpoint entry
     *
     * @return a future completed when the necessary prefixes are registered
     */
    public CompletableFuture<Void> start() {
        final Name discoveryEntry = new Name(discoveryPrefix).append(createRandomNonce());
        Data data = buildDiscoveryPacket(discoveryEntry);
        CompletableFuture future = new CompletableFuture();

        try {
            prefixId = face.registerPrefix(discoveryPrefix, new OnInterestCallback() {
                @Override
                public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
                    if (interest.matchesName(discoveryEntry)) {
                        try {
                            face.putData(data);
                        } catch (Exception ex) {
                            logger.error("Failed to send directory entry for interest: " + interest.toUri(), ex);
                        }
                    }
                }
            }, new OnRegisterFailed() {
                @Override
                public void onRegisterFailed(Name prefix) {
                    future.completeExceptionally(new IOException("Failed to register prefix: " + prefix.toUri()));
                    logger.error("Failed to register prefix: " + prefix.toUri());
                }
            }, new OnRegisterSuccess() {

                @Override
                public void onRegisterSuccess(Name prefix, long registeredPrefixId) {
                    future.complete(null);
                }
            });
            logger.info("Registered directory prefix: " + discoveryPrefix.toUri());
        } catch (Exception ex) {
            logger.error("Failed to register on directory prefix: " + discoveryEntry.toUri());
        }

        return future;
    }

    /**
     * @param discoveryEntry the discovery packet name with its randomized nonce
     * @return a {@link Data} packet with the encoded endpoint information
     */
    private Data buildDiscoveryPacket(final Name discoveryEntry) {
        final Data data = new Data(discoveryEntry);
        data.setContent(localEndpoint.wireEncode());
        data.getMetaInfo().setFreshnessPeriod(DISCOVERY_PACKET_LIFETIME_MS);
        return data;
    }

    /**
     * @return a random nonce to distinguish this {@link Node} entry from all
     * others; theoretically more entry types could be added and this avoids
     * having to encode potentially long names into the entry name.
     */
    private byte[] createRandomNonce() {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    /**
     * @param face a {@link Face}
     * @return the list of names of devices that are registered on the default
     * directory prefix
     */
    public List<NdnNetworkEndpoint> list(Face face) {
        return list(face, discoveryPrefix);
    }

    /**
     * @param face an NDN {@link Face}
     * @param discoveryPrefix the discovery prefix to send interests to
     * @return the list of names of devices that are registered on the prefix
     */
    public List<NdnNetworkEndpoint> list(Face face, Name discoveryPrefix) {
        List<NdnNetworkEndpoint> names = new ArrayList<>();

        Interest interest = new Interest(discoveryPrefix);
        interest.setInterestLifetimeMilliseconds(2000);
        interest.setMustBeFresh(true);

        Exclude excludeNonces = new Exclude();
        boolean moreEntries = true;
        do {
            try {
                Data entry = SimpleClient.getDefault().getSync(face, interest);
                NdnNetworkEndpoint endpoint = NdnNetworkEndpoint.wireDecode(entry.getContent().buf());
                names.add(endpoint);
                logger.info("Discovered endpoint: " + endpoint.toUri());

                Name.Component nonce = entry.getName().get(-1);
                excludeNonces.appendComponent(nonce);
                interest.setExclude(excludeNonces);
                logger.info("Excluding nonce: " + nonce.toEscapedString());
            } catch (IOException | EncodingException ex) {
                logger.info("No more entries found on: " + discoveryPrefix.toUri());
                moreEntries = false;
            }
        } while (moreEntries);

        return names;
    }
}
