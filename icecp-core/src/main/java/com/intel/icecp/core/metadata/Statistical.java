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
