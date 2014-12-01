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
