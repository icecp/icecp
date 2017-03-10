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
package com.intel.icecp.node.utils;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * Helper class for retrieving the correct MAC address for the device.
 *
 */
public class NetworkUtils {

    /**
     * @return retrieve current host name address for the system
     * @throws UnknownHostException
     */
    public static String getHostName() throws UnknownHostException {
        return java.net.InetAddress.getLocalHost().getHostName();
    }

    /**
     * @return retrieve current MAC address for the system
     * @throws SocketException Error creating or accessing socket
     */
    public static String getMacAddress() throws SocketException {
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        int count = 0;
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            count++;
            byte[] bmac = networkInterface.getHardwareAddress();
            if (bmac != null) {
                String mac = macBytesToString(bmac);
                return mac;
            }
        }
        throw new SocketException("Could not determine localhost MAC address from " + count + " interfaces");
    }

    /**
     * Prettify the MAC address
     *
     * @param mac
     * @return
     */
    private static String macBytesToString(byte[] mac) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
            sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
        }
        return sb.toString();
    }
}
