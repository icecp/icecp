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
