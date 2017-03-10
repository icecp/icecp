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

package com.intel.icecp.node.channels.ndn.chronosync.algorithm;

/**
 * An object representing a value in time. States are matchable (see {@link #matches(State)})
 * so that we can determine which states can be compared against each other; states are comparable so that we can
 * determine which instance is the latest/newest/greatest (and likely should be retained).
 *
 */
public interface State extends Comparable<State> {

    /**
     * Check if a state matches another state. States may change over time: e.g. the state of a counter may increment by
     * one. In this case, we want to understand that the new state instance (e.g. counter == 1) matches the previous one
     * (e.g. counter == 0) and should replace it. The same Java instance may be used for both states and this method
     * then must return true.
     *
     * @param other another state
     * @return true if the states match
     */
    boolean matches(State other);

    /**
     * @return the bytes representing this state
     */
    byte[] toBytes();
}
