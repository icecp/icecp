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
package com.intel.icecp.node.management;

import java.util.Arrays;

/**
 * Data structure representing a JAR
 *
 */
public class IcecpJarFileInfo {
    public MavenProject mavenProject;
    public byte[] jarBytes;

    public IcecpJarFileInfo() {
        // empty public constructor for serialization
    }

    public IcecpJarFileInfo(MavenProject mavenProject, byte[] jarBytes) {
        this.mavenProject = mavenProject;
        this.jarBytes = jarBytes;
    }

    @Override
    public String toString() {
        return "IcecpJarFileInfo{mavenProject=" + mavenProject + ", numBytes=" +
                (jarBytes != null ? jarBytes.length : "null") + '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final IcecpJarFileInfo that = (IcecpJarFileInfo) o;

        if (!mavenProject.equals(that.mavenProject))
            return false;
        return Arrays.equals(jarBytes, that.jarBytes);

    }

    @Override
    public int hashCode() {
        int result = mavenProject.hashCode();
        result = 31 * result + Arrays.hashCode(jarBytes);
        return result;
    }
}
