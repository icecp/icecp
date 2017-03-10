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

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 */
public class PersistenceTest {

    @Test
    public void testMustPersist() {
        assertTrue(Persistence.DEFAULT.mustPersist());
        assertTrue(Persistence.FOREVER.mustPersist());
        assertFalse(Persistence.NEVER_PERSIST.mustPersist());
    }

    @Test
    public void testHasRetrievalLifetime() {
        assertTrue(Persistence.DEFAULT.hasRetrievalLifetime());
        assertTrue(Persistence.FOREVER.hasRetrievalLifetime());
        assertFalse(Persistence.NEVER_PERSIST.hasRetrievalLifetime());
    }

}
