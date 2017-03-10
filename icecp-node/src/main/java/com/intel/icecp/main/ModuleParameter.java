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

package com.intel.icecp.main;

import java.net.URI;
import java.nio.file.Paths;

/**
 * Encapsulates module parameters passed on the command line. It expects parameters to be a JAR URI with an optional
 * configuration URI appended after the '%' character. E.g.: my-module.jar%ndn:/some/route/to/my-configuration.json
 *
 */
public class ModuleParameter {
    private static final String SPLIT_TOKEN = "%";
    private static final String DEFAULT_CONFIGURATION_NAME = "config.json";
    public final URI modulePath;
    public final URI configurationPath;

    /**
     * @param modulePath the URI to the module JAR
     * @param configurationPath the URI to the module configuration
     */
    private ModuleParameter(URI modulePath, URI configurationPath) {
        this.modulePath = modulePath;
        this.configurationPath = configurationPath;
    }

    /**
     * @param parameter a string with a URI to a JAR with an option URI to a configuration file after a '%' character
     * @return a module parameter; {@link #configurationPath} will be null if not specified
     */
    public static ModuleParameter build(String parameter) {
        if (parameter.contains(SPLIT_TOKEN)) {
            String[] parts = parameter.split(SPLIT_TOKEN);
            return new ModuleParameter(toUri(parts[0]), toUri(parts[1]));
        } else {
            URI jarUri = toUri(parameter);
            return new ModuleParameter(jarUri, locateDefaultConfigurationUri(jarUri));
        }
    }

    /**
     * Determine the default configuration URI based on the JAR URI. For a JAR path file:///path/to/jar this will use
     * the parent path file:///path/to and append the {@link #DEFAULT_CONFIGURATION_NAME}.
     *
     * @param jarUri the location of the JAR
     * @return the default location of the configuration
     */
    protected static URI locateDefaultConfigurationUri(URI jarUri) {
        return Paths.get(jarUri).getParent().resolve(DEFAULT_CONFIGURATION_NAME).toUri();
    }

    /**
     * Convert a given location string to URI; this contains special processing to properly prepend "file:" to Windows
     * path names (which otherwise URI believes have a scheme of "C:", e.g.)
     *
     * @param location the location to convert
     * @return a URI
     */
    protected static URI toUri(String location) {
        URI uri = URI.create(location);

        // no scheme means convert to a file
        if (uri.getScheme() == null) {
            return Paths.get(location).toUri();
        }
        // schemes of size one are likely drive letters on Windows; prepend 'file:'
        else if (uri.getScheme().length() == 1) {
            return Paths.get(location).toUri();
        }

        return uri;
    }
}
