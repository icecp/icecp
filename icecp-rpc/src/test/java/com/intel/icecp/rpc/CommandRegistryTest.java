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
