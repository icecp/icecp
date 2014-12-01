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

package com.intel.icecp.node.channels.file;

import com.intel.icecp.common.TestCounter;
import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.channels.ChannelProvider;
import com.intel.icecp.core.messages.BytesMessage;
import com.intel.icecp.core.metadata.Format;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.metadata.formats.BytesFormat;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.OnPublish;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.node.pipeline.implementations.MessageFormattingPipeline;
import org.apache.logging.log4j.LogManager;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Test FileChannel with an actual file
 *
 */
public class FileChannelTestIT {

    private Path filePath;
    private Channel<BytesMessage> instance;

    @Before
    public void beforeTest() throws Exception {
        File folder = new File(this.getClass().getResource("/fixtures").getFile());
        filePath = Paths.get(folder.getAbsolutePath(), "document-file-channel.txt");
        if (!Files.exists(filePath)) {
            Files.createFile(filePath);
        }

        instance = newInstance(filePath.toUri().toString(),
                MessageFormattingPipeline.create(BytesMessage.class, new BytesFormat()),
                new Persistence(1000), new BytesFormat());
        instance.open().get();
    }

    public <T extends Message> Channel<T> newInstance(String channelName, Pipeline<T, ?> operation, Persistence persistence, Format format) {
        ChannelProvider builder = new FileChannelProvider();

        try {
            return builder.build(URI.create(channelName), operation, persistence);
        } catch (ChannelLifetimeException ex) {
            throw new IllegalArgumentException("Unable to create channel instance.", ex);
        }
    }

    @Test
    public void testPublishAndRetrieve() throws Exception {
        byte[] bytes = new byte[]{1, 2, 3};
        instance.publish(new BytesMessage(bytes));
        assertArrayEquals(bytes, instance.latest().get().getBytes());
    }

    @Test
    public void testSubscribe() throws Exception {
        final byte[] bytes = new byte[10];
        new Random().nextBytes(bytes);
        final TestCounter counter = new TestCounter();
        instance.subscribe(new OnPublish<BytesMessage>() {
            @Override
            public void onPublish(BytesMessage publishedObject) {
                LogManager.getLogger().info("File retrieved, bytes: " + publishedObject.getBytes().length);
                counter.count++;
                assertArrayEquals(bytes, publishedObject.getBytes());
            }
        });
        assertEquals(0, counter.count);

        Files.write(filePath, bytes);

        // must wait some time for the NIO watch key to pick up the file
        // modification events
        Thread.sleep(100);
        assertEquals(1, counter.count);
    }
}
