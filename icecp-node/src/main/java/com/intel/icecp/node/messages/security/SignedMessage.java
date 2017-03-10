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
package com.intel.icecp.node.messages.security;

import com.intel.icecp.core.Message;
import com.intel.icecp.node.security.crypto.signature.Signature;

/**
 * Class representing a singed {@link Message}. 
 * The class wraps an encoded message (as a string of bytes) and carries a 
 * {@link Signature}
 *
 * @param <M> Signed message type
 */
@Deprecated
public class SignedMessage<M extends Message> implements Message {


    /** The message to be signed */
    public M message;

    /** Signature field*/
    public Signature signature;

    /**
     * Default constructor, intended for testing purposes
     */
    public SignedMessage() {}

    public SignedMessage(M message, Signature signature) {
        this.message = message;
        this.signature = signature;
    }
    
    /**
     * Creates a signed message; signature timestamp is automatically generated
     * 
     * @param message Signed message
     * @param signature Signature bytes
     */
    public SignedMessage(M message, byte[] signature) {
        this(message, new Signature(signature));
    }
    
    /**
     * Creates a signed message; signature timestamp is passed in as a parameter
     * 
     * @param message Signed message
     * @param created Signature creation timestamp
     * @param signature Signature bytes
     */
    public SignedMessage(M message, long created, byte[] signature) {
        this(message, new Signature(created, signature));
    }

    
}
