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

package com.intel.icecp.node.channels.ndn;

import com.intel.icecp.common.TestMessage;
import com.intel.icecp.core.channels.OnLatest;
import com.intel.icecp.core.event.EventObservable;
import com.intel.icecp.core.metadata.formats.JsonFormat;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.node.pipeline.implementations.MessageFormattingPipeline;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test MessageRequestHandler
 *
 */
public class MessageRequestHandlerTest {

    private static final int MESSAGE_SIZE = 10000;
    private static final int MARKER = 42;
    private Name prefix;
    private EventObservable observable;
    private MessageRequestHandler instance;
    private Face face;
    private ArgumentCaptor<Data> dataCaptor;
    private OnLatest onLatest;

    @Before
    public void beforeTest() {
        prefix = new Name("/test/name");
        Data template = new Data(prefix);
        observable = mock(EventObservable.class);
        face = mock(Face.class);
        dataCaptor = ArgumentCaptor.forClass(Data.class);
        Pipeline pipeline = MessageFormattingPipeline.create(TestMessage.class, new JsonFormat<>(TestMessage.class));
        onLatest = mock(OnLatest.class);
        when(onLatest.onLatest()).thenReturn(null);

        MessageCache cache = new MessageCache(Long.MAX_VALUE, 5);
        cache.add(1, TestMessage.buildRandom(MESSAGE_SIZE));
        cache.add(2, TestMessage.buildRandom(MESSAGE_SIZE));
        cache.add(3, TestMessage.buildRandom(MESSAGE_SIZE));

        ExecutorService pool = mock(ExecutorService.class);
        doAnswer(invocation -> {
            ((Runnable) invocation.getArguments()[0]).run();
            return null;
        }).when(pool).submit(any(Runnable.class));

        instance = new MessageRequestHandler(template, cache, MARKER, pipeline, pool, observable);
        instance.setOnLatest(onLatest);
    }

    @Test
    public void testRetrieveEarliest() throws Exception {
        Interest interest = new Interest(prefix);
        interest.setChildSelector(Interest.CHILD_SELECTOR_LEFT);

        instance.onInterest(prefix, interest, face, 0, null);

        verify(observable, times(2)).notifyApplicableObservers(any());
        verify(face, atLeast(2)).putData(dataCaptor.capture());
        assertEquals(1, dataCaptor.getAllValues().get(0).getName().get(-2).toNumberWithMarker(MARKER));
    }

    @Test
    public void testRetrieveLatest() throws Exception {
        Interest interest = new Interest(prefix);
        interest.setChildSelector(Interest.CHILD_SELECTOR_RIGHT);

        instance.onInterest(prefix, interest, face, 0, null);

        verify(observable, times(2)).notifyApplicableObservers(any());
        verify(face, atLeast(2)).putData(dataCaptor.capture());
        assertEquals(3, dataCaptor.getAllValues().get(0).getName().get(-2).toNumberWithMarker(MARKER));
    }

    @Test
    public void testRetrieveLatestWithGeneration() throws Exception {
        Interest interest = new Interest(prefix);
        interest.setChildSelector(Interest.CHILD_SELECTOR_RIGHT);
        when(onLatest.onLatest()).thenReturn(new OnLatest.Response(TestMessage.buildRandom(MESSAGE_SIZE)));

        instance.onInterest(prefix, interest, face, 0, null);

        verify(onLatest, times(1)).onLatest();
        verify(face, atLeast(2)).putData(dataCaptor.capture());
        assertEquals(4, dataCaptor.getAllValues().get(0).getName().get(-2).toNumberWithMarker(MARKER));
    }

    @Test
    public void testRetrieveSpecificId() throws Exception {
        Interest interest = new Interest(prefix);
        interest.getName().append(Name.Component.fromNumberWithMarker(2, MARKER));

        instance.onInterest(prefix, interest, face, 0, null);

        verify(observable, times(2)).notifyApplicableObservers(any());
        verify(face, atLeast(2)).putData(dataCaptor.capture());
        assertEquals(2, dataCaptor.getAllValues().get(0).getName().get(-2).toNumberWithMarker(MARKER));
    }

    @Test
    public void testRetrieveSpecificIdAndSegment() throws Exception {
        Interest interest = new Interest(prefix);
        interest.getName().append(Name.Component.fromNumberWithMarker(3, MARKER));
        interest.getName().appendSegment(1);

        instance.onInterest(prefix, interest, face, 0, null);

        verify(observable, times(2)).notifyApplicableObservers(any());
        verify(face, atLeast(1)).putData(dataCaptor.capture()); // TODO optimize to send only the requested segment
        assertEquals(3, dataCaptor.getAllValues().get(0).getName().get(-2).toNumberWithMarker(MARKER));
    }

    @Test
    public void testErroneousRetrieval() throws Exception {
        Interest interest = new Interest(prefix);
        interest.getName().appendSegment(999);

        instance.onInterest(prefix, interest, face, 0, null);

        verify(observable, times(0)).notifyApplicableObservers(any());
        verify(face, atLeast(0)).putData(any());
    }

    @Test
    public void testMessageNotFound() throws Exception {
        Interest interest = new Interest(prefix);
        interest.getName().append(Name.Component.fromNumberWithMarker(999, MARKER));

        instance.onInterest(prefix, interest, face, 0, null);

        verify(observable, times(1)).notifyApplicableObservers(any());
        verify(face, atLeast(0)).putData(any());
    }

}