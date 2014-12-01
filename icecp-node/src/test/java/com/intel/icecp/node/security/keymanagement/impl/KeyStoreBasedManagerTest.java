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
package com.intel.icecp.node.security.keymanagement.impl;

import com.intel.icecp.core.management.ConfigurationManager;
import com.intel.icecp.core.security.crypto.key.asymmetric.KeyPair;
import com.intel.icecp.node.channels.file.FileChannelProvider;
import com.intel.icecp.core.security.keymanagement.exception.KeyManagerException;
import com.intel.icecp.node.management.FileConfigurationManager;
import com.intel.icecp.node.security.SecurityConstants;
import com.intel.icecp.node.security.crypto.key.KeyProvider;
import com.intel.icecp.node.security.keymanagement.MockFileOnlyChannels;
import com.intel.icecp.node.security.utils.PemEncodingUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test for {@link KeyStoreBasedManager} class
 *
 */
public class KeyStoreBasedManagerTest {

    private static final Logger LOGGER = LogManager.getLogger();
    
    /** Default directory */
    private static final String BASE_DIR = "src/test/resources/fixtures";
    private static final String KEYSTORE_DIR = BASE_DIR + "/stores";
    private static final String CERTIFICATE_MESSAGES_DIR = BASE_DIR + "/messages";

