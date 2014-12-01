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
