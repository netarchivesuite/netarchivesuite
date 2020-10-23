package dk.netarkivet.common.utils.warc;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.archive.io.warc.WARCReaderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.Settings;

public class WarcRecordClient {
    /** Logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(WarcRecordClient.class);
    /** The amount of milliseconds in a second. 1000. */
    private static final int MILLISECONDS_PER_SECOND = 1000;

    public URI getBaseUri() {
        return baseUri;
    }

    private URI baseUri;

    /** The length of time to wait for a get reply before giving up. */
    private static final String USER_AGENT = "Mozilla/5.0";
    private static int timeout = MILLISECONDS_PER_SECOND;
    private long offset;
    boolean atFirst = true;

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    private static class Singleton {
        /**
         * Used to make one, and only one instance of closeableHttpClient
         * Constructor is called once
         */
        private static CloseableHttpClient closeableHttpClient;

        private Singleton() {
        }

        public static CloseableHttpClient getCloseableHttpClient() {
            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
            cm.setMaxTotal(Settings.getInt(CommonSettings.MAX_TOTAL_CONNECTIONS));
            cm.setDefaultMaxPerRoute(Settings.getInt(CommonSettings.MAX_CONNECTIONS_PER_ROUTE));
            closeableHttpClient  = HttpClients.custom()
                    .setConnectionManager(cm)
                    .build();
            return closeableHttpClient;
        }

    }

    public WarcRecordClient(URI baseUri) {
        this.baseUri = baseUri;
    }

    /**
     * Uses WarcRecordClient to call ApacheHttpClient
     *
     * @param uri Uniform Resource Identifier including base uri and name of file
     * @param offset offset to fetch specific record from warc file
     *              index must be the same as the offset that ends up in the range header
     * @throws ArgumentNotValid if arcfilename is null or empty, or if toFile is null
     * @throws IOException if reading file fails
     * @throws UnsupportedOperationException is used if method is not implemented
     */
    public BitarchiveRecord getWarc( URI uri, long offset) throws Exception {
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder.setConnectTimeout(timeout);
        requestBuilder.setConnectionRequestTimeout(timeout);
        BitarchiveRecord reply = null;

        String fileName = Paths.get(uri.getPath()).getFileName().toString();
        log.debug("fileName: " + fileName);

        HttpUriRequest request = RequestBuilder.get()
                .setUri(uri)
                .addHeader("User-Agent",USER_AGENT)
                .addHeader("Range", "bytes=" + offset + "-")
                .build();
        log.debug("Executing request " + request.getRequestLine());

        try (CloseableHttpClient closableHttpClient = WarcRecordClient.Singleton.getCloseableHttpClient()) {
            HttpResponse httpResponse = closableHttpClient.execute(request);
            log.debug("httpResponse status: " + httpResponse.getStatusLine().toString());
            if (httpResponse.getStatusLine().getStatusCode() != 200) {
                log.error("Http request error " + httpResponse.getStatusLine().getStatusCode());
                return null;
            }


            HttpEntity entity = httpResponse.getEntity();
            if (entity != null) {
                try {
                    InputStream iStr = entity.getContent();
                    ArchiveReader archiveReader = WARCReaderFactory.get("fake.warc", iStr, atFirst);
                    ArchiveRecord archiveRecord = archiveReader.get();
                    reply = new BitarchiveRecord(archiveRecord, fileName);
                    log.debug("reply: " + reply.toString());

                    return reply;
                } catch (IOException e) {
                    log.error("IOException: ", e );
                } catch (UnsupportedOperationException e) {
                    log.error("UnsupportedOperationException: ", e);
                }
            }
            else {
                log.error("Enity is null: '" );
                throw new Exception("Enity is null:");
            }
        } catch (ClassCastException e) {
            log.error("Received invalid argument reply: '" + reply + "'", e);
            throw new IOFailure("Received invalid argument reply: '" + reply + "'", e);
        }
        return reply;
    }

    /**
     * Retrieves a single BitarchiveRecord from the repository from a given file and offset. If the operation fails for
     * any reason, this method returns null.
     *
     * @param arcfileName Name of the arcfile to retrieve.
     * @param index offset to fetch specific record from warc or arc file
     */
    public BitarchiveRecord get(String arcfileName, long index) {

        BitarchiveRecord warcInstance = null;
        try {
            ArgumentNotValid.checkNotNullOrEmpty(arcfileName, "arcfile");
            ArgumentNotValid.checkNotNegative(index, "index");

            log.debug("Requesting get of record '{}:{}'", arcfileName, index);

            String strUri = this.getBaseUri().toString() + "/" + arcfileName;

            URI uri = new URI(strUri);
            warcInstance = this.getWarc(uri, index);
        } catch (Exception e) {
            log.error("Failed to retrieve record at offset {} from file {}.", index, arcfileName, e);
        }
        return warcInstance;
    }


}