    /** Key manager configuration JSON */
    private static final String KEY_MANAGER_COFIGURATION = "{\n"
            + "	\"truststore\" : \"src/test/resources/fixtures/stores/truststore.jceks\",\n"
            + "	\"keystore\" : \"src/test/resources/fixtures/stores/keystore.jceks\"\n"
            + "}\n";
    
    
    /**
     * Trusted chain
     */
    private static final String CHAIN_TRUSTED_MESSAGE_FILE = "sample_chain_trusted.json";
    private static final String CHAIN_TRUSTED_MESSAGE = "{\n"
            + "\"certificate\" : \"-----BEGIN CERTIFICATE-----\\nMIID7zCCAtcCCQDVPJd3kD2gezANBgkqhkiG9w0BAQsFADB2MQswCQYDVQQGEwJV\\nUzELMAkGA1UECAwCT1IxETAPBgNVBAcMCFBvcnRsYW5kMQ4wDAYDVQQKDAVJbnRl\\nbDENMAsGA1UECwwESW9URzEoMCYGCSqGSIb3DQEJARYZbW9yZW5vLmFtYnJvc2lu\\nQGludGVsLmNvbTAeFw0xNjA5MjkwMDQ5NDRaFw0yNjA5MjcwMDQ5NDRaMGcxCzAJ\\nBgNVBAYTAklOMQswCQYDVQQIEwJPUjERMA8GA1UEBxMIUG9ydGxhbmQxDjAMBgNV\\nBAoTBUludGVsMQ4wDAYDVQQLEwVJbnRlbDEYMBYGA1UEAxMPTW9yZW5vIEFtYnJv\\nc2luMIIBuDCCASwGByqGSM44BAEwggEfAoGBAP1/U4EddRIpUt9KnC7s5Of2EbdS\\nPO9EAMMeP4C2USZpRV1AIlH7WT2NWPq/xfW6MPbLm1Vs14E7gB00b/JmYLdrmVCl\\npJ+f6AR7ECLCT7up1/63xhv4O1fnxqimFQ8E+4P208UewwI1VBNaFpEy9nXzrith\\n1yrv8iIDGZ3RSAHHAhUAl2BQjxUjC8yykrmCouuEC/BYHPUCgYEA9+GghdabPd7L\\nvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3\\nzwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImo\\ng9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoDgYUAAoGBAMbRR5IOCw7laFssE0BCiS3I\\ncHfwqG/+QnD0geybYuQjsntD68wR0q7zqV9su0iQhxeyeHSa901JqcWWWJ6kf31a\\nuL/K2QS7NycFV9C8KrlQgIa5W0y5M2cDqViry/X1qCo+sJuHMZpMz5wQSx21Abl3\\niBACFWZtl7gADMg1UWJYMA0GCSqGSIb3DQEBCwUAA4IBAQAe5RtI4lSsZxipI/q1\\nWmu99OHHBu+LSCECTeUTfvHeZT+HbKSBcUot9Fhb9l2bLze8rX0eSd2kIehyVjsT\\nBOCi1LB50cIMaPGf2pe+Utlw8GB2y+GVH/okbT3H2KXXbgJN4r2pcDkfNeyR+eE2\\n7Fg0L5tgDdEVPdiWZaeDZTRCEMZICTbmEHKp8Zc2zGICpz+nBkn4AoCuEnLlQhOb\\nPQCmM7l4SP6l+iK7I7OJt72wKPn8TMCasXEnPhRXs7+2y0F2oCoS09TrACplWNKW\\nSt4zlLaq6extBOGAiOaDmpqFy+qY///AwUcl3MblkuL5VyBinw8+zpViFT/7mc3O\\nrnWj\\n-----END CERTIFICATE-----\\n-----BEGIN CERTIFICATE-----\\nMIIDvzCCAqegAwIBAgIJAPc+2nj9MlJyMA0GCSqGSIb3DQEBCwUAMHYxCzAJBgNV\\nBAYTAlVTMQswCQYDVQQIDAJPUjERMA8GA1UEBwwIUG9ydGxhbmQxDjAMBgNVBAoM\\nBUludGVsMQ0wCwYDVQQLDARJb1RHMSgwJgYJKoZIhvcNAQkBFhltb3Jlbm8uYW1i\\ncm9zaW5AaW50ZWwuY29tMB4XDTE2MDkyOTAwNDAyMloXDTI2MDkyNzAwNDAyMlow\\ndjELMAkGA1UEBhMCVVMxCzAJBgNVBAgMAk9SMREwDwYDVQQHDAhQb3J0bGFuZDEO\\nMAwGA1UECgwFSW50ZWwxDTALBgNVBAsMBElvVEcxKDAmBgkqhkiG9w0BCQEWGW1v\\ncmVuby5hbWJyb3NpbkBpbnRlbC5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAw\\nggEKAoIBAQCkFkv8N5DJtfTLcSf7rQmixwfHWrxzpH7+Sb/0mZNxNPTrMEIHfuTu\\nsU947r+svAhbq3Sjts1IMSxaJ231c4rSms2WeAs3aRn2bJ22CVbOPtksEAbXLyix\\np4PC17FprJbz8i8OXiKYd2AnmBz3kBrmRUEKp9KSuOyOfn/PZe5DxfIu2ATJ18VM\\n658pGYGbBUak9V4UcEHIBsuGPi2+/rcD7I1YJLsuzlHxtJZZVEc42KGw+Pcj9GuF\\nej6zlZnzjheXQ+JvanbDgvnfdWl3L0suJyl8fyIdEKDIwIu1HLqgbUPaBbyQgnVg\\naK1gHsbcWsfSbtJn/J5Em7md1N0hmiI7AgMBAAGjUDBOMB0GA1UdDgQWBBQVdOvJ\\nzHGvkzUDyNMw6hf8M46TQDAfBgNVHSMEGDAWgBQVdOvJzHGvkzUDyNMw6hf8M46T\\nQDAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQA04JPlrSAhd7UA2qAK\\nwAZG+fOM4iCqBonJGYu+11pBIwudl10Ilnf1eyqiSlETS/a+5O+pzOGHSxqQ8Yl+\\nC3gAyXyrGh4lOl4Fcb4GNocuzDIEjtkQAF+k/Oo2RvOJ9UeUd94a5sUo7Lcrg2rK\\nJhg7YT47drO5LUC6G74ZW00cFtCofRb4Pcsi+soH7XqDRYKFYioN+/vzVdNUXQz4\\nVvzU2sgam+lpIEBdc9+yOz0mr+/RwYuHo8gYeKAXV60ZXeZiPyLYlU0cBQV5nlLg\\nxZ+2/DqWsYzW+Vft1VsN9WZ2NPs3kCgd32iLImP7Aufc6GHNbmFTfwSMzwUkyfsU\\nXNZ9\\n-----END CERTIFICATE-----\"\n"
            + "}";

