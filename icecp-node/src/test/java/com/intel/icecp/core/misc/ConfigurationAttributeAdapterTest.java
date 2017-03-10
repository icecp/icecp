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
