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
package com.intel.icecp.core.event;

import com.intel.icecp.core.misc.OnPublish;
import java.net.URI;

/**
 * Defines an event service; all triggered events should go through this service
 * so that a consistent implementation is available for all event messaging
 * (e.g. publish to channel vs in-memory only)
 *
 */
public interface Events {

    /**
     * Notify subscribers that an event has occurred
     *
     * @param event the event to trigger
     */
    void notify(Event event);

    /**
     * Listen to all events
     *
     * @param callback callback fired when an event is triggered
     */
    void listen(OnPublish<Event> callback);

    /**
     * Listen to a specific event type
     *
     * @param <T>
     * @param suffix TODO remove and figure this out from type
     * @param type the type of the event to listen for
     * @param callback callback fired when an event is triggered
     */
    <T extends Event> void listen(URI suffix, Class<T> type, OnPublish<T> callback);
}
