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

import com.intel.icecp.core.Message;
import com.intel.icecp.node.utils.ChannelUtils;

import java.net.URI;
import java.util.Arrays;
import java.util.UUID;

/**
 * Represent a request to execute a command {@link Command}
 *
 * @see CommandRegistry
 */
public class CommandRequest implements Message {
    private static final long serialVersionUID = 2838495725112425008L;
    
    public String name;
    public Object[] inputs;
    public URI responseUri;
    
    private static final URI autoResponseUri = URI.create("/icecp/intelRpc/");

    /**
     * Helper method for building requests
     *
     * @param name the method name
     * @param inputs the method inputs
     * @return a new command request from the given parameters
     */
    public static CommandRequest fromWithoutResponse(String name, Object... inputs) {
        CommandRequest request = new CommandRequest();
        request.name = name;
        request.inputs = inputs;
        return request;
    }
    
    /**
     * Helper method for building requests where a response URI is
     * automatically created.
     *
     * @param name the method name
     * @param inputs the method inputs
     * @return a new command request from the given parameters
     */
    public static CommandRequest from(String name, Object... inputs) {
        CommandRequest request = new CommandRequest();
        request.name = name;
        request.inputs = inputs;
        request.responseUri = ChannelUtils.join(autoResponseUri, UUID.randomUUID().toString());
        return request;
    }

    /**
     * Helper method for building requests
     *
     * @param name the method name
     * @param responseUri responseUri
     * @param inputs the method inputs
     * @return a new command request from the given parameters
     */
    public static CommandRequest from(String name, URI responseUri, Object... inputs) {
        CommandRequest request = new CommandRequest();
        request.name = name;
        request.inputs = inputs;
        request.responseUri = responseUri;
        return request;
    }

    /**
     * @param command a {@link Command}
     * @return true if the names of the command and request match; TODO also match input objects to schema
     */
    public boolean matches(Command command) {
        return name.equals(command.name());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(inputs);
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((responseUri == null) ? 0 : responseUri.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CommandRequest other = (CommandRequest) obj;
        if (!Arrays.equals(inputs, other.inputs))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (responseUri == null) {
            if (other.responseUri != null)
                return false;
        } else if (!responseUri.equals(other.responseUri))
            return false;
        return true;
    }
}