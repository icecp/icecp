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