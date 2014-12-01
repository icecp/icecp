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

/**
 * Test helper for counting accesses to asynchronous calls; in callbacks, any
 * outside variables must be final. For example:
 * <pre><code>
 * final TestCounter c = new TestCounter();
 * face.expressInterest(interest, new OnData(){
 *	@Override
 *  public void onData(Interest interest, Data data){
 *		c.count++;
 *  }
 * }, null);
 * </code></pre>
 *
 */
public class TestCounter {

    volatile public int count = 0;

    /**
     * Wait for the counter to increment to the desired number or until the
     * specified time period ends
     *
     * @param toCount desired number to increment to
     * @param milliseconds the maximum period to wait for
     * @throws InterruptedException
     */
    public void waitAtMost(int toCount, int milliseconds) throws InterruptedException {
        long endTime = System.currentTimeMillis() + milliseconds;
        while (System.currentTimeMillis() < endTime) {
            if (count >= toCount) {
                break;
            } else {
                Thread.sleep(30);
            }
        }
    }
}
