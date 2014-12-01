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
package com.intel.icecp.core;

import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.channels.OnLatest;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.OnPublish;
import com.intel.icecp.core.permissions.ChannelPermission;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * Represent the transport for transmitting messages; producers and consumers use this to publish and subscribe to
 * messages. Channels are uniquely identified by URIs (see {@link #getName()}; the scheme of this uri (e.g. 'ndn',
 * 'mqtt') should have a one-to-one mapping with the channel classes so that channel types can be provided with SPI.
 * Multiple instances of a channel with the same URI may be present in memory at any one time; this is because channel
 * instances are endpoints for the network communication (in other words, multiple local channel instances still use the
 * network stack).
 * <p>
 * Note that there are several requirements for using and implementing channels. First, channels should be opened before
 * use and closed after use (use try-with-resources). Second, a channel is required to publish its own messages to any
 * subscribers locally attached through the {@link #subscribe(OnPublish)} command. Third, publishers are required to
 * maintain any published messages until they expire (see {@link com.intel.icecp.core.metadata.Persistence}); some upper
 * bound may be implemented to limit memory usage but this requirement is important to handle cases where network
 * caching may fail.
 * <p>
 * One novelty of this API in comparison with other publish/subscribe implementations is the inclusion of {@link
 * #latest()}. This provides users with limited historical access to the channel messages; in other words, a channel can
 * be opened and immediately retrieve the latest message on the channel without subscribing and waiting for the next
 * published message. Channel messages expire, however, so it is possible that no message is available when {@link
 * #latest()} is called (see {@link com.intel.icecp.core.metadata.Persistence})
 * <p>
 * The addition of latest message retrieval adds the ability for on-demand message generation. A network "latest"
 * request can be trapped and a message generated with the callback passed to {@link #onLatest(OnLatest)}.
 * <p>
 * The rationale for using generics was to rapidly point out coding errors due to the built-in serialization and
 * deserialization. The channel uses a pipeline for this and the class type that is used could be hidden away in the
 * pipeline; the generics should ensure that users use a matching message type.
 * <p>
 * A channel's lifecycle will look like:
 * <pre>{@code
 * Channel<SomeMessage> channel = new SomeChannelImplementation(uri, new DefaultPersistence(), new BytesFormat());
 * channel.open();
 * channel.publish(new SomeMessage()); // or subscribe
 * channel.close();
 * }</pre>
 *
 * @param <T> the {@link Message} type to use on this channel
 */
public interface Channel<T extends Message> extends AutoCloseable {

    /**
     * @return the channel's unique name. Note that the namespace used for this name will impact the network
     * performance; e.g. in NDN this must a routable prefix and the name length will (e.g. "ndn:/name/of/channel", where
     * routers have prefixes like "/name" or "/name/of"). TODO change this to name() for consistency
     */
    URI getName();

    /**
     * Perform required open procedures. For example, in NDN this may involve registering prefixes and event handlers.
     * Note that the corresponding close() method is inherited from {@link AutoCloseable}.
     * <p>
     * This method must be protected using the {@link ChannelPermission}'s {@code open} action.
     *
     * @return a future that completes once the channel is open and ready for use
     * @throws ChannelLifetimeException if the channel fails to open
     */
    CompletableFuture<Void> open() throws ChannelLifetimeException;

    /**
     * @return true if the {@link Channel} has been opened, false if the channel has not yet been opened or has been
     * closed
     */
    boolean isOpen();

    /**
     * Perform required close procedures. This operation may not completely Note that this inherits from from {@link
     * AutoCloseable} so channels may be used in try-with-resources blocks. TODO this should not throw an exception to
     * avoid an extra catch when using try-with-resources; TODO if it doesn't throw, it can be removed
     * <p>
     * This method must be protected using the {@link ChannelPermission}'s {@code close} action.
     *
     * @throws ChannelLifetimeException if the channel fails to close
     */
    @Override
    void close() throws ChannelLifetimeException;

    /**
     * Publish a message on this channel. Messages must be available on the network for the persistence period specified
     * during channel construction. Note that this operation may not "send" any network traffic; in pull-based paradigms
     * such as NDN the subscribers may be responsible for requesting published messages.
     * <p>
     * This method must be protected using the {@link ChannelPermission}'s {@code publish} action.
     *
     * @param message the {@link Message} to publish on this channel
     * @throws ChannelIOException when the channel fails to publish {@link Message}s
     */
    void publish(T message) throws ChannelIOException;

    /**
     * Publish a message on this channel with the given attributes. Note that the channel is free to disregard any
     * passed attributes that it does not implement (e.g. if a channel has an associated persistence and the user passes
     * a different one in this message, some implementations, like MQTT with QoS, may not be able to modify the
     * time-to-live at message granularity and would disregard the attribute). See {@link #publish(Message)} for more
     * details. The default implementation is provided here for channel implementations that do not yet implement
     * this method.
     *
     * @param message the {@link Message} to publish on this channel
     * @param attributes the set of attributes to apply to this message
     * @throws ChannelIOException if the message cannot be published
     */
    default void publish(T message, Attributes attributes) throws ChannelIOException {
        publish(message);
    }

    /**
     * @return true when the {@link Channel} has published at least one message; false otherwise
     */
    boolean isPublishing();

    /**
     * Subscribe to messages on this channel. Channel implementations should allow multiple calls to this method that
     * return published messages to all registered callbacks. Users should quickly exit the callback to avoid taxing the
     * thread pool.
     * <p>
     * This method must be protected using the {@link ChannelPermission}'s {@code subscribe} action.
     *
     * @param callback the {@link OnPublish} code to run when a message is received
     * @throws ChannelIOException when the channel subscribe operation fails
     */
    void subscribe(OnPublish<T> callback) throws ChannelIOException;

    /**
     * @return true when the {@link Channel} is successfully subscribed to receive messages; false otherwise
     */
    boolean isSubscribing();

    /**
     * Retrieve the latest message available on this channel. Note that channel messages expire so no message may be
     * available when the user calls this method.
     * <p>
     * This method must be protected using the {@link ChannelPermission}'s {@code subscribe} action; this uses the same
     * action as {@link #subscribe(OnPublish)} since they both "retrieve messages"
     *
     * @return the latest {@link com.intel.icecp.core.Message} published on this channel
     * @throws ChannelIOException when the channel fails to retrieve the latest {@link Message}
     */
    CompletableFuture<T> latest() throws ChannelIOException;

    /**
     * Register a callback to generate the latest message. Calling this method again should replace the previous
     * callback. When this has been set and the latest message is requested on a channel, the callback will be called to
     * generate a message to return to the requestor. If the callback returns a message, this generated message should
     * be added to the cache of published messages as if it were published; however, no additional network communication
     * should result beyond returning the generated message to the requestor (TODO or do we want to publish it?). If the
     * callback returns null, processing continues as usual (e.g. checking the published message cache for the latest
     * message to return to the requestor.
     * <p>
     * Note that the latest message requestor has sent a network request and is waiting for a response so the callback
     * should avoid time-consuming computations that will slow down the network communication
     * <p>
     * This method must be protected using the {@link ChannelPermission}'s {@code publish} action; this uses the same
     * action as {@link #publish(Message)} since they both "send messages"
     *
     * @param callback the callback to call
     */
    void onLatest(OnLatest<T> callback);
}
