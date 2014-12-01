package com.intel.icecp.rpc;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.management.Channels;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.node.utils.ChannelUtils;

/**
 * Provides a common place for clients to publish command requests
 * on a command request channel.  The client can also optionally subscribe
 * to a command response channel.
 *
 */
class RpcClientImpl extends RpcBase implements RpcClient {
    public RpcClientImpl() {
        // Empty constructor for Jackson.
    }

    /**
     * Constructor
     *
     * @param channels the method name
     * @param uri URI of the module or node
     */
    public RpcClientImpl(Channels channels, URI uri) {
        super(channels, uri);
    }

    /**
     * Client calls to make a command request and receives a future response.
     *
     * @param request Command request.
     * @return future command response
     */
    @Override
    public CompletableFuture<CommandResponse> call(CommandRequest request) {
        CompletableFuture<CommandResponse> future = new CompletableFuture<>();

        try {
            subscribeResponse(future, request);
            URI requestUri = ChannelUtils.join(uri, URI_SUFFIX);
            
             // TODO: For now we will close the channel immediately after the publish. In a future fix we will close the channel based on the persistence time.
            try (Channel<CommandRequest> requestChannel = channels.openChannel(requestUri, CommandRequest.class, Persistence.DEFAULT)) {
                requestChannel.publish(request);
            }

        } catch (ChannelIOException | ChannelLifetimeException e) {
            LOGGER.error("Command request failed, no channel available for request: {}", request, e);
            future.completeExceptionally(e);
        }

        return future;
    }

    /**
     * Client subscribes to the response channel if provided.
     *
     * @param future future command response
     * @param request command request containing the responseUri.
     * @return future command response
     */
    private void subscribeResponse(CompletableFuture<CommandResponse> future, CommandRequest request) throws ChannelLifetimeException, ChannelIOException {
        if (request.responseUri != null) {
            Channel<CommandResponse> responseChannel = channels.openChannel(createResponseUri(request.responseUri), CommandResponse.class, Persistence.DEFAULT);
            responseChannel.subscribe(message -> {
                if (message.err) {
                    future.completeExceptionally(new RpcServiceException((String) message.out));
                } else {
                    future.complete(message);
                }
                // TODO close channel?
            });
        } else {
            future.complete(null);
        }
    }
}
