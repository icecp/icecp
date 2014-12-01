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
package com.intel.icecp.node.channels.dds;

import com.intel.icecp.core.metadata.formats.FormatEncodingException;
import com.intel.icecp.core.misc.OnPublish;
import com.intel.icecp.core.pipeline.Pipeline;
import com.rti.dds.infrastructure.RETCODE_NO_DATA;
import com.rti.dds.infrastructure.ResourceLimitsQosPolicy;
import com.rti.dds.subscription.DataReader;
import com.rti.dds.subscription.InstanceStateKind;
import com.rti.dds.subscription.SampleInfo;
import com.rti.dds.subscription.SampleStateKind;
import com.rti.dds.subscription.ViewStateKind;
import com.rti.dds.type.builtin.Bytes;
import com.rti.dds.type.builtin.BytesDataReader;
import java.io.IOException;

/**
 *
 */
public class LatestDdsListener extends DdsListener {

    public LatestDdsListener(OnPublish callback, Pipeline formattingOperations) {
        super(callback, formattingOperations);
    }

    @Override
    public void on_data_available(DataReader reader) {
        BytesDataReader bytes = (BytesDataReader) reader;
        try {
            bytes.read(packetSequence, packetInformationSequence,
                    ResourceLimitsQosPolicy.LENGTH_UNLIMITED,
                    SampleStateKind.ANY_SAMPLE_STATE,
                    ViewStateKind.ANY_VIEW_STATE,
                    InstanceStateKind.ANY_INSTANCE_STATE);

            // TODO do we really need to do this after a read()?
            for (int i = 0; i < packetSequence.size(); ++i) {
                SampleInfo info = (SampleInfo) packetInformationSequence.get(i);
                if (info.valid_data) {
                    process(((Bytes) packetSequence.get(i)).value);
                }
            }
        } catch (RETCODE_NO_DATA noData) {
            LOGGER.trace("no data to process", noData); // No data to process
        } catch (IOException | FormatEncodingException ex) {
            LOGGER.error(ex);
        } finally {
            bytes.return_loan(packetSequence, packetInformationSequence);
        }
    }
}
