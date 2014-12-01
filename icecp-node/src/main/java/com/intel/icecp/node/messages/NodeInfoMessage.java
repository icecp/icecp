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
import com.intel.icecp.node.utils.VersionUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Represent the node's detailed information.
 *
 */
public class NodeInfoMessage implements Message {

    public final String name;
    public final String uri;
    public final String[] features;
    public final String[] channels;
    public final List<FaceConfigurationMessage> mesh = new ArrayList<>();
    public final String version = VersionUtils.retrieveCurrentVersion();

    public NodeInfoMessage(String name, String[] featureNames, String[] channelNames, String uri) {
        this.name = name;
        this.features = featureNames;
        this.channels = channelNames;
        this.uri = uri;
    }
}
