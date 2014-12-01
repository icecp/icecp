package com.intel.icecp.node.management.modules;

import com.intel.icecp.common.TestModule;
import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Module;
import com.intel.icecp.core.Node;
import com.intel.icecp.core.misc.Configuration;
import com.intel.icecp.core.modules.ModuleProperty;

/**
 * Helper class for testing module inheritance
 */
@ModuleProperty(name="d-module", attributes={TestModule.TestAttribute.class})
public class DModule implements Module {
    @Override
    public void run(Node node, Configuration moduleConfiguration, Channel<State> moduleStateChannel, long moduleId) {
    }

    @Override
    public void stop(StopReason reason) {
    }
}
