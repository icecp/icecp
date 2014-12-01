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
