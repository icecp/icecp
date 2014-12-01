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

package com.intel.icecp.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Random;

/**
 * Helper methods for testing
 *
 */
public class TestHelper {

    private static final String NFD_IP = "ndn-lab2.jf.intel.com";

    /**
     * Attempts to retrieve a host name from system properties in the following order: 1) -Dnfd.ip=..., 2) -Dnfd.host,
     * or 3) defaults to {@link #NFD_IP}
     *
     * @return the configured host name (or IP) to an NFD
     */
    public static String getNfdHostName() {
        String hostName = System.getProperty("nfd.ip");
        if (hostName == null) {
            hostName = System.getProperty("nfd.host");
        }
        if (hostName == null) {
            hostName = NFD_IP;
        }
        return hostName;
    }

    /**
     * Verify that the given IP/host name is reachable from the current device.
     *
     * @param ipOrHost IP or host name
     * @return true if reachable
     */
    public static boolean isReachable(String ipOrHost) {
        try {
            InetAddress address = InetAddress.getByName(ipOrHost);
            return address.isReachable(2000);
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * @param stringLength the number of dots to add to the string
     * @return a string containing dots, '.'
     */
    public static String generateString(int stringLength) {
        StringBuilder builder = new StringBuilder(stringLength);
        for (int i = 0; i < stringLength; i++) {
            builder.append('.');
        }
        return builder.toString();
    }

    /**
     * @param stringLength the number of random alpha-numeric characters to add
     * to the string
     * @return a string containing random alpha-numeric characters
     */
    public static String generateRandomString(int stringLength) {
        byte[] symbols = "abcdefghijklmnopqrstuvwxyz0123456789".getBytes();
        Random random = new Random();
        byte[] buffer = new byte[stringLength];
        for (int i = 0; i < stringLength; i++) {
            buffer[i] = symbols[random.nextInt(symbols.length)];
        }
        return new String(buffer);
    }

    /**
     * @param stream the {@link InputStream} to read
     * @return an array of the read bytes
     * @throws IOException if the stream fails
     */
    public static byte[] readAllBytes(InputStream stream) throws IOException {
        ByteArrayOutputStream builder = new ByteArrayOutputStream();
        int read = stream.read();
        while (read != -1) {
            builder.write(read);
            read = stream.read();
        }
        builder.flush();
        stream.close();
        return builder.toByteArray();
    }
}
