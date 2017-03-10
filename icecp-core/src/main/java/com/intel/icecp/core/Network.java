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
package com.intel.icecp.core;

import com.intel.icecp.core.Network.Endpoint;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * Represent a network of connected nodes; implementing classes should be written as service providers
 * (https://docs.oracle.com/javase/tutorial/sound/SPI-intro.html) and included by adding a line with the class name
 * (e.g. com.implementing.network.class) to META-INF/services/com.intel.icecp.core.Network.
 *
 * @param <T> the endpoint descriptor type specific to each network
 */
public interface Network<T extends Endpoint> {

    /**
     * @return the protocol scheme of the network implemented (e.g. "ndn")
     */
    String scheme();

    /**
     * @return an {@link Endpoint} representing the current, local {@link Node}
     */
    T local();

    /**
     * @return a list of all known, non-local endpoints that are connected to {@link #local()}
     */
    T[] list();

    /**
     * Add an endpoint to the network
     *
     * @param endpoint the {@link Endpoint} to add
     * @return a future that completes when all network IO is finished
     * @throws IOException if the connection fails
     */
    CompletableFuture<Void> connect(T endpoint) throws IOException;

    /**
     * Remove an endpoint from the network
     *
     * @param endpoint the {@link Endpoint} to remove
     * @return a future that completes when all network IO is finished
     * @throws IOException if the disconnection fails
     */
    CompletableFuture<Void> disconnect(T endpoint) throws IOException;

    /**
     * Advertise the {@link #local()} endpoint on the network; a corresponding {@link #discover()} from another endpoint
     * should return this endpoint's information.
     *
     * @return a future that completes when all network IO is finished
     * @throws IOException if the advertisement fails
     */
    CompletableFuture<Void> advertise() throws IOException;

    /**
     * @return the list of advertising endpoints (see {@link #advertise()}) that are reachable from this endpoint.
     * @throws IOException if the discovery operation fails
     */
    CompletableFuture<T[]> discover() throws IOException;

    /**
     * @param endpoint the {@link Endpoint} to measure
     * @return the round-trip time (in milliseconds) to reach a distant endpoint over the network
     * @throws IOException if the round-trip time operation fails
     */
    CompletableFuture<Integer> rtt(T endpoint) throws IOException;

    /**
     * Represent an endpoint in the network; implement a subclass of this for each network type (e.g.
     * NdnNetworkEndpoint)
     */
    interface Endpoint {
        URI toUri();
    }
}
