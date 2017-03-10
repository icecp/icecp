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

package com.intel.icecp.node;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.Message;
import com.intel.icecp.core.Metadata;
import com.intel.icecp.core.Module;
import com.intel.icecp.core.Node;
import com.intel.icecp.core.attributes.ArchitectureAttribute;
import com.intel.icecp.core.attributes.AttributeRegistrationException;
import com.intel.icecp.core.attributes.Attributes;
import com.intel.icecp.core.attributes.NodeUptimeAttribute;
import com.intel.icecp.core.attributes.OperatingSystemAttribute;
import com.intel.icecp.core.attributes.ProcessorLoadAttribute;
import com.intel.icecp.core.attributes.StorageUsageAttribute;
import com.intel.icecp.core.event.Events;
import com.intel.icecp.core.event.types.ChannelEvent;
import com.intel.icecp.core.event.types.NodeEvent;
import com.intel.icecp.core.management.Channels;
import com.intel.icecp.core.management.ConfigurationManager;
import com.intel.icecp.core.management.PermissionsManager;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.modules.Modules;
import com.intel.icecp.core.permissions.NodePermission;
import com.intel.icecp.node.management.ModulesImpl;
import com.intel.icecp.node.messages.NodeInfoMessage;
import com.intel.icecp.node.utils.SecurityUtils;
import com.intel.icecp.rpc.Rpc;
import com.intel.icecp.rpc.RpcServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implement an NDN-based ICECP node.
 *
 */
class NodeImpl implements Node {

    private static final int DEFAULT_PUBLISH_STATUS_INTERVAL_MS = 60000;
    private static final Logger LOGGER = LogManager.getLogger();
    private final String name;
    private final Channels channels;
    private final ModulesImpl modules;
    private final ScheduledExecutorService eventLoop;
    private final Events events;
    private final Attributes attributes;
    private Channel<Node.State> statusChannel;
    private long startTime = -1;
    private RpcServer rpcServer;

    /**
     * Build a {@link Node} using NDN channels.
     *
     * @param name the unique name of the device
     * @param channels the channels and providers available to this node
     * @param permissionsManager the permissions manager, or how the node retrieves permissions for modules
     * @param configurationManager the configuration manager, or how the node retrieves configurations for modules (and
     * itself)
     * @param eventLoop the executor service used for managing IO tasks in an event loop
     */
    NodeImpl(String name, Channels channels, PermissionsManager permissionsManager, ConfigurationManager configurationManager, ScheduledExecutorService eventLoop, Events events) {
        this.name = name;
        this.channels = channels;
        this.modules = new ModulesImpl(this, permissionsManager, configurationManager);
        this.eventLoop = eventLoop;
        this.events = events;
        this.attributes = new AttributesImpl(channels, getDefaultUri());
        describe();
        createServer(channels, name);
    }

    RpcServer getRpcServer() {
        return rpcServer;
    }

