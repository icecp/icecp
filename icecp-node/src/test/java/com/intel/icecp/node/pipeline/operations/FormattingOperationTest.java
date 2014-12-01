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