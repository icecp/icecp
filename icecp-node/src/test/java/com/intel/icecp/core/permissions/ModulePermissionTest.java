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

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Test ModulePermission
 *
 */
public class ModulePermissionTest {

    @Test
    public void testConstruction() {
        ModulePermission p1 = new ModulePermission("feature-name", "start");
    }

    @Test
    public void testValidActions() {
        ModulePermission p1 = new ModulePermission("/test/name", "access");
        assertArrayEquals(new String[]{"start", "access", "stop"}, p1.getValidActions());
    }

    @Test
    public void testImplies() {
        // see BasePermissionTest for more
        ModulePermission p1 = new ModulePermission("test-name", "start");
        ModulePermission p2 = new ModulePermission("test-name", "start,stop");
        assertTrue(p2.implies(p1));
        assertFalse(p1.implies(p2));
    }
}
