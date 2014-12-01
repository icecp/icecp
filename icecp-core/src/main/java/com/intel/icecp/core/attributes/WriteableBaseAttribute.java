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

package com.intel.icecp.core.attributes;

/**
 * Extension for the base attribute to allow simple reading and writing; note that if the value stored is a reference
 * this class will return that reference, making the inner data modifiable across multiple users (if this is an issue
 * consider cloning {@link #value} and returning the clone).
 *
 * @param <T> the type of the value stored in this attribute; beware of using nested generics here
 */
public abstract class WriteableBaseAttribute<T> extends BaseAttribute<T> implements WriteableAttribute<T> {
    private T value;

    public WriteableBaseAttribute(String name, Class type) {
        super(name, type);
    }

    @Override
    public T value() {
        return value;
    }

    @Override
    public void value(T newValue) {
        value = newValue;
    }
}
