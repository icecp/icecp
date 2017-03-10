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
