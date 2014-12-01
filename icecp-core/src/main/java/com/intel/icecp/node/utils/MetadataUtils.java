/*
 * ******************************************************************************
 *
 * INTEL CONFIDENTIAL
 *
 * Copyright 2013 - 2016 Intel Corporation All Rights Reserved.
 *
 * The source code contained or described herein and all documents related to
 * the source code ("Material") are owned by Intel Corporation or its suppliers
 * or licensors. Title to the Material remains with Intel Corporation or its
 * suppliers and licensors. The Material contains trade secrets and proprietary
 * and confidential information of Intel or its suppliers and licensors. The
 * Material is protected by worldwide copyright and trade secret laws and treaty
 * provisions. No part of the Material may be used, copied, reproduced,
 * modified, published, uploaded, posted, transmitted, distributed, or disclosed
 * in any way without Intel's prior express written permission.
 *
 * No license under any patent, copyright, trade secret or other intellectual
 * property right is granted to or conferred upon you by disclosure or delivery
 * of the Materials, either expressly, by implication, inducement, estoppel or
 * otherwise. Any license under such intellectual property rights must be
 * express and approved by Intel in writing.
 *
 * Unless otherwise agreed by Intel in writing, you may not remove or alter this
 * notice or any other notice embedded in Materials by Intel or Intel's
 * suppliers or licensors in any way.
 *
 * ******************************************************************************
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
