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
package com.intel.icecp.core.metadata.formats;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import net.named_data.jndn.Name;

import java.io.IOException;

/**
 * NDN Name serializer for Jackson
 *
 * @deprecated this is NDN-specific and should be removed as a part of 0.11.*; TODO remove this
 */
class NdnNameSerializer extends JsonSerializer<Name> {

    @Override
    public void serialize(Name value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {
        jgen.writeString(value.toUri());
    }
}