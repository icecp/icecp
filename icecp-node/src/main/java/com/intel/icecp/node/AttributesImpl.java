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
package com.intel.icecp.node;

import com.intel.icecp.core.Attribute;
import com.intel.icecp.core.Channel;
import com.intel.icecp.core.attributes.AttributeMessage;
import com.intel.icecp.core.attributes.AttributeNotFoundException;
import com.intel.icecp.core.attributes.AttributeNotWriteableException;
import com.intel.icecp.core.attributes.AttributePermission;
import com.intel.icecp.core.attributes.AttributeRegistrationException;
import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.attributes.AttributesNamespace;
import com.intel.icecp.core.attributes.OnAttributeChanged;
import com.intel.icecp.core.attributes.WriteableAttribute;
import com.intel.icecp.core.channels.OnLatest;
import com.intel.icecp.core.channels.Token;
import com.intel.icecp.core.management.Channels;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.node.utils.ChannelUtils;
import com.intel.icecp.node.utils.SecurityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link Attributes} collection. This implementation creates a read channel with name {@link
 * #baseUri} + {@link AttributesNamespace#READ_SUFFIX} + {@code attributeName} and a write channel with {@link
 * #baseUri} + {@link AttributesNamespace#WRITE_SUFFIX} + {@code attributeName} using the passed channel provider.
 * <p>
 * Two maps are used for accommodating the API decision to allow access by class reference or by attribute name. These
 * maps must be maintained in a consistent state, i.e. if an entry exists in {@link #names} it must exist in {@link
 * #attributes} and vice versa.
 * <p>
 * This class is thread-safe in the following senses: 1) use of an {@link Attribute} through this class will always be
 * synchronized, e.g. no two calls to {@code attribute.value()} will occur simultaneously; 2) adding and removing
 * attributes to the set is protected from multiple thread access
 *
 */
class AttributesImpl implements Attributes {

    public static final String SUFFIX = "$attributes";
    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<String, Class<? extends Attribute>> names = new ConcurrentHashMap<>();
    private final Map<Class<? extends Attribute>, AttributeEntry> attributes = new ConcurrentHashMap<>();
    private final Object indexLock = new Object();
    private final Map<String, Collection<OnAttributeChanged>> callbacks = new ConcurrentHashMap<>();
    private final Channels channels;
    private final URI baseUri;

    /**
     * @param channels the channels on which to expose the attributes for reading and writing
     * @param baseUri the URI of the thing described by these attributes
     */
    AttributesImpl(Channels channels, URI baseUri) {
        this.channels = channels;
        this.baseUri = baseUri;
    }

    /**
     * Add a new attribute implementation, opening necessary channels for receiving remote values and publishing changed
     * values remotely. TODO permission logic for remote write is unimplemented, will allow remote access
     *
     * @param attribute a new attribute implementation
     * @throws AttributeRegistrationException if this operation fails
     */
    @SuppressWarnings("unchecked")
    @Override
    public void add(Attribute attribute) throws AttributeRegistrationException {
        SecurityUtils.checkPermission(new AttributePermission(baseUri.toString(), "add"));

        try {
            // expose attribute remotely
            Channel readChannel = setupReadChannel(baseUri, attribute);

            // monitor remote changes
            Channel writeChannel = null;
            if (attribute instanceof WriteableAttribute) {
                writeChannel = setupWriteChannel(baseUri, (WriteableAttribute) attribute, readChannel);
            }

            // add internally
            AttributeEntry attributeEntry = new AttributeEntry(attribute, readChannel, writeChannel);
            synchronized (indexLock) {
                names.put(attribute.name(), attribute.getClass());
                attributes.put(attribute.getClass(), attributeEntry);
            }

            // trigger initial change event
            triggerCallbacks(attributeEntry, null, attribute.value());
        } catch (ChannelLifetimeException | ChannelIOException e) {
            throw new AttributeRegistrationException("The attribute could not be added to the attribute set: " + baseUri, e);
        }
    }

