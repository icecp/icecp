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
package com.intel.icecp.node.management;

import com.intel.icecp.core.channels.ChannelProvider;
import com.intel.icecp.core.management.ModulePermissions;
import com.intel.icecp.core.management.PermissionsManager;
import com.intel.icecp.core.metadata.formats.FormatEncodingException;
import com.intel.icecp.core.metadata.formats.JsonFormat;
import com.intel.icecp.core.pipeline.Pipeline;
import com.intel.icecp.core.pipeline.exception.PipelineException;
import com.intel.icecp.node.channels.file.FileChannelProvider;
import com.intel.icecp.node.messages.PermissionsMessage;
import com.intel.icecp.node.security.SecurityConstants;
import com.intel.icecp.core.security.crypto.exception.siganture.UnsupportedSignatureAlgorithmException;
import com.intel.icecp.node.security.crypto.utils.CryptoUtils;
import com.intel.icecp.core.security.crypto.exception.hash.HashError;
import com.intel.icecp.core.security.crypto.exception.hash.HashVerificationFailedException;
import com.intel.icecp.core.security.crypto.exception.key.InvalidKeyTypeException;
import com.intel.icecp.core.security.keymanagement.exception.KeyManagerNotSupportedException;
import com.intel.icecp.node.pipeline.implementations.MessageFormattingPipeline;
import com.intel.icecp.node.security.RandomBytesGenerator;
import com.intel.icecp.node.utils.StreamUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test for FilePermisssioManager in case of signed permissions files
 *
 *
 */
public class FilePermissionManagerTest {

    private static final byte[] randomBytesToHash = RandomBytesGenerator.getRandomBytes(2048);

    @Before
    public void init() throws FormatEncodingException, IOException, UnsupportedSignatureAlgorithmException, KeyManagerNotSupportedException, HashError, InvalidKeyTypeException, PipelineException {

        // Create a fake signed permissions file
        PermissionsMessage message = new PermissionsMessage();
        message.grants = new ArrayList();
        message.name = "/com/intel/module";
        message.hash = new PermissionsMessage.ModuleHash();
        message.hash.hashAlgorithm = SecurityConstants.SHA256;
        message.hash.moduleJarHash = CryptoUtils.base64Encode(CryptoUtils.hash(randomBytesToHash, message.hash.hashAlgorithm));

        message.grants = new ArrayList();
        PermissionsMessage.Grant g = new PermissionsMessage.Grant();
        g.action = "subscribe,publish";
        g.target = "ndn:/intel/node";
        g.permission = "com.intel.icecp.core.permissions.ChannelPermission2";
        message.grants.add(g);

        Pipeline pipeline = MessageFormattingPipeline.create(PermissionsMessage.class, new JsonFormat<>(PermissionsMessage.class));

        // @TODO: TO WORK WITH SIGNED MESSAGES REPLACE THE PIPELINE CREATION WITH THE LINES BELOW
//		new GenericPipelineBuilder(PermissionsMessage.class, InputStream.class)
//				.addOperation(new FormattingOperation(new JsonFormat<>(PermissionsMessage.class)))
//				.addOperation(new AsymmetricSignatureOperation("/ndn/intel/node/key", null))
//				.addOperation(new FormattingOperation(new JsonFormat<>(SignedMessage.class)))
//				.build();
        // Save the message encoded into a file
        FileOutputStream fout = new FileOutputStream(new File("perm_file.json"));
        fout.write(StreamUtils.readAll((InputStream) pipeline.execute(message)));
        fout.close();

    }

    @Ignore // TODO avoid this until it can be re-factored to not depend on file system changes
    @Test
    public void testPermissionsVerification() throws Exception {
        ChannelProvider fileChannelBuilder = new FileChannelProvider();
        PermissionsManager manager = new FilePermissionsManager(Paths.get(""), fileChannelBuilder);
        ModulePermissions permissions = manager.retrievePermissions("perm_file");

        // Permissions must be not null
        Assert.assertNotNull(permissions);

        // Verify the hash of the module bytes
        if (!CryptoUtils.compareBytes(
                CryptoUtils.hash(randomBytesToHash, permissions.getHashAlgorithm()),
                CryptoUtils.base64Decode(permissions.getModuleHash()))) {
            // @Moreno: throw an exception
            throw new HashVerificationFailedException("Invalid hash in permissions file.");
        }

        try {
            Files.delete(Paths.get("perm_file.json"));
        } catch (IOException ex) {

        }

    }

}
