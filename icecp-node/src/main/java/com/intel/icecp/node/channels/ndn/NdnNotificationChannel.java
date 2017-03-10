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
package com.intel.icecp.node.channels.ndn;

import com.intel.icecp.core.Message;
import com.intel.icecp.core.Metadata;
import com.intel.icecp.core.channels.ChannelBase;
import com.intel.icecp.core.channels.OnLatest;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.metadata.Window;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.OnPublish;
import com.intel.icecp.core.permissions.ChannelPermission;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.node.channels.ndn.notification.Filterable;
import com.intel.icecp.node.channels.ndn.notification.NdnChannelPublisher;
import com.intel.icecp.node.channels.ndn.notification.NdnChannelSubscriber;
import com.intel.icecp.node.channels.ndn.notification.OnPublishNotification;
import com.intel.icecp.node.utils.SecurityUtils;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnRegisterSuccess;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The base NDN implementation of a notification channel; uses NDN version components to distinguish between published
 * messages. Note that two faces are used per channel so that a channel can subscribe to itself (one face is used for
 * interests and the other to register prefixes, i.e. receive interests); NDN does not allow a face to receive an
 * interest from itself.
 *
 */
public class NdnNotificationChannel extends ChannelBase implements Filterable {

    public static final String DATA_SUFFIX = "data";
    public static final long LATEST_REQUEST_LIFETIME = 1000;
    private static final int NDN_VERSION_MARKER = 0xFD;
    private static final String UPDATE_NOTIFICATION_SUFFIX = "update";
    private static final long UPDATE_NOTIFICATION_LIFETIME = 1000;
    private static final Logger logger = LogManager.getLogger();
    private static final String PERMISSION_TAG_SUBSCRIBE = "subscribe";
    private static final String PERMISSION_TAG_PUBLISH = "publish";
    private static final String PERMISSION_TAG_CLOSE = "close";
    private static final String PERMISSION_TAG_OPEN = "open";
    private final Persistence persistence;
    private final ScheduledExecutorService eventLoop;
    private final Face prefixFace;
    private final Face interestFace;
    private final Name ndnName;
    private final List<Long> ndnPrefixes = new ArrayList<>();
    private final List<Long> ndnFilters = new ArrayList<>();
    private final Window windowState;
    private NdnChannelPublisher publisher;
    private NdnChannelSubscriber subscriber;
    private boolean isChannelOpen = false;
    private boolean isChannelCloseScheduled = false;

    /**
     * Build a channel that understands NDN.
     *
     * @param name the channel name, uniquely identifying the channel
     * @param pipeline the pipeline for serializing/deserializing messages for network transport
     * @param prefixFace the NDN {@link net.named_data.jndn.Face} for receiving interests
     * @param interestFace the NDN {@link net.named_data.jndn.Face} for sending interests
     * @param eventLoop the event loop for scheduling IO
     * @param persistence the time to retain messages
     * @param metadata a list of {@link com.intel.icecp.core.Metadata} objects
     */
    protected NdnNotificationChannel(URI name, Pipeline<Message, InputStream> pipeline, Face prefixFace, Face interestFace, ScheduledExecutorService eventLoop, Persistence persistence, Metadata... metadata) {
        super(name, pipeline);

        if (name == null || !name.getScheme().equals("ndn")) {
            throw new IllegalArgumentException("NDN channels must have non-null URIs with the 'ndn:' scheme.");
        }

        this.ndnName = new Name(name.getSchemeSpecificPart());
        this.prefixFace = prefixFace;
        this.eventLoop = eventLoop;
        this.windowState = new Window();
        this.persistence = persistence;
        this.interestFace = interestFace;
    }

    /**
     * Convenience method to convert a version number into a {@link Name.Component}.
     *
     * @param version the version number to convert
     * @return the encoded {@link Name.Component}
     */
    private static Name.Component asComponent(long version) {
        return Name.Component.fromNumberWithMarker(version, NDN_VERSION_MARKER);
    }

