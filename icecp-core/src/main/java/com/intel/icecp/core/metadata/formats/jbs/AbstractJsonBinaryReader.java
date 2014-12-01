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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteOrder;
import java.sql.Timestamp;
import java.util.Map.Entry;

import javax.imageio.stream.MemoryCacheImageInputStream;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatTypes;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonValueFormat;
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.fasterxml.jackson.module.jsonSchema.types.ArraySchema.Items;

/**
 * AbstractJsonBinaryReader: Auto-closable abstract class for all JsonSchema
 * related Binary Readers
 *
 *
 */
public abstract class AbstractJsonBinaryReader implements AutoCloseable {

    final static String REF$_LONG = "long";
    final static String REF$_DOUBLE = "double";

    protected static final int NO_ARRAY = -1;

    protected JsonSchema schema;
    protected MemoryCacheImageInputStream dataInput;
    protected boolean hasNext = true;

    protected AbstractJsonBinaryReader(InputStream dataInputStream, JsonSchema schema) throws FileNotFoundException {
        this.schema = schema;

        dataInput = new MemoryCacheImageInputStream(dataInputStream);
        dataInput.setByteOrder(ByteOrder.LITTLE_ENDIAN);
    }

    public boolean hasNext() {
        return hasNext;
    }

    /**
     * Auto close the input stream
     */
    @Override
    public void close() throws IOException {
        dataInput.close();
    }

    /*
	 * Create a new instance of the specified object type
     */
    abstract protected Object createNewInstance(Class<?> objectClass) throws NoSuchMethodException, SecurityException, InstantiationException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException;

    /**
     * Recurse one layer down and store result in the specified field key
     */
    abstract protected void recurseObjectAndSetField(Class<?> objectClass, Object pObject, Entry<String, JsonSchema> obj, String key) throws NoSuchFieldException,
            SecurityException, InstantiationException, IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException,
            SchemaErrorException, IOException;

    // Helper functions to read various combinations of data
    final Object readTimestampData(Class<?> objectClass, int arrayElements) throws IOException {
        if (arrayElements == NO_ARRAY) {
            return new Timestamp(dataInput.readLong());
        } else {
            Timestamp[] obj = new Timestamp[arrayElements];
            for (int e = 0; e < arrayElements; obj[e++] = new Timestamp(dataInput.readLong()));
            return obj;
        }
    }

    @SuppressWarnings("unchecked")
    final <T extends Object> T readLongData(Class<T> objectClass, int arrayElements) throws IOException {
        if (arrayElements == NO_ARRAY) {
            return (T) new Long(dataInput.readLong());
        } else {
            Long[] obj = new Long[arrayElements];
            for (int e = 0; e < arrayElements; obj[e++] = new Long(dataInput.readLong()));
            return (T) obj;
        }
    }

    @SuppressWarnings("unchecked")
    final <T extends Object> T readIntData(Class<T> objectClass, int arrayElements) throws IOException {
        if (arrayElements == NO_ARRAY) {
            return (T) new Integer(dataInput.readInt());
        } else {
            Integer[] obj = new Integer[arrayElements];
            for (int e = 0; e < arrayElements; obj[e++] = new Integer(dataInput.readInt()));
            return (T) obj;
        }
    }

    @SuppressWarnings("unchecked")
    final <T extends Object> T readDoubleData(Class<T> objectClass, int arrayElements) throws IOException {
        if (arrayElements == NO_ARRAY) {
            return (T) new Double(dataInput.readDouble());
        } else {
            Object pObject = java.lang.reflect.Array.newInstance(objectClass == null ? double.class : objectClass.getComponentType(), arrayElements);
            if (objectClass == null || objectClass.getComponentType().isAssignableFrom(double[].class)) {
                dataInput.readFully(double[].class.cast(pObject), 0, arrayElements);
            } else {
                for (int e = 0; e < arrayElements; Double[].class.cast(pObject)[e++] = dataInput.readDouble());
            }
            return (T) pObject;
        }
    }

