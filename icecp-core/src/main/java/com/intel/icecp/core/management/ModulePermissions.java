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

import java.security.Permissions;

/**
 * Wrapper for java permissions. This class adds a hash of the module bytes to univocally bind permissions to the
 * corresponding module.
 *
 */
public class ModulePermissions {

    // The permissions, as a java.security.Permissions object
    private final Permissions permissions;
    // Bytes of the hash 
    private final byte[] moduleHash;
    // Algorithm used to compute the hash
    private final String hashAlgorithm;

    public ModulePermissions(Permissions permissions, byte[] moduleHash, String hashAlgorithm) {
        this.permissions = permissions;
        this.hashAlgorithm = hashAlgorithm;
        this.moduleHash = moduleHash;
    }

    /**
     * Getter for permissions
     *
     * @return the Java permissions object for a module
     */
    public Permissions getPermissions() {
        return permissions;
    }

    /**
     * Getter for hash algorithm
     *
     * @return the name of the hash algorithm
     */
    public String getHashAlgorithm() {
        return hashAlgorithm;
    }

    /**
     * Getter for module hash
     *
     * @return the hash value of the module
     */
    public byte[] getModuleHash() {
        return moduleHash;
    }

}
