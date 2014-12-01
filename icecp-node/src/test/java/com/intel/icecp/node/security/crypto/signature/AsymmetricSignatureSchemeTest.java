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
package com.intel.icecp.node.security.crypto.signature;

import com.intel.icecp.core.security.crypto.signature.SignatureScheme;
import com.intel.icecp.node.security.crypto.key.KeyProvider;
import com.intel.icecp.core.security.crypto.key.asymmetric.KeyPair;
import com.intel.icecp.node.security.RandomBytesGenerator;
import org.junit.Assert;

/**
 * Test for {@link AsymmetricSignatureScheme} subclasses 
 * 
 * Note that only key of size 1024 is allowed on system where the 
 * "Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy"
 * file is not installed. Therefore the field {@link AsymmetricSignatureSchemeTest#KEY_SIZES}
 * contains only 1024.
 * 
 */
public abstract class AsymmetricSignatureSchemeTest {
    
    /** Bytes to sign */
    private final byte[] toSign = RandomBytesGenerator.getRandomBytes(1024);
    
    /**
     * Tests an asymmetric signature creation
     * 
     * @param algorithm Key algorithm
     * @param schemes Asymmetric signature schemes to test
     * @param keySizes Array of key sizes (in bits) to use for testing
     * @throws Exception In case of error
     */
    protected void signTest(String algorithm, SignatureScheme[] schemes, int[] keySizes) throws Exception {
        for (SignatureScheme s : schemes) {
            for (int size : keySizes) {
                KeyPair kp = KeyProvider.generateKeyPair(algorithm, size);
                Assert.assertNotNull(s.sign(toSign, kp.getPrivateKey()));
            }
        }
    }
    
    /**
     * Tests an asymmetric signature verification
     * 
     * @param algorithm Key algorithm
     * @param schemes Asymmetric signature schemes to test
     * @param keySizes Array of key sizes (in bits) to use for testing
     * @throws Exception If verification fails (and consequently the test)
     */
    protected void verifyTest(String algorithm, SignatureScheme[] schemes, int[] keySizes) throws Exception {
        for (SignatureScheme s : schemes) {
            for (int size : keySizes) {
                KeyPair kp = KeyProvider.generateKeyPair(algorithm, size);
                // If fails, throws an exception and invalidates the test
                s.verify(s.sign(toSign, kp.getPrivateKey()), toSign, kp.getPublicKey());
            }
        }
    }
    

}
