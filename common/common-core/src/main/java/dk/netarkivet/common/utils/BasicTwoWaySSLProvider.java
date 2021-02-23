package dk.netarkivet.common.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.bitrepository.protocol.security.SecurityModuleConstants;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;

/**
 * Class for loading certificates and keys from a key- and truststore
 * and configuring an Apache HTTP Registry to use these.
 *
 * To enable SSL for an HTTPClient using this class follow the below steps:
 * - Configure {@link CommonSettings#ACCESS_TRUSTSTORE_PATH} to point at a Java KeyStore file containing your trusted
 *   certificates (e.g. the standard truststore provided by Java located by default at /etc/ssl/certs/java/cacerts).
 * - Configure {@link CommonSettings#TRUSTSTORE_PASSWORD} with the truststore password (default truststore pw is 'changeit').
 * - Instantiate this class with a provided keyfile to use for authentication.
 * - Configure the HTTPClient's connection socket to use the SSLContext provided through {@link #getSSLContext()}.
 */
public class BasicTwoWaySSLProvider {
    private static final Logger log = LoggerFactory.getLogger(BasicTwoWaySSLProvider.class);
    private final String defaultPassword = "123456";
    private SSLContext sslContext;
    private KeyStore keyStore;

    /**
     * Constructor that initializes the SSLContext for use.
     * - Creates keystore object from truststore
     * - Loads private key and certificate
     * - Sets up SSLContext
     *
     * @param privateKeyFile The path to the private key file to use for authentication. Expects file in PEM format.
     */
    public BasicTwoWaySSLProvider(String privateKeyFile) {
        Security.addProvider(new BouncyCastleProvider());
        try {
            keyStore = loadSystemTrustStore();
            loadPrivateKey(privateKeyFile);
            buildSSLContext();
        } catch (Exception e) {
            log.error("Failed setting up SSL.", e);
        }
    }

    /**
     * Load the truststore specified by the common settings.
     * @return KeyStore object representing the truststore provided by the settings.
     * @throws KeyStoreException If the security provider somehow can't load the default JKS-keystore.
     * @throws IOException If the truststore file cannot be found or read.
     * @throws NoSuchAlgorithmException If the truststore can't be loaded with the used algorithm (should never happen with default).
     * @throws CertificateException If any of the certificates can't be loaded from the truststore.
     */
    private KeyStore loadSystemTrustStore() throws KeyStoreException, IOException, NoSuchAlgorithmException,
            CertificateException {
        KeyStore store = null;
        String defaultTrustStoreLocation = Settings.get(CommonSettings.ACCESS_TRUSTSTORE_PATH);
        if (defaultTrustStoreLocation != null) {
            File defaultTrustStore = new File(defaultTrustStoreLocation);
            if (defaultTrustStore.isFile() && defaultTrustStore.canRead()) {
                store = KeyStore.getInstance(KeyStore.getDefaultType());
                String trustStorePassword = Settings.get(CommonSettings.TRUSTSTORE_PASSWORD);
                try (FileInputStream fis = new FileInputStream(defaultTrustStore)) {
                    store.load(fis, trustStorePassword.toCharArray());
                }
            }
        }

        return store;
    }

    /**
     * Attempts to load a private key and certificate from a PEM formatted file.
     * @param privateKeyFile path to the file containing the components private key and certificate, may be null.
     * @throws IOException if the file cannot be found or read.
     * @throws KeyStoreException if there is problems with adding the privateKeyEntry to keyStore.
     * @throws CertificateException if a read certificate can't be loaded correctly.
     */
    private void loadPrivateKey(String privateKeyFile) throws IOException, KeyStoreException, CertificateException {
        PrivateKey privKey = null;
        X509Certificate privCert = null;
        if (privateKeyFile == null || !(new File(privateKeyFile)).isFile()) {
            log.info("Key file '" + privateKeyFile + "' with private key and certificate does not exist!");
            return;
        }
        BufferedReader bufferedReader = new BufferedReader(new FileReader(privateKeyFile));
        PEMParser pemParser = new PEMParser(bufferedReader);
        Object pemObj = pemParser.readObject();

        while (pemObj != null) {
            if (pemObj instanceof X509Certificate) {
                log.debug("Certificate for PrivateKeyEntry found");
                privCert = (X509Certificate) pemObj;
            } else if (pemObj instanceof PrivateKey) {
                log.debug("Key for PrivateKeyEntry found");
                privKey = (PrivateKey) pemObj;
            } else if (pemObj instanceof X509CertificateHolder) {
                log.debug("X509CertificateHolder found");
                privCert = new JcaX509CertificateConverter().setProvider("BC")
                        .getCertificate((X509CertificateHolder) pemObj);
            } else if (pemObj instanceof PrivateKeyInfo) {
                log.debug("PrivateKeyInfo found");
                PrivateKeyInfo pki = (PrivateKeyInfo) pemObj;
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
                privKey = converter.getPrivateKey(pki);
            } else {
                log.debug("Got something, that we don't (yet) recognize. Class: " + pemObj.getClass().getSimpleName());
            }
            pemObj = pemParser.readObject();
        }

        pemParser.close();
        if (privKey == null || privCert == null ) {
            log.info("No material to create private key entry found!");
        } else {
            privCert.checkValidity();
            KeyStore.PrivateKeyEntry privateKeyEntry = new KeyStore.PrivateKeyEntry(privKey, new Certificate[] {privCert});
            keyStore.setEntry(SecurityModuleConstants.privateKeyAlias, privateKeyEntry,
                    new KeyStore.PasswordProtection(defaultPassword.toCharArray()));
        }
    }

    /**
     * Builds an SSLContext from the combined private key and truststore.
     * @throws NoSuchAlgorithmException If the keystore can't be loaded with the used algorithm (should never happen with default).
     * @throws KeyStoreException If there are problems with loading the keystore object.
     * @throws UnrecoverableKeyException If a key in the keystore can't be recovered.
     * @throws KeyManagementException If there is anything wrong with the loaded key - e.g. it's expired.
     */
    private void buildSSLContext()
            throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, KeyManagementException {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(SecurityModuleConstants.keyTrustStoreAlgorithm);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(SecurityModuleConstants.keyTrustStoreAlgorithm);
        tmf.init(keyStore);
        kmf.init(keyStore, defaultPassword.toCharArray());
        sslContext = SSLContext.getInstance(SecurityModuleConstants.defaultSSLProtocol);
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), SecurityModuleConstants.defaultRandom);
    }

    public SSLContext getSSLContext() {
        return sslContext;
    }
}
