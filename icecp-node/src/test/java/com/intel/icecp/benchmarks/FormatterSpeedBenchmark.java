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

package com.intel.icecp.benchmarks;

import com.intel.icecp.common.TestHelper;
import com.intel.icecp.common.TestMessage;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.metadata.Format;
import com.intel.icecp.core.metadata.formats.CborFormat;
import com.intel.icecp.core.metadata.formats.FormatEncodingException;
import com.intel.icecp.core.metadata.formats.JavaSerializationFormat;
import com.intel.icecp.core.metadata.formats.JsonFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Benchmark the {@link Format} types against each other. As a side note, we did test another serialization format,
 * Kryo, and it beat the CBOR serialization by around 10-20% on encode times and packet size. We removed it because CBOR
 * was ratified as a standards by some standards board (ask Seb).
 *
 */
public class FormatterSpeedBenchmark {

    private static final Logger logger = LogManager.getLogger();
    private static final int NUM_MESSAGES = 1000;
    private static final int TEXT_SIZE = 20;
    private List<TestMessage> messages;

    @Before
    public void setUp() {
        messages = new ArrayList<>();
        for (int i = 0; i < NUM_MESSAGES; i++) {
            messages.add(TestMessage.buildRandom(TEXT_SIZE));
        }
    }

    @Test
    public void testJsonFormatter() throws FormatEncodingException, IOException {
        Format<TestMessage> format = new JsonFormat<>(TestMessage.class);
        timeEncodeDecode(format, messages);
    }

    @Test
    public void testJavaSerializationFormatter() throws FormatEncodingException, IOException {
        Format<TestMessage> format = new JavaSerializationFormat<>(TestMessage.class);
        timeEncodeDecode(format, messages);
    }

    @Test
    public void testCborFormatter() throws FormatEncodingException, IOException {
        Format<TestMessage> format = new CborFormat<>(TestMessage.class);
        timeEncodeDecode(format, messages);
    }

    private <T extends Message> void timeEncodeDecode(Format<T> format, List<T> messages) throws FormatEncodingException, IOException {
        List<byte[]> encodedMessages = new ArrayList<>();

        long startEncoding = System.nanoTime();
        for (T m : messages) {
            InputStream stream = format.encode(m);
            encodedMessages.add(TestHelper.readAllBytes(stream));
        }
        long endEncoding = System.nanoTime();

        long startDecoding = System.nanoTime();
        for (byte[] encoded : encodedMessages) {
            ByteArrayInputStream stream = new ByteArrayInputStream(encoded);
            format.decode(stream);
        }
        long endDecoding = System.nanoTime();

        logger.info(String.format("Encoded and decoded %d packets with %s: ", messages.size(), format.getClass().getSimpleName()));
        logger.info("\tAverage message size (bytes): " + averageSize(encodedMessages));
        logger.info("\tAverage message encoding time (ms): " + (endEncoding - startEncoding) / 1000000);
        logger.info("\tAverage message decoding time (ms): " + (endDecoding - startDecoding) / 1000000);
    }

    private double averageSize(List<byte[]> list) {
        long total = 0;
        for (byte[] item : list) {
            total += item.length;
        }
        return total / (double) list.size();
    }
}
