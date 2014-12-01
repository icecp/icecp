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

package com.intel.icecp.module.example;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.Module;
import com.intel.icecp.core.Node;
import com.intel.icecp.core.attributes.WriteableBaseAttribute;
import com.intel.icecp.core.attributes.AttributeNotFoundException;
import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.attributes.IdAttribute;
import com.intel.icecp.core.messages.BytesMessage;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.Configuration;
import com.intel.icecp.core.modules.ModuleInstance;
import com.intel.icecp.core.modules.ModuleProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.intel.icecp.core.modules.ModuleProperty;

import java.net.URI;
import java.util.Map;

/**
 * An example module; it implements the {@link Module} interface and conforms to several requirements specified by that
 * API (see {@link ModuleProperty}, module naming, {@link Module#stop(StopReason)}, etc.). Since this
 */
@ModuleProperty(name = "ExampleModule", attributes = {ExampleModule.ExampleAttribute.class})
// note how we match this property (necessary for ICECP to find the module) with the module class name
public class ExampleModule implements Module {
    private static final Logger LOGGER = LogManager.getLogger();
    private long moduleId;
    private boolean running = false;
    private Thread childThread;
    private Channel<ExampleMessage> input;

    @Override
    public void run(Node node, Configuration moduleConfiguration, Channel<State> moduleStateChannel, long moduleId) {
        // this method is deprecated and will be removed; use run(Node node, Attributes attributes) instead
        throw new UnsupportedOperationException("Unimplemented due to deprecation");
    }

    @Override
    public void run(Node node, Attributes attributes) {
        try {
            run0(node, attributes);
        } catch (AttributeNotFoundException e) {
            LOGGER.error("Failed while attempting to run {}", this, e);
        }
    }

    private void run0(Node node, Attributes attributes) throws AttributeNotFoundException {
        moduleId = attributes.get(IdAttribute.class); // we can expect ICECP to provide us with certain attributes
        running = true;
        LOGGER.info("Now running: {}", this);


        // list all available attributes
        for (Map.Entry<String, Object> a : attributes.toMap().entrySet()) {
            LOGGER.info("Attribute '{}' provided: {}", a.getKey(), a.getValue());
        }

        // check what other modules are loaded; note that if "-Dicecp.sandbox=enabled" then permissions will prevent us from doing this
        for (ModuleInstance m : node.modules().getAll()) {
            LOGGER.info("Neighbor module {}: {}", m.name(), m.state());
        }

        // open up a channel to communicate externally (e.g. other modules, other nodes)
        try (Channel<BytesMessage> output = node.openChannel(URI.create("icecp:/example/module/output"), BytesMessage.class, Persistence.DEFAULT)) {
            assert (!output.isPublishing());
            String x = "";
            for (Map.Entry<String, Object> a : attributes.toMap().entrySet()) {
                x += "Attribute '" + a.getKey() + "' provided: " + a.getValue() + "\n";
            }
            output.publish(new BytesMessage(x.getBytes()));
        } catch (ChannelLifetimeException | ChannelIOException e) {
            LOGGER.error("Failed to send data.", e);
        }

        // perhaps retrieve data remotely; note that if "-Dicecp.sandbox=enabled" then permissions may prevent us from opening certain channels
        try {
            input = node.openChannel(URI.create("icecp:/example/module/input"), ExampleMessage.class, Persistence.DEFAULT);
            assert (!input.isSubscribing());
            input.subscribe(m -> LOGGER.info("Received m: {}", m)); // this callback will live on past
        } catch (ChannelLifetimeException | ChannelIOException e) {
            LOGGER.error("Failed to subscribe to data.", e);
        }

        // if necessary, spawn other threads to continue working; the ModuleInstance will ensure this class is not garbage collected before stop() is called
        childThread = new Thread(new Loop());
        childThread.setDaemon(true);
        childThread.start();
    }

    @Override
    public void stop(StopReason reason) {
        LOGGER.info("Stopping: {}", reason);

        // stop any spawned threads
        running = false;
        childThread.interrupt();

        // close any open channels
        try {
            input.close();
        } catch (ChannelLifetimeException e) {
            LOGGER.error("Failed to close: {}", input, e);
        }
    }

    @Override
    public String toString() {
        return "ExampleModule{id=" + moduleId + '}';
    }

    /**
     * Helper method for inspecting module state
     *
     * @return true if the module is running
     */
    boolean isRunning() {
        return running;
    }

    /**
     * Note the `public static`; necessary for correct serialization by certain libraries
     */
    public static class ExampleMessage implements Message {
        public String a;
        public long b;

        public ExampleMessage() {
            // ensure a default constructor is available for serialization purposes
        }

        public ExampleMessage(String a, long b) {
            this.a = a;
            this.b = b;
        }
    }

    /**
     * A simple loop
     */
    private class Loop implements Runnable {
        @Override
        public void run() {
            while (running) {
                LOGGER.info("Running in spawned childThread.");
                sleep(4000);
            }
        }

        void sleep(int ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static class ExampleAttribute extends WriteableBaseAttribute<String> {
        public ExampleAttribute() {
            super("example", String.class);
        }
    }
}
