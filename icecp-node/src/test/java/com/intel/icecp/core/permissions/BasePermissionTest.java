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
package com.intel.icecp.core.permissions;

import java.io.FilePermission;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test BasePermission
 *
 */
public class BasePermissionTest {

    public static final String ACTIONS = "a,c";
    public static final String NAME = "/test/name";
    BasePermission instance;

    public BasePermissionTest() {
        instance = new BasePermissionImpl(NAME, ACTIONS);
    }

    @Test
    public void testGetValidActions() {
        assertNotNull(instance.getValidActions());
        assertTrue(instance.getValidActions().length > 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidActionsException() {
        BasePermission x = new BasePermissionImpl(NAME, "d");
    }

    @Test
    public void testGetActions() {
        assertEquals(ACTIONS, instance.getActions());
    }

    @Test
    public void testImpliesOtherPermission() {
        assertFalse(new BasePermissionImpl(NAME, "a,b").implies(new FilePermission(NAME, "read")));
    }

    @Test
    public void testImpliesActions() {
        assertTrue(new BasePermissionImpl(NAME, "a,b,c").implies(instance));
        assertTrue(new BasePermissionImpl(NAME, "c,b,a").implies(instance));
        assertTrue(new BasePermissionImpl(NAME, "a,c").implies(instance));
        assertTrue(new BasePermissionImpl(NAME, "*").implies(instance));

        assertFalse(new BasePermissionImpl(NAME, "a,b").implies(instance));
        assertFalse(new BasePermissionImpl(NAME, "a,*").implies(instance));
    }

    @Test
    public void testImpliesNames() {
        assertTrue(new BasePermissionImpl(NAME, ACTIONS).implies(instance));
        assertTrue(new BasePermissionImpl("/test/*", ACTIONS).implies(instance));
        assertTrue(new BasePermissionImpl("*", ACTIONS).implies(instance));
        assertTrue(new BasePermissionImpl("/test/name*", ACTIONS).implies(instance));

        assertFalse(new BasePermissionImpl("...", ACTIONS).implies(instance));
        assertFalse(new BasePermissionImpl("/test/name1", ACTIONS).implies(instance));
        assertFalse(new BasePermissionImpl("/test/name/", ACTIONS).implies(instance));
    }

    @Test
    public void testEquals() {
        assertTrue(instance.equals(new BasePermissionImpl(NAME, ACTIONS)));
        assertTrue(instance.equals(instance));
    }

    @Test
    public void testNotEquals() {
        assertFalse(instance.equals(new BasePermissionImpl("*", "*")));
        assertFalse(instance.equals(new FilePermission("/some/name", "read")));
    }

    @Test
    public void testHelperConstructor() {
        assertTrue((new BasePermissionImpl("c")).equals(new BasePermissionImpl("*", "c")));
    }

    @Test
    public void testHashCode() {
        assertEquals(instance.hashCode(), new BasePermissionImpl(NAME, ACTIONS).hashCode());
        assertNotSame(instance.hashCode(), new BasePermissionImpl("/", "a,c").hashCode());
    }

    public class BasePermissionImpl extends BasePermission {

        public BasePermissionImpl(String name, String actions) {
            super(name, actions);
        }

        private BasePermissionImpl(String actions) {
            super(actions);
        }

        public String[] getValidActions() {
            return new String[]{"a", "b", "c"};
        }
    }

}
