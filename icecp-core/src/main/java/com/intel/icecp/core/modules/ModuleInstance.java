/*
 * ******************************************************************************
 *
 * INTEL CONFIDENTIAL
 *
 * Copyright 2013 - 2016 Intel Corporation All Rights Reserved.
 *
 * The source code contained or described herein and all documents related to
 * the source code ("Material") are owned by Intel Corporation or its suppliers
 * or licensors. Title to the Material remains with Intel Corporation or its
 * suppliers and licensors. The Material contains trade secrets and proprietary
 * and confidential information of Intel or its suppliers and licensors. The
 * Material is protected by worldwide copyright and trade secret laws and treaty
 * provisions. No part of the Material may be used, copied, reproduced,
 * modified, published, uploaded, posted, transmitted, distributed, or disclosed
 * in any way without Intel's prior express written permission.
 *
 * No license under any patent, copyright, trade secret or other intellectual
 * property right is granted to or conferred upon you by disclosure or delivery
 * of the Materials, either expressly, by implication, inducement, estoppel or
 * otherwise. Any license under such intellectual property rights must be
 * express and approved by Intel in writing.
 *
 * Unless otherwise agreed by Intel in writing, you may not remove or alter this
 * notice or any other notice embedded in Materials by Intel or Intel's
 * suppliers or licensors in any way.
 *
 * ******************************************************************************
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
