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

package com.intel.icecp.core.channels;

import com.intel.icecp.common.TestMessage;
import com.intel.icecp.core.Channel;
import com.intel.icecp.core.metadata.Format;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.metadata.formats.JsonFormat;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.node.pipeline.implementations.MessageFormattingPipeline;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Common test to apply to all channel implementations; to use this test,
 * implement the {@link #newInstance(String, Pipeline, Persistence)} method
 * returning the type of the channel in question.
 *
 */
public abstract class ChannelTest implements ChannelInstanceCreator {

    public static final Logger logger = LogManager.getLogger();
    public static final String CHANNEL_NAME = "/test/channel";
    public static final Format FORMAT = new JsonFormat<>(TestMessage.class);
    private static final int CHANNEL_PERSISTENCE_MS = 1000;
    public static final Persistence PERSISTENCE = new Persistence(CHANNEL_PERSISTENCE_MS);
    public static Pipeline PIPELINE = MessageFormattingPipeline.create(TestMessage.class, FORMAT);
    private final Channel instance;

    public ChannelTest() {
        instance = newInstance(CHANNEL_NAME, PIPELINE, PERSISTENCE);
    }

    @Test
    public void testGetName() {
        assertNotNull(instance.getName());
    }

    @Test
    public void testThatItOpens() throws ChannelLifetimeException {
        instance.open();
    }

    @Test
    public void testThatItCloses() throws ChannelLifetimeException {
        instance.close();
    }

    @Test
    public void testPublish_GenericType() throws Exception {
        instance.publish(TestMessage.buildRandom(50));
        instance.publish(TestMessage.buildRandom(500));
        instance.publish(TestMessage.buildRandom(5000));
        instance.publish(TestMessage.buildRandom(50000));
    }

    @Test(expected = TimeoutException.class)
    public void testGetLatestMethodFailure() throws Exception {
        instance.latest().get(10, TimeUnit.MILLISECONDS);
    }

    @Test(expected = ChannelLifetimeException.class)
    public void testCannotReopenChannelWhenCloseIsScheduled() throws Exception {
        instance.publish(TestMessage.buildRandom(50));
        instance.close();
        instance.open();
        fail("'open' should have thrown a ChannelLifetimeException because the channel is scheduled to close");
    }

    @Test
    public void testCanReopenClosedChannel() throws Exception {
        instance.publish(TestMessage.buildRandom(50));
        instance.close();
        Thread.sleep((long) (1.2 * CHANNEL_PERSISTENCE_MS));  // Wait for message persistence to expire before reopening channel
        instance.open();
    }
}