    /**
     * Trusted chain with one element
     */
    private static final String SINGLE_TRUSTED_MESSAGE_FILE = "sample_single_trusted.json";
    private static final String SINGLE_TRUSTED_MESSAGE = "{\n"
            + "\"certificate\" : \"-----BEGIN CERTIFICATE-----\\nMIIDvzCCAqegAwIBAgIJAPc+2nj9MlJyMA0GCSqGSIb3DQEBCwUAMHYxCzAJBgNV\\nBAYTAlVTMQswCQYDVQQIDAJPUjERMA8GA1UEBwwIUG9ydGxhbmQxDjAMBgNVBAoM\\nBUludGVsMQ0wCwYDVQQLDARJb1RHMSgwJgYJKoZIhvcNAQkBFhltb3Jlbm8uYW1i\\ncm9zaW5AaW50ZWwuY29tMB4XDTE2MDkyOTAwNDAyMloXDTI2MDkyNzAwNDAyMlow\\ndjELMAkGA1UEBhMCVVMxCzAJBgNVBAgMAk9SMREwDwYDVQQHDAhQb3J0bGFuZDEO\\nMAwGA1UECgwFSW50ZWwxDTALBgNVBAsMBElvVEcxKDAmBgkqhkiG9w0BCQEWGW1v\\ncmVuby5hbWJyb3NpbkBpbnRlbC5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAw\\nggEKAoIBAQCkFkv8N5DJtfTLcSf7rQmixwfHWrxzpH7+Sb/0mZNxNPTrMEIHfuTu\\nsU947r+svAhbq3Sjts1IMSxaJ231c4rSms2WeAs3aRn2bJ22CVbOPtksEAbXLyix\\np4PC17FprJbz8i8OXiKYd2AnmBz3kBrmRUEKp9KSuOyOfn/PZe5DxfIu2ATJ18VM\\n658pGYGbBUak9V4UcEHIBsuGPi2+/rcD7I1YJLsuzlHxtJZZVEc42KGw+Pcj9GuF\\nej6zlZnzjheXQ+JvanbDgvnfdWl3L0suJyl8fyIdEKDIwIu1HLqgbUPaBbyQgnVg\\naK1gHsbcWsfSbtJn/J5Em7md1N0hmiI7AgMBAAGjUDBOMB0GA1UdDgQWBBQVdOvJ\\nzHGvkzUDyNMw6hf8M46TQDAfBgNVHSMEGDAWgBQVdOvJzHGvkzUDyNMw6hf8M46T\\nQDAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQA04JPlrSAhd7UA2qAK\\nwAZG+fOM4iCqBonJGYu+11pBIwudl10Ilnf1eyqiSlETS/a+5O+pzOGHSxqQ8Yl+\\nC3gAyXyrGh4lOl4Fcb4GNocuzDIEjtkQAF+k/Oo2RvOJ9UeUd94a5sUo7Lcrg2rK\\nJhg7YT47drO5LUC6G74ZW00cFtCofRb4Pcsi+soH7XqDRYKFYioN+/vzVdNUXQz4\\nVvzU2sgam+lpIEBdc9+yOz0mr+/RwYuHo8gYeKAXV60ZXeZiPyLYlU0cBQV5nlLg\\nxZ+2/DqWsYzW+Vft1VsN9WZ2NPs3kCgd32iLImP7Aufc6GHNbmFTfwSMzwUkyfsU\\nXNZ9\\n-----END CERTIFICATE-----\"\n"
            + "}";

