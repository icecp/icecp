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

package com.intel.icecp.core.modules;

import com.intel.icecp.common.TestAttributes;
import com.intel.icecp.common.TestModule;
import com.intel.icecp.core.Module;
import com.intel.icecp.core.Node;
import com.intel.icecp.node.NodeFactory;
import com.intel.icecp.node.management.ModulesImpl;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * TODO more exception-covering tests required
 *
 */
public class ModuleInstanceTest {
    private static final String MODULE_NAME = "test-module";
    private static final long MODULE_ID = 99;
    private ModuleInstance instance;

    @Before
    public void beforeTest() throws Exception {
        instance = ModuleInstance.create(TestModule.class,
                TestAttributes.defaultModuleAttributes(MODULE_NAME, MODULE_ID, null));
    }

    @Test
    public void build() throws Exception {
        assertEquals(TestModule.class, instance.module().getClass());
    }

    @Test
    public void buildUri() throws Exception {
        Node node = NodeFactory.buildMockNode();
        assertEquals(node.getDefaultUri().toString() + "/modules/" + MODULE_ID, ModulesImpl.buildUri(node, 99).toString());
    }

    @Test
    public void name() throws Exception {
        assertEquals(MODULE_NAME, instance.name());
    }

    @Test
    public void id() throws Exception {
        assertEquals(MODULE_ID, instance.id());
    }

    @Test
    public void state() throws Exception {
        assertEquals(Module.State.INSTANTIATED, instance.state());
        instance.state(Module.State.RUNNING);
        assertEquals(Module.State.RUNNING, instance.state());
    }
}