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

package com.intel.icecp.core.mock;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.Metadata;
import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.channels.ChannelProvider;
import com.intel.icecp.core.channels.OnLatest;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.Configuration;
import com.intel.icecp.core.misc.OnPublish;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.core.pipeline.exception.PipelineException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Mock channel provider, opens mock channels
 *
 */
public class MockChannelProvider implements ChannelProvider {

    private final Map<URI, Channel> channels = new HashMap<>();

    @Override
    public String scheme() {
        return "mock";
    }

    @Override
    public void start(ScheduledExecutorService pool, Configuration configuration) {
        // do nothing
    }

    @Override
    public void stop() {
        // do nothing
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Message> Channel<T> build(URI uri, Pipeline pipeline, Persistence persistence, Metadata... metadata) throws ChannelLifetimeException {
        if (!channels.containsKey(uri)) {
            channels.put(uri, new MockChannel(uri, pipeline));
        }
        return channels.get(uri);
    }

    private static class MockChannel implements Channel {
        private static final Logger LOGGER = LogManager.getLogger();
        private final URI uri;
        private final Map<OnPublish, Pipeline> subscribers = new HashMap<>();
        protected Pipeline pipeline;
        private Message latest;
        private boolean open = false;
        private boolean publishing = false;
        private OnLatest onLatest;

        public MockChannel(URI uri, Pipeline pipeline) {
            this.uri = uri;
            this.pipeline = pipeline;
        }

        @Override
        public URI getName() {
            return uri;
        }

        @Override
        public CompletableFuture<Void> open() throws ChannelLifetimeException {
            open = true;
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        @Override
        public void close() throws ChannelLifetimeException {
            open = false;
        }

        @Override
        public void publish(Message message) throws ChannelIOException {
            publish(message, Optional.empty());
        }

        @Override
        public void publish(Message message, Attributes attributes) throws ChannelIOException {
            publish(message, Optional.of(attributes));
        }

        private void publish(Message message, Optional<Attributes> attributes) throws ChannelIOException {
            publishing = true;
            latest = message;
            subscribers.forEach((s, p) -> publishThroughPipeline(message, s, p, attributes));
        }

        private void publishThroughPipeline(final Message message, final OnPublish s, final Pipeline p,
                                            final Optional<Attributes> attributes) {
            try {
                if (p != null) {
                    InputStream serialized = (InputStream) pipeline.execute(message);
                    Message deserialized = (Message) p.executeInverse(serialized);
                    if (attributes.isPresent()) {
                        s.onPublish(deserialized, attributes.get());
                    } else {
                        s.onPublish(deserialized);
                    }
                } else {
                    if (attributes.isPresent()) {
                        s.onPublish(message, attributes.get());
                    } else {
                        s.onPublish(message);
                    }
                }
            } catch (PipelineException ex) {
                LOGGER.error("Failed to serialize/deserialize message", ex);
            }
        }

        @Override
        public boolean isPublishing() {
            return publishing;
        }

        @Override
        public void subscribe(OnPublish callback) throws ChannelIOException {
            subscribers.put(callback, pipeline);
        }

        @Override
        public boolean isSubscribing() {
            return !subscribers.isEmpty();
        }

        @Override
        public CompletableFuture latest() throws ChannelIOException {
            CompletableFuture<Message> future = new CompletableFuture<>();

            if (onLatest != null) {
                OnLatest.Response dynamicResponse = onLatest.onLatest();
                if (dynamicResponse != null) {
                    latest = dynamicResponse.message;
                }
            }

            if (latest == null) {
                future.completeExceptionally(new ChannelIOException("No latest message"));
            } else {
                future.complete(latest);
            }

            return future;
        }

        @Override
        public void onLatest(OnLatest callback) {
            onLatest = callback;
        }
    }
}