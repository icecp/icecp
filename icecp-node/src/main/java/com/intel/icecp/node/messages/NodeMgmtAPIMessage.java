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

package com.intel.icecp.node.messages;

import com.intel.icecp.core.Message;

import java.net.URI;

public class NodeMgmtAPIMessage implements Message {
    public COMMAND command;
    public String returnStatus;
    public String featureName;
    public URI featureJarChannel;
    public String[] returnArray;
    public String other;

    public NodeMgmtAPIMessage() {
    }

    public NodeMgmtAPIMessage(COMMAND command) {
        this.command = command;
    }

    public NodeMgmtAPIMessage(COMMAND command, String returnStatus) {
        this.command = command;
        this.returnStatus = returnStatus;
    }

    @Override
    public String toString() {
        return "NodeMgmtAPIMessage{" +
                "command=" + command +
                ", returnStatus='" + returnStatus + '\'' +
                '}';
    }

    public enum COMMAND {
        LOAD_FEATURE,
        STOP_FEATURE,
        GET_FEATURE_STATUS,
        GET_ALL_FEATURE_STATUSES,
        GET_ALL_CHANNELS,
        GET_MQTT_BROKER_CHANNEL_LATEST,
        GET_ALL_FEATURE_NAMES,
        PUBLISH_TO_MQTT_BROKER
    }
}
