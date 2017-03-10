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

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;

/**
 * Helper class to set java.util.logging log levels. Some of the libraries use
 * this for logging and this will ease setup.
 *
 */
public class LoggingUtils {

    /**
     * Set logging level on java.util.logging root logger; use to set logging
     * levels for libraries like jndn, jndn-utils, etc.
     *
     * @param level
     */
    public static void setRootLevel(Level level) {
        Handler fh = new ConsoleHandler();
        fh.setLevel(level);
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("");
        logger.addHandler(fh);
        logger.setLevel(level);
    }
}
