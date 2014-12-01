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
package com.intel.icecp.node;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.messages.ConfigurationMessage;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.metadata.formats.JsonFormat;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.PropertyNotFoundException;
import com.intel.icecp.node.channels.file.FileChannelProvider;
import com.intel.icecp.node.pipeline.implementations.MessageFormattingPipeline;
import java.net.URI;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Test the Configuration implementation.
 *
 */
public class ConfigurationImplTest {

    protected static final String JSON_CONFIGURATION_RESOURCE = "/sample-configuration.json";
    public ConfigurationImpl configuration;

    @Before
    public void beforeTest() throws Exception {
        configuration = new ConfigurationImpl(newInstance());
        configuration.load();
    }

    private Channel<ConfigurationMessage> newInstance() throws Exception {
        URI file = this.getClass().getResource(JSON_CONFIGURATION_RESOURCE).toURI();
        FileChannelProvider builder = new FileChannelProvider();
        Channel<ConfigurationMessage> channel = builder.build(file,
                MessageFormattingPipeline.create(ConfigurationMessage.class, new JsonFormat(ConfigurationMessage.class)),
                new Persistence());
        channel.open().get();
        return channel;
    }

    @Test
    public void testGet() throws PropertyNotFoundException, ChannelIOException {
        assertEquals("/test/node", configuration.get("name"));
        assertEquals("localhost", configuration.get("forwarder.uri"));
    }

    @Test
    public void testGetOrNull() throws ChannelIOException {
        assertEquals(null, (String) configuration.getOrNull("non-existent"));
    }

    @Test
    public void testGetOrDefault() throws ChannelIOException {
        assertEquals("123", configuration.getOrDefault("123", "non-existent", "non-existent2"));
    }

    @Test
    public void testPut() {
        configuration.put("some.test.string", "...");
        assertEquals("...", configuration.getOrNull("some.test.string"));
        configuration.put("some.test.number", 1024);
        assertEquals(1024, (int) configuration.getOrNull("some.test.number"));
    }

    @Test
    public void testSaveAndReload() throws Exception {
        configuration.put("name", "/change/me");
        configuration.save();
        configuration.load();
        assertEquals("/change/me", configuration.get("name"));
        configuration.put("name", "/test/node");
        configuration.save();
        assertEquals("/test/node", configuration.get("name"));
    }

    @Test
    public void testBracketSubstitution() {
        assertEquals("a.b.1", configuration.replaceArrayBrackets("a.b[1]"));
        assertEquals("a.b.1.2.c", configuration.replaceArrayBrackets("a.b[1][2].c"));
    }

    @Test
    public void testArrayNotation() throws Exception {
        assertEquals("a", configuration.get("list.0"));
        assertEquals("c", configuration.get("list[2]"));
    }

    @Test
    public void testKeySet() {
        int startSize = configuration.keySet().size();
        configuration.put("a", "A");
        assertEquals(startSize + 1, configuration.keySet().size());
        assertTrue(configuration.keySet().contains("a"));

        configuration.put("b", "B");
        assertEquals(startSize + 2, configuration.keySet().size());
        assertTrue(configuration.keySet().contains("b"));
    }

    @Test
    public void testKeySetIndependent() throws PropertyNotFoundException {
        int startSize = configuration.keySet().size();
        configuration.put("a", "A");
        configuration.put("b", "B");

        // configuration contains "a" and "b"
        assertEquals("A", configuration.get("a"));
        assertEquals("B", configuration.get("b"));

        // get keyset and modify local copy
        configuration.keySet().clear();

        // configuration still contains "a" and "b"
        assertEquals("A", configuration.get("a"));
        assertEquals("B", configuration.get("b"));

        // fetch snapshot of key set
        Set<String> keySet = configuration.keySet();
        assertEquals(startSize + 2, keySet.size());

        // add configuration "c"
        configuration.put("c", "C");

        // ensure snapshot is unchanged
        assertEquals(startSize + 2, keySet.size());
    }
}
