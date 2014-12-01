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