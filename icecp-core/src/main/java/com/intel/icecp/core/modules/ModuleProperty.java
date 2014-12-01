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

package com.intel.icecp.core.modules;

import com.intel.icecp.core.Attribute;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to annotate the main class of a loadable module that implements the {@link com.intel.icecp.core.Module}
 * interface.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ModuleProperty {

    /**
     * @return the name of the module; this name will be used by ICECP for identifying the module against other modules
     * and will serve as an index for permission matching
     */
    String name();

    /**
     * @return the attributes used by this module; do not add the common attributes already specified in {@link
     * com.intel.icecp.core.Module}
     */
    Class<? extends Attribute>[] attributes() default {};
}
