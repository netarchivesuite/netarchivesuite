/*
 * #%L
 * Netarchivesuite - common
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.common.distribute;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.security.SslSocketConnector;
import org.apache.commons.io.IOUtils;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.Settings;

/**
 * This is a registry for HTTPS remote file, meant for serving registered files to remote hosts. It will use secure
 * communication using a shared certificate. The embedded webserver handling remote files for HTTPSRemoteFile
 * point-to-point communication. Optimised to use direct transfer on local machine.
 */
public class HTTPSRemoteFileRegistry extends HTTPRemoteFileRegistry {

    // Constants defining default X509 algorithm for security framework.
    private static final String SUN_JCEKS_KEYSTORE_TYPE = "JCEKS";
    private static final String SUN_X509_CERTIFICATE_ALGORITHM = "SunX509";
    private static final String SSL_PROTOCOL = "SSL";
    private static final String SHA1_PRNG_RANDOM_ALGORITHM = "SHA1PRNG";
    /** Protocol used in this registry. */
    private static final String PROTOCOL = "https";

    /** A hostname verifier accepting any host. */
    private static final HostnameVerifier ACCEPTING_HOSTNAME_VERIFIER = new HostnameVerifier() {
        public boolean verify(String string, SSLSession sslSession) {
            return true;
        }
    };

    /** The path to the keystore containing the certificate used for SSL in HTTPS connections. */
    private static final String KEYSTORE_PATH = Settings.get(HTTPSRemoteFile.HTTPSREMOTEFILE_KEYSTORE_FILE);
    /** The keystore password. */
    private static final String KEYSTORE_PASSWORD = Settings.get(HTTPSRemoteFile.HTTPSREMOTEFILE_KEYSTORE_PASSWORD);
    /** The certificate password. */
    private static final String KEY_PASSWORD = Settings.get(HTTPSRemoteFile.HTTPSREMOTEFILE_KEY_PASSWORD);

    /**
     * An SSL context, used for creating SSL connections only accepting this certificate.
     */
    private final SSLContext sslContext;

    // This all initialises the ssl context to use the key in the keystore above.
    private HTTPSRemoteFileRegistry() {
        FileInputStream keyStoreInputStream = null;
        try {
            keyStoreInputStream = new FileInputStream(KEYSTORE_PATH);
            KeyStore store = KeyStore.getInstance(SUN_JCEKS_KEYSTORE_TYPE);
            store.load(keyStoreInputStream, KEYSTORE_PASSWORD.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(SUN_X509_CERTIFICATE_ALGORITHM);
            kmf.init(store, KEY_PASSWORD.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(SUN_X509_CERTIFICATE_ALGORITHM);
            tmf.init(store);
            sslContext = SSLContext.getInstance(SSL_PROTOCOL);
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(),
                    SecureRandom.getInstance(SHA1_PRNG_RANDOM_ALGORITHM));
        } catch (GeneralSecurityException | IOException e) {
            throw new IOFailure("Unable to create secure environment for keystore '" + KEYSTORE_PATH + "'", e);
        } finally {
            IOUtils.closeQuietly(keyStoreInputStream);
        }
    }

    /**
     * Get the unique instance.
     *
     * @return The unique instance.
     */
    public static synchronized HTTPRemoteFileRegistry getInstance() {
        synchronized (HTTPRemoteFile.class) {
            if (instance == null) {
                instance = new HTTPSRemoteFileRegistry();
            }
            return instance;
        }
    }

    /**
     * Get the protocol used for this registry, that is 'https'.
     *
     * @return "https", the protocol.
     */
    @Override
    protected String getProtocol() {
        return PROTOCOL;
    }
    
    /**
     * Start the server, including a handler that responds with registered files, removes registered files on request,
     * and gives 404 otherwise. Connection to this web host only possible with the shared certificate.
     */
    protected void startServer() {
        server = new Server();
        //This sets up a secure connector
        SslSocketConnector connector = new SslSocketConnector();
        connector.setKeystore(KEYSTORE_PATH);
        connector.setPassword(KEYSTORE_PASSWORD);
        connector.setKeyPassword(KEY_PASSWORD);
        connector.setTruststore(KEYSTORE_PATH);
        connector.setTrustPassword(KEYSTORE_PASSWORD);
        connector.setNeedClientAuth(true);
        connector.setPort(port);
        //This initialises the server.
        server.addConnector(connector);
        server.addHandler(new HTTPRemoteFileRegistryHandler());
        try {
            server.start();
        } catch (Exception e) {
            throw new IOFailure("Cannot start HTTPSRemoteFile registry", e);
        }
    }

    /**
     * Open a connection to an URL in this registry. Thus opens SSL connections using the certificate above.
     *
     * @param url The URL to open connection to.
     * @return an open connection to the given url
     * @throws IOException If unable to open connection to the URL
     * @throws IOFailure If the connection is not a secure connection
     */
    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        if (!(connection instanceof HttpsURLConnection)) {
            throw new IOFailure("Not a secure URL to remote file: " + url);
        }
        HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
        httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
        httpsConnection.setHostnameVerifier(ACCEPTING_HOSTNAME_VERIFIER);
        return httpsConnection;
    }

}
