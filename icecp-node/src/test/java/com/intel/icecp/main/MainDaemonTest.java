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