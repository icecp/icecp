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
package com.intel.icecp.node.messages.security.tpm.tpm1_2;

import com.intel.icecp.core.Message;

/**
 * Message to be sent to the Privacy CA, in case of TPM 1.2 Privacy CA based
 * attestation, to request AIK credentials
 *
 */
public class TpmAttestationIdentityRequest implements Message {

    // Symmetric key k1 encrypted with the Public key of the Privacy CA
    public byte[] ecryptedSymmetricKey;

    // EK certificate encrypted with the symmetric key k1
    public byte[] encryptedEkCertificate;

    // Identity request provided by the TPM
    public byte[] identityRequest;

}
