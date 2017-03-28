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
package com.intel.icecp.node.security.keymanagement.impl;

import com.intel.icecp.core.Channel;
import com.intel.icecp.core.management.Channels;
import com.intel.icecp.core.metadata.Persistence;
import com.intel.icecp.core.misc.ChannelIOException;
import com.intel.icecp.core.misc.ChannelLifetimeException;
import com.intel.icecp.core.misc.Configuration;
import com.intel.icecp.core.permissions.KeyManagementPermissions;
import com.intel.icecp.node.messages.security.CertificateMessage;
import com.intel.icecp.core.security.keymanagement.exception.KeyManagerException;
import com.intel.icecp.core.security.crypto.key.asymmetric.PrivateKey;
import com.intel.icecp.core.security.crypto.key.asymmetric.PublicKey;
import com.intel.icecp.core.security.crypto.key.symmetric.SymmetricKey;
import com.intel.icecp.node.security.SecurityConstants;
import com.intel.icecp.node.security.utils.PemEncodingUtils;
import com.intel.icecp.node.utils.SecurityUtils;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.crypto.SecretKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.intel.icecp.core.security.keymanagement.KeyManager;

/**
 * Implementation of key manager using Java {@link java.security.KeyStore}, which
 * works ONLY with JCEKS format.
 * This implementation uses two separate instances of {@link java.security.KeyStore}
 * <ol>
 * <li> A key store contains all the keys, i.e., both node's keys and channel keys;
 * this store can persist both public/private keys and symmetric keys; </li>
 * <li> trust store	contains the trust anchors X.509 certificates (e.g., administrator
 * and/or CA); node's certificate (if any) goes here as well. </li>
 * </ol>
 * Access control is performed on keys by key ID using {@link SecurityUtils}
 * and the operations specified in {@link KeyManagementPermissions} class
 *  
 * 
 */
public class KeyStoreBasedManager implements KeyManager, AutoCloseable {
    
    /** Constant error messages */
    private static final String ERR_MSG_UNABLE_TO_FETCH_CERTIFICATE = "Unable to retrieve certificate ";
    
    /** Store settings */
    private static final String STORE_TYPE = "JCEKS";
    
    /** Constants and default parameters for properties parsing */
    // @TODO: Move these constants to SecurityConstants
    private static final String TRUSTORE_TAG = "truststore";
    private static final String DEFAULT_TRUSTSTORE = "keystores/stores/truststore.jceks";
    private static final String KEYSTORE_TAG = "keystore";
    private static final String DEFAULT_KEYSTORE = "keystores/stores/keystore.jceks";
    private static final String PASSWORD_TAG = "storespassword";
    
    /** Logger */
    private static final Logger LOGGER = LogManager.getLogger();

    /** Key store and trust store */ 
    // @TODO: Move trust store into a separate TrustManager, to
    // reduce the functions provided by this class
    private KeyStore trustStore = null;
    private KeyStore keyStore = null;

    /** Local location of the key stores (e.g., path in the file system) */
    private final String trustStoreLocation;
    private final String keyStoreLocation;

    /** Key stores password */
    // @TODO: We should (try to) remove the need to keep the password in memory all the time 
    private final char[] password;

    /** handles configuration for the key store */
    private final Configuration configuration;
    
    /** Reference to channels that may be used to fetch certificates remotely */
    private final Channels channels;
    
    /** Tells whether key stores have been initialized/loaded */
    private boolean init = false;    

    public KeyStoreBasedManager(Channels channels, Configuration configuration) {
        this.channels = channels;
        // Set a file configuration manager to look for configuration file in the default path
        this.configuration = configuration;
        try {
            // Load the configuration
            this.configuration.load();
        } catch(NullPointerException | ChannelIOException ex) {
            // If fails, we use the default parameters
            LOGGER.warn("Unable to read key manager configuration from file.", ex);
        }
        // Load parameters
        this.trustStoreLocation = this.configuration.getOrDefault(DEFAULT_TRUSTSTORE, TRUSTORE_TAG);
        this.keyStoreLocation = this.configuration.getOrDefault(DEFAULT_KEYSTORE, KEYSTORE_TAG);
        this.password = this.configuration.getOrDefault(SecurityConstants.getKeyStoreDefaultPassword(), PASSWORD_TAG);
    }
    
    
    /**
     * One-time initialization of the key manager
     * 
     * @throws KeyManagerException In case of initialization error
     */
    public synchronized void load() throws KeyManagerException {
        // It has not been initialized yet
        if (!init) {
            try {
                keyStore = KeyStore.getInstance(STORE_TYPE);
                trustStore = KeyStore.getInstance(STORE_TYPE);
            } catch (KeyStoreException ex) {
                throw new KeyManagerException("Error loading keystore or truststore.", ex);
            }
            // Initalize both key stores
            init(keyStore, this.password, Paths.get(keyStoreLocation), true);
            // This call may fail if we find no trust store file
            init(trustStore, this.password, Paths.get(trustStoreLocation), false);
            init = true;
        }
    }

