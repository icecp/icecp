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

package com.intel.icecp.common;

import com.intel.jndn.utils.client.impl.SimpleClient;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertNotNull;

/**
 * Test NDN connection; relies on always-running NFD with localhop enabled
 *
 */
public class NdnPingTestIT {

    private static final Logger logger = LogManager.getLogger();

    /**
     * Test of getName method, of class NdnDevice.
     */
    @Ignore // TODO occasionally fails over VPN
    @Test
    public void isForwarderAccessible() throws IOException {
        final String ip = TestHelper.getNfdHostName();

        if (!TestHelper.isReachable(ip)) {
            logger.warn("Could not reach host, abandoning test: " + ip);
            return;
        }

        Face face = new Face(ip);
        Interest interest = new Interest(new Name("/localhop/nfd/rib")); // this query should return some data if NFD is running locally
        interest.setInterestLifetimeMilliseconds(4000);
        Data data = SimpleClient.getDefault().getSync(face, interest);
        assertNotNull(data);
    }
}
