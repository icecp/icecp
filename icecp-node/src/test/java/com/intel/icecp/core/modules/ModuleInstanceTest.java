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