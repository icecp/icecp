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

import java.security.Permission;

/**
 * Utility for checking security permissions.
 *
 */
public class SecurityUtils {

    /**
     * Check if a permission is valid if a {@link SecurityManager} exists; if it
     * doesn't, this method assumes that all code has all privilege.
     *
     * @param permission the permission to check
     * @throws java.security.AccessControlException if the current context does
     * not allow the the given permission
     */
    public static void checkPermission(Permission permission) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(permission);
        }
    }
}
