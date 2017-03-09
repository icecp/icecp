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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a Maven project; see
 * https://maven.apache.org/pom.html#Maven_Coordinates for more information.
 *
 */
public class MavenProject {

    private String groupId;
    private String artifactId;
    private String packaging;
    private String classifier;
    private String version;
    private String mavenCoordinate;

    /**
     * Format of a project from the POM is:
     * groupId:artifictId:packaging:classifier:version For example:
     * com.intel.ndn-gateway:ndn-gateway:jar::0.3.1 The path that is built from
     * this is: groupId/artifactId/version/artifact-versionclassifier.packaging
     * For example:
     * com/intel/ndn-gateway/ndn-gateway/0.3.1/ndn-gateway-0.3.1.jar
     *
     * @param groupId
     * @param artifactId
     * @param packaging
     * @param classifier
     * @param version
     */
    public MavenProject(String groupId, String artifactId, String packaging, String classifier, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.packaging = packaging;
        this.classifier = classifier;
        this.version = version;

        //Build the Maven Coordinate and store it for the toMavenCoordinate() method
        //Remember, packaging and classifier are optional
        //<groupId>:<artifactId>[:<extension>[:<classifier>]]:<version>
        String tempPackaging = "";
        String tempClassifier = "";
        if (!this.packaging.isEmpty()) {
            tempPackaging = ":" + this.packaging;
        }
        if (!this.classifier.isEmpty()) {
            tempClassifier = ":" + this.classifier;
        }
        mavenCoordinate = String.format("%s:%s%s%s:%s",
                this.groupId, this.artifactId, tempPackaging, tempClassifier, this.version);
        //System.out.println("MavenProject Coordinates: " + mavenCoordinate);
    }

    /**
     * @return a string representing the current object
     */
    @Override
    public String toString() {
        return toJarFragment();
    }

    /**
     * Return the jar file name for local file access.
     *
     * @return A jar file name. eg: commons-collections-3.2.jar
     */
    public String toJarFragment() {
        return String.format("%s-%s%s.%s",
                artifactId, version, classifier, packaging);
    }

    /**
     * @return a URL fragment for the project; we should be able to append this
     * string to a Maven repository URL to retrieve JAR files. The groupId is
     * converted to a path (eg, com.intel.ndn to com/intel/ndn)
     */
    public String toUrlFragment() {
        return String.format("%s/%s/%s/%s-%s%s.%s",
                groupId.replace(".", "/"),
                artifactId,
                version,
                artifactId,
                version,
                classifier,
                packaging);
    }

    public String toMavenCoordinate() {
        return this.mavenCoordinate;
    }

    /**
     * Create a MavenProject from a Class-Path entry in the Jar's manifest. The
     * format of the string is expected to be:
     * groupId:artifactId:type:classifier:version For example:
     * com.intel.ndn-gateway:ndn-gateway:jar::0.3.1
     *
     * @param bytes - Input stream containing the artifact for the dependency.
     * @return MavenProject filled in from parsing the specified bytes
     * @throws IllegalArgumentException If the input bytes do not have the
     * correct format or number of parts.
     */
    public static MavenProject parseFromManifest(InputStream bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("Input is null");
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(bytes));
        StringBuilder jarArtifact = new StringBuilder();
        String line;
        try {
            while ((line = br.readLine()) != null) {
                jarArtifact.append(line);
            }
            br.close();
            bytes.close();
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    String.format("Could not read the input stream.  Error: %s", e.getMessage()));
        }

        return parseFromManifest(jarArtifact.toString());
    }

    public static MavenProject parseFromDependencyTree(InputStream bytes) {
        throw new UnsupportedOperationException("Not yet defined.");
    }

    /**
     * Create a MavenProject from specified maven artifact. The only optional
     * part of the artifact is the classifier. GroupId, ArtifactId, Type, and
     * Version are required.
     *
     * @param jarArtifact A maven artifact in this format
     * {@code <groupId>:<artifactId>:<extension>:[<classifier>:]<version>}
     * Examples: example w/o classifier: com.example.group:artifact:jar::5.1.0 example w/o
     * classifier: com.example.group:artifact:jar:5.1.0 example w classifier:
     * com.example.group:artifact:jar:-debug:5.1.0
     *
     * @return MavenProject
     * @throws IllegalArgumentException
     */
    public static MavenProject parseFromManifest(String jarArtifact) {
        if (jarArtifact == null) {
            throw new IllegalArgumentException("Input is null");
        }

        //expected format is:
        //<groupId>:<artifactId>:<extension>:[<classifier>:]<version>
        //possible examples:
        //example w/o classifier: com.example.group:artifact:jar::5.1.0
        //example w/o classifier: com.example.group:artifact:jar:5.1.0
        //example w   classifier: com.example.group:artifact:jar:-debug:5.1.0
        Pattern p = Pattern.compile("([^: ]+):([^: ]+):([^: ]+):(([^: ]*):)?([^: ]+)");
        Matcher m = p.matcher(jarArtifact);
        if (!m.matches()) {
            throw new IllegalArgumentException("Bad artifact coordinates " + jarArtifact
                    + ", expected format is <groupId>:<artifactId>:<extension>:[<classifier>:]<version>");
        }
        return new MavenProject(
                m.group(1),
                m.group(2),
                m.group(3),
                get(m.group(5), ""),
                m.group(6));

    }

    private static String get(String value, String defaultValue) {
        return (value == null || value.length() <= 0) ? defaultValue : value;
    }
}
