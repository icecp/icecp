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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Represent a command that can be executed by both internal or remote agents;
 * presents an addressing pattern ({@link #name()} for finding and calling
 * methods, especially over the network.
 *
 */
public class Command {

    private final Object instance;
    private final Method method;
    private final String name;
    
    /**
     * Create a command that is callable
     *
     * @param instance the instance of the object to call when the command is executed
     * @param method the method to call (using reflection) when the command is executed
     * 
     * with default name the simple class name of the instance concatenated with method name seperated with dot
     */
    public Command(Object instance, Method method) {
        this(instance.getClass().getSimpleName() + "." + method.getName(), instance, method);
    }

    /**
     * Create a command that is callable
     * @param name of the command for command lookup
     * @param instance the instance of the object to call when the command is executed
     * @param method the method to call (using reflection) when the command is executed
     */
    public Command(String name, Object instance, Method method){
        this.name = name;
        this.instance = instance;
        this.method = method;
    }
    /**
     * @return the unique name of the command, either caller entered or e.g. "Node.getChannels"
     */
    public String name() {
        return name;
    }

    /**
     * @return a generated description of the command, including input/output
     * schemas; see {@link CommandDescription}.
     */
    public CommandDescription toDescription() {
        return CommandDescription.from(name(), method);
    }

    /**
     * Execute the command with the given inputs; the security check is handled
     * at the {@link CommandRegistry} level, not here.
     *
     * @param inputs the array of input parameters
     * @return the output object
     * @throws RpcServiceException when invoke method failed on instance with argument inputs
     */
    public Object execute(Object... inputs) throws RpcServiceException {
        try {
            return method.invoke(instance, inputs);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RpcServiceException(e);
        }
    }

    /**
     * @return the unique hash for this command
     */
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.instance);
        hash = 59 * hash + Objects.hashCode(this.method);
        return hash;
    }

    /**
     * Compare this command with another
     *
     * @param obj the other object
     * @return true if the objects refer to the same instance and method
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Command other = (Command) obj;
        return Objects.equals(this.instance, other.instance) && Objects.equals(this.method, other.method);
    }
}
