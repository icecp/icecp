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
