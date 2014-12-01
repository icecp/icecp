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
package com.intel.icecp.core.channels;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.pipeline.Pipeline;

/**
 * Convenience API for use in tests; implementors should return a new instance for use in the generic tests in this
 * package.
 *
 */
interface ChannelInstanceCreator {

    /**
     * @param channelName the name without a scheme; implementors should add the scheme
     * @param persistence the persistence to use for the test
     * @return a new instance of the channel to test
     */
    Channel newInstance(String channelName, Pipeline pipeline, Persistence persistence);
}
