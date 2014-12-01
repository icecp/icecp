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
package com.intel.icecp.node.channels.ndn.chronosync.algorithm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * A network-agnostic implementation of the ChronoSync protocol. See http://named-data.net/wp-content/uploads/2014/03/chronosync-icnp2013.pdf
 * for details.
 * <p>
 * To use this synchronization protocol over a network transport:
 * <pre>{@code
 *     ChronoSynchronizer<X> synchronizer = new ChronoSynchronizer<>(tree);
 *     synchronizer.observe([tie this to user application]);
 *     synchronizer.startRequesting([send digest bytes here]);
 *
 *     // when an incoming digest is received...
 *     synchronizer.onReceivedDigest(digest, [send state bytes here]);
 *
 *     // when incoming digest + states are received...
 *     synchronizer.onReceivedState(digest, state)
 *
 *     // when the user changes states
 *     synchronizer.updateState(state);
 * }</pre>
 * <p>
 * TODO optimize Timer--is it always needed? TODO synchronize observers
 *
 */
public class ChronoSynchronizer<T extends State> {

    public static final long DEFAULT_RESPONSE_DELAY_MS = 500;
    private static final Logger LOGGER = LogManager.getLogger();
    private final HistoricalDigestTree<T> states;
    private final Timer timer;
    private final long responseDelayMs;
    private final List<Observer<T>> localObservers = new ArrayList<>();
    private final Map<Digest, IncomingPendingRequest<T>> incomingPendingRequests = new HashMap<>();
    private TimerTask nextSyncRequest;
    private OutgoingRequestAction outgoingRequestAction;

    /**
     * @param states the starting state tree instance for this synchronizer
     */
    public ChronoSynchronizer(HistoricalDigestTree<T> states) {
        this(states, DEFAULT_RESPONSE_DELAY_MS);
    }

    /**
     * @param states the starting state tree instance for this synchronizer
     * @param responseDelayMs the average delay to wait before sending replies; updated information may come in while
     * waiting
     */
    public ChronoSynchronizer(HistoricalDigestTree<T> states, long responseDelayMs) {
        this.states = states;
        this.responseDelayMs = responseDelayMs;
        this.timer = new Timer(true);
    }

    /**
     * @return the current digest representing the states held locally by this synchronizer
     */
    public Digest currentDigest() {
        return states.digest();
    }

    /**
     * @return the set of current states held locally by this synchronizer
     */
    public Set<T> currentStates() {
        return states.all();
    }

    /**
     * Register a localObservers observer to be notified of changes to the state set; multiple localObservers are
     * supported
     *
     * @param observer the callback to fire
     */
    public void observe(Observer<T> observer) {
        localObservers.add(observer);
    }

    /**
     * Begin sync requests using the passed request action; this request action will be re-used for subsequent requests
     *
     * @param outgoingRequestAction the external code to execute to send a sync request
     */
    public void startRequesting(OutgoingRequestAction outgoingRequestAction) {
        this.outgoingRequestAction = outgoingRequestAction;
        sendSyncRequest();
    }

    /**
     * Send sync request using the latest digest available; this will fire the {@link #outgoingRequestAction}
     */
    public void sendSyncRequest() {
        if (outgoingRequestAction != null) {
            LOGGER.info("Sending sync request with {}", states.digest());
            outgoingRequestAction.request(states.digest());
        } else {
            LOGGER.warn("No sync request action is set; is ChronoSync configured incorrectly?");
        }
    }

    /**
     * Modify the tree by adding a new state; TODO should this trigger another sendSyncRequest()?
     *
     * @param state the new state to add
     */
    public void updateState(T state) {
        LOGGER.trace("Updating the current state with: {}", state);
        Digest oldDigest = states.digest();
        states.add(state);

        Set<T> complement = states.complement(oldDigest);
        Digest newDigest = states.digest();

        notifyLocalObservers(newDigest, complement);
        satisfyPendingRequests(newDigest, complement); // may need to this slower
    }

