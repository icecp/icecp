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
package com.intel.icecp.core.security;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.net.URL;

/**
 * Utility class for service provider interface tests
 *
 */
public class SecurityServicesTestUtils {
    
    /**
     * Finds the location of {@link MockSecuirtyServiceImpl}, and creates a service file into
     * META-INF/services/ to register the service for SPI loading. File is deleted after 
     * the execution terminates
     * 
     * @param serviceType
     * @param interf
     * @throws Exception In case of file-related issues
     */
    public static void createConfigurationFile(Class[] serviceType, Class<? extends SecurityService> interf) throws Exception {
        
        // This takes us to the correct location (e.g., .../target/classes or .../target/test-classes)
        URL location = serviceType[0].getProtectionDomain().getCodeSource().getLocation();
        // Create a file named with full interface class name
        File file = new File(location.getPath() + "META-INF/services/" + interf.getName());
        
        // Do not overwrite the file if exists
        if (file.exists()) {
            return;
        }
        
        // In case the folder does not exists
        file.getParentFile().mkdirs();

        // Write the name of the class we want to support
        Writer output = new FileWriter(file);
        for (int i = 0; i < serviceType.length; i++) {
            output.write(serviceType[i].getName() + "\n");
        }
        output.close();

        // Deletes the file as the JVM terminates its execution
        file.deleteOnExit();
   }
}
