package com.intel.icecp.rpc;

import com.intel.icecp.core.management.Channels;
import com.intel.icecp.node.utils.ChannelUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;

/**
 * Base class for calls to the RPC library.
 *
 */
class RpcBase {
    protected URI uri;
    protected Channels channels;
    private static final String RESPONSE_URI_SUFFIX = "$ret";
    protected static final String URI_SUFFIX = "$cmd";
    protected static final Logger LOGGER = LogManager.getLogger();

    public RpcBase() {
        // Empty constructor for Jackson.
    }

    protected RpcBase(Channels channels, URI uri) {
        this.channels = channels;
        this.uri = uri;
    }

    protected URI createResponseUri(URI responseUri) {
        return responseUri.isAbsolute() ? responseUri : ChannelUtils.join(uri, RESPONSE_URI_SUFFIX, responseUri.toString());
    }
}
