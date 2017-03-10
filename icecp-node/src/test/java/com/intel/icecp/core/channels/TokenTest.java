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