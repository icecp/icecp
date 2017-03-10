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

package com.intel.icecp.request_response.impl;

import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.Metadata;
import com.intel.icecp.core.management.Channels;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.OnPublish;
import com.intel.icecp.node.utils.ChannelUtils;
import com.intel.icecp.request_response.OnRequest;
import com.intel.icecp.request_response.Responder;

/**
 * Implementation of the Responder interface.
 *
 * @param <REQUEST>
 * @param <RESPONSE>
 */
public class HashResponder<REQUEST extends Message, RESPONSE extends Message> implements Responder<REQUEST, RESPONSE> {

    private static final Logger logger = LogManager.getLogger();
    private final Channels channels;
    private final Class<REQUEST> requestType;
    private final Class<RESPONSE> responseType;
    private final Metadata[] metadata;

    public HashResponder(Channels channels, Class<REQUEST> requestType, Class<RESPONSE> responseType, Metadata... metadata) throws Exception {
        this.channels = channels;
        this.requestType = requestType;
        this.responseType = responseType;
        logger.info(String.format("Created ResponseType[%s]", this.responseType.toString()));
        this.metadata = metadata;
    }

    @Override
    public void listen(URI requestUri, final OnRequest<REQUEST, RESPONSE> listenCallback) throws Exception {
        //open the listener channel
        Channel<REQUEST> reqChannel = channels.openChannel(requestUri, requestType, Persistence.NEVER_PERSIST, metadata);
        logger.info("Subscribe to request channel: " + reqChannel.getName());

        //subscribe to the request channel
        reqChannel.subscribe(new OnPublish<REQUEST>() {
            @Override
            public void onPublish(REQUEST request) {
                logger.info("Received request: " + reqChannel.getName());

                //When a request comes in, call the callback, they will publish the response
                RESPONSE response = listenCallback.onRequest(request);

                //create the return channel
                try {
                    URI returnURI = ChannelUtils.getResponseChannelUri(reqChannel.getName(), request);
                    Channel<RESPONSE> retChannel = channels.openChannel(returnURI, responseType, Persistence.NEVER_PERSIST, metadata);

                    logger.info("Publish to return channel: " + retChannel.getName());
                    retChannel.publish(response);

                    //TODO: close the channel
//					Thread.sleep(1000);
//					logger.info("Awake, Close the Return Channel");
//					retChannel.close();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }
}
