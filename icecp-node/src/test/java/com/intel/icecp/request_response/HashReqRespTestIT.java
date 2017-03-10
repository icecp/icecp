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

package com.intel.icecp.request_response;

import com.intel.icecp.common.TestHelper;
import com.intel.icecp.core.Node;
import com.intel.icecp.node.NodeFactory;
import com.intel.icecp.node.messages.NodeMgmtAPIMessage;
import com.intel.icecp.node.utils.ChannelUtils;
import com.intel.icecp.node.utils.ConfigurationUtils;
import com.intel.icecp.request_response.impl.HashRequestor;
import com.intel.icecp.request_response.impl.HashResponder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URI;

import static com.intel.icecp.node.messages.NodeMgmtAPIMessage.COMMAND.GET_ALL_CHANNELS;
import static com.intel.icecp.node.messages.NodeMgmtAPIMessage.COMMAND.GET_ALL_FEATURE_STATUSES;
import static org.junit.Assert.assertEquals;

public class HashReqRespTestIT {

    private static final Logger logger = LogManager.getLogger();
    private Node node;
    private HashRequestor<NodeMgmtAPIMessage, NodeMgmtAPIMessage> hashRequestor;
    private HashResponder<NodeMgmtAPIMessage, NodeMgmtAPIMessage> hashResponder;

    @Before
    public void setup() throws Exception {
        node = NodeFactory.buildDefaultNode("/test/node/req-res", ConfigurationUtils.getConfigurationPath(), ConfigurationUtils.getPermissionsPath());
    }

    @Ignore
    @Test
    public void testHashRequestResponse() throws Exception {
        hashRequestor = new HashRequestor<>(node.channels(), NodeMgmtAPIMessage.class, NodeMgmtAPIMessage.class);
        hashResponder = new HashResponder<>(node.channels(), NodeMgmtAPIMessage.class, NodeMgmtAPIMessage.class);

        URI uri = ChannelUtils.join(node.getDefaultUri(), "hash-test", TestHelper.generateRandomString(10));
        logger.info("Using URI: {}", uri);

        hashResponder.listen(uri, request -> {
            logger.info("Received request: {}", request);
            return new NodeMgmtAPIMessage(request.command, "OK");
        });

        Thread.sleep(1000);

        NodeMgmtAPIMessage response1 = hashRequestor.request(uri, new NodeMgmtAPIMessage(GET_ALL_CHANNELS, "NO")).get();
        logger.info("Received response: {}", response1);
        assertEquals("OK", response1.returnStatus);

        Thread.sleep(1000);

        NodeMgmtAPIMessage response2 = hashRequestor.request(uri, new NodeMgmtAPIMessage(GET_ALL_FEATURE_STATUSES, "NO")).get();
        logger.info("Received response: {}", response2);
        assertEquals("OK", response2.returnStatus);
    }
}