    /**
     * Untrusted chain with one element
     */
    private static final String SINGLE_UNTRUSTED_MESSAGE_FILE = "sample_single_untrusted.json";
    private static final String SINGLE_UNTRUSTED_MESSAGE = "{\n"
            + "\"certificate\" : \"-----BEGIN CERTIFICATE-----\\n-----BEGIN CERTIFICATE-----\\nMIIDpTCCAo2gAwIBAgIJAMk3+GLtCrWFMA0GCSqGSIb3DQEBCwUAMGkxCzAJBgNV\\nBAYTAklUMQswCQYDVQQIDAJQRDELMAkGA1UEBwwCUEQxDjAMBgNVBAoMBVVuaXBk\\nMQ8wDQYDVQQLDAZTcHJpenoxHzAdBgkqhkiG9w0BCQEWEHNwcml6ekBzcHJpenou\\naXQwHhcNMTYwOTI5MDExMDUwWhcNMjYwOTI3MDExMDUwWjBpMQswCQYDVQQGEwJJ\\nVDELMAkGA1UECAwCUEQxCzAJBgNVBAcMAlBEMQ4wDAYDVQQKDAVVbmlwZDEPMA0G\\nA1UECwwGU3ByaXp6MR8wHQYJKoZIhvcNAQkBFhBzcHJpenpAc3ByaXp6Lml0MIIB\\nIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnJUaSTfjAbbm777j0ht0UcWU\\n1MAAEdStL4C5VABjgeJkwi8DgsoYgpGWcqTd/Jx6fX2/3ELjjQp1u9halQDb36Iu\\nn92M6f6phLjLpqoumcbK1aTq4b4qTzAgauy4cQaANHbNsaByLYMl5kovnMxsINZi\\nDQC+mXbmd10f4TwXAdC2Y+uNotmBoCPn5GlEOKAXBATa18WqtcpW9EQJoLzD7LGP\\nB0geYFNUg3CvopB59XbwAMZEzp0vdzJDVp5/l2QXG6h4ZGkvXUTeftqb/wIh2y4N\\ntTcLdMe6YiES5n88HLEOKvuPfCnclGxnKKeAznVkd3dqvRRMBaRheOkzJPqiYQID\\nAQABo1AwTjAdBgNVHQ4EFgQUsB8SrcNI7YNPtJOi641vmZImVQUwHwYDVR0jBBgw\\nFoAUsB8SrcNI7YNPtJOi641vmZImVQUwDAYDVR0TBAUwAwEB/zANBgkqhkiG9w0B\\nAQsFAAOCAQEAij9TknSy4yZnt83D1vFsEFVPV/SIvI7PzWVTwKUEGADyi9tfSfeb\\nJ9l0U2azL7oQcOKW6FGoa0JOCsPFbaRfRWE0R9wPpZ4yl9HYSIXlmWHEhOO2gEo7\\nlrpBSI/pAYrx+qvjDNgnAXi34PP0LljBr6koieREJ4nuOHBLtp0OcB7198tnVkC9\\nE/RZJdI7WxkHKmi1F1KvpUM3MezNDzA4jHuJuKaxJYYKfY2XWaGeNiAroO1pN/0x\\nV3EYPGGoN1U1jNtR+MYkMtcETj8AvtFSoA8VG1kVb04VacEiaxVsDvL29iM2rV1u\\nerpi98z3oGGF8LUMbk41kK3tuiI6HmZ/aQ==\\n-----END CERTIFICATE-----\"\n"
            + "}";

