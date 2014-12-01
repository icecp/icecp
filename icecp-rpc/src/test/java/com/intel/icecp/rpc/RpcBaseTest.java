package com.intel.icecp.rpc;

import com.intel.icecp.core.mock.MockChannels;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;

/**
 * Test RpcBase
 *
 */
public class RpcBaseTest {
    private CommandRegistry instance;
    private MockChannels channels;

    @Before
    public void setUp() {
        instance = new CommandRegistry();
        channels = new MockChannels();
    }

    @Test
    public void createCorrectResponseUriForAbsolutePath() throws Exception {
        URI userResponseUri = URI.create("icecp:/intel/node/1/module/1/$ret/testResponseChannel");

        RpcBase rpc = new RpcBase(channels, URI.create("icecp:/intel/node/1/module/1"));

        URI responseUri = rpc.createResponseUri(userResponseUri);

        assertEquals(userResponseUri, responseUri);
    }

    @Test
    public void createCorrectResponseUriForRelativePath() throws Exception {
        URI userResponseUri = URI.create("testResponseChannel");
        URI expectedResponseUri = URI.create("icecp:/intel/node/1/module/1/$ret/testResponseChannel");

        RpcBase rpc = new RpcBase(channels, URI.create("icecp:/intel/node/1/module/1"));

        URI responseUri = rpc.createResponseUri(userResponseUri);

        assertEquals(expectedResponseUri, responseUri);
    }
}
