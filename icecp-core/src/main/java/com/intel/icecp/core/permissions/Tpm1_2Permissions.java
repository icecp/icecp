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
 * Permissions that regulate access to TPM 1.2
 *
 */
public class Tpm1_2Permissions extends BasePermission {

    // Actions
    public static final String SEAL = "seal";
    public static final String UNSEAL = "unseal";
    public static final String CERTIFY = "certify";
    public static final String ATTEST = "attest";

    private static final String[] VALID_ACTIONS = new String[]{SEAL, UNSEAL, CERTIFY, ATTEST};

    public Tpm1_2Permissions(String actions) {
        super(actions);
    }

    public Tpm1_2Permissions(String name, String actions) {
        super(name, actions);
    }

    @Override
    public String[] getValidActions() {
        return VALID_ACTIONS;
    }

}
