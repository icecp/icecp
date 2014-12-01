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
package com.intel.icecp.core.security.crypto.exception.siganture;

import com.intel.icecp.core.security.crypto.exception.CryptoException;
import com.intel.icecp.core.security.crypto.signature.SignatureScheme;

/**
 * Exception thrown when attempting to load an unavailable {@link SignatureScheme}
 * service
 *
 */
public class UnsupportedSignatureAlgorithmException extends CryptoException {

    public UnsupportedSignatureAlgorithmException(String string) {
        super(string);
    }
    
    public UnsupportedSignatureAlgorithmException(String string, Throwable cause) {
        super(string, cause);
    }

}
