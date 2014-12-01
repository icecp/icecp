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
