package dk.netarkivet.common.utils;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import dk.netarkivet.common.CommonSettings;

/** Class for providing configured HTTPS clients to execute requests over SSL. */
public class HttpsClientBuilder {
    private final HttpClientBuilder clientBuilder;
    private final BasicTwoWaySSLProvider sslProvider;

    /**
     * Constructor that sets up the whole SSL connection when called.
     * Simply use {@link #getHttpsClient()} to get a configured client.
     *
     * @param privateKeyFile The path to the private key file to use for authentication.
     */
    public HttpsClientBuilder(String privateKeyFile) {
        clientBuilder = HttpClients.custom().setRetryHandler(new DefaultHttpRequestRetryHandler(3, true));
        sslProvider = new BasicTwoWaySSLProvider(privateKeyFile);

        setupConnection();
    }

    /**
     * Sets up the SSL socket and the connection manager and configures the client builder to use them.
     */
    private void setupConnection() {
        SSLConnectionSocketFactory sslsf =
                new SSLConnectionSocketFactory(sslProvider.getSSLContext(), new DefaultHostnameVerifier());
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
                .register("https", sslsf) //register http also?
                .build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        configureMaxConnections(cm);
        clientBuilder.setConnectionManager(cm);
    }

    /**
     * Configure the connection managers max connections from settings.
     * @param cm Connection manager to configure.
     */
    private void configureMaxConnections(PoolingHttpClientConnectionManager cm) {
        cm.setMaxTotal(Settings.getInt(CommonSettings.MAX_TOTAL_CONNECTIONS));
        cm.setDefaultMaxPerRoute(Settings.getInt(CommonSettings.MAX_CONNECTIONS_PER_ROUTE));
    }

    /**
     * Build and deliver the client.
     * @return An HTTPS client to carry out requests over SSL.
     */
    public CloseableHttpClient getHttpsClient() {
        return clientBuilder.build();
    }
}