    /**
     * Open the channel for publishing; no need to open the data prefix, the RepositoryServer does that. TODO ensure
     * strategy for these channels is set to broadcast.
     * <p>
     * A channel may be closed and re-opened.  However, if the channel is scheduled to be closed in the future ({@link
     * #close()} was called but there are still messages that have not expired), then the channel cannot be re-opened
     * until the scheduled close has completed.
     *
     * @throws com.intel.icecp.core.misc.ChannelLifetimeException
     */
    @Override
    public CompletableFuture<Void> open() throws ChannelLifetimeException {
        SecurityUtils.checkPermission(new ChannelPermission(getName(), PERMISSION_TAG_OPEN));
        if (isChannelCloseScheduled) {
            throw new ChannelLifetimeException("Failed to open channel, channel is scheduled to be closed: " + this);
        }

        logger.debug("Opening channel: {}", this);

        // register the prefix with some forwarder
        CompletableFuture<Void> opened = new CompletableFuture<>();
        try {
            OnInterestCallback onInterestCallback = (prefix, interest, face, interestFilterId, filter) ->
                    logger.debug("Received interest: {}", interest.toUri());
            OnRegisterFailed onRegisterFailed = prefix -> {
                logger.error("Failed to register channel prefix: {}", prefix);
                opened.completeExceptionally(new ChannelLifetimeException("Failed to open channel: " + prefix.toUri()));

            };
            OnRegisterSuccess onRegisterSuccess = (prefix, registeredPrefixId) -> {
                logger.debug("Registered channel prefix: {}", prefix);
                isChannelOpen = true;
                opened.complete(null);
            };

            long prefixId = getFace().registerPrefix(ndnName, onInterestCallback, onRegisterFailed, onRegisterSuccess);
            ndnPrefixes.add(prefixId);
        } catch (IOException | net.named_data.jndn.security.SecurityException ex) {
            throw new ChannelLifetimeException("Cannot open channel due to registration failure.", ex);
        }

        return opened;
    }

    /**
     * Helper method to add filters to the list
     *
     * @param filter the filter to match incoming requests
     * @param callback the code to run on a matched request
     */
    @Override
    public void addFilter(InterestFilter filter, OnInterestCallback callback) {
        logger.debug("Adding filter: {}, {}", filter.getPrefix().toUri(), filter.getRegexFilter());
        long id = getFace().setInterestFilter(filter, callback);
        ndnFilters.add(id);
    }

