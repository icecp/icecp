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

import com.intel.icecp.core.Module;
import com.intel.icecp.node.utils.StreamUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * The ModuleClassLoader is a custom class loader for loading ICECP modules; it will sandbox the classes for the primary
 * JAR along with its dependency JARs with a given set of permissions. A ICECP Module is created by implementing the
 * {@link Module} interface, and include a ModuleProperty annotation specifying the name (e.g. {@code
 * &#64;ModuleProperty(name="ModuleSample") }). The module class must then be packaged into a JAR file. The JAR file has
 * the following requirements in order for this loader to load the module:
 * <p>
 * <ol> <li>The JAR must contain a MANIFEST.MF file</li> <li>The manifest file must contain a Maven coordinate for the
 * JAR file, specified like this: {@code ModuleMavenCoordinates: com.intel.icecp:ndn-modulesample:jar::0.1.0 }</li>
 * <li>The manifest file must contain a {@code Class-Path: } element which contains the Maven coordinates for all
 * dependencies (space delimited), specified like this: {@code Class-Path: com.intel.icecp:icecp-node:jar::0.8
 * com.fasterxml.jackson.core:jackson-annotations:jar::2.4.0 ... }</li> </ol>
 * <p>
 * Here is how to setup the POM file. These go in the maven-jar-plugin, in the build section:
 * <p>
 * <pre>
 * {@code
 * <configuration>
 *     <archive>
 *         <manifest>
 *             <addClasspath>true</addClasspath>
 *             <classpathLayoutType>custom</classpathLayoutType>
 *             <customClasspathLayout>${artifact.groupId}:${artifact.artifactId}:${artifact.type}:${dashClassifier?}:${artifact.version}
 * </customClasspathLayout>
 *         </manifest>
 *         <manifestEntries>
 *             <ModuleMavenCoordinates>${project.groupId}:${project.artifactId}:${project.packaging}::${project.version}</ModuleMavenCoordinates>
 *         </manifestEntries>
 *    </archive>
 * </configuration>
 * }
 * </pre>
 * <p>
 * See <a href="http://docs.oracle.com/javase/7/docs/technotes/guides/security/spec/security-spec.doc4.html">this
 * document</a> for more details on the sandboxing implemented by this class loader.
 *
 */
class ModuleClassLoader extends SecureClassLoader {

    private static final Logger LOGGER = LogManager.getLogger();
    private final ClassLoader systemClassLoader;
    private final Method systemClassLoaderFindMethod;
    private final Map<String, byte[]> resources = new HashMap<>();
    private ArrayList<IcecpJarFileInfo> jarBytesList;
    private Map<String, Integer[]> jarFileLookupMap = null;
    private Permissions permissions = null;

    /**
     * Pass in the Module to load, its dependencies, and the permissions for that module. Get everything setup to load
     * the module when load() is called.
     *
     * @param bytes - A byte[] that represents a jar file containing the module.
     * @param dependencies a List of IcecpJarFileInfo classes, each one containing a dependency jar file as a byte[] and
     * a MavenProject object. These IcecpJarFileInfo objects are the jar files specified in the manifest Class-Path
     * attribute (described above).
     * <p>
     * NOTE: If you want to add all of your dependencies into the module jar file, you can do that. In that case, the
     * Class-Path attribute will not be placed in the jar file manifest. The dependencies list can be passed in empty or
     * null in this case.
     * @param permissions class containing the permissions to be applied to the module class via the loader.
     */
    ModuleClassLoader(byte[] bytes, List<IcecpJarFileInfo> dependencies, Permissions permissions) {

        // TODO: is this actually needed?
        if (permissions != null) {
            permissions.add(new java.net.NetPermission("specifyStreamHandler"));
        }

        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Input bytes are empty");
        }

        //Create local ArrayList of IcecpJarFileInfo classes
        //Put the "Module" jar as the first entry, 
        jarBytesList = new ArrayList<>();

        //The maven coordinates of the module jar should be included in the
        //manifest.  If they are, then use it to create the mavenProject.
        //If not, then create the IcecpJarFileInfo without it.
        String moduleMavenCoordinates = JarUtils.getAttributeFromJarManifest("ModuleMavenCoordinates", bytes);
        MavenProject mavenProject = null;
        if (moduleMavenCoordinates != null) {
            mavenProject = MavenProject.parseFromManifest(moduleMavenCoordinates);
        }

        IcecpJarFileInfo moduleJarFileInfo = new IcecpJarFileInfo(mavenProject, bytes);
        jarBytesList.add(moduleJarFileInfo);

        //If there are no dependencies, they are probably inside the module jar file.
        if (dependencies != null) {
            jarBytesList.addAll(dependencies);
        }

        //Save the permissions.  Used in the getPermissions() method below
        this.permissions = permissions;

        //Create a hashmap to map classnames to jarsFileIndexes
        setupHashMap();

        // setup references for class existence tests
        try {
            systemClassLoaderFindMethod = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
            systemClassLoaderFindMethod.setAccessible(true);
            systemClassLoader = ClassLoader.getSystemClassLoader();
        } catch (NoSuchMethodException e) {
            // this exception should never be thrown; ClassLoader.findLoadedClass() has been a part of Java since 1.1 and will not likely be removed in the future
            throw new IllegalStateException("Unable to retrieve the method 'findLoadedClass' from ClassLoader; this loader will not be able to check if a class is loaded in the system.", e);
        }
    }

    /**
     * @param name the class name
     * @return the package name of the class
     */
    static String packageNameOfClassname(String name) {
        return name != null && name.lastIndexOf('.') != -1 ? name.substring(0, name.lastIndexOf('.')) : null;
    }

    /**
     * setupHashMap() This method creates a hash map that maps all of the class names, and resources to the jar file
     * that contains it. The names are pulled by opening each jar file in the jarBytesList array list. The class name is
     * mapped to the index into the jarBytesList. This speeds up the loading. In findClass, the first step is to use the
     * hashmap to find the jar file containing the class. The same is done in the findResource() methods. That way, only
     * one jar file is searched. And, if the jar file is not found in the hashmap, you can simply return "not found".
     * <p>
     * NOTE: This hashmap supports duplicate entries (sort of). Its possible to have duplicate entries in the jar files.
     * An example is for ServiceLoaders and SPI's. In this case, the SPI class is stored as
     * "META-INF/services/com.intel.icecp.spi.SampleSPI" in each jar that is a provider. Since hashmaps don't support
     * duplicates, but we want fast lookup, the "value" in the map is an Integer[]. When there is a duplicate, the other
     * indexes are stored in the array.
     */
    private void setupHashMap() {

        int index = 0;
        jarFileLookupMap = new HashMap<>();
        //At this point, jarBytesList will have at least one entry (the module jar).
        for (IcecpJarFileInfo jarFileInfo : jarBytesList) {
            //jarIn is auto closed by the try(resource)
            try (JarInputStream jarIn = new JarInputStream(new ByteArrayInputStream(jarFileInfo.jarBytes))) {
                //walk through all entries in this jar, skip directories.
                JarEntry entry = jarIn.getNextJarEntry();
                while (entry != null) {
                    if (!entry.isDirectory()) {
                        LOGGER.trace("Adding entry {} from {} to hash map index {}", entry.getName(), jarFileInfo, index);
                        Integer[] indexArray = jarFileLookupMap.get(entry.getName());

                        //See if the name already exists in the hashmap
                        if (indexArray != null) {
                            //Since it exists, copy the existing Integer[] and extend it to
                            //include the new index value.  Then "add()" it again.
                            Integer[] newArray = Arrays.copyOf(indexArray, indexArray.length + 1);
                            newArray[newArray.length - 1] = index;
                            indexArray = newArray;
                        } else {
                            //Doesn't exist yet, so just add it.
                            indexArray = new Integer[1];
                            indexArray[0] = index;
                        }

                        //TODO: Should change the entry.getName() to the class name, then that conversion
                        //is not needed in other methods (eg, findClassInJar() findModules())
                        jarFileLookupMap.put(entry.getName(), indexArray);
                    }
                    entry = jarIn.getNextJarEntry();
                }
            } catch (IOException e) {
                LOGGER.error("Failed while accessing JAR: {}", jarFileInfo, e);
            }
            index++;
        }
    }

    /**
     * Find the class in one the JARs registered with this class loader. When an application attempts to load a class
     * using {@link #loadClass(String)}, class loaders must follow the Java loader delegation model (i.e. ask the parent
     * first); every class loader asks its parent until the top-level class loader is checked. If it cannot load the
     * specified class, it will call {@link super#findClass(String)} which will point to this method. Here we inspect
     * the dependency JARs and define the class with its appropriate sandbox permissions.
     *
     * @param name the name of the class to load
     * @return the loaded, sandboxed class
     * @throws ClassNotFoundException if the class cannot be found and defined
     */
    @Override
    protected Class<?> findClass(final String name) throws ClassNotFoundException {
        LOGGER.debug("Finding class: {}", name);

        byte[] classBytes;
        try {
            // get the class bytes from the jar file
            classBytes = findClassInJar(name);

            //If its not there, then we didn't find it either.
            if (classBytes == null) {
                throw new ClassNotFoundException(name);
            }

        } catch (Exception e) {
            throw new ClassNotFoundException(name, e);
        }

        return findClassHelper(name, classBytes);
    }

    /**
     * Helper class to finish the "findClass()" method. This is broken into two methods, because findModules() needs to
     * findClass but it already has the class name from the hash map (the code that is in the findClass() method. So
     * this method was created for that. See findModules().
     *
     * @param name - the name of the class in package.classname format (eg, com.intel.icecp.node.NodeImpl).
     * @param classBytes - the actual bytes for the class, found in the jar file.
     * @return - an instance of the class.
     */
    private Class<?> findClassHelper(String name, byte[] classBytes) {

        // Create an appropriate code source
        CodeSource cs = getCodeSource("/" + name);

        // Define the associated package (if class is in a package)
        String packageName = packageNameOfClassname(name);
        if (packageName != null && getPackage(packageName) == null) {
            LOGGER.trace("Define package: {}", packageName);

            // definePackage(packageName, "specTitle", "specVersion", "specVendor", "implTitle", "implVersion", "implVendor", cs.getLocation()); from MANIFEST
            assert cs != null;
            definePackage(packageName, null, null, null, null, null, null, cs.getLocation());
        }

        //The missing link!
        //IMPORTANT:
        //These next two calls, enable us to build the sandbox for the module.
        //Because we are creating our own ProtectionDomain, and giving it our CodeSource and Permissions,
        //It will use this domain for checking permissions, and not consult the policy file.
        //See the documentation on the ProtectionDomain constructors.
        //
        //If you call the defineClass method and pass in a CodeSource instead of a ProtectionDomain,
        //then it will not only use our permissions, but will consult with the current policy file,
        //which we DO NOT want to do.
        //
        ProtectionDomain pd = new ProtectionDomain(cs, permissions);

        // Define the class
        try {
            return defineClass(name, classBytes, 0, classBytes.length, pd);
        } catch (NoClassDefFoundError e) {
            LOGGER.error("Cannot find defined class: {}", name, e);
        } catch (LinkageError e) {
            LOGGER.debug("Failed to link class; class might already be loaded: {}", name, e);
        } catch (Throwable t) {
            LOGGER.info("Unspecified error while defining class: {}", name, t);
        }
        return null;
    }

    /**
     * The class we loaded was defined with a CodeSource in the findClass() method above. This binds the class being
     * loaded into our ProtectionDomain. That means the class's permissions are created here in this method. Any class
     * that gets loaded by this loader on behalf of the Module will get the same permissions set by the
     * PermissionsManager and handed to this class in the constructor.
     */
    @Override
    protected PermissionCollection getPermissions(CodeSource codesource) {
        LOGGER.debug("Returning permissions {} for code source {}", permissions, codesource.getLocation().getPath());
        return permissions;
    }

    /**
     * Find the specified class in the jar dependencies. First lookup the JAR file in the jarFileLookupMap, and then
     * look in that JAR file.
     * <p>
     * The input param is a class name in package format: com.intel.icecp.myclass. But the class names stored in the
     * jarFileLookupMap are stored in the format provided by jarEntry.getName(): com/intel/icecp/myclass.class. See
     * setupHashMap() method above. Therefore convert the input param to the right format before looking up.
     *
     * @param className The name of the class to lookup.
     * @return The class in byte[] format if its found. If not found, return null.
     */
    private byte[] findClassInJar(String className) {
        //convert class name to correct format, see comment above
        String _className = className.replace('.', '/') + ".class";

        //Use the class name to find the jar file in the jarFileLookupMap.
        //Then you only have to look in one jar file.
        //If the class is not in the map, then its "not found".
        Integer[] index = jarFileLookupMap.get(_className);
        if (index == null) {
            return null;
        }

        //The hashmap returns an Integer[], but for classes there should 
        //only be one entry.  Class names are unique in the jar files.
        //Therefore use index 0 in the returned Integer[].
        //Return the class bytes from the jar.
        return getBytesFromJar(_className, index[0]);

    }

    /**
     * Create a code source for a jar file that comes in via a byte[]. The "url" is not really used, since we are
     * providing the permissions in the ProtectionDomain
     *
     * @param name The class name.
     * @return CodeSource for the class.
     */
    private CodeSource getCodeSource(String name) {
        try {
            URL u = new URL("file", "localhost", name);
            return new CodeSource(u, (Certificate[]) null); // TODO can we provide these certs?
        } catch (MalformedURLException e) {
            LOGGER.error("Failed to retrieve code source for {}, continuing", name, e);
            return null;
        }
    }

    /**
     * Find the specified resource and return a URL for it. The parent ClassLoaders getResource() method is called, and
     * when it can't find the resource, it calls this overridden method. In here, we locate the resource and return a
     * URL. Since our URL is not a standard URL protocol (see toURL()) we build the URL with a handler that is called
     * when ever someone wants to open a stream to the resource. See the handler code below. This is needed because our
     * resources are in byte[].
     *
     * @param name of the resource to find. For example it could be /images/logo.png, or
     * /META-INF/services/com.intel.icecp.Dictionary
     * @return URL for the resource or null if not found.
     * <p>
     * Note: Its possible to have duplicate resource names in the jar files. Our jarFileLookupMap supports this. But for
     * this method, the assumption is that there is only one. So if multiple resources are found, the first one found is
     * returned.
     */
    @Override
    protected URL findResource(String name) {
        LOGGER.trace("Finding resource: {}", name);

        if (jarFileLookupMap.containsKey(name)) {
            Integer[] index = jarFileLookupMap.get(name);
            IcecpJarFileInfo jarFileInfo = jarBytesList.get(index[0]);
            URL url = toURL(jarFileInfo, name, index[0]);
            LOGGER.trace("Found resource: {}", url);
            return url;
        } else {
            LOGGER.warn("Failed to find resource, continuing: {}", name);
            return null;
        }
    }

    /**
     * Find all locations of the specified resource and return an enumeration of URLs for it. The parent ClassLoaders
     * getResources() method is called, and when it can't find the resource, it calls this overridden method. In here,
     * we locate all of the occurrences of the resource and return an enumeration of them. Since our URL is not a
     * standard URL protocol (see toURL()) we build the URL with a handler that is called when ever someone wants to
     * open a stream to the resource. See the handler code below. This is needed because our resources are in byte[].
     *
     * @param name of the resource to find. For example it could be /images/logo.png, or
     * /META-INF/services/com.intel.icecp.Dictionary
     * @return Enumeration<URL> containing all of the URL's for specified resource or an emptyEnumeration() if not
     * found.
     * <p>
     * Note: Its possible to have duplicate resource names in the jar files. Our jarFileLookupMap supports this.
     */
    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        LOGGER.trace("Finding resources: {}", name);

        if (jarFileLookupMap.containsKey(name)) {
            //needs to return multiple finds from the map, may be more than one
            Integer[] indexes = jarFileLookupMap.get(name);
            List<URL> resourceList = new ArrayList<>();
            for (Integer i : indexes) {
                try {
                    IcecpJarFileInfo jarFileInfo = jarBytesList.get(i);
                    resourceList.add(toURL(jarFileInfo, name, i));
                } catch (Exception e) {
                    // TODO convert to specific exception
                    LOGGER.error("Failed to add {}#{} to resource collection", name, i, e);
                }
            }
            return Collections.enumeration(resourceList);
        }

        return Collections.emptyEnumeration();
    }

    /**
     * Find {@link Module}-implementing classes matching a regular expression. Note that this method cannot be called
     * twice because the first call will load classes and the second will fail due to duplicate class loads TODO fix
     * this.
     *
     * @param regex a regular expression to match class names or null to search all classes; typical use case would be
     * all classes ending in Module ".*Module$"
     * @return a list of classes in the JAR that implement the {@link Module} interface and with a class name matching
     * the regular expression
     */
    @SuppressWarnings("unchecked")
    public Collection<Class<? extends Module>> findModules(String regex) {
        List<Class<? extends Module>> matches = new ArrayList<>();
        Class loadedClass;

        for (String className : jarFileLookupMap.keySet()) {
            LOGGER.trace("Inspecting {}", className);

            //Only look for class names
            if (!className.endsWith(".class")) {
                continue;
            }

            String _className = className.replaceFirst("\\.class$", "").replace('/', '.');

            // If a regex is given, but the class does not match continue
            if (regex != null && !_className.matches(regex)) {
                continue;
            }

            try {
                if (isClassLoadedInSystem(_className)) {
                    continue;
                }
            } catch (InvocationTargetException | IllegalAccessException e) {
                LOGGER.warn("Failed to discover whether {} is loaded in the system class loader", _className, e);
            }

            Integer[] index = jarFileLookupMap.get(className);

            // Skip all classes from dependency JARs
            if (index != null && index[0] == 0) {
                loadedClass = findClassHelper(_className, getBytesFromJar(className, index[0]));
                if (loadedClass != null && Module.class.isAssignableFrom(loadedClass)) {
                    LOGGER.debug("Found class " + _className + " implementing ICECP Module");
                    matches.add(loadedClass);
                }
            }
        }

        return matches;
    }

    /**
     * Check if the system class loader has a given class already loaded; if it already has been loaded, we do not want
     * to reload it.
     *
     * @param className the name of the class
     * @return true if the class has already been loaded, false otherwise
     * @throws InvocationTargetException if the request to the system class loader fails
     * @throws IllegalAccessException if the request to the system class loader fails
     */
    boolean isClassLoadedInSystem(String className) throws InvocationTargetException, IllegalAccessException {
        return systemClassLoaderFindMethod.invoke(systemClassLoader, className) != null;
    }

    /**
     * Create a specific URL for a resource. This URL is specific to this class loader. The new protocol
     * "jar:icecpjar://" is handled via the ModuleURLStreamHandler. It knows how to find the resource specified by the
     * URL. The format is: {@code jar:icecpjar://<maven coordinates>!/<resourceName> }. For example: {@code
     * jar:icecpjar://:com.intel.icecp:dictionary-general:jar:0.0.1!/META-INF/services/com.intel.icecp.dictionary.spi.Dictionary#20
     * }. TODO make this spec more intelligible, perhaps use channel schemes
     *
     * @param icecpJarFileInfo containing the MavenCoordinates
     * @param name of the resource
     * @param fragment which is the index into our list of jar file dependencies.
     * @return the URL for this resource or null if it cannot be created
     */
    private URL toURL(IcecpJarFileInfo icecpJarFileInfo, String name, int fragment) {
        URL url = null;
        String jarPath = String.format("%s!/%s", icecpJarFileInfo.mavenProject == null ? "" : icecpJarFileInfo.mavenProject.toMavenCoordinate(), name);

        if (fragment >= 0) {
            jarPath += String.format("#%d", fragment);
        }

        try {
            url = new URL(
                    "jar:icecpjar://", //protocol
                    null, //host
                    -1, //port
                    jarPath, //Jar file name in MavenCoordinates including resource and [fragment]
                    new ModuleURLStreamHandler());        //handler to provide the stream for the jar

        } catch (MalformedURLException e) {
            LOGGER.error("Unable to create URL for name, continuing: {}", name, e);
        }

        return url;
    }

    /**
     * Return the bytes for the specified name at the specified index. The assumption here is that the item exists in
     * the jarBytesList. Use the index to lookup the jar, and then get the bytes for the specified name. It might be a
     * class, or a resource. This method will use the cached resource bytes if available; it trades space for time
     * because we found that in a 28MB JAR repeatedly deflating the JAR caused a 2:29.000 load time per module. Caching
     * reduced this to 2.90 seconds. TODO in the future we may want to initialize a timer to clean up the cache some
     * time after the module is successfully loaded so that GC can reclaim the space.
     *
     * @param name The name of the class or resource to find.
     * @param index The index for the jar file where this name exists
     * @return the bytes for the specified name
     */
    private byte[] getBytesFromJar(String name, Integer index) {
        LOGGER.debug("Retrieving bytes for resource: {}", name);

        if (resources.containsKey(name)) {
            byte[] bytes = resources.get(name);
            assert bytes != null;
            LOGGER.debug("Using {} cached bytes for resource: {}", bytes.length, name);
            return bytes;
        }

        try (JarInputStream jarIn = new JarInputStream(new ByteArrayInputStream(jarBytesList.get(index).jarBytes))) {
            JarEntry entry = jarIn.getNextJarEntry();
            while (entry != null) {
                if (!entry.isDirectory()) {
                    String entryName = entry.getName();
                    // we must not close the input stream since we will re-use it in the next iteration; apparently
                    // JarInputStreams, when read completely, only return the bytes for the current entry
                    byte[] bytes = StreamUtils.readAllAndKeepOpen(jarIn, StreamUtils.DEFAULT_BUFFER_SIZE);
                    LOGGER.debug("Caching {} bytes for resource: {}", bytes.length, entryName);
                    resources.put(entryName, bytes);
                }

                // TODO we don't actually need to iterate over every entry; if we keep a pointer to the latest entry
                // we could resume there if we are asked for an uncached resource
                entry = jarIn.getNextJarEntry();
                LOGGER.debug("Next entry is: {}", entry);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to retrieve entry {} from JAR", name, e);
        }

        return resources.get(name);
    }

    /**
     * Private URLStreamHandler class to map internal JAR content to URLs
     */
    private class ModuleURLStreamHandler extends URLStreamHandler {

        @Override
        protected URLConnection openConnection(final URL resourceUrl) throws IOException {
            LOGGER.trace("Opening connection: {}", resourceUrl);

            return new URLConnection(resourceUrl) {
                final URL resourceStreamUrl = resourceUrl;

                @Override
                public void connect() throws IOException {
                    // no connection necessary, resource is in memory
                }

                @Override
                public InputStream getInputStream() {
                    //Get the name from the URL
                    String name = resourceStreamUrl.toString();
                    String fragment = name.substring(name.indexOf("#") + 1);
                    Integer iFrag = Integer.parseInt(fragment);

                    LOGGER.trace("Retrieving input stream: {}", resourceStreamUrl);

                    //Get the actual name from the URL path.  Its after the "!/"
                    int iBang = resourceStreamUrl.getPath().indexOf("!/");
                    if (iBang != -1) {
                        name = resourceStreamUrl.getPath().substring(iBang + 2);
                    } else {
                        //If there is no resource name, then nothing to return
                        return null;
                    }
                    LOGGER.trace("Parsed resource name: {}", name);

                    //Get the resource bytes from the jar file.  The resource
                    //should exist, because the URL was build with a specific index (iFrag).
                    //return an Input stream.
                    byte[] resourceBytes = getBytesFromJar(name, iFrag);
                    return resourceBytes != null ? new ByteArrayInputStream(resourceBytes) : null;
                }
            };
        }
    }
}
