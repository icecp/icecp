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
