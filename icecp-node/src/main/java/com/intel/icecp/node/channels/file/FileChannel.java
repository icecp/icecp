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
package com.intel.icecp.node.channels.file;

import com.intel.icecp.core.Message;
import com.intel.icecp.core.channels.ChannelBase;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.OnPublish;
import com.intel.icecp.core.permissions.ChannelPermission;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.core.pipeline.exception.PipelineException;
import com.intel.icecp.node.utils.SecurityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * A channel for retrieving file data and subscribing to file changes. Uses Java
 * NIO to watch the file; each file modification will bump the channel version.
 * TODO perhaps implement versioning of some sort, e.g. file.0, file.1...
 *
 */
public class FileChannel extends ChannelBase {

    public static final int MODIFICATION_WINDOW_MS = 1000;
    private static final Logger LOGGER = LogManager.getLogger();
    private final ScheduledExecutorService eventLoop;
    private boolean hasPublished = false;
    private boolean hasSubscribed = false;
    private long latestVersion = 0;
    private Path path = null;
    private FileSystem zipfs = null;
    private boolean isChannelOpen = false;

    /**
     * Constructor
     *
     * @param file the file to watch for notifications
     * @param pipeline
     * @param eventLoop
     */
    protected FileChannel(URI file, Pipeline pipeline, ScheduledExecutorService eventLoop) {
        super(file, pipeline);
        this.eventLoop = eventLoop;
    }

    /**
     * Constructor
     *
     * @param fileName the file to watch for notifications
     * @param pipeline
     * @param eventLoop
     * @throws java.net.URISyntaxException
     */
    public FileChannel(String fileName, Pipeline pipeline, ScheduledExecutorService eventLoop) throws URISyntaxException {
        this(new URI("file", fileName, null), pipeline, eventLoop);
    }

    /**
     * This open() method initializes the file for this channel. If the file is
     * on the local file system, or if the file is in the jar file it does the
     * right thing. This method does not need to be called by the user, since it
     * is called for you in subscribe(), publish, and getLastMessage().
     *
     * @return
     * @throws ChannelLifetimeException
     */
    @Override
    public CompletableFuture<Void> open() throws ChannelLifetimeException {
        SecurityUtils.checkPermission(new ChannelPermission(getName(), "open"));
        LOGGER.debug("Opening channel: " + getName());
        if (path == null) {
            path = createFileSyStem();
        }
        isChannelOpen = true;

        return CompletableFuture.completedFuture(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws ChannelLifetimeException {
        SecurityUtils.checkPermission(new ChannelPermission(getName(), "close"));
        LOGGER.debug("Closing channel: " + getName());
        hasSubscribed = false;
        isChannelOpen = false;
    }

    /**
     * Try the local file system first to create and store the path; if the path
     * doesn't exist, we assume its trying to pull the file from a JAR file. So
     * first we need to create the file system and then the path.
     *
     * @return the {@link Path} to the file to monitor
     * @throws ChannelLifetimeException if the file system cannot be created
     */
    public Path createFileSyStem() throws ChannelLifetimeException {
        try {
            return Paths.get(getName());
        } catch (FileSystemNotFoundException fsne) {
            Map<String, String> env = new HashMap<>();
            env.put("create", "true");
            try {
                zipfs = FileSystems.newFileSystem(getName(), env);
            } catch (FileSystemAlreadyExistsException e) {
                //Already exists, so just continue.
                LOGGER.trace("New File System is already mapped. ", e);
            } catch (IOException e) {
                throw new ChannelLifetimeException("Could not create file system for " + getName(), e);
            }
            return Paths.get(getName());
        }
    }

    /**
     * Subscribe to changes to the underlying file; uses Java NIO WatchService
     * in a separate thread to wait for changes. TODO use EventLoop instead of
     * separate thread?
     *
     * @param callback a callback method to receive the changed file bytes.
     * @throws ChannelIOException
     */
    @Override
    public void subscribe(final OnPublish callback) throws ChannelIOException {
        SecurityUtils.checkPermission(new ChannelPermission(getName(), "subscribe"));
        LOGGER.debug("Subscribing to channel: " + getName());
        checkOpenChannel();

        final WatchService watcher;
        try {
            watcher = FileSystems.getDefault().newWatchService();
            path.getParent().register(watcher, ENTRY_MODIFY);
        } catch (IOException e) {
            throw new ChannelIOException("Failed to subscribe to: " + getName(), e);
        }

        Runnable handler = new Runnable() {
            @Override
            public void run() {
                hasSubscribed = true;
                long lastModified = 0;
                while (hasSubscribed) {
                    // retrieve key
                    WatchKey key;
                    try {
                        key = watcher.take();
                    } catch (InterruptedException x) {
                        LOGGER.error("File watching interrupted: " + getName());
                        return;
                    }

                    // process events
                    boolean changed = false;
                    for (WatchEvent<?> event : key.pollEvents()) {
                        Path changedFile = (Path) event.context();
                        LOGGER.debug("Event on " + changedFile + ", kind " + event.kind());
                        long currentModified = path.toFile().lastModified();
                        // due to file systems throwing multiple events (see below),
                        // we set a modification window to filter out duplicates
                        if (path.endsWith(changedFile) && currentModified - lastModified > MODIFICATION_WINDOW_MS) {
                            changed = true;
                        }
                        lastModified = currentModified;
                    }

                    // do callback; we do this after the event loop because some
                    // file systems register multiple events (timestamp modified,
                    // contents modified)
                    if (changed) {
                        try {
                            latestVersion++;
                            callback.onPublish(latest().get());
                        } catch (ChannelIOException | InterruptedException | ExecutionException e) {
                            LOGGER.error("File publish callback failed: " + getName());
                            return;
                        }
                    }

                    // reset the key
                    boolean valid = key.reset();
                    if (!valid) {
                        LOGGER.error("Invalid key: " + getName());
                        return;
                    }
                }
            }
        };
        Thread thread = new Thread(handler);
        thread.setName(this.getClass().getName() + ": " + getName());
        thread.start();

        while (!hasSubscribed) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();

            }
        }
        LOGGER.debug("Subscribed to channel: " + getName());
    }

