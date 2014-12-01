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
package com.intel.icecp.core.metadata;

import com.intel.icecp.core.Message;
import com.intel.icecp.core.Metadata;
import com.intel.icecp.core.metadata.formats.FormatEncodingException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Define the format and functionality for encoding messages.
 *
 * @param <T> the type of the message to encode
 */
public interface Format<T extends Message> extends Metadata {

    /**
     * Encode the message for transfer.
     *
     * @param message the {@link Message} to encode
     * @return an {@link InputStream} of message bytes
     * @throws FormatEncodingException if encoding fails
     */
    InputStream encode(T message) throws FormatEncodingException;

    /**
     * Decode the message after transfer.
     *
     * @param stream an {@link InputStream} of message bytes
     * @return the decoded {@link Message}
     * @throws FormatEncodingException if decoding fails
     * @throws IOException if the passed stream cannot be read
     */
    T decode(InputStream stream) throws FormatEncodingException, IOException;
}
