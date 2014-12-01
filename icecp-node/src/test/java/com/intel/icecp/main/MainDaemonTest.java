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

package com.intel.icecp.main;

import com.intel.icecp.core.Node;
import org.junit.Test;

import java.net.UnknownHostException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 */
public class MainDaemonTest {

    @Test
    public void testHasHelpOption(){
        assertFalse(MainDaemon.hasHelpOption(new String[]{"a", "b", "c"}));
        assertTrue(MainDaemon.hasHelpOption(new String[]{"-h"}));
        assertTrue(MainDaemon.hasHelpOption(new String[]{"a", "b", "--help"}));
    }

    @Test
    public void testParseModules(){
        List<ModuleParameter> moduleParameters = MainDaemon.parseModules(new String[]{"a.jar", "b.jar", "-DsomeProperty"});
        assertEquals(2, moduleParameters.size());
    }

    @Test
    public void testApplyCommandLineArguments(){
        Node node = mock(Node.class);

        MainDaemon.applyCommandLineArguments(node, new String[]{"a.jar", "b.jar"});

        verify(node, times(2)).loadAndStartModules(any(), any());
    }

    @Test
    public void testGenerateNodeName() throws UnknownHostException {
        String nodeName = MainDaemon.generateNodeName();

        assertTrue(nodeName.startsWith("/intel"));
    }

    @Test
    public void tesShowHelpString() throws UnknownHostException {
        String helpString = MainDaemon.showHelpOptions();

        assertTrue(!helpString.isEmpty());
    }
}