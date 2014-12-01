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
