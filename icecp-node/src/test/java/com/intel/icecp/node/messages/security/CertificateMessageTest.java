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
package com.intel.icecp.node.messages.security;

import com.intel.icecp.core.Message;
import com.intel.icecp.core.metadata.formats.JsonFormat;
import java.io.FileInputStream;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 */
@Ignore
public class CertificateMessageTest implements Message {

    @Test
    public void decodingTest() throws Exception {
        JsonFormat<CertificateMessage> format = new JsonFormat<>(CertificateMessage.class);

        FileInputStream inputStream = new FileInputStream("keystore/chain.json");
        try {
//			String everything = StreamUtils.readAllToString(inputStream);

            CertificateMessage m = format.decode(inputStream);

            System.out.println(m.certificate);

        } finally {
            inputStream.close();
        }

    }

}
