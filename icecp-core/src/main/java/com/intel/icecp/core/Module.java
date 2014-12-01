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
package com.intel.icecp.core;

import com.intel.icecp.core.attributes.AttributeNotFoundException;
import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.attributes.IdAttribute;
import com.intel.icecp.core.attributes.StateChannelAdapter;
import com.intel.icecp.core.misc.Configuration;
import com.intel.icecp.core.misc.ConfigurationAttributeAdapter;
import com.intel.icecp.core.modules.ModuleProperty;


/**
 * Represent a runnable process for processing on the device. A module has the ability to stop its execution by
 * returning from the {@link #run(Node, Configuration, Channel, long)} block but module control (loadFromClass, start, stop,
 * etc.) should be accomplished by calling the appropriate at the node level.
 * <p>
 * Modules are identified using {@link ModuleProperty}--the unique name specified in this attribute is used when
 * determining what modules to load from a JAR. Modules that do not have a corresponding {@link ModuleProperty} with a
 * unique name will not load correctly.
 * <p>
 * Note that the implementor is responsible for setting the state at appropriate times (see {@link Module.State}) by
 * publishing to the passed state channel; this is necessary for the managing node and other modules to interact with it
 * correctly. When a module is first loaded, the node will set the initial state to {@link State#LOADED} but
 * implementors should set the module state to {@link State#RUNNING} soon after entering the {@link
 * #run(com.intel.icecp.core.Node, com.intel.icecp.core.misc.Configuration, com.intel.icecp.core.Channel, long) }method;
 * the node will set the state to {@link State#STOPPED} after it calls {@link #stop(StopReason)}. Likewise, if the
 * module enters an error state, the module itself should publish {@link State#ERROR}.
 *
 */
public interface Module {

    /**
     * Start and run the module; a {@link Node} will call this method after it has instantiated a module.
     *
     * @param node the node instance the module is running on
     * @param moduleConfiguration the {@link Configuration} for this module
     * @param moduleStateChannel the {@link Channel} on which to publish state changes; e.g.
     * moduleStateChannel.publish(State.RUNNING).
     * @param moduleId the unique identifier for the newly created module instance; this is necessary because a node
     * could run multiple instances of the same module
     * @deprecated use {@link #run(Node, Attributes)} instead; expect this method to be removed by 0.11.* TODO remove
     * this method in 0.11.*
     */
    @Deprecated
    default void run(Node node, Configuration moduleConfiguration, Channel<Module.State> moduleStateChannel, long moduleId){
        throw new UnsupportedOperationException("This method is deprecated and will no longer be supported in icecp-node > 0.11; use run(Node, Attributes) instead.");
    }

    /**
     * Start and run the module; a {@link Node} will call this method after it has instantiated a module. TODO
     * eventually this method will be the only way to run a module; new modules should implement this and not {@link
     * #run(Node, Configuration, Channel, long)}
     *
     * @param node the node instance the module is running on
     * @param attributes the initially-configured attributes for the module
     */
    default void run(Node node, Attributes attributes) {
        ConfigurationAttributeAdapter configurationAttributeAdapter = new ConfigurationAttributeAdapter(attributes);
        StateChannelAdapter moduleStateChannel = new StateChannelAdapter(attributes);
        try {
            run(node, configurationAttributeAdapter, moduleStateChannel, attributes.get(IdAttribute.class));
        } catch (AttributeNotFoundException e) {
            throw new IllegalArgumentException("The passed attributes must always contain an 'id' attribute.", e);
        }
    }

    /**
     * Stop the module; because Java has deprecated {@link Thread#stop()}, the implementor has the responsibility to
     * safely tear down the module. This means that implementors need to implement their own loop handling so that they
     * exit gracefully from run() if it is still running in a different thread (implementation note: run() will likely
     * be running in one thread and stop() will be called from another, see link below for suggestions on thread-safe
     * ways to exit).
     * <p>
     * This method is a command from the managing node to the module instance, there is little reason for the module to
     * call its own stop()--instead, use the node-level {@code Node.modules().stop(StopReason)}. Modules that do not
     * respond correctly to this command may interfere with correct operation of the node (e.g. rebooting, upgrading,
     * etc.) and may result in undefined behavior.
     *
     * @param reason a code indicating the reason this module was stopped
     * @see <a href="https://www.securecoding.cert.org/confluence/display/java/THI05-J.+Do+not+use+Thread.stop()+to+terminate+threads">THI05-J.
     * Do not use Thread.stop() to terminate threads</a>
     */
    void stop(Module.StopReason reason);

    /**
     * Allowed module states.
     */
    enum State implements Message {
        INSTANTIATED, LOADED, RUNNING, ERROR, STOPPED
    }

    /**
     * Allowed reasons to stop a module.
     */
    enum StopReason implements Message {
        USER_DIRECTED, NODE_SHUTDOWN, OUT_OF_RESOURCES
    }
}
