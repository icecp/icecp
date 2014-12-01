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

/**
 * Helper methods for determining the current code version
 *
 */
public class VersionUtils {

    private static String version = null; // cache the retrieval

    /**
     * @return the current version of the ICECP implementation; relies on
     * MANIFEST.MF entries set according to
     * https://docs.oracle.com/javase/8/docs/technotes/guides/versioning/spec/versioning2.html#wp89936
     */
    public static String retrieveCurrentVersion() {
        if (version == null) {
            version = VersionUtils.class.getPackage().getImplementationVersion();
        }
        return version;
    }
}
