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

import com.intel.icecp.common.TestHelper;
import com.intel.icecp.common.TestMessage;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.Module;
import com.intel.icecp.core.attributes.AttributeMessage;
import com.intel.icecp.core.channels.Token;
import com.intel.icecp.core.metadata.Format;
import net.named_data.jndn.Name;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.io.InputStream;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Test {@link JsonFormat}
 *
 */
public class JsonFormatTest extends DefaultFormatTest {

    private static final String JSON_MESSAGE = "{\"a\":\".\",\"b\":1.0,\"c\":1,\"d\":true}";
    private static final String JSON_MESSAGE_WITH_BYTES = "{\"x\":\"123\"}";
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public <T extends Message> Format<T> buildFormat(Class<T> type) {
        return new JsonFormat<>(type);
    }

    @Test
    public void testEncodeDecodeSimilarObjects() throws Exception {
        A a = A.build("123".getBytes());
        Format<A> formatA = buildFormat(A.class);
        InputStream stream = formatA.encode(a);

        // Note: Jackson converts byte[] to base64 encoded data. Client should have custom decoder if
        // de-serializing to basic BytesMessage/Bytes format at the receiver end
        String base64Encoded = new String(TestHelper.readAllBytes(formatA.encode(a)));
        LOGGER.info("Base64 encoded byte array: {}", base64Encoded);
        assertThat(JSON_MESSAGE_WITH_BYTES, is(not(base64Encoded)));

        Format<B> formatB = buildFormat(B.class);
        B b = formatB.decode(stream);
        assertArrayEquals(a.x, b.x);
    }

    public static class A implements Message {
        public byte[] x;

        public static A build(byte[] x) {
            A a = new A();
            a.x = x;
            return a;
        }
    }

    public static class B implements Message {
        public byte[] x;
    }

    @Test
    public void testEncodeOutput() throws Exception {
        TestMessage message = TestMessage.build(".", 1.0, 1, true);
        Format<TestMessage> format = buildFormat(TestMessage.class);
        InputStream stream = format.encode(message);
        String json = new String(TestHelper.readAllBytes(stream));
        assertEquals(JSON_MESSAGE, json);
    }

    // TODO move this test to DefaultFormatTest so that all serialization methods test against tokens
    @Test
    public void testGenericJsonFormat() throws Exception {
        JsonFormat<AttributeMessage<Module.State>> formatter = new JsonFormat<>(new Token<AttributeMessage<Module.State>>() {
        });

        AttributeMessage<Module.State> message = new AttributeMessage<>(Module.State.LOADED);

        InputStream stream1 = formatter.encode(message);
        InputStream stream2 = formatter.encode(message);
        String encoded = new String(TestHelper.readAllBytes(stream1));
        LOGGER.info("Encoded as: " + encoded);

        AttributeMessage<Module.State> decoded = formatter.decode(stream2);
        assertEquals(message.d, decoded.d);
        assertEquals(message.ts, decoded.ts);
    }

    // TODO remove this with NdnNameSerializer
    @Test
    public void testNameJsonFormat() throws Exception {
        AttributeMessage<Name> message = new AttributeMessage<>(new Name("uri"));
        JsonFormat<AttributeMessage<Name>> formatter = new JsonFormat<>(new Token<AttributeMessage<Name>>() {
        });
        DefaultFormatTest.genericFormat(message, formatter);
    }

    @Test
    public void serializeFromOneTypeToAnother() throws Exception {
        Format<From> format1 = buildFormat(From.class);
        From message1 = new From(42, "...".getBytes());
        InputStream encoded = format1.encode(message1);

        Format<To> format2 = buildFormat(To.class);
        To message2 = format2.decode(encoded);

        assertEquals(message1.a, message2.a);
        assertArrayEquals(message2.b, message2.b);
    }

    private static class From implements Message {
        public int a;
        public byte[] b;

        public From() {
            // do nothing
        }

        From(int a, byte[] b) {
            this.a = a;
            this.b = b;
        }
    }

    static class To implements Message {
        public int a;
        public byte[] b;
    }
}
