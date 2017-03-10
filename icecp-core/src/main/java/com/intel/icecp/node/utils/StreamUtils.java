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

package com.intel.icecp.node.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Collects common functionality for reading bytes from streams
 *
 */
public class StreamUtils {

    public static final int DEFAULT_BUFFER_SIZE = 4096;

    private StreamUtils() {
        // do not allow instances of this class
    }

    /**
     * Read all of the bytes in an input stream.
     *
     * @param bytes the {@link InputStream} of bytes to read
     * @return an array of all bytes retrieved from the stream
     * @throws IOException if the stream fails
     */
    public static byte[] readAll(InputStream bytes) throws IOException {
        return readAll(bytes, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Read all of the bytes in an input stream. Note that this will close the passed stream after all bytes have been
     * read.
     *
     * @param bytes the {@link InputStream} of bytes to read
     * @param bufferSize the number of bytes to buffer at each read
     * @return an array of all bytes retrieved from the stream
     * @throws IOException if the stream fails
     */
    private static byte[] readAll(InputStream bytes, int bufferSize) throws IOException {
        try {
            return readAllAndKeepOpen(bytes, bufferSize);
        } finally {
            bytes.close();
        }
    }

    /**
     * Read all of the bytes in an input stream. Note that this will close the passed stream after all bytes have been
     * read.
     *
     * @param bytes the {@link InputStream} of bytes to read
     * @param bufferSize the number of bytes to buffer at each read
     * @return an array of all bytes retrieved from the stream
     * @throws IOException if the stream fails
     */
    public static byte[] readAllAndKeepOpen(InputStream bytes, int bufferSize) throws IOException {
        try (ByteArrayOutputStream builder = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[bufferSize];
            int read;
            while ((read = bytes.read(buffer)) != -1) {
                builder.write(buffer, 0, read);
            }
            return builder.toByteArray();
        }
    }
}
