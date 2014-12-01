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

import com.intel.icecp.common.TestCounter;
import com.intel.icecp.core.Attribute;
import com.intel.icecp.core.Channel;
import com.intel.icecp.core.attributes.AttributeMessage;
import com.intel.icecp.core.attributes.AttributeNotFoundException;
import com.intel.icecp.core.attributes.AttributeNotWriteableException;
import com.intel.icecp.core.attributes.AttributeRegistrationException;
import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.attributes.AttributesNamespace;
import com.intel.icecp.core.attributes.BaseAttribute;
import com.intel.icecp.core.attributes.WriteableAttribute;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.mock.MockChannels;
import com.intel.icecp.node.utils.ChannelUtils;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 */
public class AttributesImplTest {

    private Attributes attributes;
    private URI baseUri;
    private MockChannels channels;

    @Before
    public void setUp() {
        baseUri = URI.create("icecp:/attributes");
        channels = new MockChannels();
        attributes = new AttributesImpl(channels, baseUri);
    }

    @Test
    public void testSize() {
        assertEquals(0, attributes.size());
    }

    @Test
    public void testAddingAttributes() throws AttributeRegistrationException {
        assertEquals(0, attributes.size());
        attributes.add(new A(1));
        assertEquals(1, attributes.size());
    }

    @Test
    public void testHasAttributes() throws AttributeRegistrationException {
        A attribute = new A(1);
        attributes.add(attribute);

        assertTrue(attributes.has(attribute.name()));
        assertTrue(attributes.has(A.class));

        assertFalse(attributes.has("not-found"));
        assertFalse(attributes.has(B.class));
    }

    @Test
    public void testKeySet() throws AttributeRegistrationException {
        A attributeA = new A(1);
        attributes.add(attributeA);

        assertEquals(1, attributes.keySet().size());
        assertTrue(attributes.keySet().contains("a"));

        B attributeB = new B(2);
        attributes.add(attributeB);

        assertEquals(2, attributes.keySet().size());
        assertTrue(attributes.keySet().contains("b"));
    }

    @Test
    public void testKeySetIndependent() throws AttributeRegistrationException, AttributeNotFoundException {
        attributes.add(new A(1));
        attributes.add(new B(1));

        // attributes contain "a" and "b"
        assertTrue(attributes.has("a"));
        assertTrue(attributes.has("b"));

        // get keyset and modify local copy
        attributes.keySet().clear();

        // attributes still contain "a" and "b"
        assertTrue(attributes.has("a"));
        assertTrue(attributes.has("b"));

        // fetch snapshot of key set
        Set<String> keySet = attributes.keySet();
        assertEquals(2, keySet.size());

        // remove attribute "b"
        attributes.remove("b", B.class);

        // ensure snapshot is unchanged
        assertEquals(2, keySet.size());
    }

    @Test
    public void testRemovingAttributesByClass() throws Exception {
        Attribute attr1 = new A(1);
        attributes.add(attr1);

        Integer lastValue = attributes.remove(A.class);
        assertEquals(0, attributes.size());
        assertEquals(attr1.value(), lastValue);
    }

    @Test
    public void testRemovingAttributesByName() throws Exception {
        Attribute attr1 = new A(1);
        attributes.add(attr1);

        Integer lastValue = attributes.remove("a", Integer.class);
        assertEquals(0, attributes.size());
        assertEquals(attr1.value(), lastValue);
    }

    @Test
    public void testRetrievingAttributesByClass() throws Exception {
        Attribute attribute = new A(1);
        attributes.add(attribute);
        assertEquals(1, (int) attributes.get(A.class));
    }

    @Test
    public void testRetrievingAttributesByName() throws Exception {
        Attribute attribute = new A(1);
        attributes.add(attribute);
        assertEquals(1, attributes.get("a", attribute.type()));
    }

    @Test(expected = AttributeNotFoundException.class)
    public void testNotFindingAnAttributeByClass() throws AttributeNotFoundException {
        attributes.get(C.class);
    }

    @Test(expected = AttributeNotFoundException.class)
    public void testNotFindingAnAttributeByName() throws AttributeNotFoundException {
        attributes.get("not-found", Object.class);
    }

