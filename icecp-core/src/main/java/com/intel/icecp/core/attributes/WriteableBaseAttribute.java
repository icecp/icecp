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
