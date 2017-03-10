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
 * Used for identifying a {@link com.intel.icecp.core.Describable} thing by number; e.g. a module. TODO: this should not
 * be writeable.
 */
public class IdAttribute extends WriteableBaseAttribute<Long> {
    public static final String NAME = "id";

    public IdAttribute() {
        super(NAME, Long.class);
    }

    public IdAttribute(long id) {
        this();
        value(id);
    }
}
