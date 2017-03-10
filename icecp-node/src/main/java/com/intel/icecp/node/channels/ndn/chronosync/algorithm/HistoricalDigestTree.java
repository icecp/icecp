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

package com.intel.icecp.node.channels.ndn.chronosync.algorithm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.security.MessageDigest;
import java.util.*;

/**
 * Track changes to a digest tree over time; as new states are added, a historical log is kept that allows us to
 * determine the difference between the current set of states and a previous set (see {@link
 * #complement(Digest)}). The digest of the current set of states is retrievable
 * using {@link #digest()}.
 * <p>
 * A common use case would be to maintain a Merkle-like tree (one level deep) which would allow users to determine the
 * difference between a past state (identified using a digest) and the current state:
 * <p>
 * <pre>{@code
 * Digest oldDigest = instance.digest();
 * instance.add(state1, state2);
 * Set diff = instance.complement(oldDigest);
 * assertTrue(diff.contains(state1));
 * assertTrue(diff.contains(state2));
 * }</pre>
 *
 * States should be comparable and matchable; a {@link State} must implement the common Java {@link Comparable}
 * interface but also the {@link State#matches(State)}, which allows this class to
 * find states that match but then compare them and replace older versions.
 *
 */
public class HistoricalDigestTree<T extends State> {

    private static final Logger LOGGER = LogManager.getLogger();
    private final Digest empty;
    private final List<T> current = new ArrayList<>();
    private final DigestSetLinkedHashMap<Digest, Set<T>> history;
    private final int maxHistorySize;
    private final MessageDigest digestAlgorithm;

    /**
     * @param maxHistorySize  the number of change sets to track
     * @param digestAlgorithm the digest algorithm to use, e.g. MessageDigest.getInstance("SHA-256")
     */
    public HistoricalDigestTree(int maxHistorySize, MessageDigest digestAlgorithm) {
        this.maxHistorySize = maxHistorySize;
        this.history = new DigestSetLinkedHashMap<>(maxHistorySize);
        this.digestAlgorithm = digestAlgorithm;
        this.empty = buildDigest();
        log(empty, current);
    }

    /**
     * @return the digest for the current set of states
     */
    public final Digest digest() {
        return history.latest();
    }

    private Digest buildDigest() {
        for (T s : current) {
            digestAlgorithm.update(s.toBytes());
        }
        return new Digest(digestAlgorithm.digest());
    }

    /**
     * Add some states to the current state set; this method will not change the current state set when a state is older
     * than the matching state currently held
     *
     * @param states the state objects to add to the current set
     * @return true if the current state has changed
     */
    public boolean add(Collection<T> states) {
        LOGGER.trace("Adding {} states", states.size());
        boolean changed = false;
        for (T s : states) {
            changed |= update(s);
        }

        if (changed) {
            log(buildDigest(), current);
        }

        return changed;
    }

    /**
     * Helper method for {@link #add(java.util.Collection)}
     *
     * @param states the state objects to add to the current set
     * @return true if the current state has changed
     */
    public boolean add(T... states) {
        return add(Arrays.asList(states));
    }

    /**
     * @param newState asdf
     * @return true if the overall current state has changed
     */
    private boolean update(T newState) {
        int index = find(newState);
        if (index > -1) {
            if (isNewer(newState, current.get(index))) {
                current.set(index, newState);
                LOGGER.trace("Replaced state: {}", newState);
                return true;
            } else {
                LOGGER.trace("Skipped state: {}", newState);
                return false; // drop new state, current state was newer
            }
        } else {
            current.add(newState);
            LOGGER.trace("Added state: {}", newState);
            return true;
        }
    }

    /**
     * Uses {@link State#matches(State)} to compare states for
     *
     * @param state the state to match with
     * @return the index to a state matching the given state
     */
    protected int find(T state) {
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).matches(state)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Log the given states in the historical log
     *
     * @param digest the digest to use
     * @param states the states to log
     */
    private void log(Digest digest, Collection<T> states) {
        history.put(digest, new HashSet<>(states));
    }

    /**
     * @param a the first state
     * @param b the second state
     * @return true if state a is a greater/newer state than state b; this depends on the implementation of the state
     * type
     */
    public boolean isNewer(T a, T b) {
        return a.compareTo(b) > 0;
    }

    /**
     * @param digest the digest to test
     * @return true if the digest given matches the empty digest for this tree
     */
    public boolean isEmpty(Digest digest) {
        return empty.equals(digest);
    }

    /**
     * @param digest the digest to test
     * @return true if the digest given matches the current digest for this tree
     */
    public boolean isCurrent(Digest digest) {
        return digest().equals(digest);
    }

    /**
     * @param digest the digest to test
     * @return true if the digest given matches any digest logged in this tree's history
     */
    public boolean isKnown(Digest digest) {
        return history.containsKey(digest);
    }

    /**
     * Determine the set of states that are held currently but not held in the states denoted by the given digest; if
     * the digest is unrecognized, the entire current set of states is returned
     *
     * @param digest the digest to compare against
     * @return an unmodifiable set of states
     */
    public Set<T> complement(Digest digest) {
        if (isKnown(digest)) {
            Set<T> oldStates = history.get(digest);
            Set<T> diffStates = new HashSet<>(current);
            diffStates.removeAll(oldStates);
            return Collections.unmodifiableSet(diffStates);
        } else {
            return all();
        }
    }

    /**
     * @return an unmodifiable set of current states
     */
    public Set<T> all() {
        return Collections.unmodifiableSet(new HashSet<>(current));
    }

    private static class DigestSetLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
        private final int maxHistorySize;
        private K latest;

        /**
         * @param maxSize the maximum allowed number of records to store
         */
        public DigestSetLinkedHashMap(int maxSize) {
            this.maxHistorySize = maxSize;
        }

        /**
         * Override the LinkedHashMap implementation to limit the size of the history; see
         * https://docs.oracle.com/javase/8/docs/api/java/util/LinkedHashMap.html#removeEldestEntry-java.util.Map.Entry-
         *
         * @param eldest the oldest entry in the history log
         * @return true if this entry should be removed
         */
        @Override
        public boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            LOGGER.trace("Removing digest from history: {}", eldest.getKey());
            return size() > maxHistorySize;
        }

        /**
         * TODO look at Josh Bloch's ForwardingMap, item 16 in Effective Java
         *
         * @param key   the map key
         * @param value the map value
         * @return the map value
         */
        @Override
        public V put(K key, V value) {
            latest = key;
            return super.put(key, value);
        }

        /**
         * @return the latest item added to this set
         */
        public K latest() {
            return latest;
        }
    }
}