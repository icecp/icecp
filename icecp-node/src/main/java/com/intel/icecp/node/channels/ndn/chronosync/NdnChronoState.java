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

package com.intel.icecp.node.channels.ndn.chronosync;

import com.intel.icecp.node.channels.ndn.chronosync.algorithm.State;
import net.named_data.jndn.Name;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Represent a ChronoSync state for NDN channels; using this class channels can determine what client and what message
 * to retrieve This class is a data structure with two members, the client and the message. TODO in the future this may
 * need to add actions (e.g. update, delete, etc.) when serializing to match the serialization in
 * https://github.com/named-data/jndn/blob/master/src/net/named_data/jndn/sync/sync-state-proto.proto
 *
 */
public class NdnChronoState implements State {

    public static final int CLIENT_MARKER = 128; // see http://named-data.net/doc/ndn-tlv/types.html
    public static final int MESSAGE_MARKER = 129; // see http://named-data.net/doc/ndn-tlv/types.html
    private static final Logger LOGGER = LogManager.getLogger();

    private final long client;
    private final long message;

    /**
     * @param client the ID of the client
     * @param message the ID of the message
     */
    public NdnChronoState(long client, long message) {
        this.client = client;
        this.message = message;
    }

    /**
     * Encode multiple states for network transmission
     *
     * @param states the states to encode
     * @return the encoded buffer
     */
    public static ByteBuffer wireEncodeMultiple(Set<NdnChronoState> states) {
        ByteBuffer buffer = ByteBuffer.allocate(states.size() * 16);
        for (NdnChronoState state : states) {
            buffer.put(state.toBytes());
        }
        buffer.position(0);
        return buffer;
    }

    /**
     * Decode multiple states from a network transmission
     *
     * @param buffer the encoded buffer
     * @return the decoded states
     */
    public static Set<NdnChronoState> wireDecodeMultiple(ByteBuffer buffer) {
        Set<NdnChronoState> states = new LinkedHashSet<>();
        while (buffer.position() + 16 <= buffer.limit()) {
            NdnChronoState state = new NdnChronoState(buffer.getLong(), buffer.getLong());
            states.add(state);
        }

        if (buffer.position() != buffer.limit()) {
            LOGGER.warn("While deserializing a buffer of NdnChronoStates, {} bytes were left over", buffer.limit() - buffer.position());
        }

        return states;
    }

    /**
     * @return the client identifier; e.g. the client ID
     */
    public long client() {
        return client;
    }

    /**
     * @return the message identifier
     */
    public long message() {
        return message;
    }

    /**
     * @param other another state
     * @return true if this state's client ID matches another state's client ID
     */
    @Override
    public boolean matches(State other) {
        return other instanceof NdnChronoState && this.client == ((NdnChronoState) other).client;
    }

    /**
     * TODO perhaps this should match https://github.com/named-data/jndn/blob/master/src/net/named_data/jndn/sync/sync-state-proto.proto
     *
     * @return the bytes representing this state
     */
    @Override
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(client);
        buffer.putLong(message);
        return buffer.array();
    }

    /**
     * Compare the message IDs of matching states, see {@link #matches(State)}
     *
     * @param other the state to compare
     * @return a number greater than 0 if this state matches the other state's client ID and this state's message is
     * larger; if the states don't match this method will return 0
     */
    @Override
    public int compareTo(State other) {
        if (matches(other)) {
            return (int) (this.message - ((NdnChronoState) other).message);
        }
        return 0;
    }

    /**
     * @return an NDN name serialization of the client ID
     */
    public Name.Component toClientComponent() {
        return Name.Component.fromNumberWithMarker(client, CLIENT_MARKER);
    }

    /**
     * @return an NDN name serialization of the message ID
     */
    public Name.Component toMessageComponent() {
        return Name.Component.fromNumberWithMarker(message, MESSAGE_MARKER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NdnChronoState that = (NdnChronoState) o;

        return client == that.client && message == that.message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = (int) (client ^ (client >>> 32));
        result = 31 * result + (int) (message ^ (message >>> 32));
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "NdnChronoState{" + "client=" + client + ", message=" + message + '}';
    }
}