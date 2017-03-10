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

package com.intel.icecp.node.channels.ndn.chronosync;

import com.intel.icecp.node.channels.ndn.chronosync.algorithm.ChronoSynchronizer;
import com.intel.icecp.node.channels.ndn.chronosync.algorithm.ChronoSynchronizer.IncomingPendingRequest;
import com.intel.icecp.node.channels.ndn.chronosync.algorithm.ChronoSynchronizer.Observer;
import com.intel.icecp.node.channels.ndn.chronosync.algorithm.Digest;
import com.intel.icecp.node.channels.ndn.chronosync.algorithm.HistoricalDigestTree;
import com.intel.icecp.node.channels.ndn.chronosync.algorithm.SynchronizationException;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnRegisterSuccess;
import net.named_data.jndn.OnTimeout;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.util.Blob;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ConnectException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Set;

/**
 * Synchronize states using NDN; see the following for details: <ul> <li>Original paper,
 * http://named-data.net/wp-content/uploads/2014/03/chronosync-icnp2013.pdf</li> <li>C++ implementation,
 * https://github.com/named-data/ChronoSync/blob/master/src/logic.cpp</li> <li>Java implementation,
 * https://github.com/named-data/jndn/blob/master/src/net/named_data/jndn/sync/ChronoSync2013.java</li> </ul>
 * <p>
 * For proper operation with NDN, the passed {@link #broadcastPrefix} must broadcast to all intended recipients; e.g.
 * nfdc set-strategy /broadcast /localhost/nfd/strategy/broadcast
 * <p>
 * TODO interest suppression TODO interest exclude filters TODO interest freshness
 *
 */
public class NdnChronoSynchronizerClient {

    public static final long DEFAULT_SYNC_REQUEST_LIFETIME_MS = 10000;
    public static final int DEFAULT_MAX_HISTORICAL_RECORDS = 20;
    public static final String DEFAULT_DIGEST_ALGORITHM = "SHA-256";
    private static final Logger LOGGER = LogManager.getLogger();
    private final ChronoSynchronizer<NdnChronoState> synchronizer;
    private final Face face;
    private final Name broadcastPrefix;
    private final long clientId;
    private final long syncRequestLifetimeMs;
    private long broadcastPrefixId;

    /**
     * Build a synchronizer client
     *
     * @param synchronizer the ChronoSync implementation to use
     * @param face the NDN face on which to transmit sync requests
     * @param broadcastPrefix the NDN prefix on which to transmit sync requests; note that this should be configured
     * with a broadcast strategy (see http://named-data.net/doc/NFD/0.1.0/manpages/nfdc.html) for correct operation
     * @param clientId the unique identifier for this client; should be large enough (and random enough) to avoid any
     * collisions
     * @param syncRequestLifetimeMs the amount of time (in ms) between refreshing the sync request sent to remote
     * clients
     */
    public NdnChronoSynchronizerClient(ChronoSynchronizer<NdnChronoState> synchronizer, Face face, Name broadcastPrefix, long clientId, long syncRequestLifetimeMs) {
        this.synchronizer = synchronizer;
        this.face = face;
        this.broadcastPrefix = broadcastPrefix;
        this.clientId = clientId;
        this.syncRequestLifetimeMs = syncRequestLifetimeMs;
    }

    /**
     * Build a synchronizer client using sane defaults, see DEFAULT_* constants
     *
     * @param face
     * @param broadcastPrefix
     * @param clientId
     */
    public NdnChronoSynchronizerClient(Face face, Name broadcastPrefix, long clientId) {
        this(buildDefaultSynchronizer(), face, broadcastPrefix, clientId, DEFAULT_SYNC_REQUEST_LIFETIME_MS);
    }

    /**
     * Build a synchronizer client using sane defaults, see DEFAULT_* constants
     *
     * @param face the NDN face on which to transmit sync requests
     * @param broadcastPrefix the NDN prefix on which to transmit sync requests; note that this should be configured
     * with a broadcast strategy (see http://named-data.net/doc/NFD/0.1.0/manpages/nfdc.html) for correct operation
     */
    public NdnChronoSynchronizerClient(Face face, Name broadcastPrefix) {
        this(buildDefaultSynchronizer(), face, broadcastPrefix, new SecureRandom().nextLong(), DEFAULT_SYNC_REQUEST_LIFETIME_MS);
    }

