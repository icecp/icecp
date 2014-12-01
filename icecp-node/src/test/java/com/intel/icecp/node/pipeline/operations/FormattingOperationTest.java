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

package com.intel.icecp.node.pipeline.operations;

import com.intel.icecp.common.TestMessage;
import com.intel.icecp.core.metadata.formats.BytesFormat;
import com.intel.icecp.core.metadata.formats.JsonFormat;
import com.intel.icecp.core.pipeline.exception.OperationException;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;

/**
 */
public class FormattingOperationTest {

    private FormattingOperation instance;

    @Before
    public void before() {
        instance = new FormattingOperation(new JsonFormat<>(TestMessage.class));
    }

    @Test
    public void executeAndInverse() throws Exception {
        TestMessage a = TestMessage.buildRandom(10);
        InputStream inputStream = instance.execute(a);
        TestMessage b = (TestMessage) instance.executeInverse(inputStream);
        assertEquals(a, b);
    }

    @Test(expected = OperationException.class)
    public void executeIncorrectType() throws Exception {
        FormattingOperation instance = new FormattingOperation(new BytesFormat());
        instance.execute(TestMessage.buildRandom(10));
    }
}