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
package com.intel.icecp.node.utils;

import com.intel.icecp.core.Metadata;
import com.intel.icecp.core.metadata.Format;
import com.intel.icecp.core.metadata.formats.JsonFormat;

/**
 * Helper for publishing/subscribing to Metadata over the network
 *
 */
public class MetadataUtils {

    /**
     * @param m a {@link Metadata} instance
     * @return the human-readable, normalized, lower-cased name of the metadata
     */
    public static String toName(Metadata m) {
        return toName(m.getClass());
    }

    /**
     * @param <T> the {@link Metadata} type
     * @param type the type of a {@link Metadata} instance
     * @return the human-readable, normalized, lower-cased name of the metadata
     */
    public static <T extends Metadata> String toName(Class<T> type) {
        return type.getSimpleName().toLowerCase();
    }

    /**
     * @param <T> the {@link Metadata} type
     * @param type the type of a {@link Metadata} instance
     * @return the default format to use for serializing {@link Metadata}
     */
    public static <T extends Metadata> Format getDefaultFormat(Class<T> type) {
        return new JsonFormat(type);
    }

    /**
     * Retrieve a specific metadata type from a passed list of Metadatas.
     *
     * @param <T>
     * @param type the type (i.e. interface) to search for
     * @param metadata the list of metadata objects to search in
     * @return the found metadata object or null if not found
     */
    public static <T extends Metadata> T find(Class<T> type, Metadata[] metadata) {
        for (Metadata m : metadata) {
            if (type.isAssignableFrom(m.getClass())) {
                return (T) m;
            }
        }
        return null;
    }

    /**
     * Retrieve a specific metadata type from a passed list of Metadatas; if not
     * found, the default instance is returned.
     *
     * @param <T>
     * @param type
     * @param metadata
     * @param defaultInstance
     * @return
     */
    public static <T extends Metadata> T findOrDefault(Class<T> type, Metadata[] metadata, T defaultInstance) {
        T found = find(type, metadata);
        return found == null ? defaultInstance : found;
    }
}
