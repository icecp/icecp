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

import com.intel.icecp.core.Message;
import com.intel.icecp.core.metadata.Format;
import com.intel.icecp.core.metadata.formats.FormatEncodingException;
import com.intel.icecp.core.misc.OnPublish;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.core.pipeline.exception.PipelineException;
import com.rti.dds.infrastructure.RETCODE_NO_DATA;
import com.rti.dds.infrastructure.ResourceLimitsQosPolicy;
import com.rti.dds.subscription.DataReader;
import com.rti.dds.subscription.DataReaderListener;
import com.rti.dds.subscription.InstanceStateKind;
import com.rti.dds.subscription.LivelinessChangedStatus;
import com.rti.dds.subscription.RequestedDeadlineMissedStatus;
import com.rti.dds.subscription.RequestedIncompatibleQosStatus;
import com.rti.dds.subscription.SampleInfo;
import com.rti.dds.subscription.SampleInfoSeq;
import com.rti.dds.subscription.SampleLostStatus;
import com.rti.dds.subscription.SampleRejectedStatus;
import com.rti.dds.subscription.SampleStateKind;
import com.rti.dds.subscription.SubscriptionMatchedStatus;
import com.rti.dds.subscription.ViewStateKind;
import com.rti.dds.type.builtin.Bytes;
import com.rti.dds.type.builtin.BytesDataReader;
import com.rti.dds.type.builtin.BytesSeq;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 */
public class DdsListener implements DataReaderListener {

    protected static final Logger LOGGER = LogManager.getLogger();
    private final OnPublish callback;
    private final Pipeline format;
    protected SampleInfoSeq packetInformationSequence;
    protected BytesSeq packetSequence;

    public DdsListener(OnPublish callback, Pipeline format) {
        this.callback = callback;
        this.format = format;
        this.packetSequence = new BytesSeq();
        this.packetInformationSequence = new SampleInfoSeq();
    }

    @Override
    public void on_data_available(DataReader reader) {
        BytesDataReader bytes = (BytesDataReader) reader;
        try {
            bytes.take(packetSequence, packetInformationSequence,
                    ResourceLimitsQosPolicy.LENGTH_UNLIMITED,
                    SampleStateKind.ANY_SAMPLE_STATE,
                    ViewStateKind.ANY_VIEW_STATE,
                    InstanceStateKind.ANY_INSTANCE_STATE);
            for (int i = 0; i < packetSequence.size(); ++i) {
                SampleInfo info = (SampleInfo) packetInformationSequence.get(i);
                if (info.valid_data) {
                    process(((Bytes) packetSequence.get(i)).value);
                }
            }
        } catch (RETCODE_NO_DATA noData) {
            LOGGER.trace("Ran out of data to process. ", noData); // No data to process
        } catch (IOException | FormatEncodingException ex) {
            LOGGER.error(ex);
        } finally {
            bytes.return_loan(packetSequence, packetInformationSequence);
        }
    }

    protected void process(byte[] value) throws FormatEncodingException, IOException {
        InputStream stream = new ByteArrayInputStream(value);
        Message message;
        try {
            message = (Message) format.executeInverse(stream);
        } catch (PipelineException ex) {
            throw new IOException();
        }
        callback.onPublish(message);
    }

    @Override
    public void on_requested_deadline_missed(DataReader reader, RequestedDeadlineMissedStatus rdms) {
        LOGGER.info("Deadline missed: " + rdms);
    }

    @Override
    public void on_requested_incompatible_qos(DataReader reader, RequestedIncompatibleQosStatus riqs) {
        LOGGER.info("Incompatible QOS: " + riqs);
    }

    @Override
    public void on_sample_rejected(DataReader reader, SampleRejectedStatus srs) {
        LOGGER.info("Sample rejected: " + srs);
    }

    @Override
    public void on_liveliness_changed(DataReader reader, LivelinessChangedStatus lcs) {
        LOGGER.info("Liveliness changed " + lcs);
    }

    @Override
    public void on_sample_lost(DataReader reader, SampleLostStatus sls) {
        LOGGER.info("Sample lost: " + sls);
    }

    @Override
    public void on_subscription_matched(DataReader reader, SubscriptionMatchedStatus sms) {
        LOGGER.info("Subscription matched: " + sms);
    }

}
