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

package com.intel.icecp.node.management;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;

import org.junit.Test;

import com.intel.jndn.utils.Client;
import com.intel.jndn.utils.client.impl.AdvancedClient;

public class SegmentedTestIT {

    /**
     * See if you can get a jar file from the dependency server. If its not
     * running, the test will timeout and not fail.
     *
     */
    @Test
    public void testSegmentation() {

        Client client = AdvancedClient.getDefault();

        Face face = new Face("ndn-lab2.jf.intel.com");
        Interest interest = new Interest(new Name("/intel/dependency-server/commons-collections:commons-collections:jar:3.2"));
        interest.setMustBeFresh(true);
        interest.setChildSelector(Interest.CHILD_SELECTOR_RIGHT);
        interest.setInterestLifetimeMilliseconds(2000);
        CompletableFuture<Data> future = client.getAsync(face, interest);
        int p = 0;
        System.out.println("Wait for request...");
        while (!future.isDone()) {

            try {
                face.processEvents();
            } catch (IOException ioe) {
                fail("IOException: " + ioe.getMessage());
            } catch (EncodingException ene) {
                fail("Encoding Exception: " + ene.getMessage());
            }
//				System.out.println("LoopCount: " + p);
//				p++;
        }

        try {
            System.out.println("Loop Finished, see if we received any content.");
            int count = future.get().getContent().size();
            System.out.println("Yes, we received some content, size=" + count);
        } catch (InterruptedException ie) {
            fail("InterruptedException: " + ie.getMessage());
        } catch (ExecutionException ee) {
            System.out.println("Timed out, dependency Server is probably down.  Error: " + ee.getMessage());
        }
    }

}