    /**
     * Helper method for instantiating a default ChronoSync implementation
     *
     * @return a default ChronoSync implementation
     */
    private static ChronoSynchronizer<NdnChronoState> buildDefaultSynchronizer() {
        try {
            MessageDigest digest = MessageDigest.getInstance(DEFAULT_DIGEST_ALGORITHM);
            return new ChronoSynchronizer(new HistoricalDigestTree(DEFAULT_MAX_HISTORICAL_RECORDS, digest));
        } catch (NoSuchAlgorithmException e) {
            String message = String.format("Unable to instantiate digest algorithm \"{0}\"; this client instance has not been setup correctly and further use would result in undefined behavior", DEFAULT_DIGEST_ALGORITHM);
            throw new IllegalStateException(message, e);
        }
    }

    /**
     * @return the client identifier for this synchronizer instance; TODO can this be removed?
     */
    public long clientId() {
        return clientId;
    }

    /**
     * @return the current states held by this client
     */
    public Set<NdnChronoState> currentState() {
        return synchronizer.currentStates();
    }

    /**
     * Starts network transmissions; since events on the face are asynchronous, failure is returned through a callback.
     * If this method fails, use of the client is undefined behavior; users may attempt to retry by calling this method
     * again.
     *
     * @param onSuccess fired with the client ID when this client is started successfully
     * @param onError fired with the exception thrown when this client fails to start
     */
    public void start(Callback<Long> onSuccess, Callback<Exception> onError) {
        try {
            broadcastPrefixId = face.registerPrefix(broadcastPrefix, new OnInterestCallback() {
                @Override
                public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
                    handleSyncRequest(face, interest);
                }
            }, new OnRegisterFailed() {
                @Override
                public void onRegisterFailed(Name prefix) {
                    onError.accept(new ConnectException("Failed to register prefix on the given face: " + prefix.toUri()));
                }
            }, new OnRegisterSuccess() {
                @Override
                public void onRegisterSuccess(Name prefix, long registeredPrefixId) {
                    synchronizer.startRequesting(new NdnOutgoingRequestAction());
                }
            });
        } catch (IOException | SecurityException e) {
            onError.accept(e);
        }
    }

    /**
     * Handle sync requests by parsing and passing on to the {@link ChronoSynchronizer} instance
     *
     * @param face the incoming face
     * @param interest the incoming interest
     */
    protected void handleSyncRequest(Face face, Interest interest) {
        Digest incomingDigest = new Digest(interest.getName().get(-1).getValue().getImmutableArray());
        LOGGER.info("Received sync interest on client {} for digest: {}", clientId, incomingDigest);

        // when the synchronizer updates, it will call this to send updated state to requestors
        IncomingPendingRequest incomingRequest = new IncomingPendingRequest<NdnChronoState>() {
            @Override
            public void satisfy(Digest digest, Set<NdnChronoState> states) {
                LOGGER.info("Sending sync data on client {} for digest {}", clientId, digest);
                Name outgoingName = interest.getName().append(digest.toBytes());
                Data data = new Data(outgoingName);
                data.setContent(new Blob(NdnChronoState.wireEncodeMultiple(states), false));
                try {
                    face.putData(data);
                } catch (IOException e) {
                    LOGGER.error("Failed to send response for incoming digest: " + incomingDigest, e);
                }
            }
        };

        synchronizer.onReceivedDigest(incomingDigest, incomingRequest);
    }

    /**
     * Send a sync request by appending the digest to the broadcast prefix: e.g. /broadcast/00112233...; when another
     * client has an updated digest, they will respond with a data named /broadcast/[old digest]/[new digest]
     *
     * @param face the incoming face
     * @param digest the current digest
     * @throws IOException
     */
    protected void sendSyncRequest(Face face, Digest digest) throws IOException {
        LOGGER.info("Sending sync interest on client {} for digest {}", clientId, digest);

        Interest interest = new Interest(broadcastPrefix);
        interest.getName().append(digest.toBytes());
        interest.setInterestLifetimeMilliseconds(syncRequestLifetimeMs);
        // interest.setMustBeFresh(true);
        // interest.setExclude();

        face.expressInterest(interest, new OnData() {
            @Override
            public void onData(Interest interest, Data data) {
                Digest digest = new Digest(data.getName().get(-1).getValue().getImmutableArray());
                LOGGER.info("Received sync data on client {} for digest {}", clientId, digest);

                Set<NdnChronoState> states = NdnChronoState.wireDecodeMultiple(data.getContent().buf());
                try {
                    synchronizer.onReceivedState(digest, states);
                } catch (SynchronizationException e) {
                    LOGGER.warn("Client {} failed to synchronize with the received data packet: {}", clientId, data.getName().toUri(), e);
                }
            }
        }, new OnTimeout() {
            @Override
            public void onTimeout(Interest interest) {
                LOGGER.warn("No response for sync interest on client {} (either no clients have an updated digest, no clients are participating, or the network dropped the packet).", clientId);
                synchronizer.sendSyncRequest();
            }
        });
    }

    /**
     * Publish a new state; this method allows the client to publish states that do not have the client ID
     *
     * @param state the state to publish
     */
    void publish(NdnChronoState state) {
        LOGGER.info("Client {} is publishing state: {}", clientId, state);
        if (state.client() != clientId) {
            LOGGER.warn("Publishing a state for a non-local client ID {} when the local client ID is {}; is this intentional?", state.client(), clientId);
        }
        synchronizer.updateState(state);
    }

    /**
     * Publish the ID of the latest thing available on this client; note that IDs must grow sequentially for
     * NdnChronoState to understand what the latest state is. For example:
     * <p>
     * <pre><code>
     *     client.publish(0);
     *     client.publish(1);
     *     client.publish(9);
     *     client.publish(3); // will have no effect
     * </code></pre>
     *
     * @param id the ID of the latest thing available on this client
     */
    public void publish(long id) {
        publish(new NdnChronoState(clientId, id));
    }

    /**
     * Subscribe to synchronization events, e.g. when a remote client publishes a new ID. The returned set are the
     * changes necessary to bring this client up to date with the remote
     *
     * @param callback the callback fired when the synchronization occurs
     */
    public void subscribe(Callback<Set<NdnChronoState>> callback) {
        LOGGER.trace("Client {} is subscribing with an observer", clientId);
        synchronizer.observe(new Observer<NdnChronoState>() {
            @Override
            public void notify(Digest digest, Set<NdnChronoState> states) {
                callback.accept(states);
            }
        });
    }

    /**
     * Stop the network transmissions for this client; TODO in the future, this should might send a delete action to
     * other clients
     *
     * @param callback fired when this client successfully stops
     */
    public void stop(Callback<Long> callback) {
        face.removeRegisteredPrefix(broadcastPrefixId);
        // TODO remove synchronizer observers?

        if (callback != null) {
            callback.accept(clientId);
        }
    }

    /**
     * Helper method for {@link #stop(Callback)};
     */
    public void stop() {
        stop(null);
    }

    /**
     * API for callbacks exposed by this class; this is very similar to Java 8's Consumer class but Java 7 compatibility
     * is preserved
     *
     * @param <T> the type of the thing to accept
     */
    public interface Callback<T> {

        /**
         * Accept a thing from the calling context
         *
         * @param thing the callback parameter
         */
        void accept(T thing);
    }

    /**
     * Wrapper class defining to ChronoSync client how sync requests should be sent; see {@link #sendSyncRequest(Face,
     * Digest)}.
     */
    private class NdnOutgoingRequestAction implements ChronoSynchronizer.OutgoingRequestAction {
        @Override
        public void request(Digest digest) {
            try {
                sendSyncRequest(face, digest);
            } catch (IOException e) {
                LOGGER.error(e);
            }
        }
    }
}