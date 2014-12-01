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
package com.intel.icecp.node.channels.ndn.notification;

import com.intel.icecp.core.Message;
import com.intel.icecp.core.event.types.MessageRequestedEvent;
import com.intel.icecp.core.event.types.ReceivedMessageEvent;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.node.channels.ndn.MessageDeserializer;
import com.intel.icecp.node.channels.ndn.NdnNotificationChannel;
import com.intel.jndn.utils.Client;
import com.intel.jndn.utils.client.impl.AdvancedClient;
import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;

/**
 * Manage {@link Message} retrieval on an {@link NdnNotificationChannel}.
 *
 */
public class NdnChannelSubscriber {

    private static final Logger LOGGER = LogManager.getLogger();
    private final NdnNotificationChannel channel;
    private final Client client;

    /**
     * Creates a new instance of <code>NdnChannelPublisher</code>
     *
     * @param channel the channel this subscriber will request for
     */
    public NdnChannelSubscriber(NdnNotificationChannel channel) {
        this.channel = channel;
        this.client = AdvancedClient.getDefault();
    }

    /**
     * @param id the unique identifier for a {@link Message}
     * @return a future {@link Message} from the network
     */
    public CompletableFuture<Message> getMessage(long id) {
        channel.notifyApplicableObservers(new MessageRequestedEvent(id));

        Name.Component marker = Name.Component.fromNumberWithMarker(id, channel.getNdnMarkerType());
        Interest interest = new Interest(new Name(channel.getNdnName()).append(NdnNotificationChannel.DATA_SUFFIX).append(marker));
        interest.setMustBeFresh(true);

        // set interest lifetime
        if (channel.getPersistence().hasRetrievalLifetime()) {
            interest.setInterestLifetimeMilliseconds(channel.getPersistence().retrieveUnder);
        }

        // send out interest packets
        LOGGER.debug(String.format("Requesting message %d: %s", id, interest.toUri()));
        CompletableFuture<Data> futurePacket = client.getAsync(channel.getInterestFace(), interest);
        return futurePacket.thenApply(new MessageDeserializer(channel.getFormattingPipeline()));
    }

    /**
     * @return the latest {@link Message} available on the network
     * @throws ChannelIOException
     */
    public CompletableFuture<Message> getLatestMessage() throws ChannelIOException {
        long latestIdHolder = -1; // TODO need new event type or stop requiring ID
        channel.notifyApplicableObservers(new MessageRequestedEvent(latestIdHolder));

        Interest interest = new Interest(new Name(channel.getNdnName()).append(NdnNotificationChannel.DATA_SUFFIX));
        interest.setChildSelector(Interest.CHILD_SELECTOR_RIGHT);
        interest.setMustBeFresh(true);

        // set interest lifetime
        if (channel.getPersistence().hasRetrievalLifetime()) {
            interest.setInterestLifetimeMilliseconds(channel.getPersistence().retrieveUnder);
        }

        // send out interest packets
        LOGGER.debug(String.format("Requesting latest message: %s", interest.toUri()));
        CompletableFuture<Data> futurePacket = client.getAsync(channel.getInterestFace(), interest);
        return futurePacket.thenApply(new MessageDeserializer(channel.getFormattingPipeline()));
    }

    /**
     * @return the earliest {@link Message} available on the network
     * @throws ChannelIOException
     */
    public CompletableFuture<Message> getEarliestMessage() throws ChannelIOException {
        long latestIdHolder = -1; // TODO need new event type or stop requiring ID
        channel.notifyApplicableObservers(new MessageRequestedEvent(latestIdHolder));

        Interest interest = new Interest(new Name(channel.getNdnName()).append(NdnNotificationChannel.DATA_SUFFIX));
        interest.setChildSelector(Interest.CHILD_SELECTOR_LEFT);
        interest.setMustBeFresh(true);

        // set interest lifetime
        if (channel.getPersistence().hasRetrievalLifetime()) {
            interest.setInterestLifetimeMilliseconds(channel.getPersistence().retrieveUnder);
        }

        // send out interest packets
        LOGGER.debug("Requesting earliest message: {}", interest.toUri());
        CompletableFuture<Data> futurePacket = client.getAsync(channel.getInterestFace(), interest);
        return futurePacket.thenApply(new MessageDeserializer(channel.getFormattingPipeline()));
    }

    /**
     * Handler for receiving messages; records latest marker available and fires off the listeners.
     */
    public class MessageHandler implements OnMessageReceived {

        @Override
        public void onMessageReceived(Data data, Message message) throws Exception {

            Name.Component marker = data.getName().get(-1);
            long id;
            try {
                id = marker.toNumberWithMarker(channel.getNdnMarkerType());
            } catch (EncodingException ex) {
                String errorMessage = String.format("Received message with incorrect message type; was 0x%x but should be 0x%x", marker.getValue().buf().get(0), channel.getNdnMarkerType());
                throw new ChannelIOException(errorMessage, ex);
            }

            channel.notifyApplicableObservers(new ReceivedMessageEvent(id, message, data.getContent().size()));

            if (id > channel.getWindow().latest) {
                channel.getWindow().latest = id;
            }

            if (channel.getWindow().earliest == -1) {
                channel.getWindow().earliest = id;
            }
        }
    }
}
