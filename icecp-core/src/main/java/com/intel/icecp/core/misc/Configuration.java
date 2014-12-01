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
import com.intel.icecp.core.messages.ConfigurationMessage;

import java.util.Set;

/**
 * A configuration object; the purpose of this interface is to hide the mechanics of loading configuration data from a
 * channel so that it may be used in a variety of places throughout the application.
 *
 * @deprecated this API will be replaced by the {@link com.intel.icecp.core.attributes.Attributes} API in 0.11.*; TODO
 * remove this
 */
public interface Configuration {

    /**
     * Load the configuration file from the assigned channel, see {@link Configuration#getChannel()}; if called again,
     * this will force the configuration to retrieve its latest version.
     *
     * @throws com.intel.icecp.core.misc.ChannelIOException if the channel fails
     */
    void load() throws ChannelIOException;

    /**
     * Persist any changes to the configuration; this will publish the changes as a new version on the channel.
     *
     * @throws com.intel.icecp.core.misc.ChannelIOException if the channel fails
     */
    void save() throws ChannelIOException;

    /**
     * Retrieve a value from the configuration file; if not loaded, the configuration will load itself synchronously.
     *
     * @param <T> the inferred type to return
     * @param propertyPath the path to the value, e.g. prop1.prop2.prop3
     * @return the property value
     * @throws com.intel.icecp.core.misc.PropertyNotFoundException if the path is not found
     */
    <T> T get(String propertyPath) throws PropertyNotFoundException;

    /**
     * Retrieve a value from the configuration file or return null if not found; if not loaded, the configuration will
     * load itself synchronously.
     *
     * @param <T> the inferred type to return
     * @param propertyPath the path to the value, e.g. prop1.prop2.prop3
     * @return the property value
     */
    <T> T getOrNull(String propertyPath);

    /**
     * Retrieve a value from the configuration file or return the default value given if not found; this method will
     * search each property path from left to right.
     *
     * @param <T> the inferred type to return
     * @param defaultValue the value to return if no path is found
     * @param propertyPaths a list of paths to values, e.g. prop1.prop2.prop3
     * @return the property value
     */
    <T> T getOrDefault(T defaultValue, String... propertyPaths);

    /**
     * Change a value in the configuration file; note that changes will not be persisted until {@link
     * Configuration#save()} is called. If necessary, this method will create the path structure.
     *
     * @param propertyPath the path to the value, e.g. prop1.prop2.prop3
     * @param value the property value
     */
    void put(String propertyPath, Object value);

    /**
     * @return a reference to the underlying channel; this may be permission-protected by implementing classes.
     */
    Channel<ConfigurationMessage> getChannel();

    /**
     * @return Current set of valid configuration parameters(note, static snapshot!)
     */
    Set<String> keySet();
}
