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
package com.intel.icecp.core.metadata.formats;

import com.intel.icecp.core.Message;
import com.intel.icecp.core.Module;
import com.intel.icecp.core.attributes.AttributeMessage;
import com.intel.icecp.core.channels.Token;
import com.intel.icecp.core.metadata.Format;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

/**
 */
public class CborFormatTest extends DefaultFormatTest {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public <T extends Message> Format<T> buildFormat(Class<T> type) {
        return new CborFormat<>(type);
    }

    // TODO move this test to DefaultFormatTest so that all serialization methods test against tokens
    @Test
    public void testGenericJsonFormat() throws Exception {
        CborFormat<AttributeMessage<Module.State>> formatter = new CborFormat<>(new Token<AttributeMessage<Module.State>>() {
        });

        AttributeMessage<Module.State> message = new AttributeMessage<>(Module.State.LOADED);
        DefaultFormatTest.genericFormat(message, formatter);
    }
}
