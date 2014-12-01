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
