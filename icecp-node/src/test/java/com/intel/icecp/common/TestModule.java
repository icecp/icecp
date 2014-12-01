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