    /**
     * Build the read channel for remote access to attribute values
     *
     * @param baseUri the URI of the thing described by these attributes
     * @param attribute the attribute to build the channel for
     * @param <T> the type of value in the attribute
     * @return a channel for reading attribute values
     * @throws ChannelLifetimeException if the channel cannot be opened
     */
    @SuppressWarnings("unchecked")
    private <T> Channel<AttributeMessage<T>> setupReadChannel(URI baseUri, Attribute<T> attribute) throws ChannelLifetimeException {
        // create channel
        URI uri = ChannelUtils.join(baseUri, AttributesNamespace.READ_SUFFIX, attribute.name());
        Channel<AttributeMessage<T>> channel = channels.openChannel(uri,
                Token.fromTree(AttributeMessage.class, attribute.type()), new Persistence());

        // answer requests with latest value
        channel.onLatest(() -> {
            synchronized (attribute) {
                return new OnLatest.Response<>(new AttributeMessage(attribute.value()));
            }
        });

        return channel;
    }

    /**
     * Build the write channel for remote access to attribute values
     *
     * @param baseUri the URI of the thing described by these attributes
     * @param attribute the attribute to build the channel for
     * @param readChannel the read channel, necessary for publishing new values if an attribute is changed
     * @param <T> the type of value in the attribute
     * @return the channel for writing attributes
     * @throws ChannelLifetimeException if the channel cannot be opened
     * @throws ChannelIOException if the channel cannot be subscribed to
     */
    @SuppressWarnings("unchecked")
    private <T> Channel<AttributeMessage<T>> setupWriteChannel(URI baseUri, WriteableAttribute<T> attribute, Channel<AttributeMessage<T>> readChannel) throws ChannelLifetimeException, ChannelIOException {
        // create channel
        URI uri = ChannelUtils.join(baseUri, AttributesNamespace.WRITE_SUFFIX, attribute.name());
        Channel<AttributeMessage<T>> channel = channels.openChannel(uri,
                Token.fromTree(AttributeMessage.class, attribute.type()), new Persistence());

        // on incoming change, modify attribute value and re-publish
        channel.subscribe(message -> {
            if (isAllowed()) {
                synchronized (attribute) {
                    attribute.value(message.d);
                }

                try {
                    readChannel.publish(new AttributeMessage<>(attribute.value()));
                } catch (ChannelIOException e) {
                    LOGGER.error("Failed to publish new attribute '{}' value: {}", attribute.name(), attribute.value(), e);
                    // no good way to propagate this exception so we just log
                }
            }
        });

        return channel;
    }

    /**
     * TODO this method should perform any ACL logic to allow disallow users, additional parameters will likely be
     * needed
     *
     * @return true if the remote requestor is allowed to c6ange the attribute value
     */
    private static boolean isAllowed() {
        return true;
    }

    /**
     * Remove an attribute, closing the associated channel. {@inheritDoc}
     */
    @Override
    public <T> T remove(Class<? extends Attribute<T>> attributeType) throws AttributeNotFoundException {
        SecurityUtils.checkPermission(new AttributePermission(baseUri.toString(), "remove"));
        AttributeEntry<T> attribute = find(attributeType);
        return remove(attribute);
    }

    /**
     * Remove an attribute, closing the associated channel. {@inheritDoc}
     */
    @Override
    public <T> T remove(String attributeName, Class<T> attributeValueType) throws AttributeNotFoundException {
        SecurityUtils.checkPermission(new AttributePermission(baseUri.toString(), "remove"));
        AttributeEntry<T> entry = find(attributeName);
        return remove(entry);
    }

