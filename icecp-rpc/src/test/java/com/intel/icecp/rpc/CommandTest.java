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
