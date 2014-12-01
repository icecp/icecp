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
package com.intel.icecp.common;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Module;
import com.intel.icecp.core.Node;
import com.intel.icecp.core.attributes.WriteableBaseAttribute;
import com.intel.icecp.core.metadata.Frequency;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.Configuration;
import com.intel.icecp.core.modules.ModuleProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;

/**
 * Module for use in unit tests
 *
 */
@ModuleProperty(name = "test-module", attributes = {TestModule.TestAttribute.class})
public class TestModule implements Module {

    private static final Logger logger = LogManager.getLogger();
    private final Frequency frequency = new Frequency(100, 100, 100);
    private final Persistence persistence = new Persistence(10000);

    private int count = 0;
    private volatile boolean stopped = false;
    private Node node;

    @Override
    public void run(Node node, Configuration moduleConfiguration, Channel<State> moduleStateChannel, long moduleId) {
        this.node = node;
        stopped = false;

        try {
            moduleStateChannel.publish(State.RUNNING);
        } catch (ChannelIOException ex) {
            logger.warn("Failed to publish running state for module: " + moduleId);
        }

        Channel<TestMessage> channel = openTestChannel();
        while (!stopped) {
            publishTestPacket(channel);
            try {
                Thread.sleep(frequency.average);
            } catch (InterruptedException ex) {
                logger.info("Interrupted test-module.");
            }
        }
    }

    @Override
    public void stop(StopReason reason) {
        logger.info("Shutting down test-module.");
        stopped = true;
    }

    private Channel<TestMessage> openTestChannel() {
        try {
            URI channelName = URI.create("ndn:" + node.getName() + "/test-module/stream");
            Channel<TestMessage> channel = node.openChannel(channelName, TestMessage.class, persistence, frequency);
            logger.info("Opened channel: " + channelName);
            return channel;
        } catch (ChannelLifetimeException ex) {
            logger.warn("Incorrect URI: ", ex);
            throw new IllegalArgumentException(ex);
        }
    }

    private void publishTestPacket(Channel<TestMessage> channel) {
        try {
            channel.publish(TestMessage.build("Test Message", 1.0, count, stopped));
            logger.info("Published test message (" + count + ") to: " + channel.getName());
            count++;
        } catch (ChannelIOException ex) {
            logger.warn("Failed to publish test message: ", ex);
        }
    }

    public static class TestAttribute extends WriteableBaseAttribute<String> {
        public TestAttribute() {
            super("test", String.class);
        }
    }
}