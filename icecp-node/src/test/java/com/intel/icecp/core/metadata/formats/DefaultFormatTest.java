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
