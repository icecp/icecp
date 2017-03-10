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
 * Permissions regulating access to keys and certificates
 *
 */
public class KeyManagementPermissions extends BasePermission {

    public static final String CREATE = "create";
    public static final String READ = "read";
    public static final String UPDATE = "update";
    public static final String DELETE = "delete";

    public static final String[] VALID_ACTIONS = new String[]{CREATE, READ, UPDATE, CREATE};

    public KeyManagementPermissions(String actions) {
        super(actions);
    }

    public KeyManagementPermissions(String name, String actions) {
        super(name, actions);
    }

    @Override
    public String[] getValidActions() {
        return VALID_ACTIONS;
    }

}
