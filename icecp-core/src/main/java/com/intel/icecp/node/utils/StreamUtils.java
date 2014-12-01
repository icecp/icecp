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
