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
package com.intel.icecp.rpc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test commands
 *
 */
public class CommandTest {

    private static final Logger logger = LogManager.getLogger();
    private Command instance;

    @Before
    public void setUp() throws NoSuchMethodException {
        FakeClass fakeClass = new FakeClass();
        instance = fakeClass.toCommand();
    }

    @Test
    public void testName() {
        assertEquals("FakeClass.testMethod", instance.name());
    }

    @Test
    public void testToDescription() {
        logger.info(instance.toDescription());
    }

    @Test
    public void testInvoke() throws Exception {
        FakeDataStructure testData = new FakeDataStructure();
        assertEquals(new FakeClass().testMethod(testData), instance.execute(testData));
    }

    @Test
    public void testEqualsAndHashCode() throws NoSuchMethodException {
        Command different = new Command(this, this.getClass().getMethod("testEqualsAndHashCode"));
        assertNotSame(instance.hashCode(), different.hashCode());
        assertFalse(instance.equals(different));

        // Warning: this may be unintuitive but makes sense because the command
        // is bound to an instance of an object, not a class
        Command sameClass = (new FakeClass()).toCommand();
        assertNotSame(instance.hashCode(), sameClass.hashCode());
        assertFalse(instance.equals(sameClass));
    }

    @Test
    public void testEqualsWithNullObject() {
        Command different = null;
        assertFalse(instance.equals(different));
    }
}
