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

package com.intel.icecp.core.attributes;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
//import java.lang.management.OperatingSystemMXBean;

/**
 * Capture and report the instantaneous CPU usage of the machine. //TODO Currently uses internal com.sun to
 * return the value. Determine why java.lang OSMXBean returns no usage while the internal com.sun does...
 *
 */
public class ProcessorLoadAttribute extends BaseAttribute<Double> {

    private OperatingSystemMXBean os;

    public ProcessorLoadAttribute() {
        super("cpu-load", Double.class);
    }

    @Override
    public Double value() {
        return os().getSystemCpuLoad();
    }

    private OperatingSystemMXBean os() {
        if (os == null) {
            os = (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        }
        return os;
    }
}