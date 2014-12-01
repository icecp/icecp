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
package com.intel.icecp.node.security.crypto.cipher.symmetric;

import com.intel.icecp.core.security.crypto.cipher.Cipher;
import com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey;
import com.intel.icecp.node.security.SecurityConstants;
import static com.intel.icecp.node.security.crypto.cipher.symmetric.AesSymmetricCipherTest.KEY_SIZES;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for {@link AesCbcCipher}.
 * 
 */
public class AesCbcSchemeTest extends AesSymmetricCipherTest {

    /** AES-CBC scheme to use */
    protected final Cipher aesCbc = new AesCbcCipher();
    
    /**
     * Test for {@link AesCbcCipher#encrypt(byte..., com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey, java.lang.Object...) }
     * 
     * @throws Exception 
     */
    @Test
    public void aesCbcEncTest() throws Exception {
        System.out.println(textToEncrypt.length);
        for (int size : KEY_SIZES) {
            Assert.assertNotNull(aesCbc.encrypt(textToEncrypt, symmetricKeyGen(size, SecurityConstants.AES)));
        }
    }
    
    /**
     * Test for {@link AesCbcCipher#encrypt(byte..., com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey, java.lang.Object...)  }
     * 
     * @throws Exception 
     */
    @Test
    public void aesCbcDecTest() throws Exception {
        for (int size : KEY_SIZES) {
            SymmetricKey sk = symmetricKeyGen(size, SecurityConstants.AES);
            Assert.assertArrayEquals(aesCbc.decrypt(aesCbc.encrypt(textToEncrypt, sk), sk), textToEncrypt);
        }
    }
    
}
