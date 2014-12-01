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
package com.intel.icecp.core.misc;

import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.messages.ConfigurationMessage;
import com.intel.icecp.core.mock.MockChannels;
import com.intel.icecp.node.AttributesFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 */
public class ConfigurationAttributeAdapterTest {
    private static final Logger LOGGER = LogManager.getLogger();
    private Attributes attributes;
    private ConfigurationAttributeAdapter instance;

    @Before
    public void setup() throws Exception {
        attributes = AttributesFactory.buildEmptyAttributes(new MockChannels(), URI.create("icecp:/attributes/test"));
        instance = new ConfigurationAttributeAdapter(attributes);
    }

    @Test
    public void testAttributePutGet() throws Exception {
        ConfigurationMessage message = new ConfigurationMessage();
        message.put("bar", "test");
        instance.put("foo", message);
        assertEquals(message, instance.get("foo"));
    }

    @Test
    public void testAttributeGetOrNull() throws Exception {
        ConfigurationMessage message = new ConfigurationMessage();
        message.put("bar", "test");
        instance.put("foo", message);
        assertEquals(message, instance.getOrNull("foo"));
        assertNull(instance.getOrNull("foo1"));
    }

    @Test
    public void testAttributeGetOrDefault() throws Exception {
        ConfigurationMessage message = new ConfigurationMessage();
        message.put("bar", "test");
        instance.put("foo", message);
        assertEquals(message, instance.getOrDefault(null, "foo"));
        assertNull(instance.getOrDefault(null, "foo1"));
        assertEquals(message, instance.getOrDefault(null, "foo", "foo1"));
    }

    @Test
    public void testAttributekeySet() throws Exception {
        assertEquals(0, instance.keySet().size());

        ConfigurationMessage message = new ConfigurationMessage();
        message.put("bar", "test");
        instance.put("foo", message);
        assertEquals(1, instance.keySet().size());
    }

}
