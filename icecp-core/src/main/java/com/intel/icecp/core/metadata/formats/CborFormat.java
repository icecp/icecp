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
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.channels.Token;
import com.intel.icecp.core.metadata.Format;
import net.named_data.jndn.Name;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CborFormat<T extends Message> implements Format<T> {

    private final ObjectMapper jacksonObjectMapper;
    private final Token<T> type;

    public CborFormat(Class<T> type) {
        this(Token.of(type));
    }

    /**
     * Build a {@link Format} instance that parses JSON into the specified class.  Use this constructor for generic
     * types (e.g. {@literal List<String>}).
     *
     * @param type the type of message to encode/decode
     */
    public CborFormat(Token<T> type) {
        CBORFactory factory = new CBORFactory();
        this.jacksonObjectMapper = new ObjectMapper(factory);
        this.type = type;
        init();
    }

    private void init() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Name.class, new NdnNameSerializer());
        jacksonObjectMapper.registerModule(module);
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
            byte[] bytes = jacksonObjectMapper.writeValueAsBytes(object);
            return new ByteArrayInputStream(bytes);
        } catch (JsonProcessingException e) {
            throw new FormatEncodingException("Failed to encode to CBOR stream", e);
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
            return jacksonObjectMapper.readValue(stream, tra);
        } catch (IOException e) {
            throw new FormatEncodingException("Unable to parse CBOR stream.", e);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }
}
