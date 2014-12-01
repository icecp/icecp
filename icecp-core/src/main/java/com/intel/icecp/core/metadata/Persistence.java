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
package com.intel.icecp.core.metadata;

import com.intel.icecp.core.Metadata;

/**
 * Define how publishers and subscribers should persist messages.
 *
 */
public class Persistence implements Metadata {

    private static final long DEFAULT_PERSISTENCE_MS = 2000;
    private static final long PERSIST_FOREVER_MS = Long.MAX_VALUE;
    private static final long NEVER_PERSIST_MS = 0;

    public static final Persistence FOREVER = new Persistence(PERSIST_FOREVER_MS);
    public static final Persistence DEFAULT = new Persistence(DEFAULT_PERSISTENCE_MS);
    public static final Persistence NEVER_PERSIST = new Persistence(NEVER_PERSIST_MS);

    /**
     * The number of milliseconds to persist a message
     */
    public final long persistFor;

    /**
     * The number of milliseconds under which persistence implementations must retrieve and serve persisted messages.
     */
    public final long retrieveUnder;

    /**
     * Build a default persistence object with the default time interval, {@link #DEFAULT_PERSISTENCE_MS}.
     */
    public Persistence() {
        this(DEFAULT_PERSISTENCE_MS, DEFAULT_PERSISTENCE_MS);
    }

    /**
     * Build a persistence object with the same storage and retrieval times
     *
     * @param milliseconds the number of milliseconds to use for both {@link #persistFor} and {@link #retrieveUnder}
     */
    public Persistence(long milliseconds) {
        this(milliseconds, milliseconds);
    }

    /**
     * Build a persistence object
     *
     * @param persistFor see {@link #persistFor}
     * @param retrieveUnder see {@link #retrieveUnder}
     */
    public Persistence(long persistFor, long retrieveUnder) {
        this.persistFor = persistFor;
        this.retrieveUnder = retrieveUnder;
    }

    /**
     * Define whether all messages MUST be persisted; e.g. if a message is dropped and cannot be retrieved, Channels
     * adhering to this policy must raise an error condition.
     *
     * @return true if the publisher/subscriber MUST retain the message for the specified {@link #persistFor} period; if
     * false the publisher/subscriber must only make a best effort to persist for the {@link #persistFor} period.
     */
    public boolean mustPersist() {
        return persistFor > NEVER_PERSIST_MS;
    }

    /**
     * Define whether requests MUST be retrieved in the specified time interval;
     *
     * @return true if subscribers SHOULD time out requests longer than {@link #retrieveUnder} and publishers MUST
     * return a response in under the same; if false, publishers and subscribers are free to ignore time limits during
     * IO.
     */
    public boolean hasRetrievalLifetime() {
        return retrieveUnder > NEVER_PERSIST_MS;
    }
}
