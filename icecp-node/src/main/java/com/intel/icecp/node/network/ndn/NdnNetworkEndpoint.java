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
