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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator;
import com.intel.icecp.core.Message;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

/**
 * Describes a {@link Command} using JSON schemas for inputs and outputs; this object is used for discovering what
 * commands are available and how to call them. TODO this class seems necessary but is JsonSchema the right library?
 *
 */
public class CommandDescription implements Message {

    public String name;
    public JsonSchema[] inputs;
    public JsonSchema output;

    /**
     * Default constructor, needed for Jackson
     */
    public CommandDescription() {
    }

    /**
     * @param name the {@link Command} name
     * @param inputs the schemas for the input parameter
     * @param output the schema for the output response
     */
    public CommandDescription(String name, JsonSchema[] inputs, JsonSchema output) {
        this.name = name;
        this.inputs = inputs;
        this.output = output;
    }

    /**
     * Helper method to create a description from method
     *
     * @param name the name of the command
     * @param method the method from the command to describe
     * @return a new description instance
     * @throws IllegalArgumentException if the JSON schemas cannot be created from the inputs/outputs
     */
    public static CommandDescription from(String name, Method method) {
        try {
            ArrayList<JsonSchema> inputs = new ArrayList<>();
            for (Class input : method.getParameterTypes()) {
                inputs.add(toSchema(input));
            }
            JsonSchema output = toSchema(method.getReturnType());
            return new CommandDescription(name, inputs.toArray(new JsonSchema[inputs.size()]), output);
        } catch (JsonMappingException ex) {
            throw new IllegalArgumentException("Failed to create command.", ex);
        }
    }

    /**
     * Helper method to generate a JSON schema from a class type
     *
     * @param type the class type
     * @return a JSON schema
     * @throws JsonMappingException if the generation fails
     */
    public static JsonSchema toSchema(Class type) throws JsonMappingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonSchemaGenerator generator = new JsonSchemaGenerator(mapper);
        return generator.generateSchema(type);
    }

    /**
     * @return the JSON representation of the CommandDescription
     */
    @Override
    public String toString() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.name);
        hash = 67 * hash + Arrays.deepHashCode(this.inputs);
        hash = 67 * hash + Objects.hashCode(this.output);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        final CommandDescription other = (CommandDescription) obj;
        return Objects.equals(this.name, other.name) && Arrays.deepEquals(this.inputs, other.inputs) && Objects.equals(this.output, other.output);

    }
}
