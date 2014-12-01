package com.intel.icecp.node.management.modules;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Module;
import com.intel.icecp.core.Node;
import com.intel.icecp.core.misc.Configuration;
import com.intel.icecp.core.modules.ModuleProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Helper class for testing module inheritance
 */
@ModuleProperty(name="c-module", attributes={})
public class C implements Module {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void run(Node node, Configuration moduleConfiguration, Channel<State> moduleStateChannel, long moduleId) {
    }

    @Override
    public void stop(StopReason reason) {
    }
}
