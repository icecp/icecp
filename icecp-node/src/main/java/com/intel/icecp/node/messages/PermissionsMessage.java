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
package com.intel.icecp.node.messages;

import com.intel.icecp.core.Message;
import java.util.ArrayList;
import java.util.List;

/**
 * Represent a list of feature permissions
 *
 */
public class PermissionsMessage implements Message {

    public String name;
    public List<Grant> grants = new ArrayList<>();

    // @Moreno: this represents a hash of the JAR (to connect the permissions to the module).
    public ModuleHash hash;

    public static class Grant {

        public String permission;
        public String target;
        public String action;
    }

    public static class ModuleHash {
        // Hash value of module's JAR file 

        public byte[] moduleJarHash;
        // Algorithm used to produce the hash
        public String hashAlgorithm;
    }

}
