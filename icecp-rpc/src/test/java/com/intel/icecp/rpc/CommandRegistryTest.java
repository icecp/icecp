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
package com.intel.icecp.rpc;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.*;

/**
 * Test command registry
 *
 */
public class CommandRegistryTest {

    private CommandRegistry instance;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() {
        instance = new CommandRegistry();
    }

    @Test
    public void testCommandLifecycle() throws Exception {
        Command command = new FakeClass().toCommand();

        assertEquals(0, instance.size());
        instance.add(command);
        assertEquals(1, instance.size());

        CommandRequest request = CommandRequest.fromWithoutResponse(command.name(), new FakeDataStructure());
        CommandResponse response = instance.execute(request);
        assertNull(response.out);
        assertTrue(response instanceof EmptyCommandResponse);
        
        instance.remove(command.name());
        assertEquals(0, instance.size());
    }

    @Test
    public void testListing() throws NoSuchMethodException {
        assertEquals(0, instance.list().size());
        Command command = new FakeClass().toCommand();
        instance.add(command);
        assertEquals(1, instance.list().size());
    }

    @Test
    public void throwWhenCommandNotFoundTest() throws NoSuchMethodException, RpcServiceException {
        Command command = new FakeClass().toCommand();
        assertEquals(0, instance.size());

        CommandRequest request = CommandRequest.fromWithoutResponse(command.name(), new FakeDataStructure());
        exception.expect(IllegalArgumentException.class);
        instance.execute(request);
    }
}
