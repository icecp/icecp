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

import com.intel.icecp.core.messages.BytesMessage;
import com.intel.icecp.core.metadata.Format;
import com.intel.icecp.core.metadata.formats.JsonFormat;
import com.intel.icecp.core.security.CryptoProvider;
import com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey;
import com.intel.icecp.node.messages.PermissionsMessage;
import com.intel.icecp.node.security.SecurityConstants;
import com.intel.icecp.node.security.crypto.key.KeyProvider;
import com.intel.icecp.node.security.crypto.signature.Signature;
import com.intel.icecp.node.utils.FormatUtils;
import com.intel.icecp.node.utils.StreamUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;

/**
 */
public class BytesMessageTest {

    private static final Logger logger = LogManager.getLogger();

    @Ignore
    @Test
    public void test() throws Exception {
        // TEST: Wrap a PermissionMessage into an BytesMessage
        PermissionsMessage mess = new PermissionsMessage();
        mess.grants = new ArrayList<>();
        mess.name = "TestModule";
        PermissionsMessage.Grant g = new PermissionsMessage.Grant();
        g.action = "open";
        g.permission = "perm";
        g.target = "/ndn/intel/";
        mess.grants.add(g);

        SignedMessage sigMess = new SignedMessage();
        sigMess.message = new BytesMessage(StreamUtils.readAll(new JsonFormat<>(PermissionsMessage.class).encode(mess)));
        sigMess.signature = new Signature();

        JsonFormat<SignedMessage> f = new JsonFormat<>(SignedMessage.class);

        SymmetricKey key = KeyProvider.generateSymmetricKey(SecurityConstants.AES);
        // Encrypt the bytes
        byte[] encBytes = CryptoProvider.getCipher(SecurityConstants.AES_CBC_ALGORITHM, false).encrypt(
                StreamUtils.readAll(f.encode(sigMess)), key);

        BytesMessage encryptedMessage = new BytesMessage(encBytes);

        Format<BytesMessage> encryptedMsgFormat = FormatUtils.getMessageFormatter(f, BytesMessage.class);

        // Set the encoded encrypted message as content
        // if we comment the whole block, or if the block fails,
        // the execution of the method still continue.
        logger.debug(new String(StreamUtils.readAll(encryptedMsgFormat.encode(encryptedMessage))));

        // TEST: DECRYPT
        encryptedMsgFormat = FormatUtils.getMessageFormatter(f, BytesMessage.class);

        InputStream is = encryptedMsgFormat.encode(encryptedMessage);

        // First retrieve the encrypted message
        BytesMessage encMsg = encryptedMsgFormat.decode(is);

        // Decrypt message bytes (for now we can ignore the other fields)
        byte[] bytes = CryptoProvider.getCipher(
                SecurityConstants.AES_CBC_ALGORITHM,            // THIS SHOULD BE RETRIEVED FROM CHANNEL METADATA
                false).decrypt(encMsg.getBytes(), key);

        logger.debug(new String(StreamUtils.readAll(new ByteArrayInputStream(bytes))));
    }
}