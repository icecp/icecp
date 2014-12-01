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

import com.intel.icecp.core.Message;
import com.intel.icecp.core.messages.BytesMessage;
import com.intel.icecp.core.metadata.Format;
import com.intel.icecp.common.TestMessage;
import java.io.InputStream;
import java.util.Random;
import static org.junit.Assert.assertArrayEquals;
import org.junit.Test;

/**
 * Test {@link BytesFormat}
 *
 */
public class BytesFormatTest extends DefaultFormatTest {

    @SuppressWarnings("unchecked")
    @Override
    <T extends Message> Format<T> buildFormat(Class<T> type) {
        return (Format<T>) new BytesFormat();
    }

    @Test(expected = ClassCastException.class)
    public void testIllegalArguments() throws FormatEncodingException {
        Format<TestMessage> format = buildFormat(TestMessage.class);
        format.encode(TestMessage.build("...", 1, 1, true));
    }

    @Test
    @Override
    public void testEncode() throws Exception {
        Format<Message> format = buildFormat(null);
        Message message = new BytesMessage(new byte[]{0, 1, 2, 3});
        format.encode(message);
    }

    @Test
    @Override
    public void testDecode() throws Exception {
        Format<Message> format = buildFormat(null);
        byte[] bytes = new byte[20];
        new Random().nextBytes(bytes);
        Message message = new BytesMessage(bytes);

        InputStream stream = format.encode(message);
        BytesMessage decoded = (BytesMessage) format.decode(stream);

        assertArrayEquals(bytes, decoded.getBytes());
    }
}