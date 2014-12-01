package com.intel.icecp.node;

import com.intel.icecp.common.TestMessage;
import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Metadata;
import com.intel.icecp.core.channels.Token;
import com.intel.icecp.core.management.ConfigurationManager;
import com.intel.icecp.core.messages.BytesMessage;
import com.intel.icecp.core.metadata.Format;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.Configuration;
import com.intel.icecp.core.mock.MockChannelProvider;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.concurrent.ScheduledExecutorService;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 */
public class ChannelsImplTest {

    private ChannelsImpl instance;

    @Before
    public void beforeTest() {
        ScheduledExecutorService pool = mock(ScheduledExecutorService.class);
        ConfigurationManager configurationManager = mock(ConfigurationManager.class);
        Configuration configuration = mock(Configuration.class);
        when(configurationManager.get(any())).thenReturn(configuration);
        instance = new ChannelsImpl(pool, configurationManager);
    }

    @Test
    public void basicUsage() throws Exception {
        String SCHEME = "icecp";
        register(SCHEME);
        Channel<TestMessage> channel = openChannel("icecp:/channels/impl/test");

        instance.unregister(SCHEME);
        assertFalse(channel.isOpen());
        assertNull(instance.get(SCHEME));
    }

    @Test
    public void shutdown() throws Exception {
        register("http");
        openChannel("http://a/b/c");
        openChannel("http://d/e/f");
        assertEquals(2, instance.getOpenChannels().length);

        instance.shutdown();
        assertEquals(0, instance.getOpenChannels().length);
    }

    @Test
    public void chooseBytesFormatWhenNecessary() throws Exception {
        @SuppressWarnings("unchecked")
        Format<BytesMessage> format = instance.chooseFormat(Token.of(BytesMessage.class), new Metadata[]{});
        BytesMessage decoded = format.decode(new ByteArrayInputStream("abdcd".getBytes()));
        assertEquals(BytesMessage.class, decoded.getClass());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullScheme(){
        instance.get(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyScheme(){
        instance.get("");
    }

    private Channel<TestMessage> openChannel(String uri) throws com.intel.icecp.core.misc.ChannelLifetimeException {
        Channel<TestMessage> channel = instance.openChannel(URI.create(uri), TestMessage.class, Persistence.DEFAULT);
        assertTrue(channel.isOpen());
        return channel;
    }

    private void register(String scheme) {
        instance.register(scheme, new MockChannelProvider());
        assertNotNull(instance.get(scheme));
    }
}