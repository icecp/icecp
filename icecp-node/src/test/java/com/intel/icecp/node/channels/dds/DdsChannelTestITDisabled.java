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

import com.intel.icecp.core.channels.ChannelTestIT;
import java.net.URI;

/*Imports for individual tests*/
import com.intel.icecp.core.Channel;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.pipeline.Pipeline;
import org.junit.Ignore;

/**
 * Don't enable this test until DdsChannel subscribe timing issue is resolved.
 *
 */
@Ignore
public class DdsChannelTestITDisabled extends ChannelTestIT {

    @Override
    public Channel newInstance(String channelName, Pipeline pipeline, Persistence persistence) {
        try {
            DdsChannelProvider builder = new DdsChannelProvider();
            builder.start(null, null);
            return builder.build(URI.create("dds:" + channelName), pipeline, persistence);
        } catch (ChannelLifetimeException ex) {
            throw new IllegalArgumentException("Unable to create channel instance.", ex);
        }
    }
}
