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
package com.intel.icecp.node.utils;

import com.intel.icecp.core.Message;
import com.intel.icecp.core.metadata.Format;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * Utility class for Format
 *
 */
public class FormatUtils {

    /**
     * Returns an instance of formatter, of the same type of the one given as an
     * input, parametrized on the given Message class
     *
     * @param input
     * @param c
     * @return
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws NoSuchMethodException
     * @throws InstantiationException
     * @throws InvocationTargetException
     */
    public static Format getMessageFormatter(Format input, Class<? extends Message> c) throws IllegalAccessException, IllegalArgumentException, NoSuchMethodException, InstantiationException, InvocationTargetException {

        Class frmtClass = input.getClass();
        Format format = (Format) frmtClass.getDeclaredConstructor(Class.class).newInstance(c);

        return format;
    }

}
