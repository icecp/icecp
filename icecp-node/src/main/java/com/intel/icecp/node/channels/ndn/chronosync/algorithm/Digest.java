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