    /**
     * Adds commands to the CommandRegistry and creates a new RpcServer instance to create a listening channel.
     */
    private void createServer(Channels channels, String nodeName) {
        rpcServer = Rpc.newServer(channels, getDefaultUri());
        new RpcCommandAdapter(this).addCommands(rpcServer);
        rpcServer.registry().list().forEach(c -> LOGGER.info("Exposing {} method as RPC command on {}", c.name, nodeName));

        try {
            rpcServer.serve();
        } catch (ChannelLifetimeException | ChannelIOException e1) {
            LOGGER.error("Failed to expose RPC commands", e1);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI getDefaultUri() {
        try {
            return new URI("ndn", getName(), null);
        } catch (URISyntaxException ex) {
            throw new Error(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Channel<Node.State> getStateChannel() {
        if (statusChannel == null) {
            throw new IllegalStateException("Status channel has not been opened; ensure start() has run.");
        }
        return statusChannel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Modules modules() {
        return modules;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Collection<Long>> loadAndStartModules(URI moduleUri, URI configurationUri) {
        final CompletableFuture<Collection<Long>> onAllStarted = new CompletableFuture<>();

        CompletableFuture<Collection<Long>> onAllLoaded;
        try {
            onAllLoaded = modules.load(moduleUri, configurationUri);
        } catch (ChannelLifetimeException e) {
            LOGGER.error("Failed to open channel", e);
            onAllStarted.completeExceptionally(e);

            return onAllStarted;
        }

        onAllLoaded.exceptionally(t -> {
            LOGGER.error("Failed to load and start modules", t);
            onAllStarted.completeExceptionally(t);
            return null;
        });

        onAllLoaded.thenAcceptAsync((Collection<Long> loadedIds) -> {
            ArrayList<Long> startedIds = new ArrayList<>();
            ArrayList<CompletableFuture<Long>> onStarted = new ArrayList<>(loadedIds.size());

            // start all loaded modules
            for (long id : loadedIds) {
                CompletableFuture<Long> onStart = modules.start(id);
                onStarted.add(onStart);
                onStart.thenAccept(startedIds::add);
            }

            // wait for starts to complete
            try {
                CompletableFuture<Collection<Long>>[] _started = onStarted.toArray(new CompletableFuture[onStarted.size()]);
                CompletableFuture.allOf(_started).get();
                onAllStarted.complete(startedIds);
            } catch (InterruptedException | ExecutionException ex) {
                LOGGER.error("Failed to complete modules", ex);
                onAllStarted.completeExceptionally(ex);
            }
        });
        return onAllStarted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Channels channels() {
        return channels;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Message> Channel<T> openChannel(URI uri, Class<T> messageType, Persistence persistence, Metadata... metadata) throws ChannelLifetimeException {
        if (isGlobalPrefix(uri, getDefaultUri())) {
            LOGGER.warn("Attempting to create the channel {} outside the device prefix {}; ensure the channel transport allows this (e.g. NDN routing) or data transfer may be impossible.", uri, getDefaultUri());
        }

        Channel<T> openChannel = channels.openChannel(uri, messageType, persistence, metadata);
        events.notify(new ChannelEvent(this.getName(), uri, ChannelEvent.Action.OPENED));
        return openChannel;
    }

    /**
     * Tests if a URI fits under the namespace of the given node URI, e.g. ndn:/a/b/c fits into ndn:/a/b. Used for
     * logging warning when an NDN URI (e.g. ndn:/.../...) is used outside the registered device prefix (NFD routing
     * must be set up correctly)
     *
     * @param newChannelUri the newChannelUri of the channel to open
     */
    boolean isGlobalPrefix(URI nodeUri, URI newChannelUri) {
        return newChannelUri.getScheme().equals(nodeUri.getScheme())
                && !newChannelUri.getSchemeSpecificPart().startsWith(nodeUri.getSchemeSpecificPart());
    }

    /**
     * @return the IO event-loop service; use this to add IO-bound tasks to a processing queue and thus avoid blocking
     * requests (the module model is inherently multi-threaded and shares the node's transport mechanism)
     * NdnStreamChannel manages its own looping
     */
    public ScheduledExecutorService getEventLoop() {
        SecurityUtils.checkPermission(new NodePermission("event-loop"));
        return eventLoop;
    }

    /**
     * Start the node.
     */
    @Override
    public void start() {
        SecurityUtils.checkPermission(new NodePermission("start"));
        startTime = System.currentTimeMillis();
        setupStatusChannel();
        onNodeChanged();
        events.notify(new NodeEvent(name, NodeEvent.Action.STARTED));
    }

    /**
     * Stop the node.
     */
    @Override
    public void stop() {
        SecurityUtils.checkPermission(new NodePermission("stop"));
        modules.stopAll(Module.StopReason.NODE_SHUTDOWN);
        onNodeChanged();
        events.notify(new NodeEvent(name, NodeEvent.Action.STOPPED));
    }

    /**
     * If the node info module is enabled, tell it to publish new node information
     */
    private void onNodeChanged() {
        // TODO
    }

    /**
     * Setup the EphemeralChannel for publishing {@link NodeInfoMessage}s describing the current node's status; TODO
     * have a mock channel to publish OFF, LOADING status message before this method is called
     *
     * @throws Error if the channel cannot be created; this stops execution because a working status channel is required
     * by the spec.
     */
    private void setupStatusChannel() throws Error {
        // TODO make publish interval configurable
        long publishIntervalMs = DEFAULT_PUBLISH_STATUS_INTERVAL_MS;
        Persistence persistence = new Persistence(publishIntervalMs);

        try {
            URI uri = new URI("ndn", getName(), null);
            LOGGER.debug("Opening device status channel: " + uri);
            this.statusChannel = openChannel(uri, Node.State.class, persistence);
        } catch (URISyntaxException | ChannelLifetimeException e) {
            throw new Error("Unable to create device status channel.", e);
        }

        LOGGER.debug("Start publishing device status.");
        eventLoop.scheduleAtFixedRate(new NodeStatusPublisher(), 0, publishIntervalMs, TimeUnit.MILLISECONDS);
    }

    /**
     * TODO eventually these attributes should be retrieved using SPI or annotations
     *
     * @return the attributes available for this node; this implementation will lazily generate the attributes
     */
    @Override
    public Attributes describe() {
        if (attributes.size() == 0) {
            try {
                attributes.add(new NodeUptimeAttribute(startTime));
                attributes.add(new OperatingSystemAttribute());
                attributes.add(new ArchitectureAttribute());
                attributes.add(new ProcessorLoadAttribute());
                attributes.add(new StorageUsageAttribute());
            } catch (AttributeRegistrationException e) {
                throw new IllegalStateException("Node attributes could not be added.", e);
            }

        }
        return attributes;
    }

    /**
     * Publish the device status, a {@link NodeInfoMessage} to the device root prefix (e.g. /device/name). Will continue
     * to publish after the persistence interval expires.
     */
    private class NodeStatusPublisher implements Runnable {

        @Override
        public void run() {
            try {
                LOGGER.info("Publishing status: " + Node.State.ON);
                getStateChannel().publish(Node.State.ON);
            } catch (Exception e) {
                LOGGER.error("Failed to publish device status.", e);
            }
        }
    }
}
