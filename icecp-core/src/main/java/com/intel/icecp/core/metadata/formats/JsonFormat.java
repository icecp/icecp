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
