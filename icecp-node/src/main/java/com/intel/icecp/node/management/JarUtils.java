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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import com.intel.icecp.node.utils.StreamUtils;

/**
 * Helper methods for interacting with JAR files
 *
 */
public class JarUtils {

    /**
     * Retrieve the byte code for a class using resource streams. This method can handle inner classes but has not been
     * tested against anonymous classes.
     *
     * @param object the class reference
     * @return the byte code for the class
     * @throws IOException if the resource cannot be read
     */
    public static byte[] getClassByteCode(Class object) throws IOException {
        String classFile = object.getEnclosingClass() == null ? object.getSimpleName() :
                object.getEnclosingClass().getSimpleName() + '$' + object.getSimpleName();
        InputStream inputStream = object.getResource(classFile + ".class").openStream();
        return StreamUtils.readAll(inputStream);
    }

    /**
     * Build the bytes of a JAR from class byte code. Note that in all but the simplest of cases this will produce
     * un-runnable JARs; this is intended as a helper library for testing JAR loading. Thanks to
     * http://stackoverflow.com/a/1281295/3113580 for the example.
     *
     * @param classes the classes to load in to the JAR
     * @return the assembled JAR bytes
     */
    public static byte[] buildJar(Class... classes) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        List<Class> classesWithoutNulls =
                Arrays.stream(classes).filter(x -> x != null).collect(Collectors.toList());
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            try (JarOutputStream target = new JarOutputStream(bytes, manifest)) {
                for (Class c : classesWithoutNulls) {
                        String name = c.getEnclosingClass() == null ? c.getCanonicalName() :
                                c.getEnclosingClass().getCanonicalName() + '$' + c.getSimpleName();
                        JarEntry entry = new JarEntry(name.replace('.', '/') + ".class");
                        entry.setTime(System.currentTimeMillis());
                        target.putNextEntry(entry);
                        target.write(getClassByteCode(c));
                        target.closeEntry();
                    }
            }
            return bytes.toByteArray();
        }
    }

    /**
     * Return an array of MavenProject objects, each of them representing one of the dependency jars specified in the
     * input JAR file. The JAR file specified is a "Feature" jar file, meaning it contains certain requirements. See the
     * sample feature code for more info.
     *
     * @param jarFile a byte[] for a feature jar file.
     * @return an array of MavenProject objects.
     */
    public static MavenProject[] parseDependencies(byte[] jarFile) throws IllegalArgumentException {
        // get dependencies from manifest attribute
        String classPath = getAttributeFromJarManifest("Class-Path", jarFile);
        if (classPath == null) {
            // no classpath found; JAR is valid, so return empty list
            return new MavenProject[0];
        }

        // class path is a "space" separated list of jar files.
        // Split the list. If list is empty, return empty array.
        String classPathParts[] = classPath.split(" ");
        MavenProject[] mavenProjectList;
        if (classPathParts.length == 0) {
            mavenProjectList = new MavenProject[0];
        } else {
            mavenProjectList = new MavenProject[classPathParts.length];
            for (int mp = 0; mp < classPathParts.length; mp++) {
                mavenProjectList[mp] = MavenProject.parseFromManifest(classPathParts[mp]);
            }
        }

        return mavenProjectList;
    }

    /**
     * Utility method to return a specified Attribute value from a jar files manifest. This method will look in the
     * specified jar file's manifest for the specified attribute and return the value. If the attribute is not present,
     * returns null.
     *
     * @param attributeName the attribute to look for
     * @param jarFile the jar file containing the manifest to look into.
     * @return if attribute found, returns the value found in the manifest. if attribute !found or manifest not present,
     * returns null
     * @throws IllegalArgumentException if JAR file cannot be read
     */
    public static String getAttributeFromJarManifest(String attributeName, byte[] jarFile) throws IllegalArgumentException {
        try (JarInputStream jarInStream = new JarInputStream(new ByteArrayInputStream(jarFile))) {
            Manifest manifest = jarInStream.getManifest();
            if (manifest == null) {
                return null;
            }

            return getAttribute(attributeName, manifest);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the JAR file");
        }
    }

    private static String getAttribute(String attributeName, Manifest manifest) {
        Attributes attributes = manifest.getMainAttributes();
        if (attributes == null) {
            return null;
        }

        if (attributes.containsKey(attributeName)) {
            return attributes.getValue(attributeName);
        } else {
            return null;
        }
    }

    /**
     * Debug method to describe the class loader stack for the passed object
     *
     * @param obj the object to inspect
     * @return a debugging string
     */
    public static String getClassLoaderStack(Object obj) {
        StringBuilder classLoaderDetail = new StringBuilder();
        Stack<ClassLoader> classLoaderStack = new Stack<>();
        ClassLoader currentClassLoader = (obj == null) ? Thread.currentThread().getContextClassLoader() :
                obj.getClass().getClassLoader();

        while (currentClassLoader != null) {
            classLoaderStack.push(currentClassLoader);
            currentClassLoader = currentClassLoader.getParent();
        }

        classLoaderDetail.append("Loaders:");
        while (classLoaderStack.size() > 0) {
            ClassLoader classLoader = classLoaderStack.pop();
            if (classLoader.toString().contains("AppClassLoader")) {
                classLoaderDetail.append(" [AppCL] ");
            } else if (classLoader.toString().contains("ModuleClassLoader")) {
                classLoaderDetail.append(" [ModCL] ");
            } else if (classLoader.toString().contains("ExtClassLoader")) {
                classLoaderDetail.append(" [ExtCL] ");
            } else {
                classLoaderDetail.append(classLoader.toString());
            }
        }
        return classLoaderDetail.toString();
    }
}