    /**
     * Helper method to avoid code duplication
     *
     * @param entry the attribute to remove
     * @param <T> the attribute value type
     * @return the latest attribute value
     */
    private <T> T remove(AttributeEntry<T> entry) {
        // retrieve entry
        synchronized (indexLock) {
            assert names.remove(entry.attribute.name()) != null;
            assert attributes.remove(entry.attribute.getClass()) != null;
        }

        T oldValue;
        synchronized (entry.attribute) {
            oldValue = entry.attribute.value();
        }

        // send new value == null
        try {
            triggerCallbacks(entry, oldValue, null);
        } catch (ChannelIOException e) {
            LOGGER.error("Failed to publish attribute change, continuing on: {}", entry.attribute.name(), e);
        }

        // clean up open channels
        entry.closeChannels();

        return oldValue;
    }

    @Override
    public boolean has(Class<? extends Attribute> attributeType) {
        return attributes.containsKey(attributeType);
    }

    @Override
    public boolean has(String attributeName) {
        return names.containsKey(attributeName);
    }

    @Override
    public <T> T get(Class<? extends Attribute<T>> attributeType) throws AttributeNotFoundException {
        SecurityUtils.checkPermission(new AttributePermission(baseUri.toString(), "read"));
        AttributeEntry<T> entry = find(attributeType);
        synchronized (entry.attribute) {
            return entry.attribute.value();
        }
    }

    @Override
    public <T> T get(String attributeName, Class<T> attributeValueType) throws AttributeNotFoundException {
        SecurityUtils.checkPermission(new AttributePermission(baseUri.toString(), "read"));
        AttributeEntry<T> entry = find(attributeName);
        synchronized (entry.attribute) {
            return entry.attribute.value();
        }
    }

    /**
     * Helper method for finding an attribute; synchronizes on the indices
     *
     * @param attributeType the attribute type
     * @return the found attribute
     * @throws AttributeNotFoundException if the attribute is not found
     */
    @SuppressWarnings("unchecked")
    <T> AttributeEntry<T> find(Class<? extends Attribute> attributeType) throws AttributeNotFoundException {
        synchronized (indexLock) {
            AttributeEntry<T> attributeEntry = attributes.get(attributeType);
            if (attributeEntry == null)
                throw new AttributeNotFoundException("Failed to find attribute by class: " + attributeType);
            return attributeEntry;
        }
    }

    /**
     * Helper method for finding an attribute; synchronizes on the indices
     *
     * @param attributeName the attribute name
     * @return the found attribute
     * @throws AttributeNotFoundException if the attribute is not found
     */
    @SuppressWarnings("unchecked")
    private <T> AttributeEntry<T> find(String attributeName) throws AttributeNotFoundException {
        synchronized (indexLock) {
            Class<? extends Attribute> attributeClass = names.get(attributeName);
            if (attributeClass == null)
                throw new AttributeNotFoundException("Failed to find attribute by name: " + attributeName);
            AttributeEntry<T> attributeEntry = attributes.get(attributeClass);
            if (attributeEntry == null)
                throw new AttributeNotFoundException("Failed to find attribute by class: " + attributeClass);
            return attributeEntry;
        }
    }


    @Override
    public <T> void set(Class<? extends Attribute<T>> attributeType, T newValue) throws AttributeNotFoundException, AttributeNotWriteableException {
        SecurityUtils.checkPermission(new AttributePermission(baseUri.toString(), "write"));
        AttributeEntry<T> entry = find(attributeType);
        set(entry, newValue);
    }

    @Override
    public <T> void set(String attributeName, T newValue) throws AttributeNotFoundException, AttributeNotWriteableException {
        SecurityUtils.checkPermission(new AttributePermission(baseUri.toString(), "write"));
        AttributeEntry<T> entry = find(attributeName);
        set(entry, newValue);
    }

