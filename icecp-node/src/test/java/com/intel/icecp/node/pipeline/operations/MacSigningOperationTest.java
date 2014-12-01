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
package com.intel.icecp.node.pipeline.operations;

import com.intel.icecp.core.messages.BytesMessage;
import com.intel.icecp.core.metadata.formats.JsonFormat;
import com.intel.icecp.core.security.SecurityServicesTestUtils;
import com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey;
import com.intel.icecp.core.security.crypto.mac.MacScheme;
import com.intel.icecp.core.security.trust.TrustModel;
import com.intel.icecp.core.security.trust.exception.TrustModelException;
import com.intel.icecp.node.security.SecurityConstants;
import com.intel.icecp.node.security.crypto.key.KeyProvider;
import com.intel.icecp.node.security.crypto.mac.hmac.HmacSha1Scheme;
import java.net.URI;
import org.junit.Before;

/**
 * Test for the {@link MacSigningOperation} class
 *
 */
public class MacSigningOperationTest extends SignatureOperationTest {

    private final Class[] hmacTypes = {HmacSha1Scheme.class};
    
    /**
     * Mock trust model that always returns the same symmetric key.
     */
    private class MockSymmetricKeyTrustModel implements TrustModel<SymmetricKey, SymmetricKey> {

        private final SymmetricKey symmKey;

        public MockSymmetricKeyTrustModel() throws Exception {
            this.symmKey = KeyProvider.generateSymmetricKey(SecurityConstants.HmacSHA1);
        }

        @Override
        public SymmetricKey fetchSigningKey(URI keyId) throws TrustModelException {
            return symmKey;
        }

        @Override
        public SymmetricKey fetchVerifyingKey(URI keyId) throws TrustModelException {
            return symmKey;
        }
    }
    
    /**
     * Initialization steps, involving creating the configuration file to be able to
     * later load SHA-1 HMAC algorithm.
     * 
     * @throws Exception In case initialization fails
     */
    @Before
    public void init() throws Exception {
        // Create configuration file for SPI loading 
        SecurityServicesTestUtils.createConfigurationFile(hmacTypes, MacScheme.class);
        // Creat the necessary mock objects
        this.trustModel = new MockSymmetricKeyTrustModel();
        this.format = new JsonFormat(BytesMessage.class);
        
        // Key Id is not important, as the mock trust model will return always a constant key
        // Key should be published under a URI that makes the resource fetchable via a channel
        // e.g., an NDN or file channel. Note that, in this case the key is a secret key, 
        // and therefore is legitimate to assume it would be never exchanged over the network 
        this.signatureOperation = new MacSigningOperation(trustModel, URI.create("file://somekeyfile.key"), SecurityConstants.HmacSHA1, format);
    }

}
