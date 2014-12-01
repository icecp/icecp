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