    /**
     * Helper method for setting the value of writeable attributes
     *
     * @param entry the attribute (possibly writeable) to attempt to write to
     * @param newValue the new value to write
     * @param <T> the attribute value type
     */
    private <T> void set(AttributeEntry<T> entry, T newValue) throws AttributeNotWriteableException {
        assert entry != null; // this check should have already occurred by this point

        if (!(entry.attribute instanceof WriteableAttribute)) {
            throw new AttributeNotWriteableException("The specified attribute is not writeable: " + entry.attribute.name());
        }

        // change value
        T oldValue;
        synchronized (entry.attribute) {
            oldValue = entry.attribute.value();
            ((WriteableAttribute<T>) entry.attribute).value(newValue);
        }

        // publish the change
        try {
            triggerCallbacks(entry, oldValue, newValue);
        } catch (ChannelIOException e) {
            LOGGER.error("Failed to publish attribute change, continuing on: {}", entry.attribute.name(), e);
        }
    }

    @Override
    public int size() {
        assert attributes.size() == names.size();
        synchronized (indexLock) {
            return attributes.size();
        }
    }

    @Override
    public Set<String> keySet() {
        Set<String> myKeySet = names.keySet();
        Set<String> clonedSet = new HashSet<>();
        clonedSet.addAll(myKeySet);
        return clonedSet;
    }

    @Override
    public void observe(String attributeName, OnAttributeChanged onAttributeChanged) {
        SecurityUtils.checkPermission(new AttributePermission(baseUri.toString(), "read"));
        if (!callbacks.containsKey(attributeName)) {
            callbacks.put(attributeName, new ArrayList<>(1));
        }
        callbacks.get(attributeName).add(onAttributeChanged);
    }

    /**
     * Helper method for triggering all of the callbacks TODO do this in a thread pool? Note that access to this is not
     * synchronized because we don't care if callbacks are added while we trigger already-registered callbacks
     *
     * @param entry the attribute name
     * @param oldValue the old value
     * @param newValue the new value
     */
    @SuppressWarnings("unchecked")
    private <T> void triggerCallbacks(AttributeEntry<T> entry, T oldValue, T newValue) throws ChannelIOException {
        // publish new value remotely
        entry.readChannel.publish(new AttributeMessage<>(newValue));

        // fire all locally registered callbacks
        String attributeName = entry.attribute.name();
        if (callbacks.containsKey(attributeName)) {
            callbacks.get(attributeName).forEach((OnAttributeChanged cb) -> cb.onAttributeChanged(attributeName, oldValue, newValue));
        }
    }

    @Override
    public Map<String, Object> toMap() {
        SecurityUtils.checkPermission(new AttributePermission(baseUri.toString(), "read"));

        return attributes.values().stream().collect(Collectors.toMap((AttributeEntry a) -> a.attribute.name(), (AttributeEntry a) -> a.attribute.value()));
    }

    @Override
    public String toString() {
        return toMap().toString();
    }

    /**
     * Helper class for mapping attributes and their channels
     */
    class AttributeEntry<T> {
        public final Attribute<T> attribute;
        final Channel<AttributeMessage<T>> readChannel;
        final Channel<AttributeMessage<T>> writeChannel;

        AttributeEntry(Attribute<T> attribute, Channel<AttributeMessage<T>> readChannel, Channel<AttributeMessage<T>> writeChannel) {
            this.attribute = attribute;
            this.readChannel = readChannel;
            this.writeChannel = writeChannel;
        }

        void closeChannels() {
            if (readChannel != null) {
                try {
                    readChannel.close();
                } catch (ChannelLifetimeException e) {
                    LOGGER.error("Failed to close channel, continuing on: {}", readChannel, e);
                    // note: if we change the channel API to not throw on close, this try-catch can be removed
                }
            }

            if (writeChannel != null) {
                try {
                    writeChannel.close();
                } catch (ChannelLifetimeException e) {
                    LOGGER.error("Failed to close channel, continuing on: {}", writeChannel, e);
                    // note: if we change the channel API to not throw on close, this try-catch can be removed
                }
            }
        }
    }
}
