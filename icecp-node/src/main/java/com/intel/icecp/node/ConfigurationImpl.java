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
package com.intel.icecp.node;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.messages.ConfigurationMessage;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.Configuration;
import com.intel.icecp.core.misc.PropertyNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

/**
 * Implement the {@link Configuration} interface using channels.
 *
 * @deprecated this implementation will be replaced by the {@link AttributesImpl} API in 0.11.*; TODO remove this
 */
public class ConfigurationImpl implements Configuration {

    private static final Logger logger = LogManager.getLogger();
    private static final Pattern bracketSubstitution = Pattern.compile("\\[(\\d+)\\]");
    private final Channel<ConfigurationMessage> channel;
    private ConfigurationMessage data;

    /**
     * Build a configuration object from a {@link Channel}
     *
     * @param channel
     */
    public ConfigurationImpl(Channel<ConfigurationMessage> channel) {
        this.channel = channel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        logger.trace("Loading configuration from: " + getChannel());
        try {
            channel.open().get();
            data = channel.latest().get();
        } catch (ChannelIOException | ExecutionException | InterruptedException | ChannelLifetimeException e) {
            data = new ConfigurationMessage();
            logger.warn(String.format("Failed to load configuration from %s, setting data to empty and ignoring error.", channel.getName()));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save() throws ChannelIOException {
        getChannel().publish(data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T get(String propertyPath) throws PropertyNotFoundException {
        T value = getOrNull(propertyPath);
        if (value == null) {
            throw new PropertyNotFoundException("Failed to find property: " + propertyPath);
        }
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getOrNull(String propertyPath) {
        checkIfLoaded();
        propertyPath = replaceArrayBrackets(propertyPath);
        List<String> properties = new ArrayList<>(Arrays.asList(propertyPath.split("\\.")));
        try {
            Object value = traverseTree(data, properties);
            while (value != null && !properties.isEmpty()) {
                if (value instanceof Map) {
                    value = traverseTree((Map<String, Object>) value, properties);
                } else if (value instanceof List) {
                    value = traverseTree((List<Object>) value, properties);
                } else {
                    value = null;
                }
            }
            return (T) value;
        } catch (ClassCastException e) {
            logger.warn("Configuration get failed due to class cast; aborting and returning null.", e);
            return null;
        }
    }

    /**
     * @param propertyPath a list of properties or indices like "a.b.c[1]"
     * @return a normalized list of properties using only ".", e.g. "a.b.c.1"
     */
    protected String replaceArrayBrackets(String propertyPath) {
        return bracketSubstitution.matcher(propertyPath).replaceAll(".$1");
    }

    /**
     * Consume the list of properties
     *
     * @param data a map of properties to objects
     * @param properties the list of remaining properties in the path
     * @return a sub-map or null if the property is not found
     */
    private Object traverseTree(Map<String, Object> data, List<String> properties) {
        String property = properties.remove(0);
        return data != null && data.containsKey(property) ? data.get(property) : null;
    }

    /**
     * Consume the list of properties
     *
     * @param data a list of objects
     * @param properties the list of remaining properties in the path
     * @return a sub-map or null if the property is not found
     */
    private Object traverseTree(List<Object> data, List<String> properties) {
        int index = Integer.decode(properties.remove(0));
        return data != null && index < data.size() ? data.get(index) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getOrDefault(T defaultValue, String... propertyPaths) {
        for (String propertyPath : propertyPaths) {
            T value = getOrNull(propertyPath);
            if (value != null) {
                return value;
            }
        }
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(String propertyPath, Object value) {
        checkIfLoaded();
        String[] properties = (propertyPath.contains(".")) ? propertyPath.split("\\.") : new String[]{propertyPath};

        Map<String, Object> current = data;
        int i = 0;
        for (; i < properties.length - 1; i++) {
            Map child;
            if (!current.containsKey(properties[i])
                    || !(current.get(properties[i]) instanceof Map)) {
                child = new LinkedHashMap<String, Object>();
                current.put(properties[i], child);
            } else {
                child = (Map) current.get(properties[i]);
            }
            current = child;
        }
        current.put(properties[i], value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Channel<ConfigurationMessage> getChannel() {
        return channel;
    }

    @Override
    public Set<String> keySet() {
        checkIfLoaded();
        return new HashSet<>(data.keySet());
    }

    /**
     * Test if the configuration has been loaded.
     *
     * @throws IllegalStateException if the configuration has not been loaded
     */
    private void checkIfLoaded() {
        if (data == null) {
            logger.error("Call load() before attempting to access configuration data.");
            throw new IllegalStateException("Call load() before attempting to access configuration data.");
        }
    }
}
