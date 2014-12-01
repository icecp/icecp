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
package com.intel.icecp.node.utils;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.misc.ChannelIOException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;

/**
 * Helper methods for {@link com.intel.icecp.core.Channel} operations.
 *
 */
public class ChannelUtils {

    /**
     * Join components on to the end of a base URI
     *
     * @param base the URI on which to append the components
     * @param components the components to append
     * @return a URI with components appended to the base URI
     */
    public static URI join(URI base, String... components) {
        URI out = base.toString().endsWith("/") ? base : URI.create(base.toString() + "/");
        for (int i = 0; i < components.length; i++) {
            String c = i + 1 == components.length ? components[i] : components[i] + "/";
            c = c.startsWith("/") ? c.substring(1) : c;
            out = out.resolve(c);
        }
        return out;
    }

    /**
     * Subscribe to a channel and retrieve the next message published on this channel. This solves the problem that
     * frequently a call to {@link Channel#latest()} is sent too early; in these cases the request will fail because of
     * timing issues. TODO this method should cancel its subscription once channels allow this operation
     *
     * @param channel the channel to listen on
     * @param <T> the type of the message to retrieve
     * @return a future that will resolve to the next message published on a channel
     */
    public static <T extends Message> CompletableFuture<T> nextMessage(Channel<T> channel) {
        CompletableFuture<T> future = new CompletableFuture<>();

        try {
            channel.subscribe(future::complete);
        } catch (ChannelIOException e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    /**
     * Convenience method for generating a file URI.
     *
     * @param channelName
     * @return
     * @deprecated will be replaced by {@link #join(URI, String...)}
     */
    public static URI toNdnUri(String channelName) {
        try {
            return new URI("ndn", channelName, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid channel name: " + channelName);
        }
    }

    /**
     * Converts a string into something you can safely insert into a URL; thanks to
     * http://stackoverflow.com/questions/573184.
     */
    public static String encodeURIcomponent(String s) {
        StringBuilder o = new StringBuilder();
        for (char ch : s.toCharArray()) {
            if (isUnsafe(ch)) {
                o.append('%');
                o.append(toHex(ch / 16));
                o.append(toHex(ch % 16));
            } else {
                o.append(ch);
            }
        }
        return o.toString();
    }

    private static char toHex(int ch) {
        return (char) (ch < 10 ? '0' + ch : 'A' + ch - 10);
    }

    private static boolean isUnsafe(char ch) {
        return ch > 128 || ch < 0 || " %$&+,/:;=?@<>#%{}\"".indexOf(ch) >= 0;
    }

    /**
     * Create a unique response channel URI based on a request channel name and a request message.
     *
     * @param <M>
     * @param requestChannelName URI of the channel the request will go out
     * @param requestObject the object that will be sent. Must not be changed AFTER channel name has been created
     * @return the URI of the channel where the response will be sent
     * @throws URISyntaxException
     * @throws NotSerializableException
     */
    public static <M extends Message> URI getResponseChannelUri(URI requestChannelName, M requestObject) throws URISyntaxException, NotSerializableException {
        MessageDigest md;
        ByteArrayOutputStream bos;

        try (ObjectOutputStream oos = new ObjectOutputStream(bos = new ByteArrayOutputStream())) {
            md = MessageDigest.getInstance("SHA-256");
            oos.writeObject(requestObject);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new NotSerializableException("Message " + requestObject.getClass().getName() + "not serializable");
        }
        BigInteger x = new BigInteger(md.digest(bos.toByteArray()));
        return new URI(requestChannelName.getScheme(), requestChannelName.getHost(), requestChannelName.getPath() + "/" + x.toString(36), requestChannelName.getFragment());
    }

}
