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
package com.intel.icecp.node.network.ndn;

import com.intel.icecp.core.Network;
import com.intel.jndn.management.helpers.EncodingHelper;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.tlv.TlvDecoder;
import net.named_data.jndn.encoding.tlv.TlvEncoder;
import net.named_data.jndn.util.Blob;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * Represent an NDN endpoint; every NDN endpoint has an identity prefix (e.g. /intel/node/1234) and details to connect
 * to its NFD instance (i.e. protocol, host, port).
 *
 */
public class NdnNetworkEndpoint implements Network.Endpoint {

    public static final int TLV_ENDPOINT = 128;
    public static final int TLV_PROTOCOL = 129;
    public static final int TLV_HOST = 130;
    public static final int TLV_PORT = 131;

    public final Name prefix;
    public final String protocol;
    public final String host;
    public final int port;

    public NdnNetworkEndpoint(Name prefix, String host) {
        this(prefix, "tcp4", host, 6363);
    }

    public NdnNetworkEndpoint(Name prefix, String protocol, String host, int port) {
        this.prefix = prefix;
        this.protocol = protocol;
        this.host = host;
        this.port = port;
    }

    /**
     * @param input raw content bytes
     * @return an endpoint instance parsed from bytes; this should decode the output of {@link #wireEncode()}
     * @throws EncodingException
     */
    public static NdnNetworkEndpoint wireDecode(ByteBuffer input) throws EncodingException {
        TlvDecoder decoder = new TlvDecoder(input);
        int endOffset = decoder.readNestedTlvsStart(TLV_ENDPOINT);

        Name prefix = EncodingHelper.decodeName(decoder);
        String protocol = new Blob(decoder.readBlobTlv(TLV_PROTOCOL), true).toString();
        String host = new Blob(decoder.readBlobTlv(TLV_HOST), true).toString();
        int port = (int) decoder.readNonNegativeIntegerTlv(TLV_PORT);

        decoder.finishNestedTlvs(endOffset);
        return new NdnNetworkEndpoint(prefix, protocol, host, port);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI toUri() {
        return URI.create(protocol + "://" + host + ":" + port);
    }

    /**
     * @return a canonical representation of the endpoint according to http://redmine.named-data.net/projects/nfd/wiki/FaceMgmt#TCP.
     */
    public String toCanonicalForm() throws UnknownHostException {
        InetAddress ip = InetAddress.getByName(host);
        return protocol + "://" + ip.getHostAddress() + ":" + port;
    }

    /**
     * @return a Java socket address for connecting to the NFD
     */
    public InetSocketAddress toAddress() {
        return new InetSocketAddress(host, port);
    }

    /**
     * @return an encoded representation of this endpoint
     */
    public Blob wireEncode() {
        TlvEncoder encoder = new TlvEncoder();
        int saveLength = encoder.getLength();

        encoder.writeNonNegativeIntegerTlv(TLV_PORT, port);
        encoder.writeBlobTlv(TLV_HOST, new Blob(host).buf());
        encoder.writeBlobTlv(TLV_PROTOCOL, new Blob(protocol).buf());
        EncodingHelper.encodeName(prefix, encoder);

        encoder.writeTypeAndLength(TLV_ENDPOINT, encoder.getLength() - saveLength);
        return new Blob(encoder.getOutput(), false);
    }

    @Override
    public String toString() {
        return toUri().toString();
    }
}
