package com.intel.icecp.module.example;

import com.intel.icecp.core.Module;
import com.intel.icecp.core.Node;
import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.attributes.IdAttribute;
import com.intel.icecp.node.AttributesFactory;
import com.intel.icecp.node.NodeFactory;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 */
public class ExampleModuleTest {
    @Test
    public void basicUsage() throws Exception {
        ExampleModule module = new ExampleModule();
        Node node = NodeFactory.buildMockNode();
        Attributes attributes = AttributesFactory.buildEmptyAttributes(node.channels(), URI.create("icecp:/example/attribute/namespace"));
        attributes.add(new IdAttribute(42));

        module.run(node, attributes);
        assertTrue(module.isRunning());

        Thread.sleep(1000);

        module.stop(Module.StopReason.USER_DIRECTED);
        assertFalse(module.isRunning());
    }
}