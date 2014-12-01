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