    /**
     * Retrieve a future file; file access will not occur until Future.get() is
     * called.
     *
     * @return a {@link Future} to the latest available representation of the
     * file
     * @throws ChannelIOException
     */
    @Override
    public CompletableFuture<Message> latest() throws ChannelIOException {
        SecurityUtils.checkPermission(new ChannelPermission(getName(), "subscribe"));
        LOGGER.debug("Retrieving latest message from channel: " + getName());
        checkOpenChannel();

        return CompletableFuture.supplyAsync(new Supplier<Message>() {
            @Override
            public Message get() {
                try {
                    return (Message) pipeline.executeInverse(Files.newInputStream(path));
                } catch (PipelineException | IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }, eventLoop);
    }

    /**
     * {@inheritDoc}
     *
     * Over-writes the file with the serialized message.
     */
    @Override
    public void publish(Message message) throws ChannelIOException {
        SecurityUtils.checkPermission(new ChannelPermission(getName(), "publish"));
        LOGGER.debug("Publishing message to channel: " + getName());
        checkOpenChannel();

        try {
            InputStream stream = (InputStream) this.pipeline.execute(message);
            Files.copy(stream, path, StandardCopyOption.REPLACE_EXISTING);
            latestVersion++;
            hasPublished = true;
        } catch (PipelineException | IOException e) {
            throw new ChannelIOException("Failed to publish to channel: " + getName(), e);
        }
    }

    /**
     * Ensure the channel is open.
     *
     * @throws ChannelIOException if the channel is not open
     */
    private void checkOpenChannel() throws ChannelIOException {
        if (path == null) {
            throw new ChannelIOException("Channel is not open: " + getName());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isPublishing() {
        return hasPublished;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSubscribing() {
        return hasSubscribed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpen() {
        return isChannelOpen;
    }

    /**
     * Helper class for retrieving bytes from a File; this will defer the
     * retrieval until get() is called. TODO cancellation, timeouts
     */
    private class FutureFile implements Future<Message> {

        private final Path path;

        public FutureFile(Path path) {
            this.path = path;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            throw new UnsupportedOperationException("Not yet implemented.");
        }

        @Override
        public boolean isCancelled() {
            throw new UnsupportedOperationException("Not yet implemented.");
        }

        @Override
        public boolean isDone() {
            throw new UnsupportedOperationException("Not yet implemented.");
        }

        @Override
        public Message get() throws InterruptedException, ExecutionException {
            try {
                return (Message) FileChannel.this.pipeline.executeInverse(Files.newInputStream(path));//format.decode(Files.newInputStream(path));
            } catch (PipelineException | IOException e) {
                throw new ExecutionException(e);
            }
        }

        @Override
        public Message get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return get();
        }
    }

}
