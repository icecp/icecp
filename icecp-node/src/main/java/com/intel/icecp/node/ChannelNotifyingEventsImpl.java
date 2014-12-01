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
package com.intel.icecp.node;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.channels.ChannelProvider;
import com.intel.icecp.core.event.Event;
import com.intel.icecp.core.event.Events;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.metadata.formats.JsonFormat;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.OnPublish;
import com.intel.icecp.core.pipeline.exception.PipelineException;
import com.intel.icecp.node.pipeline.implementations.MessageFormattingPipeline;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implement an event service that publishes received events over a node's
 * channels; this implementation should reuse channels when possible and only
 * provide minimal overhead
 *
 */
public class ChannelNotifyingEventsImpl implements Events {

    private static final Logger logger = LogManager.getLogger();
    private final Map<URI, Channel> channels = new HashMap<>();
    private final URI baseUri;
    private final ChannelProvider channelProvider;

    public ChannelNotifyingEventsImpl(URI baseUri, ChannelProvider channelProvider) {
        this.baseUri = baseUri;
        this.channelProvider = channelProvider;
    }

    @Override
    public void notify(Event event) {
        //logger.info("New event: " + event);
        try {
            Channel channel = getChannel(event.type(), event.getClass());
            channel.publish(event);
        } catch (PipelineException | ChannelIOException | ChannelLifetimeException ex) {
            logger.error("Failed to send event.", ex);
        }
    }

    @Override
    public void listen(OnPublish<Event> callback) {
        logger.info("Listening for any event");
        try {
            Channel channel = getAnyEventChannel();
            channel.subscribe(callback);
        } catch (PipelineException | ChannelIOException | ChannelLifetimeException ex) {
            logger.error("Failed to subscribe to any events.", ex);
        }
    }

    @Override
    public <T extends Event> void listen(URI suffix, Class<T> type, OnPublish<T> callback) {
        logger.info("Listening for event type: " + suffix);
        try {
            Channel channel = getChannel(suffix, type);
            channel.subscribe(callback);
        } catch (PipelineException | ChannelIOException | ChannelLifetimeException ex) {
            logger.error("Failed to subscribe to any events.", ex);
        }
    }

    private Channel getChannel(URI suffix, Class<? extends Event> type) throws ChannelLifetimeException, PipelineException {
        URI uri = suffix;
        if (!suffix.getSchemeSpecificPart().startsWith(baseUri.getSchemeSpecificPart())) {
            uri = concat(baseUri, suffix);
        }

        if (channels.containsKey(uri)) {
            return channels.get(uri);
        } else {
            try {
                Channel channel = channelProvider.build(uri, MessageFormattingPipeline.create(type, new JsonFormat(type)), new Persistence());
                channel.open().get();
                channels.put(uri, channel);
                logger.info("Created new event channel: " + uri);
                return channel;
            } catch (InterruptedException | ExecutionException ex) {
                throw new ChannelLifetimeException("Failed to open channel: " + uri, ex);
            }
        }
    }

    private Channel<AnyEvent> getAnyEventChannel() throws ChannelLifetimeException, PipelineException {
        return getChannel(ANY_EVENT_SUFFIX, AnyEvent.class);
    }

    private static final URI ANY_EVENT_SUFFIX = URI.create("");

    public static class AnyEvent extends Event {

        @Override
        public URI type() {
            return ANY_EVENT_SUFFIX;
        }
    }

    private URI concat(URI base, URI relativeSuffix) {
        return URI.create(base.toString() + "/" + relativeSuffix.toString());
    }
}
