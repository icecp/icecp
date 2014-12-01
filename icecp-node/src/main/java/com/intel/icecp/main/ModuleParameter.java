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
