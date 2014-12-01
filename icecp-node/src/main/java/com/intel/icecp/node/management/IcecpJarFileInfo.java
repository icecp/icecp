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
