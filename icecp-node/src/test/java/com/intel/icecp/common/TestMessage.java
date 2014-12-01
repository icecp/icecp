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

import com.intel.icecp.core.Message;
import java.util.Objects;
import java.util.Random;

/**
 * Message for use in unit tests
 *
 */
public class TestMessage implements Message {

    public String a;
    public double b;
    public int c;
    public boolean d;

    public static TestMessage build(String a, double b, int c, boolean d) {
        TestMessage instance = new TestMessage();
        instance.a = a;
        instance.b = b;
        instance.c = c;
        instance.d = d;
        return instance;
    }

    public static TestMessage buildRandom(int stringSize) {
        Random random = new Random();
        return TestMessage.build(TestHelper.generateRandomString(stringSize), random.nextDouble(), random.nextInt(), random.nextBoolean());
    }

    @Override
    public String toString() {
        return String.format("[a = %s, b = %f, c = %d, d = %s]", a, b, c, d);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 41 * hash + Objects.hashCode(this.a);
        hash = 41 * hash + (int) (Double.doubleToLongBits(this.b) ^ (Double.doubleToLongBits(this.b) >>> 32));
        hash = 41 * hash + this.c;
        hash = 41 * hash + (this.d ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TestMessage other = (TestMessage) obj;
        if (!Objects.equals(this.a, other.a)) {
            return false;
        }
        if (Double.doubleToLongBits(this.b) != Double.doubleToLongBits(other.b)) {
            return false;
        }
        return this.c == other.c && this.d == other.d;
    }
}
