package com.intel.icecp.node;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 */
public class NodeImplTest {

    private NodeImpl instance;

    @Before
    public void before() throws Exception {
        instance = (NodeImpl) NodeFactory.buildMockNode();
    }

    @Test
    public void addCommandsToRegistry() throws Exception {
        assertEquals((instance).getRpcServer().registry().size(), 3);
    }

    @Test
    public void isGlobalUri() throws Exception {
        URI nodeUri = URI.create("ndn:/a/b/c");
        URI newChannelUri1 = URI.create("ndn:/a/b");
        URI newChannelUri2 = URI.create("ndn:/a/b/c/d");
        assertTrue(instance.isGlobalPrefix(nodeUri, newChannelUri1));
        assertFalse(instance.isGlobalPrefix(nodeUri, newChannelUri2));
    }
}
