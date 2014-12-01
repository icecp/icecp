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
package com.intel.icecp.core.metadata.formats;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.channels.Token;
import com.intel.icecp.core.metadata.Format;
import net.named_data.jndn.Name;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * A Jackson-based serializer/deserializer that may optionally use a JsonSchema
 * (TODO). Should be thread-safe according to
 * http://wiki.fasterxml.com/JacksonFAQThreadSafety
 *
 * @param <T>
 */
public class JsonFormat<T extends Message> implements Format<T> {

    public final String mimeType = "application/json";
    protected final ObjectMapper mapper = new ObjectMapper(); // allow subtypes to configure the mapper
    private final Token<T> type;

    /**
     * Build a {@link Format} instance that parses JSON into the specified
     * class.  Use this constructor for non-generic types (e.g., String).
     *
     */
    public JsonFormat(Class<T> inputClass) {
        this(Token.of(inputClass));
    }

    /**
     * Build a {@link Format} instance that parses JSON into the specified
     * class.  Use this constructor for generic types (e.g. {@literal List<String>}).
     *
     */
    public JsonFormat(Token<T> type) {
        this.type = type;
        init();
    }

    private void init() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Name.class, new NdnNameSerializer());
        mapper.registerModule(module);
    }

    /**
     * {@inheritDoc}
     *
     * @param object a beans-styled {@link Message}
     * @return a stream of encoded bytes
     */
    @Override
    public InputStream encode(T object) throws FormatEncodingException {
        try {
            byte[] bytes = mapper.writeValueAsBytes(object);
            return new ByteArrayInputStream(bytes);
        } catch (JsonProcessingException e) {
            throw new FormatEncodingException("Failed to encode to JSON", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param stream a stream of encoded bytes
     * @return a beans-styled {@link Message}; ensure that the passed {@link #type} has a default constructor
     */
    @Override
    public T decode(InputStream stream) throws FormatEncodingException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            // this is necessary because serialization may happen in a network event handler that does not understand the types of the module
            Thread.currentThread().setContextClassLoader(type.getClass().getClassLoader());
            TypeReferenceAdapter<T> tra = new TypeReferenceAdapter<>(type.type());
            return mapper.readValue(stream, tra);
        } catch (IOException e) {
            throw new FormatEncodingException("Unable to parse JSON stream.", e);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }
}
