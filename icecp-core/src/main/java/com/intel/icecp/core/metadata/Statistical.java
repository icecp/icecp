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
package com.intel.icecp.core.metadata;

import com.intel.icecp.core.Metadata;

/**
 * Describe a Channel's statistical variables. This is intended as a
 * future-facing contract for how a Channel will behave; the Channel may choose
 * to adjust the statistical metadata on-the-fly or serve a statistical
 * descriptor that is pre-configured.
 *
 * @param <P> primitive class used for measuring statistical values.
 */
public interface Statistical<P> extends Metadata {

    /**
     * @param property the {@link com.intel.icecp.core.Message} property in
     * question
     * @return the mean value for a given message property
     */
    P getMean(String property);

    /**
     * @param property the {@link com.intel.icecp.core.Message} property in
     * question
     * @return the maximum value for a given message property
     */
    P getMaximum(String property);

    /**
     * @param property the {@link com.intel.icecp.core.Message} property in
     * question
     * @return the minimum value for a given message property
     */
    P getMinimum(String property);

    /**
     * @param property the {@link com.intel.icecp.core.Message} property in
     * question
     * @return the standard deviation for a given message property
     */
    P getStandardDeviation(String property);
}
