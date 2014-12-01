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

import java.nio.ByteBuffer;

/**
 */
public class TestState implements State {

    public final String type;
    public final int id;

    public TestState(String type, int id) {
        this.type = type;
        this.id = id;
    }

    public TestState(int id) {
        this("default", id);
    }

    @Override
    public byte[] toBytes() {
        return ByteBuffer.allocate(4).putInt(id).array();
    }

    @Override
    public int compareTo(State that) {
        if (that instanceof TestState) {
            return this.id - ((TestState) that).id;
        }
        return -1;
    }

    @Override
    public boolean matches(State other) {
        return other instanceof TestState && this.type.equals(((TestState) other).type);
    }

    @Override
    public String toString() {
        return "TestState{" +
                "type='" + type + '\'' +
                ", id=" + id +
                '}';
    }
}