    private static final String CA_CERT = "-----BEGIN CERTIFICATE-----\n" +
"MIIDvzCCAqegAwIBAgIJAPc+2nj9MlJyMA0GCSqGSIb3DQEBCwUAMHYxCzAJBgNV\n" +
"BAYTAlVTMQswCQYDVQQIDAJPUjERMA8GA1UEBwwIUG9ydGxhbmQxDjAMBgNVBAoM\n" +
"BUludGVsMQ0wCwYDVQQLDARJb1RHMSgwJgYJKoZIhvcNAQkBFhltb3Jlbm8uYW1i\n" +
"cm9zaW5AaW50ZWwuY29tMB4XDTE2MDkyOTAwNDAyMloXDTI2MDkyNzAwNDAyMlow\n" +
"djELMAkGA1UEBhMCVVMxCzAJBgNVBAgMAk9SMREwDwYDVQQHDAhQb3J0bGFuZDEO\n" +
"MAwGA1UECgwFSW50ZWwxDTALBgNVBAsMBElvVEcxKDAmBgkqhkiG9w0BCQEWGW1v\n" +
"cmVuby5hbWJyb3NpbkBpbnRlbC5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAw\n" +
"ggEKAoIBAQCkFkv8N5DJtfTLcSf7rQmixwfHWrxzpH7+Sb/0mZNxNPTrMEIHfuTu\n" +
"sU947r+svAhbq3Sjts1IMSxaJ231c4rSms2WeAs3aRn2bJ22CVbOPtksEAbXLyix\n" +
"p4PC17FprJbz8i8OXiKYd2AnmBz3kBrmRUEKp9KSuOyOfn/PZe5DxfIu2ATJ18VM\n" +
"658pGYGbBUak9V4UcEHIBsuGPi2+/rcD7I1YJLsuzlHxtJZZVEc42KGw+Pcj9GuF\n" +
"ej6zlZnzjheXQ+JvanbDgvnfdWl3L0suJyl8fyIdEKDIwIu1HLqgbUPaBbyQgnVg\n" +
"aK1gHsbcWsfSbtJn/J5Em7md1N0hmiI7AgMBAAGjUDBOMB0GA1UdDgQWBBQVdOvJ\n" +
"zHGvkzUDyNMw6hf8M46TQDAfBgNVHSMEGDAWgBQVdOvJzHGvkzUDyNMw6hf8M46T\n" +
"QDAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBCwUAA4IBAQA04JPlrSAhd7UA2qAK\n" +
"wAZG+fOM4iCqBonJGYu+11pBIwudl10Ilnf1eyqiSlETS/a+5O+pzOGHSxqQ8Yl+\n" +
"C3gAyXyrGh4lOl4Fcb4GNocuzDIEjtkQAF+k/Oo2RvOJ9UeUd94a5sUo7Lcrg2rK\n" +
"Jhg7YT47drO5LUC6G74ZW00cFtCofRb4Pcsi+soH7XqDRYKFYioN+/vzVdNUXQz4\n" +
"VvzU2sgam+lpIEBdc9+yOz0mr+/RwYuHo8gYeKAXV60ZXeZiPyLYlU0cBQV5nlLg\n" +
"xZ+2/DqWsYzW+Vft1VsN9WZ2NPs3kCgd32iLImP7Aufc6GHNbmFTfwSMzwUkyfsU\n" +
"XNZ9\n" +
"-----END CERTIFICATE-----";
    
    
    private static final URI NON_EXISTING_KEY = URI.create("/non/existing/key");
    private static final URI ROOT_CA_KEY = URI.create("/root/ca/key");
    private static final URI NODE_KEY = URI.create("/com/inte/node/123/key");
    
    
    /** Key manager instance to test */ 
    private static KeyStoreBasedManager km;