    /**
     * Perform teardown operations
     * 
     * {@inheritDoc }
     * 
     */
    @Override
    public void close() {
        // Erase the content of password
        Arrays.fill(password, '0');
        init = false;
    }

    /**
     * Utility method that fetches a key from a given {@link java.security.KeyStore}, 
     * and returns it in the following cases:
     * <ol>
     *  <li> The entry exists </li>
     *  <li> The entry is a key entry </li>
     *  <li> The key is of (or assignable from) the given {@link java.security.Key} type </li>
     * </ol>
     * 
     * @param <T>
     * @param keyAlias Alias under which the key is stored inside the {@link java.security.KeyStore}
     * @param keyType Expected key type, as a subclass of {@link java.security.Key}
     * @param keyStore Key store
     * @return The key if the above requirements are met
     * @throws KeyManagerException In case of error
     */
    protected <T extends Key> T getKey(String keyAlias, Class<T> keyType, KeyStore keyStore) throws KeyManagerException {
        // The key manager must have been initialized
        assert init;
        // Check READ permission for the given key
        SecurityUtils.checkPermission(new KeyManagementPermissions(keyAlias, KeyManagementPermissions.READ));
        try {
            // First, we check whether an entry exists; if not, we throw an exception
            if (!keyStore.containsAlias(keyAlias)) {
                throw new KeyManagerException("Unable to retrieve the given entry " + keyAlias + ": key not found");
            }
            // Then, we check if the entry is a key; if not, we throw an exception
            if(!keyStore.isKeyEntry(keyAlias)) {
                throw new KeyManagerException("Unable to retrieve the given entry " + keyAlias + ": is not a key");
            }
            Key key = keyStore.getKey(keyAlias, password);
            // Check whether the type corresponds; otherwise we simply throw an exception
            if (keyType.isAssignableFrom(key.getClass())) {
                // This cast is safe
                return (T) key;
            }
            throw new KeyManagerException("Unable to retrieve symmetric key " + keyAlias + ": key does not have the expected type");
        } catch (NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException ex) {
            throw new KeyManagerException("Unable to retrieve symmetric key " + keyAlias + ".", ex);
        }
        
    }
    
    /**
     * {@inheritDoc}
     *
     */
    @Override
    public PrivateKey getPrivateKey(URI keyAlias) throws KeyManagerException {
        return new PrivateKey(getKey(keyAlias.toASCIIString(), java.security.PrivateKey.class, keyStore));
    }
    
