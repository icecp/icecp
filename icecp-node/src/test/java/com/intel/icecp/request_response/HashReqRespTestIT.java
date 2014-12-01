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
