package dk.netarkivet.common.utils.service;

import java.net.URI;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.bitrepository.BitmagUtils;
import dk.netarkivet.common.utils.Settings;

/** Abstraction layer above the Apache RequestBuilder to use for building requests to interact with cgi-services */
public class CGIRequestBuilder {
    private static final String USER_AGENT = "Mozilla/5.0";
    private static final int MILLISECONDS_PER_SECOND = 1000;
    private static final int timeout = MILLISECONDS_PER_SECOND;
    private final URI uri;
    private RequestBuilder requestBuilder;

    /**
     * Constructor.
     * @param uri The full URI with a path to a specific service's cgi-script and its parameters/queries.
     */
    public CGIRequestBuilder(URI uri) {
        this.uri = uri;
        setUpDefaultRequestBuilder();
    }

    /**
     * Initializes the request builder and sets the default configurations that every request will have.
     */
    private void setUpDefaultRequestBuilder() {
        String collectionId = Settings.get(BitmagUtils.BITREPOSITORY_COLLECTIONID);
        if (collectionId == null || "".equals(collectionId)) {
            collectionId = Settings.get(CommonSettings.ENVIRONMENT_NAME);
        }
        requestBuilder = RequestBuilder.get()
                .setUri(uri)
                .addHeader("User-Agent", USER_AGENT)
                .addParameter("collectionId", collectionId);
    }

    /**
     * Adds the configurations to communicate with the warc record service (WRS) to the request builder
     * and builds the request.
     * @param offset The offset in the warc record to read from.
     * @return A request to execute against WRS.
     */
    public HttpUriRequest buildWRSRequest(long offset) {
        setWRSTimeoutConfigurations();
        requestBuilder.addHeader("Range", "bytes=" + offset + "-");
        return buildRequest(uri);
    }

    /**
     * Adds the configurations to communicate with the file resolver service to the request builder
     * and builds the request.
     * @param exactFilename Boolean specifying whether to resolve the path to an exact filename or default to regex.
     * @return A request to execute against the file resolver.
     */
    public HttpUriRequest buildFileResolverRequest(boolean exactFilename) {
        requestBuilder.addParameter("exactfilename", Boolean.toString(exactFilename));
        return buildRequest(uri);
    }

    /**
     * Builds the request with the specified uri.
     * @param uri URI of the service to interact with.
     * @return A request to execute.
     */
    public HttpUriRequest buildRequest(URI uri) {
        return requestBuilder.setUri(uri).build();
    }

    /**
     * Configures the request builder to set a connection timeout on the request.
     */
    private void setWRSTimeoutConfigurations() {
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
        requestConfigBuilder.setConnectTimeout(timeout);
        requestConfigBuilder.setConnectionRequestTimeout(timeout);
        RequestConfig requestConfig = requestConfigBuilder.build();
        requestBuilder.setConfig(requestConfig);
    }
}
