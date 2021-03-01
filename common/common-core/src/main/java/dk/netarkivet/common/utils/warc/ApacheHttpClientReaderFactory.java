package dk.netarkivet.common.utils.warc;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;

public class ApacheHttpClientReaderFactory {

    private final static int STREAM_ALL = -1;
    private int connectTimeout = 10000;
    private int readTimeout = 10000;
    private int timeoutSeconds = 10000;

    int CONNECTION_TIMEOUT_MS = timeoutSeconds * 1000; // Timeout in millis.
    RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
            .setConnectTimeout(CONNECTION_TIMEOUT_MS)
            .setSocketTimeout(CONNECTION_TIMEOUT_MS)
            .build();



}
