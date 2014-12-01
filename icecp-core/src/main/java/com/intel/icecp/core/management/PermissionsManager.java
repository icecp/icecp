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

package com.intel.icecp.core.management;

/**
 * Maps module names to their permission file.
 *
 */
public interface PermissionsManager {

    /**
     * Find the permission object for the specified module
     *
     * @param moduleName the name of the module
     * @return a permissions object with the permissions for the module
     * @throws PermissionLoadException if the permissions cannot be retrieved
     */
    ModulePermissions retrievePermissions(String moduleName) throws PermissionLoadException;
}
