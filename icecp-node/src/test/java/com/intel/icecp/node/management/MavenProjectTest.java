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
