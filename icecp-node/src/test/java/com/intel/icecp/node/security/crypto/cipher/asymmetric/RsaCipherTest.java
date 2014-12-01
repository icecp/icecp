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
package com.intel.icecp.node.security.crypto.cipher.asymmetric;

import com.intel.icecp.core.security.crypto.cipher.Cipher;
import com.intel.icecp.core.security.crypto.key.asymmetric.KeyPair;
import com.intel.icecp.core.security.crypto.key.asymmetric.PrivateKey;
import com.intel.icecp.core.security.crypto.key.asymmetric.PublicKey;
import com.intel.icecp.node.security.RandomBytesGenerator;
import com.intel.icecp.node.security.SecurityConstants;
import com.intel.icecp.node.security.crypto.key.KeyProvider;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for class {@link RsaCipher}
 * Note that the execution is a bit time consuming; moreover, 
 * only key of size 1024 is allowed on system where the 
 * "Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy"
 * file is not installed.
 * Therefore, tests for keys of size 2048 and 4096 are commented out.
 * 
 */
public class RsaCipherTest {
    
    /** Encryption bytes */
    private byte[] bytesToEnc;
    
    /** Key pair */
    private final KeyPair kp1024;
//    private final KeyPair kp2048;
//    private final KeyPair kp4096;
    
    /** RSA Cipher */
    private final Cipher<PublicKey, PrivateKey> cipher = new RsaCipher();

    public RsaCipherTest() throws Exception {
        kp1024 = KeyProvider.generateKeyPair(SecurityConstants.RSA_ALGORITHM, 1024);
//        kp2048 = KeyProvider.generateKeyPair(SecurityConstants.RSA_ALGORITHM, 2048);
//        kp4096 = KeyProvider.generateKeyPair(SecurityConstants.RSA_ALGORITHM, 4096);
        
    }
    
    /**
     * Test for {@link RsaCipher#encrypt(byte..., com.intel.icecp.core.security.crypto.key.asymmetric.PublicKey, java.lang.Object...) }
     * 
     * @throws Exception 
     */
    @Test
    public void encryptTest() throws Exception {
		// Maximum input size is the same size of the key - 11 bytes of padding
		Assert.assertNotNull(cipher.encrypt(RandomBytesGenerator.getRandomBytes(1024/8-11), kp1024.getPublicKey()));
//		Assert.assertNotNull(cipher.encrypt(RandomBytesGenerator.getRandomBytes(2048/8-11), kp2048.getPublicKey()));
//		Assert.assertNotNull(cipher.encrypt(RandomBytesGenerator.getRandomBytes(4096/8-11), kp4096.getPublicKey()));
    }
    
    
    /**
     * Test for {@link RsaCipher#decrypt(byte..., com.intel.icecp.core.security.crypto.key.asymmetric.PrivateKey, java.lang.Object...) }
     * 
     * @throws Exception 
     */
    @Test
    public void decryptTest() throws Exception {
        bytesToEnc = RandomBytesGenerator.getRandomBytes(1024/8-11);
        Assert.assertArrayEquals(cipher.decrypt(cipher.encrypt(bytesToEnc, kp1024.getPublicKey()), kp1024.getPrivateKey()) ,bytesToEnc);
//        bytesToEnc = RandomBytesGenerator.getRandomBytes(2048/8-11);
//        Assert.assertArrayEquals(cipher.decrypt(cipher.encrypt(bytesToEnc, kp2048.getPublicKey()), kp2048.getPrivateKey()) ,bytesToEnc);
//        bytesToEnc = RandomBytesGenerator.getRandomBytes(4096/8-11);
//        Assert.assertArrayEquals(cipher.decrypt(cipher.encrypt(bytesToEnc, kp4096.getPublicKey()), kp4096.getPrivateKey()) ,bytesToEnc);
    }

}