    /**
     * Perform initialization tasks, i.e., create trust store and key store, populate
     * it with necessary entries, and create an instance of {@link KeyStoreBasedManager}
     * 
     * @throws Exception If files creation fail
     */
    @BeforeClass
    public static void init() throws Exception {
        
        // Create the configuration file
        try (FileOutputStream os = new FileOutputStream(new File(BASE_DIR + "/keymanager_config.json"))) {
            os.write(KEY_MANAGER_COFIGURATION.getBytes());
        }
        // Configurations
        ConfigurationManager cm = new FileConfigurationManager(Paths.get(BASE_DIR), new FileChannelProvider());
        
        // (1) Create certificate messages
        File messagesDir = new File(CERTIFICATE_MESSAGES_DIR);
        if (!messagesDir.exists()) {
            messagesDir.mkdir();
            LOGGER.info("Folder {} does not exist", CERTIFICATE_MESSAGES_DIR);
            try (FileOutputStream os = new FileOutputStream(CERTIFICATE_MESSAGES_DIR + "/" + CHAIN_TRUSTED_MESSAGE_FILE)) {
                os.write(CHAIN_TRUSTED_MESSAGE.getBytes());
            }
            try (FileOutputStream os = new FileOutputStream(CERTIFICATE_MESSAGES_DIR + "/" + SINGLE_TRUSTED_MESSAGE_FILE)) {
                os.write(SINGLE_TRUSTED_MESSAGE.getBytes());
            }
            try (FileOutputStream os = new FileOutputStream(CERTIFICATE_MESSAGES_DIR + "/" + SINGLE_UNTRUSTED_MESSAGE_FILE)) {
                os.write(SINGLE_UNTRUSTED_MESSAGE.getBytes());
            }
        }
        
        // (2) Create truststore and keystore
        File keystoreDir = new File(KEYSTORE_DIR);
        if (!keystoreDir.exists()) {
            
            LOGGER.info("Folder {} does not exist", KEYSTORE_DIR);
            // Create the directory (and parent directories as well)
            keystoreDir.mkdirs();
            // Cteate a trust store. This operation may fail if JCEKS is not supported
            KeyStore trustStore = KeyStore.getInstance("JCEKS");
            trustStore.load(null, SecurityConstants.getKeyStoreDefaultPassword());
            // Add the CA certificate
            trustStore.setEntry(ROOT_CA_KEY.toASCIIString(), new KeyStore.TrustedCertificateEntry(PemEncodingUtils.decodeX509CertificateChain(CA_CERT.getBytes()).get(0)), null);
            
            // Cteate a key store. This operation may fail if JCEKS is not supported
            KeyStore keystore = KeyStore.getInstance("JCEKS");
            keystore.load(null, SecurityConstants.getKeyStoreDefaultPassword());
            
            // We add a key pair to it. This should not create problems, 
            // even if Java security extension is not installed
            KeyPair kp = KeyProvider.generateKeyPair("RSA", 1024);
            
            // We add a public key, and a private one.
            List certs = PemEncodingUtils.decodeX509CertificateChain(CA_CERT.getBytes());
            Certificate[] certsArray = Arrays.copyOf(certs.toArray(), certs.size(), Certificate[].class);
            keystore.setEntry(NODE_KEY.toASCIIString(), new KeyStore.PrivateKeyEntry(
                    kp.getPrivateKey().getKey(),
                    certsArray), new KeyStore.PasswordProtection(SecurityConstants.getKeyStoreDefaultPassword()));
            
            // Finally, we write both stores to file.
            try (FileOutputStream os = new FileOutputStream(KEYSTORE_DIR + "/keystore.jceks")) {
                keystore.store(os, SecurityConstants.getKeyStoreDefaultPassword());
            }
            try (FileOutputStream os = new FileOutputStream(KEYSTORE_DIR + "/truststore.jceks")) {
                trustStore.store(os, SecurityConstants.getKeyStoreDefaultPassword());
            }
            
        }
        
        // Lookup configuration and pass it to the key manager
        km = new KeyStoreBasedManager(new MockFileOnlyChannels(), cm.get("keymanager_config"));
        // Initialize the key manager (this call may fail due to initialization errors)
        km.load();        
        
        
    }
    
    
    /**
     * UTILITY: Deletes files recursively
     * 
     * @param file file from which to start
     */
    static void deleteFiles(File file) {
        if (file != null && file.exists()) {
            if (file.isDirectory()) {
                for (File f : file.listFiles()) {
                    deleteFiles(f);
                }
            }
            file.delete();
        }        
    }
    
    /**
     * Cleanup; delete the created files
     */
    @AfterClass
    public static void cleanup() {
        if (km != null) {
            km.close();
        }
        // Delete created folders and files
        deleteFiles(new File(KEYSTORE_DIR));
        deleteFiles(new File(CERTIFICATE_MESSAGES_DIR));
        deleteFiles(new File(BASE_DIR + "/keymanager_config.json"));
    }
    
    
    /**
     * Test for {@link KeyStoreBasedManager#getPublicKey(java.net.URI) }
     * with an existing key
     * 
     * @throws Exception 
     */
    @Test
    public void getPublicKeyExistingKeyTest() throws Exception {
        // Should neither throw exception, nor be null
        Assert.assertNotNull(km.getPublicKey(NODE_KEY));
    }
    
