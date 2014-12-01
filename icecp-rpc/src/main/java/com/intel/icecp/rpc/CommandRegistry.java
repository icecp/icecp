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
