package com.intel.icecp.rpc;


import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.ChannelLifetimeException;

/**
 * Created by nmgaston on 5/20/2016.
 */
public interface RpcServer {
    void serve() throws ChannelLifetimeException, ChannelIOException;

    void close() throws ChannelLifetimeException;

    CommandRegistry registry();
}
