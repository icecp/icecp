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
