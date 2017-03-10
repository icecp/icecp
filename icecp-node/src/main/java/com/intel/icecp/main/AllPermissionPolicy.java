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

import java.security.AllPermission;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Policy;
import java.security.ProtectionDomain;

/**
 * Policy class to provide all permissions. Must override both getPermissions()
 * methods; if you don't override the method that takes a ProtectionDomain,
 * things will break. Also, when you call .newPermissionsCollection, it returns an
 * AllPermissionsCollection that is empty. You need to add one "AllPermission"
 * to it, in order for it to return the correct AllPermission.
 *
 */
final class AllPermissionPolicy extends Policy {

    @Override
    public PermissionCollection getPermissions(ProtectionDomain domain) {
        PermissionCollection allPerms = new AllPermission().newPermissionCollection();
        allPerms.add(new AllPermission());
        return allPerms;
    }

    @Override
    public PermissionCollection getPermissions(CodeSource codesource) {
        PermissionCollection allPerms = new AllPermission().newPermissionCollection();
        allPerms.add(new AllPermission());
        return allPerms;
    }
}
