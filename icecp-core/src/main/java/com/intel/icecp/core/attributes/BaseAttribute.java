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
