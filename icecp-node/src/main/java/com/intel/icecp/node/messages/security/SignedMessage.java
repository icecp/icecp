/*
 * ******************************************************************************
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
 * ******************************************************************************
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
