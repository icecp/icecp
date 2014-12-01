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

import com.intel.icecp.core.channels.Token;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Abstract class that provides basic service loading functionalities for
 * security services extending {@link SecurityService}. Services are loaded
 * using Java {@link ServiceLoader}.
 *
 * @param <I> Security service id type parameter
 * @param <T> Service type (subclass of {@link SecurityService}) service
 */
public class SecurityServices<I, T extends SecurityService<I>> {

    /**
     * Logging component
     */
    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * Service instances, mapped from elements of type I
     */
    private final Map<I, T> services = new HashMap<>();
    /**
     * Type of service
     */
    private final Token<T> serviceType;

    /**
     * Constructor with package visibility
     *
     * @param serviceType Class of services to be loaded
     */
    public SecurityServices(Token<T> serviceType) {
        this.serviceType = serviceType;
    }

    /**
     * Loads the available services using {@link ServiceLoader}
     *
     */
    private void loadAvailableServices() {
        // Load available services implementing T
        ServiceLoader<T> loader = ServiceLoader.load(serviceType.toClass());
        Iterator<T> it = loader.iterator();
        // Add each service into the hash map
        while (it.hasNext()) {
            T service = it.next();
            try {
                services.put(service.id(), service);
            } catch (ServiceConfigurationError ex) {
                // Log the exception and continue
                LOGGER.warn("Failed to load security service {} from SPI", service.id(), ex);
            }
        }
    }

    /**
     * Returns a specific service identified by the input service type, and
     * reloads it, if reload == true
     *
     * @param serviceType string representation of the service to return
     * @param reload if true, forces the execution of
     * {@link SecurityServices#loadAvailableServices()}
     * @return the requested service or null if not available
     */
    public T get(I serviceType, boolean reload) {
        // services is never null
        if (services.isEmpty() || reload) {
            loadAvailableServices();
        }
        return services.get(serviceType);

    }

}
