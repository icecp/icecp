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
package com.intel.icecp.node.management;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.channels.ChannelProvider;
import com.intel.icecp.core.management.ModulePermissions;
import com.intel.icecp.core.management.PermissionLoadException;
import com.intel.icecp.core.management.PermissionsManager;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.metadata.formats.JsonFormat;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.node.messages.PermissionsMessage;
import com.intel.icecp.node.messages.PermissionsMessage.Grant;
import com.intel.icecp.node.pipeline.implementations.MessageFormattingPipeline;
import com.intel.icecp.node.utils.ConfigurationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.file.Path;
import java.security.AllPermission;
import java.security.Permission;
import java.security.Permissions;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Retrieve permissions from a directory
 *
 */
public class FilePermissionsManager implements PermissionsManager {

    private static final String FILE_SUFFIX = ".json";
    private static final int DEFAULT_RETRIEVAL_TIMEOUT = 30;
    private static final Logger logger = LogManager.getLogger();
    private final Path rootPermissionsFolder;
    private final ChannelProvider channelBuilder;

    /**
     * Constructor
     *
     * @param rootPermissionsFolder root folder where the module permission
     * files are located.
     * @param channelBuilder mechanism for creating channels to retrieve
     * permissions
     */
    public FilePermissionsManager(Path rootPermissionsFolder, ChannelProvider channelBuilder) {
        this.rootPermissionsFolder = rootPermissionsFolder;
        this.channelBuilder = channelBuilder;
    }

    /**
     * Retrieve permissions for a module; Pull them from the json file specified
     * in the parameter. Convert the permissions into classes and build a
     * Permissions object to return. If a permission in the file does not get
     * constructed for any reason, the loop continues. It builds as many
     * permissions as possible.
     *
     * @param moduleName Name of the module. Assumes permissions file is
     * moduleName.json
     * @return a Permissions class containing the permissions from the file.
     * @throws PermissionLoadException if the permissions cannot be retrieved
     */
    @Override
    public ModulePermissions retrievePermissions(String moduleName) throws PermissionLoadException {

        if (moduleName == null || moduleName.isEmpty()) {
            throw new IllegalArgumentException("No module name given.");
        }

        Permissions permissions = new Permissions();

        //If security is disabled, then just return "all permissions"
        //Don't fail because the permissions file is not present.
        if (!ConfigurationUtils.isSandboxEnabled()) {
            permissions.add(new AllPermission());
            // TODO: set moduleHash and hashAlgorithm properly
            return new ModulePermissions(permissions, null, null);
        }

        URI uri = rootPermissionsFolder.resolve(moduleName + FILE_SUFFIX).toUri();
        logger.info("Permission file for {} resolves to {}", moduleName, uri);

        Class<?> c;
        Constructor<?> cons;
        Object permissionObject;

        // We expect to receive an instance of PermissionMessage
        PermissionsMessage permMessage;
        
        Pipeline pipeline = MessageFormattingPipeline.create(PermissionsMessage.class, new JsonFormat<>(PermissionsMessage.class));
        
        try {
            // @TODO: USING METADATA FOR THIS CHANNEL, SIGNATURE VERIFICATION SHOULD BE
            // ADDED AUTOMATICALLY TO THE PIPELINE!
            Channel<PermissionsMessage> channel = channelBuilder.build(uri, pipeline, new Persistence());
            channel.open().get(DEFAULT_RETRIEVAL_TIMEOUT, TimeUnit.SECONDS);

            // now we retrieve the inner message, which should be of type PermissionMessage
            permMessage = channel.latest().get(DEFAULT_RETRIEVAL_TIMEOUT, TimeUnit.SECONDS);

        } catch (ChannelIOException | InterruptedException | ExecutionException | TimeoutException | ChannelLifetimeException e) {
            logger.error("Failed to retrieve permissions file: {}", uri, e);
            throw new PermissionLoadException("Failed to retrieve permissions file: " + uri, e);
        }

        // Extract the granted permissions
        for (Grant grantPerm : permMessage.grants) {
            try {
                c = Class.forName(grantPerm.permission);
                if (grantPerm.action.isEmpty()) {
                    cons = c.getConstructor(String.class);
                    permissionObject = cons.newInstance(grantPerm.target);
                } else {
                    cons = c.getConstructor(String.class, String.class);
                    if (grantPerm.action.trim().length() == 0) {
                        permissionObject = cons.newInstance(grantPerm.target, null);
                    } else {
                        permissionObject = cons.newInstance(grantPerm.target, grantPerm.action);
                    }
                }
                permissions.add((Permission) permissionObject);
            } catch (InvocationTargetException ite) {
                logger.error(String.format(
                        "Not able to create the permission[%s] Target[%s] Action[%s]: %s",
                        grantPerm.permission, grantPerm.target, grantPerm.action.trim(), ite.getTargetException().getMessage()));

            } catch (ClassNotFoundException cnfe) {
                logger.error(String.format(
                        "Not able to create the permission[%s] Target[%s] Action[%s]: Permission ClassNotFound",
                        grantPerm.permission, grantPerm.target, grantPerm.action));

            } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException se) {
                logger.error(String.format(
                        "Not able to create the permission[%s] Target[%s] Action[%s]: [%s]:[%s]",
                        grantPerm.permission, grantPerm.target, grantPerm.action, se.toString(), se.getMessage()));

            }
        }

        return new ModulePermissions(permissions, permMessage.hash.moduleJarHash, permMessage.hash.hashAlgorithm);
    }
}
