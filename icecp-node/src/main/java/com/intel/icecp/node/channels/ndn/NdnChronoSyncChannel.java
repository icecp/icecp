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

package com.intel.icecp.node.channels.ndn;

import com.intel.icecp.core.Message;
import com.intel.icecp.core.Metadata;
import com.intel.icecp.core.channels.ChannelBase;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.OnPublish;
import com.intel.icecp.core.permissions.ChannelPermission;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.node.channels.ndn.chronosync.NdnChronoState;
import com.intel.icecp.node.channels.ndn.chronosync.NdnChronoSynchronizerClient;
import com.intel.icecp.node.utils.SecurityUtils;
import com.intel.jndn.utils.Client;
import com.intel.jndn.utils.client.impl.AdvancedClient;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.security.SecurityException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Experimental implementation of a channel using the ChronoSync algorithm; the overhead of this approach is not
 * desirable in the case with constrained devices, but it does solve the multiple publisher problem.
 * <p>
 * Note: for this to work correctly, the NFDs involved must be configured with broadcast strategies on /bcast (name
 * shortened to save bytes).
 *
 */
class NdnChronoSyncChannel extends ChannelBase {

    private static final int NDN_VERSION_MARKER = 0xFD;
    private static final int CUSTOM_CLIENT_MARKER = 128;
    private static final Name BROADCAST_PREFIX = new Name("/bcast");
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int MAX_CACHED_MESSAGES = 65536;

    private final Name name;
    private final Face face;
    private final ScheduledExecutorService pool;
    private final Persistence persistence;
    private final Metadata[] metadata;
    private final MessageCache cache;
    private final NdnChronoSynchronizerClient synchronizerClient;
    private final Client retrievalClient = AdvancedClient.getDefault();
    private volatile boolean opened = false;
    private volatile boolean channelCloseScheduled = false;
    private boolean subscribing = false;
    private boolean publishing = false;
    private long localLatest = -1;
    private NdnChronoState latest;
    private long registeredPrefixId;

    NdnChronoSyncChannel(URI uri, Pipeline pipeline, Face face, ScheduledExecutorService pool, Persistence persistence, Metadata[] metadata) {
        super(uri, pipeline);
        this.name = new Name(uri.getSchemeSpecificPart());
        this.face = face;
        this.pool = pool;
        this.persistence = persistence;
        this.metadata = metadata;
        this.synchronizerClient = new NdnChronoSynchronizerClient(this.face, BROADCAST_PREFIX);
        this.cache = new MessageCache(persistence.persistFor, MAX_CACHED_MESSAGES);
    }

