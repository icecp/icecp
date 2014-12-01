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
import com.intel.icecp.core.attributes.AttributeRegistrationException;
import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.attributes.CannotInstantiateAttributeException;
import com.intel.icecp.core.attributes.WriteableAttribute;
import com.intel.icecp.core.management.Channels;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Convenience utility for setting up attributes before starting the daemon or running tests.
 *
 */
public class AttributesFactory {

    private static final Logger LOGGER = LogManager.getLogger();

    private AttributesFactory() {
        // do not allow instances of this factory
    }

    /**
     * Configure and return an empty attributes list.
     *
     * @param channels the channels we will expose the attributes on
     * @param baseUri  the base URI of the attributes for remote access
     * @return an empty attributes list
     */
    public static Attributes buildEmptyAttributes(Channels channels, URI baseUri) {
        return new AttributesImpl(channels, baseUri);
    }

    /**
     * Configure a set of attributes from a given values of values; it will only create the expected attributes. It will
     * map the attribute names (e.g. "id" for IdAttribute) to the keys of the value map; if the attribute is not found,
     * it will be added with no specified value.
     * <p>
     * First, build a dummy attribute to retrieve the name... if no zero-arg constructor exists, try the first one-arg
     * constructor (use null as the arg)
     * To set the value:
     * * if no-arg, try to assign using the WriteableAttribute interface
     * * if one-arg with an arg matching attribute type(), create new attribute using this constructor
     * * if anything else, fail
     *
     * @param channels the channels we will expose the attributes on
     * @param baseUri  the base URI of the attributes for remote access
     * @param values   the values to wrap in attributes
     * @param expected the attributes expected in the values
     * @return a set of attributes with bootstrapped values
     */
    @SuppressWarnings("unchecked")
    public static Attributes buildAttributesFromMap(Channels channels, URI baseUri, Map<String, Object> values, Collection<Class<? extends Attribute>> expected)
            throws CannotInstantiateAttributeException {
        AttributesImpl attributes = new AttributesImpl(channels, baseUri);

        for (Class<? extends Attribute> attributeClass : expected) {
            try {
                attributes.add(makeAttribute(values, attributeClass));
            } catch (AttributeRegistrationException | IllegalArgumentException e) {
                throw new CannotInstantiateAttributeException("Could not register new attribute '" + attributeClass
                        + "'", e);
            }
        }

        return attributes;
    }

    /**
     * Construct an attribute, preferring a zero-argument constructor over a one-argument constructor
     *
     * @param values         Once we know the attribute's name, we retrieve its value here.
     * @param attributeClass The attribute class to instantiate
     * @return The constructed attribute
     * @throws CannotInstantiateAttributeException
     */
    private static Attribute makeAttribute(final Map<String, Object> values, final Class<? extends Attribute> attributeClass) throws CannotInstantiateAttributeException {
        try {
            Attribute attribute = instantiateDummyAttribute(attributeClass);

            String name = attribute.name();
            LOGGER.debug("Adding expected attribute '{}'", name);

            if (values.containsKey(name)) {
                Object value = values.get(name);
                attribute = newAttributeWithValue(attributeClass, value);
                LOGGER.debug("Using provided value to bootstrap new attribute '{}': {}", name, value);
            } else {
                LOGGER.debug("No value given to bootstrap new attribute '{}'", name);
            }
            return attribute;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new CannotInstantiateAttributeException("Could not instantiate new attribute '" + attributeClass
                    + "'", e);
        }
    }

    private static Attribute instantiateDummyAttribute(final Class<? extends Attribute> attributeClass) throws
            IllegalAccessException, InvocationTargetException, InstantiationException {
        try {
            return attributeClass.newInstance();
        } catch (InstantiationException e) {
            LOGGER.trace("Could not instantiate attribute with zero-arg constructor; trying one-arg ", e);
            Optional<? extends Constructor<?>> oneArgConstructor = Arrays.stream(attributeClass.getConstructors()).filter(x ->
                    x.getParameterCount() == 1).findFirst();
            // it doesn't matter if we pick the right one-arg constructor here; all we need is a dummy instance we
            // can use to find the attribute's return type
            if (oneArgConstructor.isPresent()) {
                return (Attribute) oneArgConstructor.get().newInstance((Object) null);
            } else {
                throw new InstantiationException("Unable to construct with one-arg constructor");
            }
        }
    }

    @SuppressWarnings("unchecked")
    static <T> Attribute newAttributeWithValue(Class<? extends Attribute> attributeClass, T value) {
        final Optional<Attribute<T>> constructor = fromConstructor(attributeClass, value);
        if (constructor.isPresent()) {
            return constructor.get();
        } else if (isWritable(attributeClass)) {
            return fromWritable((Class<? extends WriteableAttribute<T>>) attributeClass, value);
        } else {
            throw new IllegalArgumentException("Cannot find a way to create attribute '" + attributeClass + "' with the assigned value: " + value);
        }
    }

    private static <T> Optional<Attribute<T>> fromConstructor(Class<? extends Attribute> attributeClass, T value) {
        try {
            if (value == null) {
                throw new IllegalArgumentException("This method should be called with a non-null value");
            }
            final Optional<? extends Constructor> constructor = findConstructor(attributeClass, value.getClass());
            if (constructor.isPresent()) {
                return Optional.of((Attribute<T>) constructor.get().newInstance(value));
            } else {
                return Optional.empty();
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException("This method should be called with a valid constructor.", e);
        }
    }

    private static Optional<? extends Constructor> findConstructor(Class<? extends Attribute> attributeClass,
                                                                   Class<?> availableValueType) {
        final Stream<Constructor<?>> attributeConstructors = Arrays.stream(attributeClass.getConstructors());
        return attributeConstructors.filter(x -> x.getParameterCount() == 1
                && isAutoCastableFrom(x.getParameterTypes()[0], availableValueType)).findFirst();
    }

    private static <T, S> boolean isAutoCastableFrom(Class<T> target, Class<S> source) {
        return target.isAssignableFrom(source) || isBoxable(target, source);
    }

    private static <T, S> boolean isBoxable(final Class<T> target, final Class<S> source) {
        final HashMap<Class, Class> boxPrimitiveMap = new HashMap<>();
        boxPrimitiveMap.put(Byte.class, byte.class);
        boxPrimitiveMap.put(Short.class, short.class);
        boxPrimitiveMap.put(Integer.class, int.class);
        boxPrimitiveMap.put(Long.class, long.class);
        boxPrimitiveMap.put(Float.class, float.class);
        boxPrimitiveMap.put(Double.class, double.class);
        boxPrimitiveMap.put(Boolean.class, boolean.class);
        boxPrimitiveMap.put(Character.class, char.class);

        return (boxPrimitiveMap.containsKey(source) && boxPrimitiveMap.get(source) == target) ||
                (boxPrimitiveMap.containsKey(target) && boxPrimitiveMap.get(target) == source);
    }

    static boolean isWritable(Class<? extends Attribute> attributeClass) {
        return WriteableAttribute.class.isAssignableFrom(attributeClass);
    }

    static <T> Attribute fromWritable(Class<? extends WriteableAttribute<T>> attributeClass, T value) {
        try {
            WriteableAttribute<T> writeableAttribute = attributeClass.newInstance();
            writeableAttribute.value(value);
            return writeableAttribute;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException("This method should be called with a writeable attribute with a default constructor.", e);
        }
    }
}