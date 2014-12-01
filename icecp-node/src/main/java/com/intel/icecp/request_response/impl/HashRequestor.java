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

package com.intel.icecp.request_response.impl;

import java.net.URI;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.Metadata;
import com.intel.icecp.core.management.Channels;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.OnPublish;
import com.intel.icecp.node.utils.ChannelUtils;
import com.intel.icecp.request_response.Requestor;

/**
 * Implementation of the Requestor interface.
 *
 *
 * @param <REQUEST>
 * @param <RESPONSE>
 */
public class HashRequestor<REQUEST extends Message, RESPONSE extends Message> implements Requestor<REQUEST, RESPONSE> {

    private static final Logger logger = LogManager.getLogger();
    private final Channels channels;
    private final Class<REQUEST> requestType;
    private final Class<RESPONSE> responseType;
    private final HashMap<URI, Channel<REQUEST>> requestChannels;
    private final Metadata[] metadata;

    public HashRequestor(Channels channels, Class<REQUEST> requestType, Class<RESPONSE> responseType, Metadata... metadata) throws Exception {
        this.channels = channels;
        this.requestType = requestType;
        this.responseType = responseType;
        this.requestChannels = new HashMap<URI, Channel<REQUEST>>();
        logger.info(String.format("Created RequestType[%s]", this.requestType.toString()));
        this.metadata = metadata;
    }

    private Channel<REQUEST> getChannel(URI requestURI) {
        Channel<REQUEST> reqChannel = requestChannels.get(requestURI);
        if (reqChannel == null) {
            logger.info("Open a new Request Channel: " + requestURI);
            try {
                reqChannel = channels.openChannel(requestURI, requestType, Persistence.NEVER_PERSIST, metadata);
                requestChannels.put(requestURI, reqChannel);
                return reqChannel;
            } catch (ChannelLifetimeException e) {
                logger.error("Failed to create request channel", e);
                throw new RuntimeException(e);
            }
        } else {
            logger.info("Using existing request channel: " + requestURI);
            return reqChannel;
        }
    }

    @Override
    public CompletableFuture<RESPONSE> request(URI uri, REQUEST message) throws Exception {
        logger.info(String.format("Request Cmd[%s] URI[%s]", message, uri));

        //create the future and request channel
        CompletableFuture<RESPONSE> future = new CompletableFuture<>();
        Channel<REQUEST> reqChannel = getChannel(uri);

        //create the return channel and subscribe to it
        URI returnURI = ChannelUtils.getResponseChannelUri(reqChannel.getName(), message);
        logger.info("Open Return Channel: " + returnURI.toString());
        Channel<RESPONSE> retChannel = channels.openChannel(returnURI, responseType, Persistence.NEVER_PERSIST, metadata);

        logger.info("Subscribe to Return Channel: " + retChannel.getName());
        retChannel.subscribe(new OnPublish<RESPONSE>() {

            @Override
            public void onPublish(RESPONSE returnMessage) {
                //We received our return message, complete the future.
                logger.info(String.format("Return Channel received message CMD[%s]", message));
                future.complete(returnMessage);

                //TODO: close the return channel
//				try {
//					Thread.sleep(1000);
//					logger.info("Awake, Close the Return Channel");
//					logger.info("Closing channels");
//					retChannel.close();
//					reqChannel.close();
//				} catch (ChannelLifetimeException e) {
                // TODO Auto-generated catch block
//					e.printStackTrace();
//				}
            }
        });

        //Publish the message 
        logger.info("Publish request on: " + reqChannel.getName());
        reqChannel.publish(message);

        //TODO: close the requestchannel
        //return the future
        return future;
    }

    public void stop() {
        //if !null stop the device
    }
}
