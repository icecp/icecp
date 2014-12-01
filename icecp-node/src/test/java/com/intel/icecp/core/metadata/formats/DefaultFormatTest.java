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
package com.intel.icecp.core.metadata.formats;

import com.intel.icecp.common.TestMessage;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.attributes.AttributeMessage;
import com.intel.icecp.core.metadata.Format;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;

/**
 */
public abstract class DefaultFormatTest {

    abstract <T extends Message> Format<T> buildFormat(Class<T> type);

    @Test
    public void testEncode() throws Exception {
        Format<TestMessage> format = buildFormat(TestMessage.class);
        format.encode(TestMessage.build("...", 1, 1, true));
    }

    @Test
    public void testDecode() throws Exception {
        Format<TestMessage> format = buildFormat(TestMessage.class);
        TestMessage message = TestMessage.build("...", 1, 1, true);
        InputStream stream = format.encode(message);
        TestMessage decoded = format.decode(stream);

        assertEquals(message.a, decoded.a);
        assertEquals(message.b, decoded.b, 0.0);
        assertEquals(message.c, decoded.c);
        assertEquals(message.d, decoded.d);
    }

    static <T> void genericFormat(AttributeMessage<T> message, Format<AttributeMessage<T>> formatter) throws Exception {
        AttributeMessage<T> decoded = formatter.decode(formatter.encode(message));
        assertEquals(message.d, decoded.d);
        assertEquals(message.ts, decoded.ts);
    }
}
