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
package com.intel.icecp.core.event;

import com.intel.icecp.core.event.types.ReceivedMessageEvent;
import com.intel.icecp.core.event.types.ReceivedMetadataEvent;
import com.intel.icecp.common.TestCounter;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Test EventObservableImpl
 *
 */
public class EventObservableImplTest {

    public EventObservableImplTest() {
        instance = new EventObservableImpl();
        counter = new TestCounter();
    }
    public TestCounter counter;
    public EventObservableImpl instance;

    @Test
    public void testUsage() {
        TestObserver observer1 = new TestObserver();
        instance.register(ReceivedMessageEvent.class, observer1);

        TestObserver observer2 = new TestObserver();
        instance.register(ReceivedMetadataEvent.class, observer2);

        instance.notifyApplicableObservers(new ReceivedMessageEvent(-1, null, -1));
        assertEquals(1, counter.count);

        counter.count = 0;
        instance.notifyApplicableObservers(new ReceivedMetadataEvent(null, null, -1));
        assertEquals(1, counter.count);

        counter.count = 0;
        instance.unregister(ReceivedMessageEvent.class, observer1);
        instance.unregister(ReceivedMetadataEvent.class, observer2);
        instance.notifyApplicableObservers(new ReceivedMetadataEvent(null, null, -1));
        instance.notifyApplicableObservers(new ReceivedMessageEvent(-1, null, -1));
        assertEquals(0, counter.count);
    }

    private class TestObserver<T extends Event> implements EventObserver<T> {

        @Override
        public void notify(T event) {
            counter.count++;
        }
    }

}
