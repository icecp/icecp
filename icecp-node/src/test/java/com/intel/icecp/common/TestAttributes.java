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
package com.intel.icecp.common;

import com.intel.icecp.core.Module;
import com.intel.icecp.core.Node;
import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.attributes.IdAttribute;
import com.intel.icecp.core.attributes.ModuleStateAttribute;
import com.intel.icecp.core.attributes.NameAttribute;
import com.intel.icecp.node.AttributesFactory;
import com.intel.icecp.node.NodeFactory;

import java.net.URI;

/**
 */
public class TestAttributes {
    public static Attributes defaultModuleAttributes(String name, long id, Module.State state) {
        try {
            Node node = NodeFactory.buildMockNode();
            Attributes attributes = AttributesFactory.buildEmptyAttributes(node.channels(), URI.create("icecp:/attributes/test"));
            attributes.add(new IdAttribute(id));
            attributes.add(new NameAttribute(name));
            attributes.add(new ModuleStateAttribute());
            attributes.set(ModuleStateAttribute.class, state);
            return attributes;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create default attributes.", e);
        }
    }
}
