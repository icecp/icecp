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
package com.intel.icecp.core.event.types;

import com.intel.icecp.core.Metadata;
import com.intel.icecp.core.event.Event;
import java.net.URI;

/**
 *
 */
public class ReceivedMetadataEvent extends Event {

    public static final URI TYPE = URI.create("metadata/received");
    public final String name;
    public final Metadata metadata;
    public final long byteSize;

    public ReceivedMetadataEvent(String name, Metadata metadata, long byteSize) {
        this.name = name;
        this.metadata = metadata;
        this.byteSize = byteSize;
    }

    @Override
    public URI type() {
        return TYPE;
    }
}