    @Test
    public void testModifyingAttributesByClass() throws Exception {
        attributes.add(new C("..."));
        assertEquals("...", attributes.get(C.class));

        attributes.set(C.class, "+++");
        assertEquals("+++", attributes.get(C.class));
    }

    @Test
    public void testModifyingAttributesByName() throws Exception {
        Attribute attribute = new C("...");
        attributes.add(attribute);
        assertEquals("...", attributes.get(attribute.name(), String.class));

        attributes.set(attribute.name(), "+++");
        assertEquals("+++", attributes.get(attribute.name(), String.class));
    }

    @Test
    public void testModifyingAttributeOverChannel() throws Exception {
        Attribute attribute = new C("...");
        attributes.add(attribute);

        URI readUri = ChannelUtils.join(baseUri, AttributesNamespace.READ_SUFFIX, "c");
        Channel<AttributeMessage> readChannel = channels.openChannel(readUri, AttributeMessage.class, new Persistence());
        readChannel.open().get();

        assertEquals("...", readChannel.latest().get().d);

        URI writeUri = ChannelUtils.join(baseUri, AttributesNamespace.WRITE_SUFFIX, "c");
        Channel<AttributeMessage> writeChannel = channels.openChannel(writeUri, AttributeMessage.class, new Persistence());
        writeChannel.open().get();

        writeChannel.publish(new AttributeMessage<>("remotely-changed"));
        assertEquals("remotely-changed", attributes.get(attribute.name(), String.class));
    }

    @Test(expected = AttributeNotWriteableException.class)
    public void testModifyingAnUnwriteableAttributeByClass() throws Exception {
        attributes.add(new A(99));
        attributes.set(A.class, 100);
    }

    @Test(expected = AttributeNotWriteableException.class)
    public void testModifyingAnUnwriteableAttributeByName() throws Exception {
        attributes.add(new A(99));
        attributes.set("a", 100);
    }

    @Test
    public void testObserve() throws AttributeRegistrationException {
        final TestCounter c = new TestCounter();
        attributes.observe("a", (String name, Object oldValue, Object newValue) -> c.count++);
        attributes.observe("a", (String name, Object oldValue, Object newValue) -> c.count++);

        attributes.add(new A(1));
        assertEquals(2, c.count);
    }

    @Test
    public void testToMapConversion() throws AttributeRegistrationException {
        final Object thing = new Object();
        attributes.add(new A(1));
        attributes.add(new B(thing));
        Map<String, Object> map = attributes.toMap();

        assertEquals(1, map.get("a"));
        assertEquals(thing, map.get("b"));
    }

    @Test
    public void testToString() {
        assertEquals("{}", attributes.toString());
    }

    @Test
    public void testValueImmutability() {
        String value = "...";
        C attribute = new C(value);
        assertEquals("...", attribute.value());

        value.replace('.', '+');
        assertEquals("...", attribute.value());
    }

    @Test(expected = ClassCastException.class)
    public void findWithUnsafeCast() throws Exception{
        AttributesImpl attributes = (AttributesImpl) this.attributes;
        attributes.add(new C("..."));

        AttributesImpl.AttributeEntry<String> entry = attributes.find(C.class);
        String value = entry.attribute.value();

        // note: if we use find with incorrect type parameters, we will get class cast failures
        AttributesImpl.AttributeEntry<Integer> incorrectEntry = attributes.find(C.class);
        Integer incorrectValue = incorrectEntry.attribute.value();
    }

    private class A extends BaseAttribute<Integer> {
        private final int value;

        A(int value) {
            super("a", Integer.class);
            this.value = value;
        }

        @Override
        public Integer value() {
            return value;
        }
    }

    private class B extends BaseAttribute<Object> {
        private final Object value;

        B(Object value) {
            super("b", Object.class);
            this.value = value;
        }

        @Override
        public Object value() {
            return value;
        }
    }

    private class C extends BaseAttribute<String> implements WriteableAttribute<String> {
        private String value;

        C(String value) {
            super("c", String.class);
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public void value(String newValue) {
            this.value = newValue;
        }
    }

}
