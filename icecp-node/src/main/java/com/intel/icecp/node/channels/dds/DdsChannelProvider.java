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
package com.intel.icecp.node.channels.dds;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.Metadata;
import com.intel.icecp.core.channels.ChannelProvider;
import com.intel.icecp.core.metadata.Format;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.Configuration;
import com.intel.icecp.core.pipeline.Pipeline;
import java.net.URI;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Build a DDS channel
 *
 */
public class DdsChannelProvider implements ChannelProvider {

    private static final Logger logger = LogManager.getLogger();
    public static final String SCHEME = "dds";
    public static final int DEFAULT_DOMAIN_ID = 0;

    @Override
    public void start(ScheduledExecutorService pool, Configuration configuration) {
        // do nothing
    }

    @Override
    public void stop() {
        // do nothing
    }

    @Override
    public <T extends Message> Channel<T> build(URI uri, Pipeline encodingOpeations, Persistence persistence, Metadata... metadata) throws ChannelLifetimeException {
        return new DdsChannel(uri, encodingOpeations, DEFAULT_DOMAIN_ID, persistence);
    }

    @Override
    public String scheme() {
        return SCHEME;
    }
}
