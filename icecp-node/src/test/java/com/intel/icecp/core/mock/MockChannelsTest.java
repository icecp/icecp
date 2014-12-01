package com.intel.icecp.core.mock;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.node.messages.UndefinedMessage;
import org.junit.Test;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

public class MockChannelsTest {
    @Test
    public void mockChannelsHandleUndefinedMessageTest() throws Exception {
        MockChannels mockChannels = new MockChannels();
        Channel<UndefinedMessage> channel = mockChannels.openChannel(new URI("foo:/bar"), UndefinedMessage.class, Persistence.DEFAULT);
        CountDownLatch latch = new CountDownLatch(1);
        channel.subscribe(message -> latch.countDown());
        channel.publish(new UndefinedMessage());
        assertTrue("message not received before timeout", latch.await(1000, TimeUnit.MILLISECONDS));
    }
}
