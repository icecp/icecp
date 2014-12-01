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
