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
package com.intel.icecp.node.pipeline.operations;

import com.intel.icecp.core.Message;
import com.intel.icecp.core.messages.BytesMessage;
import com.intel.icecp.core.metadata.Format;
import com.intel.icecp.core.pipeline.Operation;
import com.intel.icecp.core.pipeline.exception.OperationException;
import com.intel.icecp.core.security.trust.TrustModel;
import com.intel.icecp.node.messages.security.SignedMessage;
import com.intel.icecp.node.security.RandomBytesGenerator;
import com.intel.icecp.node.security.crypto.signature.Signature;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test for a generic signature operation
 *
 */
public abstract class SignatureOperationTest {
    
    /** Operation to be tested */
    protected Operation<Message, SignedMessage> signatureOperation;
    
    /** Message encoding format to use for signature **/
    protected Format format;
    
    /** Trust model to use for signing **/
    protected TrustModel trustModel;
    
    /**
     * Test for {@link Operation#execute(java.lang.Object) } passing in a 
     * Regular null input
     * 
     * @throws Exception 
     */
    @Test(expected = OperationException.class)
    public void executeNullInput() throws Exception {
        // Should catch the NullPointerException and throw an OperationException
        signatureOperation.execute(null);
    }
    
    /**
     * Test for {@link Operation#execute(java.lang.Object) } passing in a 
     * Regular input message
     * 
     * @throws Exception 
     */
    @Test
    public void executeRegularInput() throws Exception {
        // Generate random bytes
        byte[] randomBytes = RandomBytesGenerator.getRandomBytes(200);
        // Test signature of the given bytes
        SignedMessage m = signatureOperation.execute(new BytesMessage(randomBytes));
        Assert.assertNotNull(m);
        Assert.assertTrue(m instanceof SignedMessage);
    }
    
    /**
     * Test for {@link Operation#executeInverse(java.lang.Object) } passing in a 
     * null message
     * 
     * @throws Exception 
     */
    @Test(expected = NullPointerException.class)
    public void executeInverseNullMessage() throws Exception {
        // Test inverse operation on a null message
        signatureOperation.executeInverse(null);
    }
    
    /**
     * Test for {@link Operation#executeInverse(java.lang.Object) } passing in a 
     * null inner message
     * 
     * @throws Exception 
     */
    @Test(expected = NullPointerException.class)
    public void executeInverseInvalidSignedMessageNull() throws Exception {
        // Test inverse operation with an invalid SignedMessage
        SignedMessage invalidMessage = new SignedMessage();
        invalidMessage.message = null;
        signatureOperation.executeInverse(invalidMessage);
    }
    
    /**
     * Test for {@link Operation#executeInverse(java.lang.Object) } passing in a 
     * random inner message
     * 
     * @throws Exception 
     */
    @Test(expected = NullPointerException.class)
    public void executeInverseInvalidSignedMessageRandom() throws Exception {
        // Test inverse operation with an invalid SignedMessage
        SignedMessage invalidMessage = new SignedMessage();
        invalidMessage.message = (new BytesMessage(RandomBytesGenerator.getRandomBytes(100)));
        signatureOperation.executeInverse(invalidMessage);
    }
    
    /**
     * Test for {@link Operation#executeInverse(java.lang.Object) } passing in a 
     * null signature
     * 
     * @throws Exception 
     */
    @Test(expected = NullPointerException.class)
    public void executeInverseInvalidSignedMessageNullSignature() throws Exception {
        SignedMessage invalidMessage = new SignedMessage();
        invalidMessage.signature = null;
        signatureOperation.executeInverse(invalidMessage);
    }
    
    /**
     * Test for {@link Operation#executeInverse(java.lang.Object) } passing in an
     * invalid signature
     * 
     * @throws Exception 
     */
    @Test(expected = OperationException.class)
    public void executeInverseInvalidSignedMessageInvalidSignature() throws Exception {
        SignedMessage invalidMessage = new SignedMessage();
        invalidMessage.signature = new Signature(RandomBytesGenerator.getRandomBytes(160));
        signatureOperation.executeInverse(invalidMessage);
    }
    
    /**
     * Test for {@link Operation#executeInverse(java.lang.Object) } passing in an
     * regular signature
     * 
     * @throws Exception 
     */
    @Test
    public void mainTest() throws Exception {
        // Generate random bytes
        byte[] randomBytes = RandomBytesGenerator.getRandomBytes(200);
        // Test signature of the given bytes
        SignedMessage m = signatureOperation.execute(new BytesMessage(randomBytes));
        // TEST 5: Inverse operation test with valid message
        BytesMessage is = (BytesMessage) signatureOperation.executeInverse(m);
        Assert.assertArrayEquals(is.getBytes(), randomBytes);
    }

}
