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
