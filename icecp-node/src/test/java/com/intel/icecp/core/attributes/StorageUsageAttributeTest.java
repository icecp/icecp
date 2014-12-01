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

package com.intel.icecp.core.attributes;

import com.intel.icecp.core.Message;
import com.intel.icecp.core.metadata.Format;
import com.intel.icecp.core.metadata.formats.FormatEncodingException;
import com.intel.icecp.core.metadata.formats.JavaSerializationFormat;
import com.intel.icecp.node.utils.StreamUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Test StorageUsageAttributeTest
 *
 */
public class StorageUsageAttributeTest {
    private static final Logger logger = LogManager.getLogger();
    private StorageUsageAttribute instance;

    @Before
    public void setUp() throws Exception {
        instance = new StorageUsageAttribute();
    }

    @Test
    public void testUsage() throws Exception {
        StorageUsageAttribute.StorageUnits fileStores = instance.value();
        fileStores.forEach((fs, info) -> logger.info("{} reports {}", fs, info));
    }

    @Test
    public void serializationTest() throws FormatEncodingException, IOException {
        StorageUsageAttribute.StorageUnits storageUnits = instance.value();
        AttributeValueWrapper wrapper = new AttributeValueWrapper(storageUnits);
        Format<AttributeValueWrapper> format = new JavaSerializationFormat<>(AttributeValueWrapper.class);

        byte[] bytes = StreamUtils.readAll(format.encode(wrapper));
        logger.info("Serialized storage attribute value: {}", new String(bytes));

        AttributeValueWrapper decoded = format.decode(new ByteArrayInputStream(bytes));
        assertEquals(storageUnits, decoded.storageUnits);
    }
}

class AttributeValueWrapper implements Message {
    final StorageUsageAttribute.StorageUnits storageUnits;

    public AttributeValueWrapper() {
        this.storageUnits = null;
    }

    AttributeValueWrapper(StorageUsageAttribute.StorageUnits storageUnits) {
        this.storageUnits = storageUnits;
    }
}