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

import com.intel.icecp.common.TestCounter;
import com.intel.icecp.common.TestMessage;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.event.Event;
import com.intel.icecp.core.event.EventObservable;
import com.intel.icecp.core.event.EventObservableImpl;
import com.intel.icecp.core.event.EventObserver;
import com.intel.icecp.core.event.types.MessageCleanupEvent;
import com.intel.icecp.core.event.types.MessageSentEvent;
import com.intel.icecp.core.event.types.MetadataSentEvent;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.metadata.formats.JsonFormat;
import com.intel.icecp.node.NodeFactory;
import com.intel.icecp.node.channels.ndn.NdnNotificationChannel;
import com.intel.icecp.node.pipeline.implementations.MessageFormattingPipeline;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import org.junit.Test;

import java.util.concurrent.ScheduledExecutorService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test {@link NdnChannelPublisher}
 *
 */
public class NdnChannelPublisherTest {

    private static final int NDN_MARKER = 0x42;
    private static final String NDN_CHANNEL = "/some/channel";
    private static final Filterable EMPTY_FILTERABLE = (filter, callback) -> {/* do nothing */};
    private final NdnChannelPublisher instance;
    private final EventObservable observable;
    private final EventObserver observer;
    private final TestCounter eventCounter;

    public NdnChannelPublisherTest() {
        observable = new EventObservableImpl();
        ScheduledExecutorService eventLoop = NodeFactory.buildEventLoop();

        instance = new NdnChannelPublisher(
                new Name(NDN_CHANNEL),
                MessageFormattingPipeline.create(TestMessage.class, new JsonFormat(TestMessage.class)),
                eventLoop, NDN_MARKER, new Persistence(), observable, EMPTY_FILTERABLE);

        eventCounter = new TestCounter();
        observer = new EventObserver() {
            @Override
            public void notify(Event event) {
                eventCounter.count++;
            }
        };
        observable.register(MessageSentEvent.class, observer);
        observable.register(MetadataSentEvent.class, observer);
        observable.register(MessageCleanupEvent.class, observer);
    }

    @Test
    public void testHasMessage() {
        assertFalse(instance.hasMessage(0));
        instance.addMessage(0, null);
        assertTrue(instance.hasMessage(0));
    }

    @Test
    public void testGetMessage() {
        Message message = TestMessage.build("...", 0, 1, true);
        instance.addMessage(42, message);
        assertNotNull(instance.getMessage(42));
    }

    @Test
    public void testGetEarliestAvailable() {
        instance.addMessage(3, null); // note that this expects the messages to be added in order
        instance.addMessage(5, null);
        instance.addMessage(9, null);
        assertEquals(3, instance.getEarliestIdAvailable());
    }

    @Test
    public void testGetLatestAvailable() {
        instance.addMessage(5, null);
        instance.addMessage(3, null);
        instance.addMessage(9, null); // note that this expects the messages to be added in order
        assertEquals(9, instance.getLatestIdAvailable());
    }

    @Test
    public void testAddMessage() {
        instance.addMessage(1, null);
    }

    @Test
    public void testAddMessageThatPersistsForever() {
        NdnChannelPublisher foreverChannel = new NdnChannelPublisher(
                new Name(NDN_CHANNEL),
                MessageFormattingPipeline.create(TestMessage.class, new JsonFormat(TestMessage.class)),
                null, NDN_MARKER, Persistence.FOREVER,
                new EventObservableImpl(), EMPTY_FILTERABLE);

        foreverChannel.addMessage(1, null);
        assertEquals(Long.MAX_VALUE, foreverChannel.getEarliestCloseTime());
    }

    @Test
    public void testFilters() {
        InterestFilter dataFilter = instance.buildDataFilter(new Name(NDN_CHANNEL));

        assertTrue(dataFilter.doesMatch(new Name(NDN_CHANNEL).append(NdnNotificationChannel.DATA_SUFFIX).append("B%00")));
        assertTrue(dataFilter.doesMatch(new Name(NDN_CHANNEL).append(NdnNotificationChannel.DATA_SUFFIX)));
        assertFalse(dataFilter.doesMatch(new Name(NDN_CHANNEL).append("...").append("...")));
        assertFalse(dataFilter.doesMatch(new Name("/random/name")));
    }
}
