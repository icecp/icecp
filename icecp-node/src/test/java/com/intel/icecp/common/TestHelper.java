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
