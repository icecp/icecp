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

package com.intel.icecp.core.metadata.formats.jbs;

import java.util.HashMap;
import java.util.Map.Entry;

public class GenericObject extends HashMap<String, Object> {

    private static final long serialVersionUID = -3947577506369305772L;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("GenericObject [");

        boolean first = true;
        for (Entry<String, Object> entry : entrySet()) {
            sb.append((first ? "" : ", ") + entry.getKey()).append('=');
            Object o = entry.getValue();
            sb.append(o.toString());
            if (first) {
                first = false;
            }
        }
        return sb.append("]").toString();
    }
}
