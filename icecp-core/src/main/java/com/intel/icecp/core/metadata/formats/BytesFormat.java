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

import com.intel.icecp.core.messages.BytesMessage;
import com.intel.icecp.core.metadata.Format;
import com.intel.icecp.node.utils.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Format a message as plain bytes.
 *
 */
public class BytesFormat implements Format<BytesMessage> {
    /**
     * Pass through the bytes of a bytes message
     *
     * @param message an unencoded message, i.e. a {@link BytesMessage}
     * @return a stream of the unencoded bytes
     */
    @Override
    public InputStream encode(BytesMessage message) {
        return new ByteArrayInputStream(message.getBytes());
    }

    /**
     * Read all of the bytes from the input stream and return the byte message.
     *
     * @param stream a stream of bytes
     * @return an unencoded {@link BytesMessage}
     * @throws IOException if the stream cannot be read
     */
    @Override
    public BytesMessage decode(InputStream stream) throws IOException {
        return new BytesMessage(StreamUtils.readAll(stream));
    }
}
