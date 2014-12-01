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