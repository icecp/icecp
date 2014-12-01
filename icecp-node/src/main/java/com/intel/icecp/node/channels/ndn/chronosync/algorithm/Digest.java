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

package com.intel.icecp.node.channels.ndn.chronosync.algorithm;

import java.util.Arrays;

/**
 * Wrap the bytes resulting from digest algorithm; this class exposes {@link #toBytes()} and helper methods for OO
 * interaction.
 *
 */
public class Digest {

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    private final byte[] value;

    /**
     * @param value the digested bytes
     */
    public Digest(byte[] value) {
        this.value = value;
    }

    /**
     * @return the digested bytes
     */
    public byte[] toBytes() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 29 * hash + Arrays.hashCode(this.value);
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Digest other = (Digest) obj;
        return Arrays.equals(this.value, other.toBytes());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Digest{" + toHex() + '}';
    }

    /**
     * Thanks to http://stackoverflow.com/a/9855338
     *
     * @return the hex representation of the digest
     */
    public String toHex() {
        char[] hexChars = new char[value.length * 2];
        for (int i = 0, j = 0; i < value.length; i++, j += 2) {
            int v = value[i] & 0xFF;
            hexChars[j] = hexArray[v >>> 4];
            hexChars[j + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
