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

package com.intel.icecp.core.metadata.formats.jbs;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Map.Entry;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

public class JsonBinaryGenericObjectReader extends AbstractJsonBinaryReader {

    public JsonBinaryGenericObjectReader(File file, JsonSchema schema) throws FileNotFoundException {
        this(new FileInputStream(file), schema);
    }

    public JsonBinaryGenericObjectReader(InputStream dataInputStream, JsonSchema schema) throws FileNotFoundException {
        super(dataInputStream, schema);
    }

    public GenericObject readObject() throws SchemaErrorException, IOException {
        try {
            return (GenericObject) readObject(null, schema, schema.getId(), NO_ARRAY);
        } catch (EOFException | NoSuchFieldException | SecurityException | InstantiationException | IllegalAccessException | NoSuchMethodException | IllegalArgumentException | InvocationTargetException eof) {
            hasNext = false;
            return null;
        }
    }

    @Override
    protected Object createNewInstance(Class<?> objectClass) {
        return new GenericObject();
    }

    @Override
    protected void recurseObjectAndSetField(Class<?> objectClass, Object pObject, Entry<String, JsonSchema> obj, String key) throws
            SchemaErrorException, IOException, NoSuchFieldException, SecurityException, InstantiationException, IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {

        GenericObject genericObject = GenericObject.class.cast(pObject);

        Object rcgo = readObject(null, obj.getValue(), key, NO_ARRAY);
        genericObject.put(key, rcgo);
    }
}
