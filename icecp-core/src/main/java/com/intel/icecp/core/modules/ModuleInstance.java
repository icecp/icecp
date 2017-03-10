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

package com.intel.icecp.core.modules;

import com.intel.icecp.core.Module;
import com.intel.icecp.core.attributes.AttributeNotFoundException;
import com.intel.icecp.core.attributes.AttributeNotWriteableException;
import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.attributes.IdAttribute;
import com.intel.icecp.core.attributes.ModuleStateAttribute;
import com.intel.icecp.core.attributes.NameAttribute;

/**
 * Data structure for holding an instantiated module; TODO extract interface
 *
 */
public class ModuleInstance {
    private final Module module;
    private final Attributes attributes;
    private final String internalName;

    /**
     * Retained for testing since it is easier to mock; {@link #create(Class, Attributes)} is preferred.
     *
     * @param module the module; security context should already be applied
     * @param attributes the attributes to use for this instance; must include at a minimum 'id', 'name', and 'state'
     * @throws ModuleLoadException if the minimum attribute set is not provided
     */
    public ModuleInstance(Module module, Attributes attributes) throws ModuleLoadException {
        try {
            this.module = module;
            this.attributes = attributes;
            this.internalName = attributes.get(NameAttribute.class) + '#' + attributes.get(IdAttribute.class);
            this.attributes.set(ModuleStateAttribute.class, Module.State.INSTANTIATED);
        } catch (AttributeNotFoundException | AttributeNotWriteableException e) {
            throw new ModuleLoadException("Mis-configured attributes for module instance: they must contain an 'id', 'name', and 'state'", e);
        }
    }

    /**
     * Factory method for generating an instance of the module class before returning an instance of this one.
     *
     * @param moduleClass the module class; security context should already be applied
     * @param attributes the attributes to use for this instance; must include at a minimum 'id', 'name', and 'state'
     * @return a new module instance
     * @throws ModuleLoadException if an instance of the class cannot be created or the minimum attribute set is not
     * provided
     */
    public static ModuleInstance create(Class<? extends Module> moduleClass, Attributes attributes) throws ModuleLoadException {
        try {
            return new ModuleInstance(moduleClass.newInstance(), attributes);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ModuleLoadException("Unable to create an instance of the module, ensure a default constructor is available (public, non-inner): " + moduleClass, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return internalName;
    }

    /**
     * @return the instantiated module
     */
    public Module module() {
        return module;
    }

    /**
     * @return the attributes of the instantiated module
     */
    public Attributes attributes() {
        return attributes;
    }

    /**
     * Helper method for non-throwing access to the module attributes
     *
     * @return the module name
     */
    public String name() {
        try {
            return attributes.get(NameAttribute.class);
        } catch (AttributeNotFoundException e) {
            throw new IllegalStateException("A module instance must always have a name attribute: " + module, e);
        }
    }

    /**
     * Helper method for non-throwing access to the module attributes
     *
     * @return the module ID
     */
    public long id() {
        try {
            return attributes.get(IdAttribute.class);
        } catch (AttributeNotFoundException e) {
            throw new IllegalStateException("A module instance must always have an ID attribute: " + module, e);
        }
    }

    /**
     * Helper method for non-throwing access to the module attributes
     *
     * @return the module state
     */
    public Module.State state() {
        try {
            return attributes.get(ModuleStateAttribute.class);
        } catch (AttributeNotFoundException e) {
            throw new IllegalStateException("A module instance must always have a state attribute: " + module, e);
        }
    }

    /**
     * Helper method for non-throwing access to the module attributes
     *
     * @param state the module state to set
     */
    public void state(Module.State state) {
        try {
            attributes.set(ModuleStateAttribute.class, state);
        } catch (AttributeNotFoundException | AttributeNotWriteableException e) {
            throw new IllegalStateException("A module instance must always have a state attribute: " + module, e);
        }
    }
}
