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

package com.intel.icecp.node.utils;

import com.intel.icecp.common.TestMessage;
import com.intel.icecp.core.Channel;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.mock.MockChannels;

import org.junit.Test;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 */
public class ChannelUtilsTest {
    @Test
    public void joinRelative() throws Exception {
        assertEquals("scheme:/a/b/c/d/e", ChannelUtils.join(URI.create("scheme:/a/b/c"), "d", "e").toString());
    }

    @Test
    public void joinToRoot() throws Exception {
        assertEquals("scheme:/a/b", ChannelUtils.join(URI.create("scheme:/"), "a", "b").toString());
    }

    @Test
    public void joinComponentWithSeparator() throws Exception {
        assertEquals("scheme:/a/b/c/d", ChannelUtils.join(URI.create("scheme:/a/b"), "c/d").toString());
    }

    @Test
    public void joinRootLevelComponent() throws Exception {
        assertEquals("scheme:/a/b/c", ChannelUtils.join(URI.create("scheme:/a/b"), "/c").toString());
    }

    @Test
    public void getResponseChannelUri() throws Exception {
        TestMessage a = TestMessage.buildRandom(3);
        URI uriA = ChannelUtils.getResponseChannelUri(URI.create("icecp:/base"), a);
        TestMessage b = TestMessage.buildRandom(3);
        URI uriB = ChannelUtils.getResponseChannelUri(URI.create("icecp:/base"), b);

        assertFalse(a.equals(b));
        assertFalse(uriA.equals(uriB));
    }

    @Test
    public void nextMessage() throws Exception {
        MockChannels channels = new MockChannels();
        Channel<TestMessage> channel = channels.openChannel(URI.create("/next/message"), TestMessage.class, Persistence.DEFAULT);

        CompletableFuture<TestMessage> future = ChannelUtils.nextMessage(channel);
        assertNull(future.getNow(null));

        TestMessage nextMessage = TestMessage.buildRandom(10);
        channel.publish(nextMessage);

        assertEquals(nextMessage, future.get());
    }


}