    /**
     * {@inheritDoc}
     *
     */
    @Override
    public SymmetricKey getSymmetricKey(URI keyAlias) throws KeyManagerException {
        return new SymmetricKey(getKey(keyAlias.toASCIIString(), SecretKey.class, keyStore));
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public PublicKey getPublicKey(URI keyAlias) throws KeyManagerException {
        // The key manager must have been initialized
        assert init;
        // Check READ permissions for the key
        String keyAliasString = keyAlias.toASCIIString();
        SecurityUtils.checkPermission(new KeyManagementPermissions(keyAliasString, KeyManagementPermissions.READ));
        try {
            if (!keyStore.containsAlias(keyAliasString)) {
                throw new KeyManagerException("Unable to retrieve public key " + keyAliasString + ": certificate not found");
            }
            // Unless we have some KeyStore related error, we are sure to have the public key
            return new PublicKey(keyStore.getCertificate(keyAliasString).getPublicKey());
        } catch (KeyStoreException e) {
            throw new KeyManagerException("Unable to retrieve public key " + keyAliasString, e);
        }

    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public synchronized void addSymmetricKey(URI keyAlias, SymmetricKey k) throws KeyManagerException {
        // The key manager MUST have been initialized
        assert init;
        String keyAliasString = keyAlias.toASCIIString();
        try {
            // Check CREATE or UPDATE permissions
            if (keyStore.containsAlias(keyAliasString)) {
                SecurityUtils.checkPermission(new KeyManagementPermissions(keyAliasString, KeyManagementPermissions.UPDATE));
            } else {
                SecurityUtils.checkPermission(new KeyManagementPermissions(keyAliasString, KeyManagementPermissions.CREATE));
            }
            // Set the entry
            keyStore.setKeyEntry(keyAliasString, k.getWrappedKey(), password, null);
            // Save changes to file
            this.write(keyStore, keyStoreLocation);
        } catch (KeyStoreException ex) {
            throw new KeyManagerException("Unable to add symmetric key " + keyAliasString, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public void deleteSymmetricKey(URI keyAlias) throws KeyManagerException {
        // The key manager must have been initialized
        assert init;
        String keyAliasString = keyAlias.toASCIIString();
        SecurityUtils.checkPermission(new KeyManagementPermissions(keyAliasString, KeyManagementPermissions.DELETE));
        try {
            // Thread safe operation
            deleteStoreEntry(keyStore, keyStoreLocation, keyAliasString);
        } catch (KeyStoreException ex) {
            throw new KeyManagerException("Unable to delete key " + keyAliasString + ".", ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public Certificate getCertificate(URI certificateID) throws KeyManagerException {
        // The key manager must have been initialized
        assert init;
        // Try first to load it from the keystore
        try {
            return getTrustedCertificate(certificateID.toASCIIString());
        } catch (KeyManagerException ex) {
            // Do nothing
            LOGGER.info("Unable to find the certificate with name '{}' inside trust store", certificateID, ex);
        }
        
        // Try to retrieve the certificate form a channel, using certificate's name
        CertificateMessage msg;
        try (Channel<? extends CertificateMessage> channel = channels.openChannel(certificateID, CertificateMessage.class, Persistence.FOREVER)) {
            msg = channel.latest().get();
        } catch (ChannelIOException | ChannelLifetimeException | ExecutionException | InterruptedException ex) {
            throw new KeyManagerException(ERR_MSG_UNABLE_TO_FETCH_CERTIFICATE + certificateID.toASCIIString() + " from a channel.", ex);
        }

        // Decode the certificates (assume X.509); if not X.509, returns an empty list
        List<Certificate> certs = PemEncodingUtils.decodeX509CertificateChain(msg.certificate.getBytes());
        // This call may fail (if the certificate can not be verified);
        // if the chain can be verified, we return the first in the chain (as the list
        // returned by the verify method is sorted)
        return verify(certs).get(0);
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public Certificate verifyCertificateChain(byte[] certificate) throws KeyManagerException {
        // The key manager must have been initialized
        assert init;
        // Simply call verify and return the first certificate in the returned
        // (sorted) certificate path.
        List<Certificate> certs = PemEncodingUtils.decodeX509CertificateChain(certificate);
        return verify(certs).get(0);
    }
    
    
    
    
    
    
    
    /** PROTECTED/PRIVATE METHODS */
    
    
    
    
    
    
    /**
     * Reads (and loads if necessary) a given {@link KeyStore}.
     *
     * @param store Reference to the key store to initialize
     * @param password Password to open the key store
     * @param path Path in the file system
     * @param acceptEmpty Tells whether is acceptable to load an empty key store
     * @throws KeyManagerException If the given store name is not valid or if 
     * store loading or creation fails
     */
    protected final void init(KeyStore store, char[] password, Path path, boolean acceptEmpty) throws KeyManagerException {
        // Cannot be null
        if (store == null) {
            throw new KeyManagerException("Invalid null key store " + store + ".");
        }
        // "Synchronize" on the store before proceding
        synchronized (store) {
            // Try to open the key store at the given path
            try (InputStream keystoreFileInputStream = new FileInputStream(path.toFile())) {
                // If the given inputStream is null, this call creates a new empty KeyStore
                store.load(keystoreFileInputStream, password);
            } catch (FileNotFoundException e) {
                if(acceptEmpty) {
                    // No file found, but empty store allowed.
                    // In this case we can still proceed trying to 
                    // create a new empty keystore
                    LOGGER.warn("Error reading from path {}.", path.toString(), e);
                    try {
                        store.load(null, password);
                    } catch (IOException | NoSuchAlgorithmException | CertificateException ex) {
                        // Something went wrong 
                        throw new KeyManagerException("Unable to load empty keystore " + store, ex);
                    }
                } else {
                    // Empty key store not allowed; throw an exception
                    throw new KeyManagerException("Unable to load keystore " + store + " from path " + path.toString(), e);
                }
            } catch (CertificateException | IOException | NoSuchAlgorithmException ex) {
                // Something went wrong
                throw new KeyManagerException("Unable to load keystore " + store + " from path " + path.toString(), ex);
            }
            
        }
    }

    /**
     * Writes on a given {@link KeyStore}; to use if we modified the key store and want
     * to change its state.
     * 
     * Note that, this method does NOT perform any permission check, which should be 
     * enforced by the caller
     *
     * @param store Key store
     * @param keyStoreLocation Key store location
     * @throws KeyManagerException If the given store name is not valid or if writing in the 
     * key store fails
     */
    protected void write(KeyStore store, String keyStoreLocation) throws KeyManagerException {
        // Cannot be null
        if (store == null) {
            throw new KeyManagerException("Invalid key store " + store + ": null");
        }
        try {
            // Write the keystore to file (take a lock in the key store to prevent 
            // inconsistencies while writing to file)
            synchronized(store) {
                try (FileOutputStream fos = new FileOutputStream(keyStoreLocation)) {
                    store.store(fos, password);
                }
            }
        } catch (CertificateException | IOException | KeyStoreException | NoSuchAlgorithmException ex) {
            throw new KeyManagerException("Unable to save the changes to " + keyStoreLocation  + "", ex);
        }
    }

    /**
     * Utility method that removes an entry from a given store, and saves the
     * result on file. NOTE: This method DOES NOT check for permissions; the
     * caller should enforce access control
     *
     * @param keyStore Key store
     * @param entryId Entry identifier
     * @throws KeyStoreException If call to {@link KeyStore#deleteEntry(java.lang.String) } fails
     * @throws KeyManagerException If call to {@link KeyStoreBasedManager#write(java.security.KeyStore, java.lang.String) } fails
     */
    private void deleteStoreEntry(KeyStore keyStore, String keyStoreLocation, String entryId) throws KeyStoreException, KeyManagerException {
        synchronized (keyStore) {
            // Remove the entry
            keyStore.deleteEntry(entryId);
            // Save changes (if we are allowed to do so)
            this.write(keyStore, keyStoreLocation);
        }
    }

    /**
     * Tells whether a given {@link java.security.cert.Certificate} is self signed.
     *
     * @param certificate The certificate to check
     * @return {@code true} if the certificate is self signed; {@code false} otherwise
     */
    protected boolean isSelfSigned(Certificate certificate) {
        try {
            certificate.verify(certificate.getPublicKey());
            return true;
        } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | NoSuchProviderException | SignatureException ex) {
            // If the call fails, it means that either the certificate is
            // not self-signed, or that there are other issues with the certificate. 
            // In both cases, the method will return false.
            LOGGER.warn("Self-signed test failed (Certificate is not self-signed).", ex);
            return false;
        }
    }

    
    /**
     * Utility method that verifies the validity of a certificate; this method
     * checks whether the certificate is a subtype of {@link X509Certificate}
     * and only in this case, tries to verify the validity of the certificate
     * 
     * @param certificate The certificate to verify
     * @throws UnsupportedEncodingException If the certificate format is not supported
     * @throws CertificateExpiredException If the certificate is expired
     * @throws CertificateNotYetValidException If the certificate is not yet valid
     */
    protected void checkCertificateValidity(Certificate certificate) throws UnsupportedEncodingException, CertificateExpiredException, CertificateNotYetValidException {
        // Check if certificate is a X509Certificate, and only in this case, 
        // check its validity
        if (X509Certificate.class.isAssignableFrom(certificate.getClass())) {
            // Check validity of this certificate (cast is safe)
            ((X509Certificate) certificate).checkValidity();
        } else {
            throw new UnsupportedEncodingException("Unknown certificate format"); 
        }
    }
    
    
    
    /**
     * Returns a certificate from the trust store ONLY if it is valid; throws an
     * exception otherwise.
     * If invalid, the method tries to remove it from the trust store.
     *
     * @param certificateID Certificate unique identifier
     * @return The certificate corresponding to the given id
     * @throws KeyManagerException If certificate not found inside the trust store, or
     * the certificate is expired/not yet valid
     */
    protected Certificate getTrustedCertificate(String certificateID) throws KeyManagerException {
        Certificate certificate;
        try {
            // Inside trust store we have only certificaes; therefore this test is sufficient
            if (!trustStore.containsAlias(certificateID)) {
                // Failure (a): certificate not in trust store
                throw new KeyManagerException(ERR_MSG_UNABLE_TO_FETCH_CERTIFICATE + certificateID + ": not found in trust store");
            }
            // Fetches a cert. chain from the trust store;
            // Note that this implies that the first element, in case of 
            // certificate chain, is given.
            // Source: (https://docs.oracle.com/javase/8/docs/api/java/security/KeyStore.html#getCertificate(java.lang.String))
            certificate = trustStore.getCertificate(certificateID);
            
            // Check the validity of the certificate
            checkCertificateValidity(certificate);
            
            // If passed the check, we can return it
            return certificate;
            
        } catch (CertificateExpiredException ex) {
            // We remove the certificate since expired
            try {
                // Thread safe operation
                this.deleteStoreEntry(trustStore, trustStoreLocation, certificateID);
            } catch (KeyStoreException e) {
                // Do nothing
                LOGGER.warn(e);
            }
            // Failure (b): Certificate is expired
            throw new KeyManagerException(ERR_MSG_UNABLE_TO_FETCH_CERTIFICATE + certificateID, ex);
        } catch (UnsupportedEncodingException | KeyStoreException | CertificateNotYetValidException ex) {
            // Failure (c): Certificate is not yet valid or not a X509, or another key store failure occurred
            throw new KeyManagerException(ERR_MSG_UNABLE_TO_FETCH_CERTIFICATE + certificateID, ex);
        }
    }

    /**
     * Given a (possibly unsorted) certificate chain returns the corresponding ordered
     * certificates list if the chain can be verified against the trust anchors; 
     * throws an exception otherwise.
     * Returned certificates are sorted according to:
     * {@literal https://docs.oracle.com/javase/8/docs/api/java/security/cert/CertPath.html}
     *
     * @param certificateChain Certificate chain as a {@link List} of {@link Certificate}
     * @return An ordered certificate chain 
     * @throws KeyManagerException	If the certificate chain is not verified
     */
    protected List<Certificate> verify(List<Certificate> certificateChain) throws KeyManagerException {
        
        // Check if we have no certs, or if the cert is self signed, to avoid useless computation
        if (certificateChain.isEmpty()) {
            throw new KeyManagerException("Empty certificate chain supplied.");
        } else if (this.isSelfSigned(certificateChain.get(0))) {
            // In this case, the first certificate of the chain is self signed, and
            // therefore there is no chain; furthermore, the certificate is untrusted 
            // (otherwise we would have found it into the trusted store). 
            throw new KeyManagerException("Certificate is self signed but untrusted.");
        }
        
        LOGGER.info("Certificate is not self signed; continue verifying the certificate chain.");

        // We can now verify the validity of the certificate chain
        try {
            // Validator used for verification of the chain
            // Every Java implementation is required to support PKIX 
            // Source: https://docs.oracle.com/javase/8/docs/api/java/security/cert/CertPathValidator.html
            CertPathValidator validator = CertPathValidator.getInstance("PKIX");
            
            // The chain may have 1 or more certificates; this creates a path 
            // to follow when verifing the certificate chain validity
            // Every Java implementation is required to support "X.509"
            // Source: https://docs.oracle.com/javase/8/docs/api/java/security/cert/CertificateFactory.html
            CertPath path = CertificateFactory.getInstance("X.509").generateCertPath(certificateChain);
            
            // Set Selector for the certificate to check (in our case, the first of the chain)
            X509CertSelector selector = new X509CertSelector();
            selector.setCertificate((X509Certificate) path.getCertificates().get(0));

            // We tell the verification process to load trusted certificates from trustedKeyStore. 
            // The chain should terminate with, or contain a certifiacte in this store.
            PKIXBuilderParameters params = new PKIXBuilderParameters(trustStore, selector);
            
            // @TODO: CRL or OCSP not yet supported
            params.setRevocationEnabled(false);
            
            // We let the validator check the validity of the chain (throws an exception if invalid)
            validator.validate(path, params);
            
            LOGGER.info("Certificate chain is valid.");
            // Return the sorted certificate chain

            // (convert to non generic-wildcard type)
            List<Certificate> result = new ArrayList<>();
            result.addAll(path.getCertificates());

            return result;

        } catch (CertPathValidatorException | CertificateException | InvalidAlgorithmParameterException | NoSuchAlgorithmException | KeyStoreException ex) {
            throw new KeyManagerException("Unable to verify key chain.", ex);
        }
    }

}