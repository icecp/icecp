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
 * Callback for observing attribute changes
 *
 * @param <T> the value type of the attribute
 */
public interface OnAttributeChanged<T> {

    /**
     * Called when an attribute changes
     *
     * @param name the short name of the attribute; e.g. for an attribute /parent/child/[attribute-name], this parameter
     * would be [attribute-name].
     * @param oldValue the previously held value
     * @param newValue the current held value
     */
    void onAttributeChanged(String name, T oldValue, T newValue);
}
