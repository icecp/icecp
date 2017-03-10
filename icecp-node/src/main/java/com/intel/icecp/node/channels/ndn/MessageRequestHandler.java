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

package com.intel.icecp.node.channels.ndn;

import com.intel.icecp.core.Message;
import com.intel.icecp.core.channels.OnLatest;
import com.intel.icecp.core.event.EventObservable;
import com.intel.icecp.core.event.types.MessageRequestedEvent;
import com.intel.icecp.core.event.types.MessageSentEvent;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.core.pipeline.exception.PipelineException;
import com.intel.jndn.utils.impl.SegmentationHelper;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.encoding.EncodingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Handler for responding to requests for messages on a channel; this handler expects messages in the form: <ul>
 * <li>/channel/name/[marker][message id]</li> <li>/channel/name with rightmost selector</li> <li>/channel/name with
 * leftmost selector</li> </ul>
 *
 */
public class MessageRequestHandler implements OnInterestCallback {

    private static final Logger LOGGER = LogManager.getLogger();
    private final Data template;
    private final MessageCache cache;
    private final int marker;
    private final Pipeline<Message, InputStream> pipeline;
    private final ExecutorService pool;
    private final EventObservable observable;
    private OnLatest onLatest;

    /**
     * @param template the base data to use for building segments
     * @param cache the cache holding the channel's messages
     * @param marker the NDN component marker to use
     * @param pipeline the pipeline for encoding the message
     * @param pool the thread pool for dispatching the sendMessage action; TODO why not do this synchronously
     * @param observable API for notifying of message sent events
     */
    public MessageRequestHandler(Data template, MessageCache cache, int marker, Pipeline<Message, InputStream> pipeline, ExecutorService pool, EventObservable observable) {
        this.template = template;
        this.cache = cache;
        this.marker = marker;
        this.pipeline = pipeline;
        this.pool = pool;
        this.observable = observable;
    }

    /**
     * Set the OnLatest handler for responding dynamically to requests
     *
     * @param latest the callback returning a response
     */
    public void setOnLatest(OnLatest latest) {
        this.onLatest = latest;
    }

    /**
     * Attempt to respond to incoming interests; this must handle selectors, specific message IDs and segmentation
     *
     * @param prefix the registered prefix
     * @param interest the incoming interest
     * @param face the incoming face
     * @param interestFilterId the prefix's filter ID
     * @param filter the prefix's filter instance
     */
    @Override
    public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
        LOGGER.debug("Received interest {} on prefix {}", interest.toUri(), prefix);

        long id;
        OnLatest.Response latest;
        if (isLatest(interest) && onLatest != null && (latest = onLatest.onLatest()) != null) {
            id = cache.latest() + 1;
            cache.add(id, latest.message);
        } else {
            try {
                id = getMessageId(interest, prefix, marker);
            } catch (RequestEncodingException ex) {
                LOGGER.warn("Unable to understand message request, ignoring interest: {}", interest.toUri(), ex);
                return;
            }
        }

        observable.notifyApplicableObservers(new MessageRequestedEvent(id));

        if (cache.has(id)) {
            pool.submit(() -> {
                try {
                    sendMessage(id, cache.get(id), face);
                } catch (PipelineException | IOException ex) {
                    LOGGER.error("Failed to send message id {} for interest: {}", id, interest.toUri(), ex);
                }
            });
        } else {
            LOGGER.error("Message not found with id {}, ignoring interest: {}", id, interest.toUri());
        }
    }

    private boolean isLatest(Interest interest) {
        return interest.getChildSelector() == Interest.CHILD_SELECTOR_RIGHT;
    }

    private boolean isEarliest(Interest interest) {
        return interest.getChildSelector() == Interest.CHILD_SELECTOR_LEFT;
    }

    /**
     * Send the {@link Message} on the {@link Face}; this method was created mainly to test with mock objects
     *
     * @param id the unique identifier for a {@link Message}
     * @param message the {@link Message} to publish
     * @param face the NDN {@link Face} to send the message to
     * @throws PipelineException if the message cannot be serialized
     * @throws IOException if the network transport fails
     */
    private void sendMessage(long id, Message message, Face face) throws IOException, PipelineException {
        // build template
        Name.Component ndnId = Name.Component.fromNumberWithMarker(id, marker);
        Data templateCopy = new Data(template); // do not modify the base template
        templateCopy.getName().append(ndnId);
        InputStream stream = pipeline.execute(message);

        // segment into packets
        List<Data> segments = SegmentationHelper.segment(templateCopy, stream);
        int messageSize = 0;
        for (Data segment : segments) {
            face.putData(segment);
            messageSize += segment.getContent().size();
        }

        LOGGER.debug("Sending message: {}", templateCopy.getName());
        observable.notifyApplicableObservers(new MessageSentEvent(id, message, messageSize));
    }

    /**
     * @param interest the incoming interest
     * @param marker the name component marker to identify the message ID component
     * @return the best-matching message ID for the given interest
     * @throws RequestEncodingException if the interest cannot be understood
     */
    private long getMessageId(Interest interest, Name prefix, int marker) throws RequestEncodingException {
        Name suffix = interest.getName().getSubName(prefix.size());

        // check for marker in suffix components
        for (int i = 0, end = suffix.size(); i < end; i++) {
            Name.Component component = suffix.get(i);
            try {
                return component.toNumberWithMarker(marker);
            } catch (EncodingException e) {
                // do nothing; why try-catch? toNumberWithMarker() must be called anyways and the implementation checks
                // the first byte for the marker anyways
                LOGGER.trace("Name component does not begin with the expected marker, {}", e);
            }
        }

        // use rightmost selector if available
        if (isLatest(interest)) {
            long id = cache.latest();
            LOGGER.debug("Latest message requested, found {} in {}", id, interest.toUri());
            return id;
        }

        // use leftmost selector if available
        if (isEarliest(interest)) {
            long id = cache.earliest();
            LOGGER.debug("Earliest message requested, found {} in {}", id, interest.toUri());
            return id;
        }

        // fail
        throw new RequestEncodingException("No message ID found with NDN marker: " + marker);
    }
}
