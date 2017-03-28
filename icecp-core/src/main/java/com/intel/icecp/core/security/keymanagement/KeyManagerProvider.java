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
package com.intel.icecp.core.security.keymanagement;

import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.management.Channels;
import com.intel.icecp.core.misc.Configuration;
import com.intel.icecp.core.security.SecurityService;
import com.intel.icecp.core.security.keymanagement.exception.KeyManagerException;

/**
 * Provider class that handles key manager specific initializations.
 */
public interface KeyManagerProvider extends SecurityService<String> {
    
    /**
     * Returns a key manager
     * 
     * @param keyChannels Channels that may be used to remotely fetch keys
     * @param attributes Attributes to use for initialization
     * @return An instance of a key manager
     * @throws KeyManagerException If the key manager cannot be created
     */
    KeyManager get(Channels keyChannels, Attributes attributes) throws KeyManagerException;
    
    /**
     * Returns a key manager
     * 
     * @param keyChannels Channels that may be used to remotely fetch keys
     * @param configuration Configuration parameters
     * @return An instance of a key manager
     * @throws KeyManagerException If the key manager cannot be created
     */
    KeyManager get(Channels keyChannels, Configuration configuration) throws KeyManagerException;
    
}
