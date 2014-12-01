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
