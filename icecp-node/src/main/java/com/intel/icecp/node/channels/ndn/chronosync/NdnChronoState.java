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