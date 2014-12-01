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
package com.intel.icecp.rpc;

import java.lang.reflect.Method;

/**
 * For use as an instance when testing commands
 *
 */
class FakeClass {

    public String testMethod(int a, double b, String c, int d, double hidden) {
        //convert everything into string...
        return String.valueOf(a) + String.valueOf(b) + c + String.valueOf(d) + String.valueOf(hidden);
    }

    public String testMethod(FakeDataStructure fake) {
        return fake.toString();
    }

    Command toCommand(Class<?>... parameterTypes) throws NoSuchMethodException {
        // all the input arguments here have to be primitive type int, double, or String
        Method method = FakeClass.class.getMethod("testMethod", parameterTypes);
        return new Command(this, method);
    }

    Command toCommand() throws NoSuchMethodException {
        return toCommand(FakeDataStructure.class);
    }
}
