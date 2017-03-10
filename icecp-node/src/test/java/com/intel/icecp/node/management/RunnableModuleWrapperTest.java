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

package com.intel.icecp.node.management;

import com.intel.icecp.common.TestAttributes;
import com.intel.icecp.core.Module;
import com.intel.icecp.core.Node;
import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.attributes.IdAttribute;
import com.intel.icecp.core.attributes.ModuleStateAttribute;
import com.intel.icecp.core.attributes.NameAttribute;
import com.intel.icecp.core.misc.ConfigurationAttributeAdapter;
import com.intel.icecp.core.modules.ModuleInstance;
import com.intel.icecp.node.AttributesFactory;
import com.intel.icecp.node.NodeFactory;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test RunnableModuleWrapper; TODO simplify setup with better constructors/builders
 *
 */
public class RunnableModuleWrapperTest {

    private ConfigurationAttributeAdapter configurationAttributeAdapter;
    private ModuleInstance moduleInstance;
    private CompletableFuture<Long> future;
    private RunnableModuleWrapper wrapper;

    @Before
    public void beforeTest() throws Exception {
        Module module = mock(Module.class);
        Node node = NodeFactory.buildMockNode();
        Attributes attributes = TestAttributes.defaultModuleAttributes("test-helper", 22, Module.State.LOADED);
        moduleInstance = new ModuleInstance(module, attributes);
        future = new CompletableFuture<>();
        wrapper = new RunnableModuleWrapper(node, moduleInstance, future);
    }

    @Test
    public void testSuccessfulRun() throws Exception {
        wrapper.run();

        assertTrue(future.isDone() && !future.isCompletedExceptionally());
        assertEquals(22, (long) future.get());
        verify(moduleInstance.module(), times(1)).run(any(), any());
    }

    @Test
    public void testExceptionalRun() throws Exception {
        doThrow(RuntimeException.class).when(moduleInstance.module()).run(any(), any());

        wrapper.run();

        assertTrue(future.isDone() && !future.isCompletedExceptionally());
        verify(moduleInstance.module(), times(1)).run(any(), any());
        assertEquals(Module.State.ERROR, moduleInstance.state());
    }
}