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
package com.intel.icecp.core;

/**
 * Describes a {@link Describable} thing using a key-value pair; the key is a unique string (i.e. unique to that thing)
 * and the value is of the generic type passed to the instance (and discoverable using {@link #type()}). These
 * attributes may be serialized and published over {@link Channel}s; additionally, we may provide a way for setting
 * attributes across the network
 * <p>
 * In the future, we may add a method for retrieving the semantic model that this attribute belongs to (TODO). Currently
 * the {@link #type()} exposes the value type but using a semantic model could replace this with a string.
 * <p>
 * Note that the value type exposed by this attribute may (and probably will) be serialized for transport over the
 * network. Please pay special attention to common serialization gotchas like field accesibility, default constructors,
 * public static inner classes, anonymous classes--using these mechanisms for the value type (see MyValue below) may
 * result in errors. Simple, primitive types are encouraged (e.g. Integer, String, Map)
 * <p>
 * An attribute implementation would look like:
 * <pre>{@code
 *      class MyAttribute implements Attribute<MyValue>{
 *          public MyAttribute(){
 *              // note the presence of a default constructor
 *          }
 *
 *          public MyValue value(){
 *              MyValue value = someCalculation();
 *              return value;
 *          }
 *      }
 * }</pre>
 *
 * @param <T> attributes are of a type that can be sent as a message; beware of using nested generic types here as this
 * type must be fully serializable for network transport
 */
public interface Attribute<T> {

    /**
     * @return the name of the attribute
     */
    String name();

    /**
     * @return the {@link Class} of the value type held in this attribute
     */
    Class<T> type();

    /**
     * Note that returning a reference to an object held inside the attribute implementation means that external users
     * can modify inside the implementation; to avoid this, attribute implementations should return new instances of the
     * returned value, e.g. {@code return new String(value)}. Alternatively, implementations can implement a form of
     * cloning in order to protect the original reference. Most applications, however, will likely return the (new)
     * result of some calculation and this should not be an issue.
     *
     * @return the value of the attribute at the current time
     */
    T value();
}
