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

package com.intel.icecp.core.attributes;

import com.intel.icecp.core.permissions.BasePermission;

/**
 * Controls use of a {@link com.intel.icecp.core.Attribute}; allowed actions are: {@code[add, remove, read, write]}. The
 * canonical order of these is alphabetical, as just shown. The current implementation also allows using wildcards (i.e.
 * *) to match either any attribute name or any action. To check if the current context can access an attribute, try:
 * <p>
 * <pre><code>
 * Permission permission = new AttributePermission(fullAttributeUri.toString(), "read");
 * SecurityManager sm = System.getSecurityManager();
 * if (sm != null) {
 *  sm.checkPermission(permission);
 * }
 * </code></pre>
 *
 */
public class AttributePermission extends BasePermission {

    public static final String[] VALID_ACTIONS = new String[]{"add", "remove", "read", "write"};

    /**
     * @param name the full {@link com.intel.icecp.core.Attribute} name; e.g. /parent/child/[attribute-name]
     * @param actions the actions to check for access, comma-delimited
     */
    public AttributePermission(String name, String actions) {
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