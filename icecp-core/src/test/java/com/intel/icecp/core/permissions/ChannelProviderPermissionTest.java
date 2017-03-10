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

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 */
public class ChannelProviderPermissionTest {
    @Test
    public void testConstruction() {
        ChannelProviderPermission p1 = new ChannelProviderPermission("ndn", ChannelProviderPermission.Action.REGISTER.toString());
        ChannelProviderPermission p2 = new ChannelProviderPermission("ndn", ChannelProviderPermission.Action.REGISTER.toString()
                + "," + ChannelProviderPermission.Action.UNREGISTER.toString());
    }

    @Test
    public void testValidActions() {
        ChannelProviderPermission p1 = new ChannelProviderPermission("file", ChannelProviderPermission.Action.REGISTER.toString());
        assertArrayEquals(getChannelProviderPermissionActionNames(), p1.getValidActions());
    }

    @Test
    public void testImplies() {
        // see BasePermissionTest for more
        ChannelProviderPermission p1 = new ChannelProviderPermission("ndn", ChannelProviderPermission.Action.REGISTER.toString());
        ChannelProviderPermission p2 = new ChannelProviderPermission("ndn", ChannelProviderPermission.Action.REGISTER.toString()
                + "," + ChannelProviderPermission.Action.UNREGISTER.toString());
        assertTrue(p2.implies(p1));
        assertFalse(p1.implies(p2));
    }

    private static String[] getChannelProviderPermissionActionNames() {
        return Arrays.stream(ChannelProviderPermission.Action.class.getEnumConstants()).map(Enum::toString).toArray(String[]::new);
    }
}
