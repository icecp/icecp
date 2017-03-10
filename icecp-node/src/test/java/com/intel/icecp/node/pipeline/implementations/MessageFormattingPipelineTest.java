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
package com.intel.icecp.node.pipeline.implementations;

import com.intel.icecp.core.metadata.formats.JsonFormat;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.core.pipeline.exception.PipelineException;
import com.intel.icecp.core.security.crypto.exception.hash.HashError;
import com.intel.icecp.node.messages.PermissionsMessage;
import com.intel.icecp.node.security.RandomBytesGenerator;
import com.intel.icecp.node.security.SecurityConstants;
import com.intel.icecp.node.security.crypto.utils.CryptoUtils;
import java.io.InputStream;
import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class MessageFormattingPipelineTest {

    @Test
    public void mainTest() throws PipelineException, HashError {

        Pipeline<PermissionsMessage, InputStream> p = MessageFormattingPipeline.create(PermissionsMessage.class, new JsonFormat<>(PermissionsMessage.class));

        PermissionsMessage m = new PermissionsMessage();

        m.grants = new ArrayList<>();
        m.hash = new PermissionsMessage.ModuleHash();
        m.hash.hashAlgorithm = SecurityConstants.SHA1;
        m.hash.moduleJarHash = CryptoUtils.hash(RandomBytesGenerator.getRandomBytes(1000), SecurityConstants.SHA1);
        m.name = SecurityConstants.SHA1;

        InputStream is = p.execute(m);
        Assert.assertNotNull(is);
        PermissionsMessage m1 = p.executeInverse(is);
        Assert.assertNotNull(m1);

        Assert.assertEquals(m.hash.hashAlgorithm, m1.hash.hashAlgorithm);
        Assert.assertEquals(m.name, m1.name);
        Assert.assertArrayEquals(m.hash.moduleJarHash, m1.hash.moduleJarHash);
    }
}
