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
package com.intel.icecp.core.channels;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.misc.ChannelIOException;
import java.util.concurrent.CompletableFuture;

/**
 *
 * @param <T>
 */
public interface HistoricalChannel<T extends Message> extends Channel<T> {

    /**
     * Retrieve the earliest message available on this channel. The channel
     * serializes the message using the channel's Format so that it can be
     * deserialized by any other device. Access will be controlled with the
     * ChannelPermission grant; ChannelPermission must control what channels are
     * allowed by the caller (e.g. new ChannelPermission("subscribe",
     * "/some/prefix/*")).
     *
     * @return the latest {@link com.intel.icecp.core.Message} published on this
     * channel
     * @throws ChannelIOException when the channel fails to receive
     * {@link Message}s
     */
    CompletableFuture<T> earliest() throws ChannelIOException;

    /**
     * Retrieve a specific message from the channel. TODO think this through...
     *
     * @param id the {@link Message} ID
     * @return the latest {@link com.intel.icecp.core.Message} published on this
     * channel
     * @throws ChannelIOException when the channel fails to receive
     * {@link Message}s
     */
    CompletableFuture<T> get(long id) throws ChannelIOException;
}
