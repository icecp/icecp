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
package com.intel.icecp.core.management;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.messages.ConfigurationMessage;
import com.intel.icecp.core.misc.Configuration;

/**
 * Map modules to the channel containing their configuration data.
 *
 */
public interface ConfigurationManager {

    /**
     * @param name the name of the configuration to retrieve
     * @return the {@link Configuration} instance
     */
    Configuration get(String name);

    /**
     * @param name the name of the configuration to retrieve
     * @return the {@link Channel} on which the configuration is available
     */
    Channel<ConfigurationMessage> getChannel(String name);
}