    /**
     * Close the channel, removing any registered prefixes and filters from the {@link Face}.
     */
    @Override
    public void close() {
        SecurityUtils.checkPermission(new ChannelPermission(getName(), PERMISSION_TAG_CLOSE));
        logger.debug("Closing channel (may not close immediately if messages are queued): {}", this);

        // wait for retained messages; otherwise they aren't available for
        // requesting subscribers
        if (isPublishing()) {
            long earliestCloseTime = publisher.getEarliestCloseTime();
            // note that we schedule even impossibly long expiration times on
            // the thread pool in order to keep a reference to this channel
            // alive; otherwise, the garbage collector will kill this instance
            // and the published message queue we are holding
            eventLoop.schedule(this::doClose, earliestCloseTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
            isChannelCloseScheduled = true;
        } else {
            doClose();
        }

        isChannelOpen = false;
    }

    /**
     * Helper method for {@link #close()}; clears prefixes and filters from NDN
     */
    private void doClose() {
        for (long id : ndnPrefixes) {
            getFace().removeRegisteredPrefix(id);
        }

        for (long id : ndnFilters) {
            getFace().unsetInterestFilter(id);
        }

        isChannelCloseScheduled = false;

        logger.debug("Closed channel: {}", this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(Message message) throws ChannelIOException {
        SecurityUtils.checkPermission(new ChannelPermission(getName(), PERMISSION_TAG_PUBLISH));
        logger.debug("Publishing message on channel: {}", this);

        if (!isPublishing()) {
            getWindow().latest = 0;
            getWindow().earliest = 0;
        } else {
            getWindow().latest++;
        }

        getPublisher().addMessage(getWindow().latest, message);
        getPublisher().cleanup();
        getWindow().earliest = getPublisher().getEarliestIdAvailable();

        // send out alert, do not expect responses
        sendUpdateNotification(getWindow().latest);
    }

    /**
     * Send the update notification {@link Interest}, e.g. /channel/name/.../update. This {@link Interest} will not not
     * expect a response.
     *
     * @param version the version number of the update
     * @throws ChannelIOException if the request fails
     */
    private void sendUpdateNotification(long version) throws ChannelIOException {
        Name.Component versionComponent = asComponent(version);
        Interest interest = new Interest(getNdnName().append(UPDATE_NOTIFICATION_SUFFIX).append(versionComponent));
        interest.setInterestLifetimeMilliseconds(UPDATE_NOTIFICATION_LIFETIME);
        interest.setMustBeFresh(true);
        try {
            logger.debug("Sending update notification: {}", interest.toUri());
            getInterestFace().expressInterest(interest, null);
        } catch (IOException e) {
            throw new ChannelIOException("Failed to send notification: " + getName(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void subscribe(final OnPublish callback) throws ChannelIOException {
        SecurityUtils.checkPermission(new ChannelPermission(getName(), PERMISSION_TAG_SUBSCRIBE));
        logger.debug("Subscribing on channel: {}", this);

        // listen for update notifications
        addFilter(new InterestFilter(getNdnName().append(UPDATE_NOTIFICATION_SUFFIX)),
                new OnPublishNotification(this, callback, (Throwable t) -> logger.error("Callback failed on channel: {}", this, t)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Message> latest() throws ChannelIOException {
        SecurityUtils.checkPermission(new ChannelPermission(getName(), PERMISSION_TAG_SUBSCRIBE));
        logger.debug("Retrieving latest message on channel: {}", getName());

        return getSubscriber().getLatestMessage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onLatest(OnLatest callback) {
        SecurityUtils.checkPermission(new ChannelPermission(getName(), PERMISSION_TAG_PUBLISH));
        logger.debug("Setting new OnLatest publisher on channel: {}", this);

        getPublisher().setLatest(callback);
    }

    /**
     * {@inheritDoc}
     */
    public CompletableFuture<Message> earliest() throws ChannelIOException {
        SecurityUtils.checkPermission(new ChannelPermission(getName(), PERMISSION_TAG_SUBSCRIBE));
        logger.debug("Retrieving latest message on channel: {}", this);

        return getSubscriber().getEarliestMessage();
    }

    /**
     * Retrieve a message by its specified ID; this is helpful in child classes like NdnStreamChannel that must retrieve
     * one specific message.
     *
     * @param id the {@link Message} id
     * @return the {@link Future} {@link Message}
     */
    public CompletableFuture<Message> get(long id) {
        SecurityUtils.checkPermission(new ChannelPermission(getName(), PERMISSION_TAG_SUBSCRIBE));
        logger.debug("Retrieving message {} on channel: {}", id, this);

        return getSubscriber().getMessage(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpen() {
        return isChannelOpen;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPublishing() {
        return publisher != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSubscribing() {
        return subscriber != null;
    }

    /**
     * @return an instance of the publisher; this will create a new instance if one has not been created changing the
     * state of {@link #isPublishing()}
     */
    private NdnChannelPublisher getPublisher() {
        if (!isPublishing()) {
            publisher = new NdnChannelPublisher(getNdnName(), pipeline, getEventLoop(),
                    getNdnMarkerType(), getPersistence(), this, this);
        }
        return publisher;
    }

    /**
     * @return an instance of the subscriber; this will create a new instance if one has not been created changing the
     * state of {@link #isSubscribing()}
     */
    private NdnChannelSubscriber getSubscriber() {
        if (!isSubscribing()) {
            subscriber = new NdnChannelSubscriber(this);
        }
        return subscriber;
    }

    /**
     * @return the NDN {@link net.named_data.jndn.Name}
     */
    public Name getNdnName() {
        return new Name(ndnName);
    }

    /**
     * @return the NDN marker used for identifying the {@link Message} ID in a request; see
     * http://named-data.net/doc/tech-memos/naming-conventions.pdf for more information.
     */
    public int getNdnMarkerType() {
        return NDN_VERSION_MARKER;
    }

    /**
     * @return the NDN {@link net.named_data.jndn.Face} used for listening to prefixes
     */
    private Face getFace() {
        return prefixFace;
    }

    /**
     * @return the NDN {@link net.named_data.jndn.Face} used for sending Interest packets
     */
    public Face getInterestFace() {
        return interestFace;
    }

    /**
     * @return the event loop scheduler; use this for network IO
     */
    public ScheduledExecutorService getEventLoop() {
        return eventLoop;
    }

    /**
     * @return the {@link Window} metadata for this channel
     */
    public Window getWindow() {
        return windowState;
    }

    /**
     * {@inheritDoc}
     */
    public Persistence getPersistence() {
        return persistence;
    }

    /**
     * @return the formatting pipeline for serializing/deserializing messages
     */
    public Pipeline getFormattingPipeline() {
        return this.pipeline;
    }
}
