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
