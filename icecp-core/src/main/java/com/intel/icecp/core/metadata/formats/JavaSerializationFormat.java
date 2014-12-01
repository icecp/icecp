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

import com.intel.icecp.core.Message;
import com.intel.icecp.core.metadata.Format;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 *
 */
public class JavaSerializationFormat<T extends Message> implements Format<T> {

    private final Class<T> type;

    /**
     * Build a {@link Format} instance that parses JSON into the specified
     * class.
     *
     * @param type the
     */
    public JavaSerializationFormat(Class<T> type) {
        this.type = type;
    }

    @Override
    public InputStream encode(Message message) throws FormatEncodingException {
        ObjectOutputStream serializer;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            serializer = new ObjectOutputStream(bytes);
            serializer.writeObject(message);
            serializer.flush();
            serializer.close();
            return new ByteArrayInputStream(bytes.toByteArray());
        } catch (IOException ex) {
            throw new FormatEncodingException(ex);
        }
    }

    @Override
    public T decode(InputStream stream) throws FormatEncodingException, IOException {
        try {
            ObjectInputStream deserializer = new ObjectInputStream(stream);
            return (T) deserializer.readObject();
        } catch (ClassNotFoundException ex) {
            throw new FormatEncodingException(ex);
        }
    }
}
