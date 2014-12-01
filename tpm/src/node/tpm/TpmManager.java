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
package com.intel.icecp.node.security.tpm;

import com.intel.icecp.core.Message;
import com.intel.icecp.core.security.SecurityService;
import com.intel.icecp.node.security.tpm.data.SealedData;
import com.intel.icecp.node.security.tpm.exception.TpmOperationError;
import java.io.InputStream;
import java.security.cert.Certificate;

/**
 * TPM manager interface
 *
 */
public interface TpmManager extends SecurityService<String> {

    /**
     * **************************************** MISC
     * *****************************************
     */
    /**
     * Returns an array of size {bytesNum} of pseudo random bytes
     *
     * @param bytesNum
     * @return
     * @throws com.intel.icecp.node.security.tpm.exception.TpmOperationError
     */
    byte[] getRandomBytes(int bytesNum) throws TpmOperationError;

    /**
     * **************************************** FILE/DATA PROTECTION
     * ***************************
     */
    /**
     * Performs Sealing operation on the given data
     *
     * @param data
     * @param password
     * @return
     * @throws TpmOperationError
     */
    SealedData sealData(InputStream data, byte[] password) throws TpmOperationError;

    /**
     * Performs Unsealing operation on the given data
     *
     * @param data	Some sealed data
     * @param password	Sealing key password
     * @return	The unsealed data
     * @throws TpmOperationError
     */
    byte[] unsealData(InputStream data, byte[] password) throws TpmOperationError;

    /**
     * ************************************ ATTESTATION
     * *****************************************
     */
    /**
     * Client: Performs all the necessary steps to request identity credentials.
     * Returns a message representing an identity request
     *
     * @param identityKeySecret	Secret for the AIK created
     * @param caCertificate	Certificate of the Certification Authority in charge
     * for identity request issuance
     *
     * @return Request bytes
     * @throws TpmOperationError
     */
    Message createIdentityRequest(byte[] identityKeySecret, Certificate caCertificate) throws TpmOperationError;

    /**
     * Attester: Composes an attestation request, and returns the corresponding
     * message
     *
     * @return
     * @throws TpmOperationError
     */
    Message requestDeviceAttestation() throws TpmOperationError;

    /**
     * Privacy CA: Evaluates an identity request message and decides whether to
     * issue credentials for the given identity. If credentials are issued, they
     * are returned as a Message
     *
     * @param identityRequestMessage
     *
     * @return A message if the credentials can be created
     *
     * @throws TpmOperationError
     */
    Message issueIdentityCredentials(Message identityRequestMessage) throws TpmOperationError;

}
