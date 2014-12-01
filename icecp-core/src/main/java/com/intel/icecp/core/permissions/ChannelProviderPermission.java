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
package com.intel.icecp.core.permissions;

/**
 *  Controls use of a {@link com.intel.icecp.core.channels.ChannelProvider}; allowed actions are:
 * [register, shutdown, unregister], defined in {@link Action}. The canonical order of these is
 * alphabetical, as just shown. To check if the current channel provider can be registered, try:
 *
 * <pre><code>
 * Permission permission = new ChannelProviderPermission("ndn", "register");
 * SecurityManager sm = System.getSecurityManager();
 * if (sm != null) {
 *   sm.checkPermission(permission);
 * }
 * </code></pre>
 *
 * @see BasePermission
 *
 */
public class ChannelProviderPermission extends BasePermission {
    /**
     * Enumeration of actions for ChannelProvider permissions.
     */
    public enum Action {
        REGISTER("register"),
        SHUTDOWN("shutdown"),
        UNREGISTER("unregister");

        private final String name;

        Action(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final String[] VALID_ACTIONS = new String[] { Action.REGISTER.name,
            Action.SHUTDOWN.name, Action.UNREGISTER.name};

    /**
     * Constructor. E.g. new ChannelProviderPermission("register").
     *
     * @param actions the actions to check for access, in canonical order
     * @throws IllegalArgumentException if one of the actions is not a valid
     * action for this class
     */
    public ChannelProviderPermission(String actions) {
        super(actions);
    }

    /**
     * Constructor. E.g. new ChannelProviderPermission("ndn", "register,shutdown");
     *
     * @param schemeName the scheme name of the channel provider
     * @param actions the actions to check for access, in canonical order
     * @throws IllegalArgumentException if one of the actions is not a valid
     * action for this class
     */
    public ChannelProviderPermission(String schemeName, String actions) {
        super(schemeName, actions);
    }

    @Override
    public String[] getValidActions() {
        return VALID_ACTIONS;
    }
}
