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
    
    
    
    /**
     * Returns the available services, and loads them if needed or if
     * {@literal reload = True}
     *
     * @param reload If true, forces the execution of
     * {@link SecurityServices#loadAvailableServices()}
     * @return All the available services
     */
    public Map<I, T> getAll(boolean reload) {
        if (services.isEmpty() || reload) {
            loadAvailableServices();
        }
        return services;
    }

}
