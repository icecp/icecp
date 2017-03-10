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

import com.intel.icecp.core.Message;
import com.intel.icecp.core.metadata.Format;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 *
 */
public class JavaSerializationFormat<T extends Message> implements Format<T> {

    private final Class<T> type;

    /**
     * Build a {@link Format} instance that parses JSON into the specified
     * class.
     *
     * @param type the
     */
    public JavaSerializationFormat(Class<T> type) {
        this.type = type;
    }

    @Override
    public InputStream encode(Message message) throws FormatEncodingException {
        ObjectOutputStream serializer;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            serializer = new ObjectOutputStream(bytes);
            serializer.writeObject(message);
            serializer.flush();
            serializer.close();
            return new ByteArrayInputStream(bytes.toByteArray());
        } catch (IOException ex) {
            throw new FormatEncodingException(ex);
        }
    }

    @Override
    public T decode(InputStream stream) throws FormatEncodingException, IOException {
        try {
            ObjectInputStream deserializer = new ObjectInputStream(stream);
            return (T) deserializer.readObject();
        } catch (ClassNotFoundException ex) {
            throw new FormatEncodingException(ex);
        }
    }
}
