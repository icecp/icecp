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

package com.intel.icecp.core.attributes;

import com.intel.icecp.core.messages.ConfigurationMessage;

/**
 * @deprecated this is only present to support the API changes to {@link com.intel.icecp.core.Module}; it should be
 * removed in 0.11.* and replaced with single attributes per configuration property; TODO remove this
 */
public class ComplexConfigurationAttribute extends BaseAttribute<ConfigurationMessage> {
    private final ConfigurationMessage value;

    public ComplexConfigurationAttribute(ConfigurationMessage value) {
        super("configuration", ConfigurationMessage.class);
        this.value = value;
    }

    @Override
    public ConfigurationMessage value() {
        return value;
    }
}