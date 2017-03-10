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
 * Test NodePermission
 *
 */
public class NodePermissionTest {

    @Test
    public void testConstruction() {
        NodePermission p1 = new NodePermission("/test/name", "network");
        NodePermission p2 = new NodePermission("start,stop"); // any node
    }

    @Test
    public void testValidActions() {
        NodePermission p1 = new NodePermission("/test/name", "network");
        assertArrayEquals(new String[]{"start", "stop", "event-loop", "network", "read", "write"}, p1.getValidActions());
    }

    @Test
    public void testImplies() {
        // see BasePermissionTest for more
        NodePermission p1 = new NodePermission("test-name", "start");
        NodePermission p2 = new NodePermission("test-name", "start,stop");
        assertTrue(p2.implies(p1));
        assertFalse(p1.implies(p2));
    }
}
