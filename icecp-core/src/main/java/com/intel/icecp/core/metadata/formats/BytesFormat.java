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