    @SuppressWarnings("unchecked")
    final <T extends Object> T readFloatData(Class<?> objectClass, int arrayElements) throws IOException {
        if (arrayElements == NO_ARRAY) {
            return (T) new Float(dataInput.readFloat());
        } else {
            Object pObject = java.lang.reflect.Array.newInstance(objectClass == null ? float.class : objectClass.getComponentType(), arrayElements);
            if (objectClass == null || objectClass.getComponentType().isAssignableFrom(float[].class)) {
                dataInput.readFully(float[].class.cast(pObject), 0, arrayElements);
            } else {
                for (int e = 0; e < arrayElements; Float[].class.cast(pObject)[e++] = dataInput.readFloat());
            }
            return (T) pObject;
        }
    }

    @SuppressWarnings("unchecked")
    final <T extends Object> T readBooleanData(Class<?> objectClass, int arrayElements) throws IOException {
        if (arrayElements == NO_ARRAY) {
            return (T) Boolean.valueOf(dataInput.readBoolean());
        } else if (objectClass == null || objectClass.isAssignableFrom(boolean[].class)) {
            boolean[] obj = new boolean[arrayElements];
            for (int e = 0; e < arrayElements; obj[e++] = dataInput.readBoolean());
            return (T) obj;
        } else {
            Boolean[] obj = new Boolean[arrayElements];
            for (int e = 0; e < arrayElements; obj[e++] = Boolean.valueOf(dataInput.readBoolean()));
            return (T) obj;
        }
    }

    Object readObject(Class<?> objectClass, JsonSchema pSchema, String name, int arrayElements) throws SchemaErrorException, IOException, NoSuchFieldException,
            SecurityException, InstantiationException, IllegalAccessException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException {

        JsonFormatTypes ft;
        JsonValueFormat fmt;
        Object pObject;
        String ref;

        switch (ft = pSchema.getType()) {
            case OBJECT:
                pObject = createNewInstance(objectClass);

                ObjectSchema objSchema = pSchema.asObjectSchema();
                for (Entry<String, JsonSchema> obj : objSchema.getProperties().entrySet()) {
                    String key = obj.getKey();
                    recurseObjectAndSetField(objectClass, pObject, obj, key);
                }
                break;

            case ARRAY:
                ArraySchema arrSchema = pSchema.asArraySchema();
                Integer entries;
                if (arrSchema.getMinItems() != null && arrSchema.getMinItems().equals(entries = arrSchema.getMaxItems())) {
                    Items items = arrSchema.getItems();
                    if (items.isSingleItems()) {
                        // Propagate the $ref of the array down into the fields if not already set.
                        if (items.asSingleItems().getSchema().get$ref() == null) {
                            items.asSingleItems().getSchema().set$ref(arrSchema.get$ref());
                        }
                        pObject = readObject(objectClass, items.asSingleItems().getSchema(), name, entries);
                    } else {
                        throw new SchemaErrorException("RCGenericObject.readObject: array not SingleItem type");
                    }
                } else {
                    throw new SchemaErrorException("RCGenericObject.readObject: array schema improper element min/max size not");
                }
                break;

            case INTEGER: {
                ref = pSchema.get$ref();
                fmt = pSchema.asIntegerSchema().getFormat();

                if (fmt != null) {
                    switch (fmt) {
                        case UTC_MILLISEC:
                            pObject = readTimestampData(objectClass, arrayElements);
                            break;
                        default:
                            throw new SchemaErrorException("Unknown / unhandled schma type " + fmt);
                    }
                } else if (ref != null && ref.toLowerCase().equals(REF$_LONG)) {
                    pObject = readLongData(objectClass, arrayElements);
                } else {
                    pObject = readIntData(objectClass, arrayElements);
                }
                break;
            }
            case NUMBER: {
                ref = pSchema.get$ref();
                if (ref != null && ref.toLowerCase().equals(REF$_DOUBLE)) {
                    pObject = readDoubleData(objectClass, arrayElements);
                } else {
                    pObject = readFloatData(objectClass, arrayElements);
                }
                break;
            }

            case BOOLEAN:
                pObject = readBooleanData(objectClass, arrayElements);
                break;

            default:
                throw new JsonMappingException(String.format("GenericObject: cannot map type %s %s", ft, name));
        }
        return pObject;
    }

}
