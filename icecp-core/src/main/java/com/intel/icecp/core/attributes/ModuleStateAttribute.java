package com.intel.icecp.core.attributes;

import com.intel.icecp.core.Module;

/**
 * Describe the state of a module; see {@link com.intel.icecp.core.Module.State} for possible values. TODO eventually
 * disallow writing of this attribute from remote attackers; TODO possibly move towards using this attribute as a
 * control state to stop modules externally.
 */
public class ModuleStateAttribute implements WriteableAttribute<Module.State> {
    public static final String NAME = "state";
    private Module.State value;

    @Override
    public void value(Module.State newValue) {
        value = newValue;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Class<Module.State> type() {
        return Module.State.class;
    }

    @Override
    public Module.State value() {
        return value;
    }
}
