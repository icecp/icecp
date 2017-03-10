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

import java.net.URI;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Test ChannelPermission
 *
 */
public class ChannelPermissionTest {

    @Test
    public void testConstruction() {
        ChannelPermission p1 = new ChannelPermission("/test/name", "open");
        ChannelPermission p2 = new ChannelPermission(URI.create("ndn:/another/name"), "open,close");
    }

    @Test
    public void testValidActions() {
        ChannelPermission p1 = new ChannelPermission("/test/name", "open");
        assertArrayEquals(new String[]{"open", "publish", "subscribe", "close"}, p1.getValidActions());
    }

    @Test
    public void testImplies() {
        // see BasePermissionTest for more
        ChannelPermission p1 = new ChannelPermission("ndn:/test/name", "open");
        ChannelPermission p2 = new ChannelPermission(URI.create("ndn:/test/name"), "open,close");
        assertTrue(p2.implies(p1));
        assertFalse(p1.implies(p2));
    }
}
