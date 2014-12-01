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

package com.intel.icecp.core.attributes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.util.HashMap;

/**
 * Retrieve file storage information for the node
 *
 */
public class StorageUsageAttribute extends BaseAttribute<StorageUsageAttribute.StorageUnits> {
    private static final Logger logger = LogManager.getLogger();

    public StorageUsageAttribute() {
        super("storage-usage", StorageUnits.class);
    }

    @Override
    public StorageUnits value() {
        StorageUnits storageUnits = new StorageUnits();
        for (FileStore fileStore : FileSystems.getDefault().getFileStores()) {
            try {
                StorageInformation storageInformation = new StorageInformation(fileStore.getUsableSpace(), fileStore.getTotalSpace());
                storageUnits.put(fileStore.name(), storageInformation);
            } catch (IOException e) {
                logger.error("Failed to load file store", e);
            }
        }
        return storageUnits;
    }

    /**
     * Represent a map of storage unit names to their retrieved storage information; TODO this should likely use Josh
     * Bloch's ForwardingMap concept to avoid future incompatibilities
     */
    public static class StorageUnits extends HashMap<String, StorageInformation> {

    }

    /**
     * Expose the space metrics of a storage unit; all units are in bytes. The {@link #usagePercentage} is added for
     * ease of use
     */
    public static class StorageInformation implements Serializable {
        private static final long serialVersionUID = -8972263220509751289L;
        public final long usableSpace;
        public final long totalSpace;
        public final float usagePercentage;

        /**
         * Constructor necessary for Jackson serialization
         */
        public StorageInformation() {
            this.usagePercentage = 0;
            this.usableSpace = 0;
            this.totalSpace = 0;
        }

        public StorageInformation(long usableSpace, long totalSpace) {
            this.usableSpace = usableSpace;
            this.totalSpace = totalSpace;
            this.usagePercentage = (totalSpace - usableSpace) * 100 / (float) totalSpace;
        }

        @Override
        public String toString() {
            return "StorageInformation{" + "usableSpace=" + usableSpace + ", totalSpace=" + totalSpace +
                    ", usagePercentage=" + usagePercentage + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StorageInformation that = (StorageInformation) o;
            return usableSpace == that.usableSpace && totalSpace == that.totalSpace && Float.compare(that.usagePercentage, usagePercentage) == 0;
        }

        @Override
        public int hashCode() {
            int result = (int) (usableSpace ^ (usableSpace >>> 32));
            result = 31 * result + (int) (totalSpace ^ (totalSpace >>> 32));
            result = 31 * result + (Float.compare(usagePercentage, 0.0f) != 0 ? Float.floatToIntBits(usagePercentage) : 0);
            return result;
        }
    }
}