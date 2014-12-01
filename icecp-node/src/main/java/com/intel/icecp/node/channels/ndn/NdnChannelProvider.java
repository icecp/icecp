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

package com.intel.icecp.node.channels.ndn;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.Metadata;
import com.intel.icecp.core.channels.ChannelProvider;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.Configuration;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.node.utils.NetworkUtils;
import com.intel.jndn.utils.impl.KeyChainFactory;

import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.ThreadPoolFace;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.transport.AsyncTcpTransport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Build an NDN channel; note that while we test different ways to send messages with NDN we will expose different
 * channel types. These are configurable in the ndn.json configuration file along with: <ul> <li>channel-type:
 * notification (default), chronosync (experimental)</li> <li>uri: localhost (default)--the location of the NFD to use
 * for routing</li> </ul>
 *
 */
public class NdnChannelProvider implements ChannelProvider {

    public static final String SCHEME = "ndn";
    private static final String CHANNEL_TYPE_CHRONOSYNC = "chronosync";
    private static final String CHANNEL_TYPE_NOTIFICATION = "notification";
    private static final int ASYNC_IO_THREAD_POOL_SIZE = 8; // number of threads handling the async socket completion.
    private static final Logger logger = LogManager.getLogger();
    private Face prefixFace;
    private Face interestFace;
    private ScheduledExecutorService eventLoop;
    private Name prefix;
    private boolean started = false;
    private String channelType;

    /**
     * Default constructor used for SPI loading; the loading class must call the {@link #start(ScheduledExecutorService,
     * Configuration)} method
     */
    public NdnChannelProvider() {
        // must call start()
    }

    /**
     * Specific constructor with default channel type; equivalent to calling {@link #start(ScheduledExecutorService,
     * Configuration)}
     *
     * @param identityPrefix the name used for signature creation (e.g. /intel/node/...)
     * @param prefixFace a face used for registering prefixes for consumers to retrieve data on
     * @param interestFace a face used for sending interests; this is different from the prefixFace so that a channel
     * can retrieve its own published messages
     * @param eventLoop the thread pool used for event processing
     */
    public NdnChannelProvider(String identityPrefix, Face prefixFace, Face interestFace, ScheduledExecutorService eventLoop) {
        this(identityPrefix, prefixFace, interestFace, eventLoop, CHANNEL_TYPE_NOTIFICATION);
    }

    /**
     * Specific constructor; equivalent to calling {@link #start(ScheduledExecutorService, Configuration)}
     *
     * @param identityPrefix the name used for signature creation (e.g. /intel/node/...)
     * @param prefixFace a face used for registering prefixes for consumers to retrieve data on
     * @param interestFace a face used for sending interests; this is different from the prefixFace so that a channel
     * can retrieve its own published messages
     * @param eventLoop the thread pool used for event processing
     * @param channelType since NDN channels are actively developed, this allows users to choose between different
     * implementation
     */
    public NdnChannelProvider(String identityPrefix, Face prefixFace, Face interestFace, ScheduledExecutorService eventLoop, String channelType) {
        start0(identityPrefix, prefixFace, interestFace, eventLoop, channelType);
    }

    /**
     * @param identityPrefix the name used for signature creation (e.g. /intel/node/...)
     * @param hostName the host name used for the faces
     * @param pool the thread pool used for event processing
     */
    public NdnChannelProvider(String identityPrefix, String hostName, ScheduledExecutorService pool) {
        start0(identityPrefix, setupFace(hostName, pool), setupFace(hostName, pool), pool, CHANNEL_TYPE_NOTIFICATION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(ScheduledExecutorService pool, Configuration configuration) {
        String hostName = configuration.getOrDefault("localhost", "uri");
        String channelType = configuration.getOrDefault("default", "channel-type");
        start0(getIdentityFromHostName(), setupFace(hostName, pool), setupFace(hostName, pool), pool, channelType);
    }

    /**
     * @param hostName the host name of the
     * @param pool the thread pool to use for asynchronous IO
     * @return an asynchronous face
     */
    private Face setupFace(String hostName, ScheduledExecutorService pool) {
        ScheduledExecutorService asyncIoThreadPool = Executors.newScheduledThreadPool(ASYNC_IO_THREAD_POOL_SIZE);
        AsyncTcpTransport transport = new AsyncTcpTransport(asyncIoThreadPool);
        AsyncTcpTransport.ConnectionInfo connectionInfo = new AsyncTcpTransport.ConnectionInfo(hostName, 6363, true);
        return new ThreadPoolFace(pool, transport, connectionInfo);
    }

    /**
     * @return the host name of the machine or "UNKNOWN" if unavailable
     */
    private String getIdentityFromHostName() {
        try {
            return NetworkUtils.getHostName();
        } catch (UnknownHostException ex) {
            logger.warn("Failed to retrieve host name for signing identity");
            return "UNKNOWN";
        }
    }

    /**
     * Start the provider
     *
     * @param identityPrefix the name used for signature creation (e.g. /intel/node/...)
     * @param prefixFace a face used for registering prefixes for consumers to retrieve data on
     * @param interestFace a face used for sending interests; this is different from the prefixFace so that a channel
     * can retrieve its own published messages
     * @param pool the thread pool used for event processing
     * @param channelType since NDN channels are actively developed, this allows users to choose between different
     * implementation
     */
    private void start0(String identityPrefix, Face prefixFace, Face interestFace, ScheduledExecutorService pool, String channelType) {
        this.prefixFace = prefixFace;
        this.interestFace = interestFace;
        this.eventLoop = pool;
        this.prefix = new Name(identityPrefix);
        this.channelType = channelType;

        setupKeyChainOnFace(prefixFace);
        checkIfForwarderIsLocal(prefixFace);
        started = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String scheme() {
        return SCHEME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        prefixFace.shutdown();
        started = false;
    }

    /**
     * Setup the key chain on the face; without it, the local application will not be able to register prefixes (but
     * will be able to express interests)
     */
    private void setupKeyChainOnFace(Face face) {
        try {
            KeyChain keyChain = KeyChainFactory.configureKeyChain(prefix);
            face.setCommandSigningInfo(keyChain, keyChain.getDefaultCertificateName());
        } catch (SecurityException e) {
            throw new IllegalStateException("Failed to set command signature; builder cannot proceed without a secure face.", e);
        }
    }

    /**
     * Print logger warnings if the forwarder is not local
     *
     * @param forwarder a {@link Face} to an NFD instance
     */
    private void checkIfForwarderIsLocal(Face forwarder) {
        try {
            if (!forwarder.isLocal()) {
                logger.warn("The given NFD instance is not on the localhost; this may affect certain modules.");
            }
        } catch (IOException e) {
            logger.warn("Failed to determine if the given forwarder is local or not.");
        }
    }

    /**
     * {@inheritDoc}
     *
     * TODO fix unchecked warnings in this method
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Message> Channel<T> build(URI uri, Pipeline pipeline, Persistence persistence, Metadata... metadata) throws ChannelLifetimeException {
        if (!isStarted()) {
            throw new IllegalStateException("The builder has not configured the face; ensure start() has run.");
        }

        switch (channelType) {
            case CHANNEL_TYPE_CHRONOSYNC:
                return new NdnChronoSyncChannel(uri, pipeline, interestFace, eventLoop, persistence,
                        metadata);
            case CHANNEL_TYPE_NOTIFICATION:
            default:
                return new NdnNotificationChannel(uri, pipeline, prefixFace, interestFace, eventLoop, persistence,
                        metadata);
        }
    }

    /**
     * @return true if the builder has been started
     */
    private boolean isStarted() {
        return started;
    }
}