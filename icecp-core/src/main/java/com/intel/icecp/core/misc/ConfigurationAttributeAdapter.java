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

package com.intel.icecp.core.misc;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.Module;
import com.intel.icecp.core.Node;
import com.intel.icecp.core.attributes.AttributeNotFoundException;
import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.attributes.BaseAttribute;
import com.intel.icecp.core.attributes.WriteableAttribute;
import com.intel.icecp.core.messages.ConfigurationMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.Set;

/**
 * @deprecated this is only present to support {@link Module#run(Node, Configuration, Channel, long)}; it will be
 * removed in 0.11.*; TODO remove this
 */
public class ConfigurationAttributeAdapter implements Configuration {
    private static final Logger LOGGER = LogManager.getLogger();
    private Attributes attributes;

    public ConfigurationAttributeAdapter(Attributes attributes) {
        this.attributes = attributes;
    }

    @Override
    public void load() throws ChannelIOException {
        // do nothing, this load is unnecessary in an adapter
    }

    @Override
    public void save() throws ChannelIOException {
        // do nothing, this load is unnecessary in an adapter
    }

    @Override
    public <T> T get(String propertyPath) throws PropertyNotFoundException {
        return getOrNull(propertyPath);
    }

    @Override
    public <T> T getOrNull(String propertyPath) {
        try {
            return (T) attributes.get(propertyPath, Object.class);
        } catch (AttributeNotFoundException e) {
            return null;
        }
    }

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

    @Override
    public void put(String propertyPath, Object value) {
        // TODO this is wrong
        ConfigurationAttribute attribute = new ConfigurationAttribute(propertyPath, value.getClass());
        attribute.value((Message) value);
        try {
            attributes.add(attribute);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Channel<ConfigurationMessage> getChannel() {
        throw new UnsupportedOperationException("getChannel() should not be called; this class is deprecated and will be removed.");
    }

    @Override
    public Set<String> keySet() {
        return Collections.unmodifiableSet(attributes.keySet());
    }
}

/**
 * @deprecated this should be replaced by individual attributes, one per configuration property; TODO replace this
 */
class ConfigurationAttribute extends BaseAttribute<Message> implements WriteableAttribute<Message> {
    Message state;

    public ConfigurationAttribute(String name, Class type) {
        super(name, type);
    }

    @Override
    public Message value() {
        return state;
    }

    @Override
    public void value(Message newValue) {
        state = newValue;
    }
}