    /**
     * {@inheritDoc}
     * <p>
     * A channel may be closed and re-opened.  However, if the channel is scheduled to be closed in the future
     * ({@link #close()} was called but there are still messages that have not expired), then the channel cannot be
     * re-opened until the scheduled close has completed.
     *
     */
    @Override
    public CompletableFuture<Void> open() throws ChannelLifetimeException {
        SecurityUtils.checkPermission(new ChannelPermission(getName(), "open"));

        if (isOpen()) {
            throw new ChannelLifetimeException("The channel is already open: " + this);
        }

        if (channelCloseScheduled) {
            throw new ChannelLifetimeException("Failed to open channel, channel is scheduled to be closed: " + this);
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        synchronizerClient.start(clientId -> {
            opened = true;
            future.complete(null);
        }, exception -> future.completeExceptionally(new ChannelLifetimeException("Failed to start ChronoSync client", exception)));

        return future;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpen() {
        return opened;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws ChannelLifetimeException {
        SecurityUtils.checkPermission(new ChannelPermission(getName(), "close"));

        if (!isOpen()) {
            throw new ChannelLifetimeException("The channel is not yet open: " + this);
        }

        // wait for retained messages; otherwise they aren't available for
        // requesting subscribers
        if (isPublishing()) {
            long earliestCloseTime = cache.getEarliestCloseTime();
            // note that we schedule even impossibly long expiration times on
            // the thread pool in order to keep a reference to this channel
            // alive; otherwise, the garbage collector will kill this instance
            // and the published message queue we are holding
            pool.schedule(this::doClose, earliestCloseTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            channelCloseScheduled = true;
        } else {
            doClose();
        }

        synchronizerClient.stop();
    }

    /**
     * Helper method for {@link #close()}; clears prefixes and filters from NDN
     */
    private void doClose() {
        synchronizerClient.stop();
        if (isPublishing()) {
            unregisterPrefix();
        }
        channelCloseScheduled = false;
        LOGGER.debug("Closed channel: " + getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(Message message) throws ChannelIOException {
        SecurityUtils.checkPermission(new ChannelPermission(getName(), "publish"));

        if (!publishing) {
            try {
                registerPrefix();
            } catch (IOException | SecurityException | InterruptedException e) {
                throw new ChannelIOException("Failed to register channel: " + this, e);
            }
        }

        publishing = true;
        localLatest++;
        cache.add(localLatest, message);
        synchronizerClient.publish(localLatest);
    }

    /**
     * Register a prefix with the NFD; this method will block until a response is received from the NFD
     *
     * @throws IOException if the transport fails
     * @throws SecurityException if NDN encoding fails
     * @throws InterruptedException if while waiting for an NFD response, this thread is interrupted
     */
    private void registerPrefix() throws IOException, SecurityException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        MessageRequestHandler handler = new MessageRequestHandler(buildDataTemplate(), cache, NDN_VERSION_MARKER, pipeline, pool, this);
        face.registerPrefix(name, handler, prefix -> {
            LOGGER.error("Failed to register prefix for channel: {}", prefix);
            latch.countDown();
        }, (prefix, id) -> {
            LOGGER.trace("Registered prefix for channel: {}", prefix);
            this.registeredPrefixId = id;
            latch.countDown();
        });

        latch.await();
    }

    /**
     * Unregister the prefix with the NFD
     */
    private void unregisterPrefix() {
        face.removeRegisteredPrefix(registeredPrefixId);
    }

    /**
     * Build a {@link Data} template from the {@link Message}; this is used as the template for all segmented packets
     *
     * @return a data template
     */
    private Data buildDataTemplate() {
        Data data = new Data(name);
        if (persistence != null) {
            data.getMetaInfo().setFreshnessPeriod(persistence.persistFor);
        }
        return data;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPublishing() {
        return publishing;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void subscribe(OnPublish callback) throws ChannelIOException {
        SecurityUtils.checkPermission(new ChannelPermission(getName(), "subscribe"));

        subscribing = true;
        synchronizerClient.subscribe(changedStates -> {
            for (NdnChronoState s : changedStates) {
                latest = s;
                getMessage(s.message(), s.client()).thenAcceptAsync(message -> {
                    Thread.currentThread().setContextClassLoader(callback.getClass().getClassLoader());
                    try {
                        callback.onPublish(message);
                    } catch (Throwable t) {
                        LOGGER.error("Callback failed while handling message {} from client {}", s.message(), s.client(), t);
                    }
                }, pool);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSubscribing() {
        return subscribing;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture latest() throws ChannelIOException {
        SecurityUtils.checkPermission(new ChannelPermission(getName(), "subscribe"));

        if (latest != null) {
            return getMessage(latest.message(), latest.client());
        } else {
            return getLatestMessage();
        }
    }

    /**
     * Retrieve the latest message; expects messages to be published like: /a/b/c/[message id]/[client id]
     *
     * @param id the message ID
     * @param client the client ID
     * @return a future completed when the message is received
     */
    private CompletableFuture<Message> getMessage(long id, long client) {
        Interest interest = buildBaseInterest();
        Name.Component idMarker = Name.Component.fromNumberWithMarker(id, NDN_VERSION_MARKER);
        Name.Component clientMarker = Name.Component.fromNumberWithMarker(client, CUSTOM_CLIENT_MARKER);
        interest.getName().append(idMarker).append(clientMarker);

        // send out interest packets
        LOGGER.debug("Requesting message {}: {}", id, interest.toUri());
        return request(interest);
    }

    /**
     * Retrieve the latest message; expects messages to be published like: /a/b/c/[message id]/[client id]
     *
     * @return a future completed when the message is received
     */
    private CompletableFuture<Message> getLatestMessage() {
        Interest interest = buildBaseInterest();
        interest.setChildSelector(Interest.CHILD_SELECTOR_RIGHT);

        // send out interest packets
        LOGGER.debug("Requesting latest message: {}", interest.toUri());
        return request(interest);
    }

    /**
     * @return the base interest for the request; will contain the channel name, correct persistence metadata, and
     * require a fresh response
     */
    private Interest buildBaseInterest() {
        Interest interest = new Interest(new Name(name));
        interest.setMustBeFresh(true);

        // set interest lifetime
        if (persistence.hasRetrievalLifetime()) {
            interest.setInterestLifetimeMilliseconds(persistence.retrieveUnder);
        }

        return interest;
    }

    /**
     * Request the given interest and deserialize it
     *
     * @param interest the interest to send
     * @return a future completed when the data is received is deserialized to a message
     */
    private CompletableFuture<Message> request(Interest interest) {
        CompletableFuture<Data> futurePacket = retrievalClient.getAsync(face, interest);
        return futurePacket.thenApply(new MessageDeserializer(pipeline));
    }
}
