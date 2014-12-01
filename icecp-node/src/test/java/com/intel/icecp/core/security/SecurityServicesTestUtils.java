/* *****************************************************************************
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
 * *******************************************************************************
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
