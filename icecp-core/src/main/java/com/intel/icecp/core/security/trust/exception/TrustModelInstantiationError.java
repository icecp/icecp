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
package com.intel.icecp.core.security.trust.exception;

/**
 * Exception indicating an error during the instantiation of a trust model
 * 
 */
public class TrustModelInstantiationError extends TrustModelException {

    public TrustModelInstantiationError(String message) {
        super(message);
    }
    
    public TrustModelInstantiationError(String message, Throwable cause) {
        super(message, cause);
    }
    
}
