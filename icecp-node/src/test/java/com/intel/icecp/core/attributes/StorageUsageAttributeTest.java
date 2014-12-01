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