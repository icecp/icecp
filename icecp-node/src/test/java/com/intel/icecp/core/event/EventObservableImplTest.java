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
