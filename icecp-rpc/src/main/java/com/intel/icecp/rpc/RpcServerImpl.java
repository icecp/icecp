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

package com.intel.icecp.rpc;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.management.Channels;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.node.utils.ChannelUtils;

import java.net.URI;

/**
 * Provides a common place for servers to listen (subscribe) for commands
 * on a command request channel.  The server will also publish the response
 * to the command request on the response channel if provided.
 *
 */
class RpcServerImpl extends RpcBase implements RpcServer {
    private CommandRegistry registry;
    Channel<CommandRequest> requestChannel;

    public RpcServerImpl() {
        // Empty constructor for Jackson.
    }

    /**
     * Constructor
     *
     * @param channels the method name
     * @param uri URI of the module or node
     */
    public RpcServerImpl(Channels channels, URI uri) {
        this(channels, uri, new CommandRegistry());
    }

    /**
     * Constructor
     *
     * @param channels the method name
     * @param uri URI of the module or node
     * @param registry command registry of the creating node or module.
     */
    RpcServerImpl(Channels channels, URI uri, CommandRegistry registry) {
        super(channels, uri);
        this.registry = registry;
    }

    /**
     * Opens the channel for the server (node or module) to listen for commands.
     * Subscribes to the channel to start listening.
     */
    @Override
    public void serve() throws ChannelLifetimeException, ChannelIOException {
        URI requestUri = ChannelUtils.join(uri, URI_SUFFIX);
        requestChannel = channels.openChannel(requestUri, CommandRequest.class, Persistence.DEFAULT);
        requestChannel.subscribe(this::executeAndRespondTo);
    }

    /**
     * Closes the command channel for the server (node or module).
     * Stops listening.
     */
    @Override
    public void close() throws ChannelLifetimeException {
        requestChannel.close();
    }

    /**
     * Gets the command registry associated with the server.
     *
     * @return Command registry for the node or module.
     */
    @Override
    public CommandRegistry registry(){
        return registry;
    }

    /**
     * Callback for when a subscribed event is triggered.
     *
     * @param request command request from the client.
     */
    protected void executeAndRespondTo(CommandRequest request) {
        LOGGER.info("request received");
        try {
            CommandResponse response = registry.execute(request);
            publishResponse(request, response);
        } catch (RpcServiceException e) {
            LOGGER.error("Command request failed: {}", request, e);
            publishResponse(request, CommandResponse.fromError(e));
        }
    }

    /**
     * Publishes the response to the client on the responseURI if provided by the client.
     *
     * @param request command request from the client.
     * @param response command response published back to the client on the responseURI.
     */
    private void publishResponse(CommandRequest request, CommandResponse response){
        if (!(response instanceof EmptyCommandResponse)) {
            try {
                Channel<CommandResponse> responseChannel = channels.openChannel(createResponseUri(request.responseUri), CommandResponse.class, Persistence.DEFAULT);
                responseChannel.publish(response);
            } catch (ChannelLifetimeException | ChannelIOException e) {
                LOGGER.error("Command request failed, no channel available for response: {}", request, e);
            }
        }
    }
}
