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