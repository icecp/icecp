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

import com.intel.icecp.core.Attribute;

/**
 * Base class for building attributes
 *
 * @param <T> the type of the value stored in this attribute; beware of using nested generics here
 */
public abstract class BaseAttribute<T> implements Attribute<T> {

    protected final String name;
    protected final Class type;

    /**
     * Build an attribute
     *
     * @param name the new name of the attribute; this should uniquely identify the attribute vs other attributes
     * @param type the Java-specific class of the attribute; note that using a generic type will likely cause trouble,
     * beware.
     */
    public BaseAttribute(String name, Class type) {
        this.name = name;
        this.type = type;
    }

    /**
     * @return the name of the attribute
     */
    @Override
    public String name() {
        return name;
    }

    /**
     * @return the value of the attribute at the current time
     */
    @Override
    public abstract T value();

    /**
     * @return the {@link Class} of the value type held in this attribute
     */
    @Override
    public Class type() {
        return type;
    }
}