    /**
     * Handle incoming requests. Requests sent by remoteObservers clients contain a digest representing their latest
     * state. If our local state is different than theirs, we respond with our digest and the set of states that would
     * bring them up to date with us.
     *
     * @param digest the latest digest of the remoteObservers client
     * @param request the remoteObservers client's request, to be satisfied when we can answer with a newer state
     */
    public void onReceivedDigest(Digest digest, IncomingPendingRequest<T> request) {
        if (states.isCurrent(digest)) {
            LOGGER.info("Received current {}, waiting and observing", digest);
            incomingPendingRequests.put(digest, request);
        } else if (states.isEmpty(digest)) {
            LOGGER.info("Received empty {}, returning all latest states", digest);
            sendDelayedSyncResponse(states.digest(), states.all(), request);
        } else if (!states.isKnown(digest)) {
            LOGGER.info("Received unknown {}, returning all latest states", digest);
            sendDelayedSyncResponse(states.digest(), states.all(), request);
        } else if (states.isKnown(digest)) {
            LOGGER.info("Received known {}, returning only necessary states to update", digest);
            sendDelayedSyncResponse(states.digest(), states.complement(digest), request);
        } else {
            throw new Error("Code path should never reach here.");
        }
    }

    /**
     * Send delayed sync response to the passed request; TODO this should reset the timer, not add another
     *
     * @param digest
     * @param states
     * @param request
     */
    private void sendDelayedSyncResponse(Digest digest, Set<T> states, IncomingPendingRequest<T> request) {
        TimerTask response = new TimerTask() {
            @Override
            public void run() {
                sendSyncResponse(digest, states, request);
                sendDelayedSyncRequest();
            }
        };
        timer.schedule(response, responseDelayMs);
    }

    /**
     * Satisfy a pending request with the given states and digest
     *
     * @param digest the digest to send
     * @param latestStates the matching set of states (this may be a complement of the latest states if the pending
     * request does not require the full set)
     * @param request the pending request to satisfy
     */
    private void sendSyncResponse(Digest digest, Set<T> latestStates, IncomingPendingRequest<T> request) {
        LOGGER.trace("Sending synchronization response with {} states", latestStates.size());
        request.satisfy(digest, latestStates);
    }

    /**
     * Handle incoming state sets. A remoteObservers client will see our request and send us this response if they have
     * an updated state
     *
     * @param digest the latest digest of the remoteObservers client
     * @param complement the states to add to bring us to the same digest as the remoteObservers client
     * @throws SynchronizationException if the complement does not bring us to the expected digest
     */
    public void onReceivedState(Digest digest, Set<T> complement) throws SynchronizationException {
        LOGGER.trace("Received response with {} states at digest {}", complement.size(), digest);
        // TODO remove pending requests that match the digest, they will be satisfied by broadcast

        boolean changed = states.add(complement);
        if (changed) {
            notifyLocalObservers(digest, complement); // only notify local observers, remoteObservers observers will be notified through scheduled
            sendDelayedSyncRequest();
        }

        if (!digest.equals(states.digest())) {
            String message = String.format("Received response of %d states and digest %s but local observers updated digest is %s", complement.size(), digest, states.digest());
            throw new SynchronizationException(message);
        }
    }

    /**
     * As outlined in the paper, the sync requests are scheduled with a randomized wait timer to avoid out-of-order
     * delivery and multiple simultaneous publications
     */
    private void sendDelayedSyncRequest() {
        if (nextSyncRequest != null) {
            nextSyncRequest.cancel();
        }

        nextSyncRequest = new TimerTask() {
            @Override
            public void run() {
                sendSyncRequest();
            }
        };

        timer.schedule(nextSyncRequest, responseDelayMs);
    }

    /**
     * Notify all local observers
     *
     * @param digest the newly synchronized digest
     * @param states the changed states
     */
    protected void notifyLocalObservers(Digest digest, Set<T> states) {
        for (Observer<T> o : localObservers) {
            o.notify(digest, states);
        }
    }

    /**
     * Notify and remove all remote observers; TODO should this just retrieve the latest digest and states?
     *
     * @param digest the newly synchronized digest
     * @param states the changed states
     */
    protected void satisfyPendingRequests(Digest digest, Set<T> states) {
        for (IncomingPendingRequest<T> pr : incomingPendingRequests.values()) {
            pr.satisfy(digest, states);
        }
        incomingPendingRequests.clear();
    }

    /**
     * API for notifying observers that states have changed
     *
     * @param <T>
     */
    public interface Observer<T> {

        /**
         * @param digest the digest received
         * @param states the states received
         */
        void notify(Digest digest, Set<T> states);
    }

    /**
     * API for executing transmission-specific code to satisfy incoming requests; though this is very similar to
     * Observer, it is kept separate to distinguish the different functions they fill
     *
     * @param <T>
     */
    public interface IncomingPendingRequest<T> {

        /**
         * @param digest the digest received
         * @param states the states received
         */
        void satisfy(Digest digest, Set<T> states);
    }

    /**
     * API for executing transmission-specific code to send synchronization requests
     */
    public interface OutgoingRequestAction {

        /**
         * @param digest
         */
        void request(Digest digest);
    }
}