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
package com.intel.icecp.rpc;

import com.intel.icecp.core.metadata.Format;
import com.intel.icecp.core.metadata.formats.FormatEncodingException;
import com.intel.icecp.core.metadata.formats.JsonFormat;
import com.intel.icecp.node.utils.StreamUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

/**
 * Test that CommandFormat will deserialize polymorphic types correctly
 *
 */
public class CommandFormatTest {

    private static final Logger logger = LogManager.getLogger();

    @Test
    public void testSerializationWithPrimitiveTypes() throws FormatEncodingException, IOException {
        CommandRequest request = CommandRequest.fromWithoutResponse("Node.someMethod", "ndn:/test", 1, 2.0, true);
        assertEquals(4, request.inputs.length);

        // use standard format to verify that it can deserialize primitives correctly
        Format<CommandRequest> format = new JsonFormat<>(CommandRequest.class);
        InputStream stream = format.encode(request);
        String json = new String(StreamUtils.readAll(stream));
        logger.info(json);

        CommandRequest decoded = format.decode(new ByteArrayInputStream(json.getBytes()));
        assertEquals(request, decoded);
    }

    @Test
    public void testSerializationWithPolymorphicTypes() throws FormatEncodingException, IOException {
        CommandRequest request = CommandRequest.from("Node.someMethod", URI.create("ndn:/test"), new CustomType());

        // use extension format, CommandFormat, to verify it can deal with polymorphic types
        Format<CommandRequest> format = new CommandFormat();
        InputStream stream = format.encode(request);
        String json = new String(StreamUtils.readAll(stream));
        logger.info(json);

        CommandRequest decoded = format.decode(new ByteArrayInputStream(json.getBytes()));
        assertEquals(request, decoded);
    }

    /**
     * Note: if this class isn't static Jackson won't find it
     */
    public static class CustomType {

        public final int b = 1;
        public final CustomSubType a = new CustomSubType();

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 61 * hash + this.b;
            hash = 61 * hash + Objects.hashCode(this.a);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CustomType other = (CustomType) obj;
            return this.b == other.b && Objects.equals(this.a, other.a);
        }
    }

    public static class CustomSubType {

        public final double a = 1.0;

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 89 * hash + (int) (Double.doubleToLongBits(this.a) ^ (Double.doubleToLongBits(this.a) >>> 32));
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CustomSubType other = (CustomSubType) obj;
            return Double.doubleToLongBits(this.a) == Double.doubleToLongBits(other.a);
        }

    }
}
