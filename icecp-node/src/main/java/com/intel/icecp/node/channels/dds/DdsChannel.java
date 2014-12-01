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
import com.intel.icecp.core.channels.ChannelBase;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.OnPublish;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.node.utils.StreamUtils;
import com.rti.dds.domain.DomainParticipant;
import com.rti.dds.domain.DomainParticipantFactory;
import com.rti.dds.infrastructure.InstanceHandle_t;
import com.rti.dds.infrastructure.StatusKind;
import com.rti.dds.subscription.DataReader;
import com.rti.dds.topic.Topic;
import com.rti.dds.type.builtin.BytesDataReader;
import com.rti.dds.type.builtin.BytesDataWriter;
import com.rti.dds.type.builtin.BytesTypeSupport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * Implement the ICECP pub/sub abstraction with RTI DDS. Notes:
 * <ol>
 * <li>DDS RTI must be installed and the ndds JAR file must be included as a
 * dependency; we moved this to Artifactory (see POM.xml)</li>
 * <li>ensure the environment PATH variable is set to the location of the ndds
 * library you are using; e.g. C:\Program Files
 * (x86)\RTI\ndds.5.1.0\lib\x64Win64jdk</li>
 * <li>ensure the environment RTI_LICENSE_FILE variable points to a valid RTI
 * license file; e.g. C:\Program Files (x86)\RTI\rti_license.dat</li>
 * <li>Each channel must have its own DomainParticipant; using the same one for
 * both publishing and subscribing caused issues</li>
 * </ol>
 *
 */
public class DdsChannel extends ChannelBase {

    private static final Logger logger = LogManager.getLogger();
    private static final String userQOSLibrary = "UserQOS_Library";
    private static final String userQOSprofile = "User_QOS_Profile";
    private final int domainId;
    private final Persistence persistence;
    private DomainParticipant participant;
    private Topic topic;
    private BytesDataWriter writer;
    private BytesDataReader reader;
    private boolean isChannelOpen = false;

    protected DdsChannel(URI name, Pipeline encodingOpeations, int domainId, Persistence persistence) {
        super(name, encodingOpeations);
        this.domainId = domainId;
        this.persistence = persistence;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Void> open() throws ChannelLifetimeException {
        String messageType = BytesTypeSupport.get_type_name();

        participant = DomainParticipantFactory.get_instance().create_participant_with_profile(
                domainId,
                userQOSLibrary, userQOSprofile,
                null, // listener
                StatusKind.STATUS_MASK_NONE);
        checkNull(participant);

        topic = participant.create_topic_with_profile(
                getName().getSchemeSpecificPart(),
                messageType,
                userQOSLibrary, userQOSprofile,
                null, // listener
                StatusKind.STATUS_MASK_NONE);
        checkNull(topic);
        isChannelOpen = true;

        return CompletableFuture.completedFuture(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws ChannelLifetimeException {
        //participant.delete_topic(topic);
        participant.delete_datawriter(writer);
        participant.delete_datareader(reader);

        participant.delete_topic(topic);
        DomainParticipantFactory.get_instance().delete_participant(participant);
        isChannelOpen = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(Message message) throws ChannelIOException {
        try {
            InputStream stream = (InputStream) pipeline.execute(message);
            byte[] bytes = StreamUtils.readAll(stream);
            // TODO https://community.rti.com/best-practices/register-instance-and-use-instancehandle-when-writing-better-performance		
            getWriter().write(bytes, 0, bytes.length, InstanceHandle_t.HANDLE_NIL);
        } catch (Exception ex) {
            throw new ChannelIOException("Failed to publish message.", ex);
        }
    }

    /**
     * @return a bytes data reader
     * @throws ChannelLifetimeException
     */
    protected BytesDataWriter getWriter() throws ChannelLifetimeException {
        if (writer == null) {
            writer = (BytesDataWriter) participant.create_datawriter_with_profile(topic,
                    userQOSLibrary, userQOSprofile,
                    null, // listener
                    StatusKind.STATUS_MASK_NONE);

            checkNull(writer);
        }
        return writer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpen() {
        return isChannelOpen;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPublishing() {
        return writer != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void subscribe(OnPublish callback) throws ChannelIOException {
        reader = (BytesDataReader) participant.create_datareader_with_profile(
                topic, userQOSLibrary, userQOSprofile,
                new DdsListener(callback, pipeline),
                StatusKind.STATUS_MASK_ALL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSubscribing() {
        return reader != null;
    }

    /**
     * {@inheritDoc}
     *
     * @return
     * @throws com.intel.icecp.core.misc.ChannelIOException
     */
    @Override
    public CompletableFuture latest() throws ChannelIOException {
        CompletableFuture future = new CompletableFuture();

        OnPublish onPublish = new OnPublish() {
            @Override
            public void onPublish(Message message) {
                future.complete(message);
            }
        };

        DataReader latestReader = participant.create_datareader_with_profile(
                topic,
                userQOSLibrary, userQOSprofile,
                new LatestDdsListener(onPublish, pipeline),
                StatusKind.STATUS_MASK_ALL);

        participant.delete_datareader(latestReader);

        return future;
    }

    public Persistence getPersistence() {
        return persistence;
    }

    private void checkNull(Object object) throws ChannelLifetimeException {
        if (object == null) {
            throw new ChannelLifetimeException("The created object was null.");
        }
    }

}
