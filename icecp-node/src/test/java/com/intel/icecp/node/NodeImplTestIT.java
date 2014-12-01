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

package com.intel.icecp.node;

import com.intel.icecp.core.Node;
import com.intel.icecp.node.utils.ConfigurationUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 */
public class NodeImplTestIT {

    @Ignore
    @Test
    public void testCreatingANode() throws Exception {
        Node node = NodeFactory.buildDefaultNode("/test/node", ConfigurationUtils.getConfigurationPath(), ConfigurationUtils.getPermissionsPath());
        node.start();
        assertEquals(Node.State.ON, node.getStateChannel().latest().get(10, TimeUnit.SECONDS));
    }
}