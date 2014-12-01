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

package com.intel.icecp.core.channels;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intel.icecp.core.metadata.formats.TypeReferenceAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 */
public class TokenTest {
    private static final Logger LOGGER = LogManager.getLogger();
    private Token<Map<String, List<Integer>>> instance;

    @Before
    public void before() {
        instance = new Token<Map<String, List<Integer>>>() {
        };
    }

    @Test
    public void type() throws Exception {
        assertTrue(instance.type().getTypeName().startsWith("java.util.Map<"));
    }

    @Test
    public void fromTree() throws Exception {
        Token composed = Token.fromTree(Map.class, String.class, Token.fromTree(List.class, Integer.class).type());
        assertEquals(instance.type(), composed.type());
    }

    @Test
    public void testTokensInJackson() throws Exception {
        ObjectMapper om = new ObjectMapper();

        Map<String, List<Integer>> message = new HashMap<>();
        message.put("a", Arrays.asList(1, 2, 3));
        String encoded = om.writeValueAsString(message);
        LOGGER.info("Encoded as: " + encoded);

        Token composed = Token.fromTree(Map.class, String.class, Token.fromTree(List.class, Integer.class).type());
        TypeReferenceAdapter adapter = new TypeReferenceAdapter(composed.type());
        Map<String, List<Integer>> decoded = om.readValue(encoded, adapter);

        assertEquals(message.size(), decoded.size());
        assertEquals(message.get("a"), decoded.get("a"));
    }

    @Test
    public void testIsAssignableFrom() {
        assertTrue(new Token<Map<String, String>>() {
        }.isAssignableFrom(HashMap.class));
        assertTrue(new Token<HashMap<String, String>>() {
        }.isAssignableFrom(HashMap.class));
        assertTrue(new Token<Object>() {
        }.isAssignableFrom(HashMap.class));
        assertFalse(new Token<HashMap<String, String>>() {
        }.isAssignableFrom(Map.class));
        assertFalse(new Token<HashMap<String, String>>() {
        }.isAssignableFrom(Object.class));
    }
}