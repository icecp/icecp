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
package com.intel.icecp.rpc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Manage commands in a central registry, controlling access to the commands using Java permissions. TODO in the future,
 * for concurrent commands, this registry may need to run commands in a thread pool.
 *
 */
public class CommandRegistry {
    private static final long TIMEOUT_FOR_FUTURE_SEC = 60;
    
    private final Map<String, Command> commands = new HashMap<>();

    /**
     * @param command the command to add
     */
    public void add(Command command) {
        commands.put(command.name(), command);
    }

    /**
     * @param commandName the command's name
     */
    public void remove(String commandName) {
        commands.remove(commandName);
    }

    /**
     * @return a list of descriptions of the registered commands
     */
    public List<CommandDescription> list() {
        return commands.values().stream().map(Command::toDescription).collect(Collectors.toList());
    }

    /**
     * @return the number of commands in the registry
     */
    public int size() {
        return commands.size();
    }

    /**
     * Invoke a command from a request; TODO security permissions must be applied here
     *
     * @param request the remote request sent by the client
     * @return the response to send back to the client
     * @throws RpcServiceException the reasons can be: if the caller cannot call the specified method, or if the wrong parameters
     * were passed, or if the specified method fails while called
     */
    public CommandResponse execute(CommandRequest request) throws RpcServiceException {
        Command command = find(request);
        Object response = command.execute(request.inputs);
        if(request.responseUri == null){
            return new EmptyCommandResponse();
        }
        
        if(response instanceof Future){
            return CommandResponse.fromValidFuture((Future)response, TIMEOUT_FOR_FUTURE_SEC, TimeUnit.SECONDS);
        }
        return CommandResponse.fromValid(response);
    }

    /**
     * Find a command using a request; TODO match schemas
     * <p>
     *     If method is not found, this can throw IllegalArgumentException
     * @param request the request sent by the client
     * @return a {@link Command}
     */
    private Command find(CommandRequest request) {
        Command command = commands.get(request.name);
        if (command == null) {
            throw new IllegalArgumentException(request.name + " not found.");
        }
        return command;
    }
}
