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
package com.intel.icecp.node.network.ndn;

import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test NdnNetworkEndpoint
 *
 */
public class NdnNetworkEndpointTest {

    NdnNetworkEndpoint instance;

    public NdnNetworkEndpointTest() {
        instance = new NdnNetworkEndpoint(new Name("/test/name"), "tcp4", "10.1.1.1", 6363);
    }

    @Test
    public void testToUri() {
        assertEquals("tcp4://10.1.1.1:6363", instance.toUri().toString());
    }

    @Test
    public void testWireEncodeAndDecode() throws Exception {
        Blob blob = instance.wireEncode();
        NdnNetworkEndpoint decoded = NdnNetworkEndpoint.wireDecode(blob.buf());
        assertEquals(instance.toUri(), decoded.toUri());
    }
}
