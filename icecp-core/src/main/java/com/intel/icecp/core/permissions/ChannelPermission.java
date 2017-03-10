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

import java.net.URI;

/**
 * Controls use of a {@link com.intel.icecp.core.Channel}; allowed actions are:
 * [close, open, publish, subscribe]. The canonical order of these is
 * alphabetical, as just shown. The current implementation also allows using
 * wildcards (i.e. *) to match either any channel name (e.g. "*") or to match a
 * prefix (e.g. "/channel/*"). To check if the current context can subscribe to
 * a channel, try:
 *
 * <pre><code>
 * Permission permission = new ChannelPermission("/channel/name", "subscribe");
 * SecurityManager sm = System.getSecurityManager();
 * if (sm != null) {
 *   sm.checkPermission(permission);
 * }
 * </code></pre>
 *
 */
public class ChannelPermission extends BasePermission {

    public static final String[] VALID_ACTIONS = new String[]{"open", "publish", "subscribe", "close"};

    /**
     * Constructor
     *
     * @param name the {@link com.intel.icecp.core.Channel} name
     * @param actions the actions to check for access, comma-delimited
     */
    public ChannelPermission(String name, String actions) {
        super(name, actions);
    }

    /**
     * Constructor
     *
     * @param name the {@link com.intel.icecp.core.Channel} URI
     * @param actions the actions to check for access, in canonical order
     */
    public ChannelPermission(URI name, String actions) {
        super(name.toString(), actions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getValidActions() {
        return VALID_ACTIONS;
    }
}
