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
