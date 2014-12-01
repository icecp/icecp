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

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.intel.icecp.node.security.tpm.tpm12;

/**
 *
 */
public class Tss1_2Codes {

    /* Known key IDs */
    public static final int TPM_KEY_EK_ID = 0;
    public static final int TPM_KEY_SRK_ID = 1;

    /* TSS Object types */
    public static final int TSS_OBJECT_TYPE_POLICY = 0x01;
    public static final int TSS_OBJECT_TYPE_RSAKEY = 0x02;
    public static final int TSS_OBJECT_TYPE_ENCDATA = 0x03;
    public static final int TSS_OBJECT_TYPE_PCRS = 0x04;
    public static final int TSS_OBJECT_TYPE_HASH = 0x05;

    /* Key types */
    public static final int TSS_KEY_TYPE_DEFAULT = 0x00000000; // indicate a default key (Legacy-Key)
    public static final int TSS_KEY_TYPE_SIGNING = 0x00000010; // indicate a signing key
    public static final int TSS_KEY_TYPE_STORAGE = 0x00000020; // used as storage key
    public static final int TSS_KEY_TYPE_IDENTITY = 0x00000030; // indicate an idendity key
    public static final int TSS_KEY_TYPE_AUTHCHANGE = 0x00000040; // indicate an ephemeral key
    public static final int TSS_KEY_TYPE_BIND = 0x00000050; // indicate a key for TPM_Bind
    public static final int TSS_KEY_TYPE_LEGACY = 0x00000060; // indicate a key that can perfom sign

    /* Key Size */
    public static final int TSS_KEY_SIZE_512 = 0x00000100; // indicate a key with 512 bit
    public static final int TSS_KEY_SIZE_1024 = 0x00000200; // indicate a key with 1024 bit
    public static final int TSS_KEY_SIZE_2048 = 0x00000300; // indicate a key with 2048 bit
    public static final int TSS_KEY_SIZE_4096 = 0x00000400; // indicate a key with 4096 bit
    public static final int TSS_KEY_SIZE_8192 = 0x00000500; // indicate a key with 8192 bit
    public static final int TSS_KEY_SIZE_16384 = 0x00000600; // indicate a key with 16286 bit

    /* Migratable/Non Migratable */
    public static final int TSS_KEY_NOT_MIGRATABLE = 0x00000000; // key is not migratable
    public static final int TSS_KEY_MIGRATABLE = 0x00000008; // key is migratable

    /* Key needs authorization/No authorization */
    public static final int TSS_KEY_NO_AUTHORIZATION = 0x00000000; // no authorization needed for this key
    public static final int TSS_KEY_AUTHORIZATION = 0x00000001; // key needs authorization

    /* Persistent Storage Type */
    public static final int TSS_PS_TYPE_USER = 1;
    public static final int TSS_PS_TYPE_SYSTEM = 2;

    public static final int TSS_KEY_NON_VOLATILE = 0x00000000; // Key is non-volatile
    public static final int TSS_KEY_VOLATILE = 0x00000004; // Key is volatile

    public static final int TPM_EK_KEY_ID = 0;
    public static final int TPM_SRK_KEY_ID = 1;

}
