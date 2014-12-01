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
