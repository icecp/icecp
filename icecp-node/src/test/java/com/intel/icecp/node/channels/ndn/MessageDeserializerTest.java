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

package com.intel.icecp.node.channels.ndn;

import com.intel.icecp.common.TestMessage;
import com.intel.icecp.core.pipeline.Pipeline;
import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;
import org.junit.Test;

import java.io.InputStream;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Test MessageDeserializer
 *
 */
public class MessageDeserializerTest {

    @Test
    public void testApply() throws Exception {
        Pipeline<TestMessage, InputStream> pipeline = mock(Pipeline.class);
        MessageDeserializer<TestMessage> instance = new MessageDeserializer<>(pipeline);
        Data data = new Data(new Name());
        data.setContent(new Blob("{}".getBytes()));

        instance.apply(data);

        verify(pipeline, times(1)).executeInverse(any());
        // TODO test Jackson special annotation cases here, e.g. @JsonTypeInfo(property=...)
    }
}