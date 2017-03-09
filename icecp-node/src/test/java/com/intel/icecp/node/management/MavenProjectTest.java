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

import com.intel.icecp.node.management.MavenProject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;

public class MavenProjectTest {

    @Test
    public void testParseFromManifest() {
        //groupId:artifactId:packaging:classifier:version 
        String testProject = "GroupId:ArtifactId:jar::0.1";
        InputStream in = new ByteArrayInputStream(testProject.getBytes());
        MavenProject mp = MavenProject.parseFromManifest(in);
        System.out.println(String.format("From[%s] to MavenURL[%s]", testProject, mp.toUrlFragment()));
        assertEquals("GroupId/ArtifactId/0.1/ArtifactId-0.1.jar", mp.toUrlFragment());
        assertEquals("GroupId:ArtifactId:jar:0.1", mp.toMavenCoordinate());
    }

    @Test
    public void testNullInput() {
        try {
            MavenProject mp = MavenProject.parseFromManifest((String) null);
            mp = MavenProject.parseFromManifest((InputStream) null);
            fail("Expected an IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException anIllegalArgumentException) {
            assertEquals(anIllegalArgumentException.getMessage(), "Input is null");
        }
    }

    @Test
    public void testBadManifestData() {
        InputStream in = new ByteArrayInputStream("ArtifactId:jar::0.1".getBytes());
        try {
            MavenProject mp = MavenProject.parseFromManifest(in);
            fail("Expected an IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException anIllegalArgumentException) {
            assertEquals(anIllegalArgumentException.getMessage(),
                    "Bad artifact coordinates ArtifactId:jar::0.1, expected format is <groupId>:<artifactId>:<extension>:[<classifier>:]<version>");
        }
    }
}
