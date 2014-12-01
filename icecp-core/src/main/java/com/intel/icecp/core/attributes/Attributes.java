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
package com.intel.icecp.core.attributes;

import com.intel.icecp.core.Attribute;
import com.intel.icecp.core.Describable;

import java.util.Map;
import java.util.Set;

/**
 * Container class to manage the attributes for a {@link Describable} thing--think of this container as a set of
 * attributes. In the future, this container could (should?) be generated using Java annotations (tutorial for
 * annotation processing at http://hannesdorfmann.com/annotation-processing/annotationprocessing101/).
 * <p>
 * An attribute describes the current value of a property; for example, a node can have a "state" attribute that will
 * indicate whether the node is currently "running," "stopped," etc. Some attributes are also writeable (see {@link
 * WriteableAttribute}) which allows user code, even remote code, to change the value of an attribute.
 * <p>
 * Access to attributes falls under two categories: 1) access from code that has knowledge of the types of the
 * attributes and 2) access from code that does not. For the first case, this API exposes methods that allow passing the
 * class type (e.g. {@link Class}) as the identifier for interacting with the attribute--this ensures that while
 * changing the attribute implementations developers will see compiler errors if the generic types are not aligned
 * correctly.
 * <p>
 * The second case requires methods that identify an attribute using a pair--the attribute name and the attribute's
 * value type. This allows remote access to an attribute without having the implementing class (e.g. {@code
 * MyAttribute.class}) available. The attribute's value type (e.g. {@code Integer}) is required and should be exposed
 * using an external schema language (TODO consider JSON-LD support). Additional type checking and casting is performed
 * in these methods so local code should prefer the first method, described above.
 *
 */
public interface Attributes {

    /**
     * Add a new attribute to the set of describable attributes. If the attribute already exists in the attribute set,
     * this method will replace it.
     * <p>
     * This method must be protected using the {@link AttributePermission}'s {@code add} action.
     * <p>
     * TODO this method could return the instance itself for easier builder patterns
     *
     * @param attribute a new attribute implementation
     */
    void add(Attribute attribute) throws AttributeRegistrationException;

    /**
     * Remove an attribute from the set of describable attributes. Prefer this method if you have access to the
     * attribute implementation class.
     * <p>
     * This method must be protected using the {@link AttributePermission}'s {@code remove} action.
     *
     * @param attributeType the class of the attribute implementation
     * @param <T> the type of values returned by the attribute
     * @return the last known value of the attribute
     * @throws AttributeNotFoundException if the attribute has not been added
     */
    <T> T remove(Class<? extends Attribute<T>> attributeType) throws AttributeNotFoundException;

    /**
     * Remove an attribute from the set of describable attributes. Prefer this method if you are accessing an attribute
     * of unknown implementation; use of {@link #has(Class)} prior to this call is helpful
     * <p>
     * This method must be protected using the {@link AttributePermission}'s {@code remove} action.
     *
     * @param attributeName the unique name of the attribute
     * @param attributeValueType the type of values returned by the attribute
     * @param <T> the type of values returned by the attribute
     * @return the last known value of the attribute
     * @throws AttributeNotFoundException if the attribute has not been added
     */
    <T> T remove(String attributeName, Class<T> attributeValueType) throws AttributeNotFoundException;

    /**
     * Determine if the attribute set contains the given attribute
     *
     * @param attributeType the class of the attribute implementation
     * @return true if the attribute exists in the set, false otherwise
     */
    boolean has(Class<? extends Attribute> attributeType);

    /**
     * Determine if the attribute set contains the given attribute
     *
     * @param attributeName the unique name of the attribute
     * @return true if the attribute exists in the set, false otherwise
     */
    boolean has(String attributeName);

    /**
     * Retrieve the current value of an attribute. Prefer this method if you have access to the attribute implementation
     * class.
     * <p>
     * This method must be protected using the {@link AttributePermission}'s {@code read} action.
     *
     * @param attributeType the class of the attribute implementation
     * @param <T> the type of values returned by the attribute
     * @return the current value of the attribute
     * @throws AttributeNotFoundException if the attribute has not been added
     */
    <T> T get(Class<? extends Attribute<T>> attributeType) throws AttributeNotFoundException;

    /**
     * Retrieve the current value of an attribute
     * <p>
     * This method must be protected using the {@link AttributePermission}'s {@code read} action.
     *
     * @param attributeName the unique name of the attribute
     * @param attributeValueType the type of values returned by the attribute
     * @param <T> the type of values returned by the attribute
     * @return the current value of the attribute
     * @throws AttributeNotFoundException if the attribute has not been added
     */
    <T> T get(String attributeName, Class<T> attributeValueType) throws AttributeNotFoundException;

    /**
     * Assign a new value to an attribute. Prefer this method if you have access to the attribute implementation class.
     * <p>
     * This method must be protected using the {@link AttributePermission}'s {@code write} action.
     *
     * @param attributeType the class of the attribute implementation
     * @param newValue the value to set the attribute to
     * @param <T> the type of values accepted by the attribute
     * @throws AttributeNotFoundException if the attribute has not been added
     * @throws AttributeNotWriteableException if the added attribute is not writeable
     */
    <T> void set(Class<? extends Attribute<T>> attributeType, T newValue) throws AttributeNotFoundException, AttributeNotWriteableException;

    /**
     * Assign a new value to an attribute
     * <p>
     * This method must be protected using the {@link AttributePermission}'s {@code write} action.
     *
     * @param attributeName the unique name of the attribute
     * @param newValue the value to set the attribute to
     * @param <T> the type of values returned by the attribute
     * @throws AttributeNotFoundException if the attribute has not been added
     * @throws AttributeNotWriteableException if the added attribute is not writeable
     */
    <T> void set(String attributeName, T newValue) throws AttributeNotFoundException, AttributeNotWriteableException;

    /**
     * Observe an attribute's changes after they occur; implementations should be able to handle multiple callbacks for
     * the same attribute name. TODO add a class-indexed version of this
     *
     * @param attributeName the attribute name
     * @param onAttributeChanged callback to fire when an attribute is changed
     */
    void observe(String attributeName, OnAttributeChanged onAttributeChanged);

    /**
     * @return a map of all attribute names/values as key-pairs
     */
    Map<String, Object> toMap();

    /**
     * @return the number of attributes
     */
    int size();

    /**
     * @return Current set of valid attributes (note, static snapshot!)
     */
    Set<String> keySet();
}
