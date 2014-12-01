/*
 * ******************************************************************************
 *
 * INTEL CONFIDENTIAL
 *
 * Copyright 2013 - 2016 Intel Corporation All Rights Reserved.
 *
 * The source code contained or described herein and all documents related to
 * the source code ("Material") are owned by Intel Corporation or its suppliers
 * or licensors. Title to the Material remains with Intel Corporation or its
 * suppliers and licensors. The Material contains trade secrets and proprietary
 * and confidential information of Intel or its suppliers and licensors. The
 * Material is protected by worldwide copyright and trade secret laws and treaty
 * provisions. No part of the Material may be used, copied, reproduced,
 * modified, published, uploaded, posted, transmitted, distributed, or disclosed
 * in any way without Intel's prior express written permission.
 *
 * No license under any patent, copyright, trade secret or other intellectual
 * property right is granted to or conferred upon you by disclosure or delivery
 * of the Materials, either expressly, by implication, inducement, estoppel or
 * otherwise. Any license under such intellectual property rights must be
 * express and approved by Intel in writing.
 *
 * Unless otherwise agreed by Intel in writing, you may not remove or alter this
 * notice or any other notice embedded in Materials by Intel or Intel's
 * suppliers or licensors in any way.
 *
 * ******************************************************************************
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
