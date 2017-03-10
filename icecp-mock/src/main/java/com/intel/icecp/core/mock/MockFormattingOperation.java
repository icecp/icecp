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
package com.intel.icecp.core.mock;

import com.intel.icecp.core.Message;
import com.intel.icecp.core.metadata.Format;
import com.intel.icecp.core.metadata.formats.FormatEncodingException;
import com.intel.icecp.core.pipeline.Operation;
import com.intel.icecp.core.pipeline.exception.OperationException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Operation that encodes/decodes a {@link Message} using a give {@link Format}
 *
 */
public class MockFormattingOperation extends Operation<Message, InputStream> {

    /** Format to use to encode/decode the message */
    private final Format format;

    public MockFormattingOperation(Format format) {
        super(Message.class, InputStream.class);
        this.format = format;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public InputStream execute(Message input) throws OperationException {
        try {
            return format.encode(input);
        } catch (FormatEncodingException ex) {
            throw new OperationException("FormattingOperation direct operation failed.", ex);
        }
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public Message executeInverse(InputStream input) throws OperationException {
        try {
            return format.decode(input);
        } catch (IOException | FormatEncodingException ex) {
            throw new OperationException("FormattingOperation inverse operation failed.", ex);
        }
    }
}