    /**
     * Test for {@link KeyStoreBasedManager#addSymmetricKey(java.net.URI, com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey) }
     * and {@link KeyStoreBasedManager#getSymmetricKey(java.net.URI) }
     * 
     * @throws Exception 
     */
    @Test
    public void addAndGetSymmetricKeyTest() throws Exception {
        // Should neither throw exception, nor be null
        km.addSymmetricKey(URI.create("ndn://com/intel/new/symmetric/key"), KeyProvider.generateSymmetricKey(SecurityConstants.AES));
        km.getSymmetricKey(URI.create("ndn://com/intel/new/symmetric/key"));
    
    }
    
    /**
     * Test for {@link KeyStoreBasedManager#getPublicKey(java.net.URI) }
     * with a non-existing key
     * 
     * @throws KeyManagerException 
     */
    @Test(expected = KeyManagerException.class)
    public void getPublicKeyNonExistingKeyTest() throws KeyManagerException {
        // Should throw an exception of type KeyManagerException
        km.getPublicKey(NON_EXISTING_KEY);
    }
    
    
    /**
     * Test for {@link KeyStoreBasedManager#getPrivateKey(java.net.URI) }
     * with an existing key
     * 
     * @throws Exception 
     */
    @Test
    public void getPrivateKeyExistingKeyTest() throws Exception {
        // Should neither throw exception, nor be null
        Assert.assertNotNull(km.getPrivateKey(NODE_KEY));
    }
    
    /**
     * Test for {@link KeyStoreBasedManager#getPrivateKey(java.net.URI) }
     * with a non-existing key
     * 
     * @throws KeyManagerException
     */
    @Test(expected = KeyManagerException.class)
    public void getPrivateKeyNonExistingKeyTest() throws KeyManagerException {
        // Should throw an exception of type KeyManagerException
        km.getPrivateKey(NON_EXISTING_KEY);
    }
    
    /**
     * Test for {@link KeyStoreBasedManager#getCertificate(java.net.URI)  }
     * with correct (i.e., trusted) certificates.
     * 
     * @throws Exception 
     */
    @Test
    public void getCertificateTrustedChainTest() throws Exception {
        Assert.assertNotNull(km.getCertificate(URI.create(CERTIFICATE_MESSAGES_DIR + "/" + CHAIN_TRUSTED_MESSAGE_FILE)));
    }
    
    /**
     * Test for {@link KeyStoreBasedManager#getCertificate(java.net.URI)  }
     * asking for an inexistent certificate. It should throw a {@link KeyManagerException}
     * 
     * N.B.: this works also as a test for {@link KeyStoreBasedManager#getTrustedCertificate(java.lang.String) } 
     * in case the certificate is NOT inside the trust store.
     * 
     * @throws Exception 
     */
    @Test(expected = KeyManagerException.class)
    public void getCertificateInexistTest() throws Exception {
        km.getCertificate(NON_EXISTING_KEY);
    }
    
    
    /**
     * Test for {@link KeyStoreBasedManager#getCertificate(java.net.URI) }
     * asking for an existing certificate, and passing in a null channel.
     * It should terminate normally
     * 
     * N.B.: this works also as a test for {@link KeyStoreBasedManager#getTrustedCertificate(java.lang.String) } 
     * in case the certificate is inside the trust store.
     * 
     * @throws Exception 
     */
    @Test
    public void getCertificateExistingTest() throws Exception {
        Assert.assertNotNull(km.getCertificate(ROOT_CA_KEY));
    }
    
    

    /**
     * Test for {@link KeyStoreBasedManager#getCertificate(java.net.URI) }
     * with untrusted certificate.
     * 
     * @throws KeyManagerException 
     */
    @Test(expected = KeyManagerException.class)
    public void getCertificateUntrustedTest() throws Exception {
        // In this case, we are trying to load and verify a self signed certificate; it should fail
        km.getCertificate(URI.create(CERTIFICATE_MESSAGES_DIR + "/" + SINGLE_UNTRUSTED_MESSAGE_FILE));
    }
}