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
 * Controls use of a {@link com.intel.icecp.core.Module}; allowed actions are:
 * [start, access, stop]. The canonical order of these is alphabetical, as just
 * shown. The current implementation also allows using wildcards (i.e. *) to
 * match either any module name or any action. To check if the current context
 * can access a a module, try:
 *
 * <pre><code>
 * Permission permission = new ModulePermission("my-module", "access");
 * SecurityManager sm = System.getSecurityManager();
 * if (sm != null) {
 * sm.checkPermission(permission);
 * }
 * </code></pre>
 *
 */
public class ModulePermission extends BasePermission {

    public static final String[] VALID_ACTIONS = new String[]{"start", "access", "stop"};

    /**
     * Constructor
     *
     * @param name the {@link com.intel.icecp.core.Module} name
     * @param actions the actions to check for access, comma-delimited
     */
    public ModulePermission(String name, String actions) {
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
