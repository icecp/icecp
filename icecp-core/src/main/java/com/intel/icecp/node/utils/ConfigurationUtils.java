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

package com.intel.icecp.node.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 */
public class ConfigurationUtils {

    public static final String DEFAULT_CONFIGURATION_DIR = "configuration";
    public static final String DEFAULT_PERMISSIONS_DIR = "permissions";

    /**
     * @return true if 'icecp.sandbox' property has not been set to 'disabled' or 'false'
     */
    public static boolean isSandboxEnabled() {
        String property = System.getProperty("icecp.sandbox");
        return !("disabled".equalsIgnoreCase(property)) && !("false".equalsIgnoreCase(property));
    }

    /**
     * @return the path to the directory specified in 'icecp.configuration' or './conf' by default
     */
    public static Path getConfigurationPath() {
        String property = System.getProperty("icecp.configuration");
        return Paths.get(property != null ? property : DEFAULT_CONFIGURATION_DIR);
    }

    /**
     * @return the path to the directory specified in 'icecp.permissions' or './permissions' by default
     */
    public static Path getPermissionsPath() {
        String property = System.getProperty("icecp.permissions");
        return Paths.get(property != null ? property : DEFAULT_PERMISSIONS_DIR);
    }
}
