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

import com.intel.icecp.common.TestCounter;
import com.intel.icecp.common.TestHelper;
import com.intel.icecp.core.Channel;
import com.intel.icecp.core.event.Event;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.metadata.formats.JsonFormat;
import com.intel.icecp.node.channels.ndn.NdnChannelProvider;
import com.intel.icecp.node.pipeline.implementations.MessageFormattingPipeline;
import org.junit.Test;

import java.net.URI;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.Assert.assertEquals;

/**
 * Test ChannelNotifyingEventsImpl
 *
 */
public class ChannelNotifyingEventsImplTestIT {

    @Test
    public void testNotify() throws Exception {
        ScheduledExecutorService eventLoop = NodeFactory.buildEventLoop();
        NdnChannelProvider ndn = new NdnChannelProvider("/test/identity", TestHelper.getNfdHostName(), eventLoop);
        ChannelNotifyingEventsImpl events = new ChannelNotifyingEventsImpl(URI.create("ndn:/test/events"), ndn);

        TestCounter counter = new TestCounter();
        final String message = "...";
        SomeType sentEvent = new SomeType();
        sentEvent.message = message;

        Channel<SomeType> subscriber = ndn.build(URI.create("ndn:/test/events/some-type"),
                MessageFormattingPipeline.create(SomeType.class, new JsonFormat(SomeType.class)),
                new Persistence());
        subscriber.subscribe((SomeType receivedEvent) -> {
            counter.count++;
            assertEquals(message, receivedEvent.message);
        });

        events.notify(sentEvent);

        counter.waitAtMost(1, 4000);
        assertEquals(1, counter.count);
    }

    public static class SomeType extends Event {

        public static final URI TYPE = URI.create("some-type");

        public String message;

        @Override
        public URI type() {
            return TYPE;
        }

    }
}
