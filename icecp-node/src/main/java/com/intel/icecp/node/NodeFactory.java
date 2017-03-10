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

package com.intel.icecp.node;

import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.intel.icecp.core.Node;
import com.intel.icecp.core.channels.ChannelProvider;
import com.intel.icecp.core.event.Events;
import com.intel.icecp.core.management.Channels;
import com.intel.icecp.core.management.ConfigurationManager;
import com.intel.icecp.core.mock.MockChannelProvider;
import com.intel.icecp.node.channels.file.FileChannelProvider;
import com.intel.icecp.node.management.FileConfigurationManager;
import com.intel.icecp.node.management.FilePermissionsManager;
import com.intel.icecp.node.utils.ConfigurationUtils;

/**
 * Convenience utility for setting up all required objects before starting the daemon or running tests.
 *
 */
public class NodeFactory {
    private static final int THREADS_PER_CPU = 16;
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Configure and return a default node.
     *
     * @param nodeName the unique identifying name for the device (e.g. /intel/node/1234)
     * @return a configured {@link Node}
     */
    public static Node buildDefaultNode(String nodeName, Path configurationPath, Path permissionsPath) {
        ScheduledExecutorService eventLoop = buildEventLoop();
        ChannelProvider fileChannelBuilder = new FileChannelProvider();

        FilePermissionsManager permissionsManager = new FilePermissionsManager(permissionsPath, fileChannelBuilder);
        ConfigurationManager configurationManager = new FileConfigurationManager(configurationPath, fileChannelBuilder);

        Channels channelManager = buildChannels(eventLoop, configurationManager);
        Events eventService = new ChannelNotifyingEventsImpl(URI.create("ndn:/intel/events"), channelManager.get("ndn"));

        return new NodeImpl(nodeName, channelManager, permissionsManager, configurationManager, eventLoop, eventService);
    }

    public static Channels buildChannels(ScheduledExecutorService eventLoop, ConfigurationManager configurationManager) {
        Channels channels = new ChannelsImpl(eventLoop, configurationManager);

        //This logger statement will show you which class loader is currently being used
        //LOGGER.info(String.format("buildChannels() Stack: %s", JarUtils.getClassLoaderStack(channels)));
        //Pass in a class loader to loadImplementations().
        //Just use the channels object to get the loader.  Can't use "this" since we're static.
        //jjs (nashorn) apparently uses its own loader, so we need to pass this into the ServiceLoader to use.
        //other wise javascript won't run successfully when accessing this method.  The serviceloader will not
        //successfully load its implementations.
        for (ChannelProvider provider : loadImplementations(ChannelProvider.class, channels.getClass().getClassLoader())) {
            channels.register(provider.scheme(), provider);
        }
        return channels;

    }

    /**
     * @param <T> the interface to implement
     * @param type a {@link Class} of the SPI interface
     * @return implementations of a given interface; must be defined according to Java SPI specification
     */
    public static <T> List<T> loadImplementations(Class<T> type, ClassLoader classLoader) {

        java.util.ServiceLoader<T> load;
        if (classLoader == null) {
            load = java.util.ServiceLoader.load(type);
        } else {
            load = java.util.ServiceLoader.load(type, classLoader);
        }

        List<T> implementations = new ArrayList<>();

        // add SPI-loaded implementations
        for (T implementation : load) {
            implementations.add(implementation);
        }
        return implementations;
    }

    /**
     * @return the event loop scheduler service; TODO link this to configuration
     */
    public static ScheduledExecutorService buildEventLoop() {
        int numProcessors = Runtime.getRuntime().availableProcessors();
        int numThreads = numProcessors * THREADS_PER_CPU;
        LOGGER.debug("Using " + numThreads + " threads for thread pool on " + numProcessors + " processor(s)");
        return Executors.newScheduledThreadPool(numThreads);
    }

    /**
     * Configure and return a test node
     *
     * @param nodeName the unique identifying name for the device (e.g. /intel/device/1234)
     * @return a configured {@link Node}
     */
    public static Node buildTestNode(String nodeName) {
        ScheduledExecutorService eventLoop = buildEventLoop();
        ChannelProvider fileChannelBuilder = new FileChannelProvider();

        FilePermissionsManager permissionsManager = new FilePermissionsManager(ConfigurationUtils.getPermissionsPath(), fileChannelBuilder);
        ConfigurationManager configurationManager = new FileConfigurationManager(ConfigurationUtils.getConfigurationPath(), fileChannelBuilder);

        Channels channels = buildChannels(eventLoop, configurationManager);
        Events eventService = new ChannelNotifyingEventsImpl(URI.create("ndn:/intel/events"), channels.get("ndn"));

        return new NodeImpl(nodeName, channels, permissionsManager, configurationManager, eventLoop, eventService);
    }

    /**
     * Configure and return a mock node which creates mock channels
     *
     * @return a mock {@link Node}
     */
    public static Node buildMockNode() {
        String nodeName = "/test/" + (new Random().nextLong());
        ScheduledExecutorService eventLoop = Executors.newScheduledThreadPool(4);
        ChannelProvider fileChannelBuilder = new FileChannelProvider();

        FilePermissionsManager permissionsManager = new FilePermissionsManager(ConfigurationUtils.getPermissionsPath(), fileChannelBuilder);
        ConfigurationManager configurationManager = new FileConfigurationManager(ConfigurationUtils.getConfigurationPath(), fileChannelBuilder);

        Channels channelManager = new ChannelsImpl(eventLoop, configurationManager);
        channelManager.register("*", new MockChannelProvider());

        Events eventService = new ChannelNotifyingEventsImpl(URI.create("mock:/intel/events"), channelManager.get("mock"));

        return new NodeImpl(nodeName, channelManager, permissionsManager, configurationManager, eventLoop, eventService);
    }
}