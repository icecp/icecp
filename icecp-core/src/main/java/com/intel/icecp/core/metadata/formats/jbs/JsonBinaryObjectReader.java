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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map.Entry;

import com.fasterxml.jackson.module.jsonSchema.JsonSchema;

public class JsonBinaryObjectReader extends AbstractJsonBinaryReader {

    public JsonBinaryObjectReader(File file, JsonSchema schema) throws FileNotFoundException {
        this(new FileInputStream(file), schema);
    }

    public JsonBinaryObjectReader(InputStream dataInputStream, JsonSchema schema) throws FileNotFoundException {
        super(dataInputStream, schema);
    }

    public <T extends Object> T readObject(Class<T> objectClass) throws SchemaErrorException, IOException {
        try {
            return objectClass.cast(readObject(objectClass, schema, schema.getId(), NO_ARRAY));
        } catch (EOFException eof) {
            hasNext = false;
            return null;
        } catch (NoSuchFieldException | SecurityException | InstantiationException | IllegalAccessException | NoSuchMethodException | IllegalArgumentException | InvocationTargetException refEx) {
            hasNext = false;
            return null;
        }
    }

    @Override
    protected Object createNewInstance(Class<?> objectClass) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Constructor<?> c = objectClass.getDeclaredConstructor();
        c.setAccessible(true);
        return c.newInstance();
    }

    @Override
    protected void recurseObjectAndSetField(Class<?> objectClass, Object pObject, Entry<String, JsonSchema> obj, String key) throws NoSuchFieldException, SecurityException, InstantiationException, IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException, SchemaErrorException, IOException {
        Field field = objectClass.getDeclaredField(key);
        Object createdObject = readObject(field.getType(), obj.getValue(), key, NO_ARRAY);
        field.setAccessible(true);
        field.set(pObject, createdObject);
    }
}
