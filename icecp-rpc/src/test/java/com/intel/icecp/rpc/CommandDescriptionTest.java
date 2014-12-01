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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.intel.icecp.core.metadata.Format;
import com.intel.icecp.core.metadata.formats.JsonFormat;
import com.intel.icecp.node.utils.StreamUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test command descriptions; TODO may need to validate JSON schema matches, for
 * now we trust Jackson
 *
 */
public class CommandDescriptionTest {

    private static final Logger logger = LogManager.getLogger();

    @Test
    public void testFrom() throws NoSuchMethodException {
        CommandDescription command = new FakeClass().toCommand().toDescription();
        assertEquals("FakeClass.testMethod", command.name);
        assertEquals(1, command.inputs.length);
        assertNotNull(command.output);
        logger.info(command.toString());
    }

    @Test
    public void confirmJsonSchemaSerialization() throws Exception {
        JsonSchema schema = CommandDescription.toSchema(FakeDataStructure.class);

        ObjectMapper mapper = new ObjectMapper();
        byte[] bytes = mapper.writeValueAsBytes(schema);
        String json = new String(bytes);
        logger.info(json);

        JsonSchema decoded = mapper.readValue(json, JsonSchema.class);
        assertEquals(schema, decoded);
    }

    @Test
    public void testSerialization() throws Exception {
        CommandDescription command = new FakeClass().toCommand().toDescription();
        Format<CommandDescription> format = new JsonFormat<>(CommandDescription.class);
        InputStream encode = format.encode(command);
        String json = new String(StreamUtils.readAll(encode));
        logger.info(json);

        CommandDescription decoded = format.decode(new ByteArrayInputStream(json.getBytes()));
        assertEquals(command, decoded);
    }
}
