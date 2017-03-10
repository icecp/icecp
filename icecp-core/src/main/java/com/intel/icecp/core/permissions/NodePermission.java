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
 * Control access to the device; each permission will have an action in
 * <code>[network, read, write]</code>. The current implementation also allows
 * using wildcards (i.e. *) to match either any device name (e.g. "*") or to
 * match the device name prefix (e.g. "/intel/device/*"). For example, to check
 * read and write access to /my/device run something like:
 *
 * <pre><code>
 * Permission permission = new NodePermission("/my/device", "read,write");
 * SecurityManager sm = System.getSecurityManager();
 * if (sm != null) {
 * sm.checkPermission(permission);
 * }
 * </code></pre>
 *
 */
public class NodePermission extends BasePermission {

    public static final String[] VALID_ACTIONS = new String[]{"start", "stop", "event-loop", "network", "read", "write"};

    /**
     * Constructor. E.g. new NodePermission("read").
     *
     * @param actions the actions to check for access, in canonical order
     */
    public NodePermission(String actions) {
        super(actions);
    }

    /**
     * Constructor. E.g. new NodePermissions("/my/device/name", "read,write");
     *
     * @param name the unique name of the device
     * @param actions the actions to check for access, in canonical order
     */
    public NodePermission(String name, String actions) {
        super(name, actions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getValidActions() {
        return VALID_ACTIONS;
    }
}
