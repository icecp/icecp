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

package com.intel.icecp.node.channels.ndn.notification;

import com.intel.icecp.core.Message;
import com.intel.icecp.core.misc.OnPublish;
import com.intel.icecp.node.channels.ndn.NdnNotificationChannel;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.encoding.EncodingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;

/**
 * Upon receiving published messages, run the registered callbacks in the thread pool. This class makes an attempt to
 * setup the thread's context class loader correctly and log any exceptions thrown from the callback. TODO measure the
 * amount of time spent in the callback and flag callbacks that are too long.
 *
 */
public class OnPublishNotification implements OnInterestCallback {

    private static final Logger logger = LogManager.getLogger();
    private final OnPublish<Message> onPublish;
    private final OnCallbackFailure onCallbackFailure;
    private final NdnNotificationChannel channel;

    /**
     * @param channel the current NDN channel context; need at least the thread pool
     * @param onPublish the callback to call once the remote message is retrieved
     * @param onCallbackFailure the callback to call if {@link #onPublish} fails
     */
    public OnPublishNotification(NdnNotificationChannel channel, OnPublish<Message> onPublish, OnCallbackFailure onCallbackFailure) {
        this.onPublish = onPublish;
        this.onCallbackFailure = onCallbackFailure;
        this.channel = channel;
    }

    /**
     * Handle incoming notifications from the publisher by extracting the message version and asking for that complete
     * message
     *
     * @param prefix the NDN prefix
     * @param interest the NDN interest
     * @param face the NDN face
     * @param interestFilterId the interest filter ID of the registered prefix
     * @param filter the instance of the interest filter of the registered prefix
     */
    @Override
    public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
        logger.trace("Notification received.");

        final long version;
        try {
            version = interest.getName().get(-1).toVersion();
        } catch (EncodingException e) {
            logger.error("Failed to parse version from notification on channel: " + prefix.toUri(), e);
            return;
        }

        logger.trace("Running OnPublishNotification task: " + interest.toUri());
        CompletableFuture<Message> future = channel.get(version);
        future.thenAcceptAsync(message -> {
            Thread.currentThread().setContextClassLoader(onPublish.getClass().getClassLoader());
            try {
                onPublish.onPublish(message);
            } catch (Throwable t) {
                if (onCallbackFailure != null) {
                    onCallbackFailure.onCallbackFailure(t);
                }
            }
        }, channel.getEventLoop());
    }

    /**
     * Handler for {@link OnPublish} callback failures
     */
    @FunctionalInterface
    public interface OnCallbackFailure {

        void onCallbackFailure(Throwable throwable);
    }
}
