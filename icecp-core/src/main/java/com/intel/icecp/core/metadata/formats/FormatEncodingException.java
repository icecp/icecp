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
package com.intel.icecp.core.metadata.formats;

/**
 * Describes format encoding errors.
 *
 */
public class FormatEncodingException extends Exception {

    /**
     * Constructor
     *
     * @param message the error message
     * @param throwable the inner exception
     */
    public FormatEncodingException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * Constructor
     *
     * @param throwable the inner exception
     */
    public FormatEncodingException(Throwable throwable) {
        super(throwable);
    }
}
