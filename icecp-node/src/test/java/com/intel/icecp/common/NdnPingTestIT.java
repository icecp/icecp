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
