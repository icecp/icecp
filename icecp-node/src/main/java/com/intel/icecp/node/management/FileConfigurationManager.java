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
package com.intel.icecp.node.management;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.channels.ChannelProvider;
import com.intel.icecp.core.management.ConfigurationManager;
import com.intel.icecp.core.metadata.formats.JsonFormat;
import com.intel.icecp.core.messages.ConfigurationMessage;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.Configuration;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.node.ConfigurationImpl;
import com.intel.icecp.node.pipeline.implementations.MessageFormattingPipeline;
import java.net.URI;
import java.nio.file.Path;

/**
 * Retrieve configuration files from a specific file system location.
 *
 */
public class FileConfigurationManager implements ConfigurationManager {

    public static final String FILE_SUFFIX = ".json";
    private final Path configurationRoot;
    private final ChannelProvider channelBuilder;

    /**
     * Constructor
     *
     * @param configurationRoot the {@link Path} where all
     * @param channelBuilder mechanism for creating channels to retrieve
     * configurations
     */
    public FileConfigurationManager(Path configurationRoot, ChannelProvider channelBuilder) {
        this.configurationRoot = configurationRoot;
        this.channelBuilder = channelBuilder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Configuration get(String name) {
        return new ConfigurationImpl(getChannel(name));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Channel<ConfigurationMessage> getChannel(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("No configuration name given.");
        }
        URI uri = configurationRoot.resolve(name + FILE_SUFFIX).toUri();
        Pipeline p = MessageFormattingPipeline.create(ConfigurationMessage.class, new JsonFormat(ConfigurationMessage.class));
        try {
            return channelBuilder.build(uri, p, new Persistence());
        } catch (ChannelLifetimeException ex) {
            throw new IllegalArgumentException("Unable to create the desired channel: " + uri, ex);
        }
    }
}
