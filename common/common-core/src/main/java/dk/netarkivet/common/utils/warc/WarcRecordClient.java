package dk.netarkivet.common.utils.warc;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.Settings;

import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveReaderFactory;
import org.archive.io.ArchiveRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WarcRecordClient {
    /**
     * Logger for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(WarcRecordClient.class);
    /**
     * The amount of milliseconds in a second. 1000.
     */
    private static final int MILLISECONDS_PER_SECOND = 1000;

    public URI getBaseUri() {
        return baseUri;
    }

    private URI baseUri;

    /**
     * The length of time to wait for a get reply before giving up.
     */
    private static final String USER_AGENT = "Mozilla/5.0";
    private static int timeout = MILLISECONDS_PER_SECOND;
    private long offset;

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    private static PoolingHttpClientConnectionManager cm;

    static {
        cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(Settings.getInt(CommonSettings.MAX_TOTAL_CONNECTIONS));
        cm.setDefaultMaxPerRoute(Settings.getInt(CommonSettings.MAX_CONNECTIONS_PER_ROUTE));
    }


    public WarcRecordClient(URI baseUri) {
        this.baseUri = baseUri;
    }

    /**
     * Uses WarcRecordClient to call ApacheHttpClient
     *
     * @param uri    Uniform Resource Identifier including base uri and name of file
     * @param offset offset to fetch specific record from warc file index must be the same as the offset that ends up in
     *               the range header
     * @throws ArgumentNotValid              if arcfilename is null or empty, or if toFile is null
     * @throws IOException                   if reading file fails
     * @throws UnsupportedOperationException is used if method is not implemented
     */
    private BitarchiveRecord fetchBitarchiveRecord(URI uri, long offset) throws Exception {
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder.setConnectTimeout(timeout);
        requestBuilder.setConnectionRequestTimeout(timeout);
        String fileName = Paths.get(uri.getPath()).getFileName().toString();
        log.debug("fileName: " + fileName);
        HttpUriRequest request = RequestBuilder.get()
                .setUri(uri)
                .addHeader("User-Agent", USER_AGENT)
                .addHeader("Range", "bytes=" + offset + "-")
                .build();
        log.debug("Executing request " + request.getRequestLine());
        boolean atFirst = (offset == 0L);
        CloseableHttpClient closableHttpClient = HttpClients.custom().setConnectionManager(cm).build();
        try (CloseableHttpResponse httpResponse = closableHttpClient.execute(request)) {
            log.debug("httpResponse status: " + httpResponse.getStatusLine().toString());
            if (httpResponse.getStatusLine().getStatusCode() != 200) {
                log.error("Http request error " + httpResponse.getStatusLine().getStatusCode());
                return null;
            }
            HttpEntity entity = httpResponse.getEntity();
            if (entity != null) {
                InputStream iStr = entity.getContent();
                //Note that data that comes back from WarcRecordService has been decompressed so to get the
                //right arc/warc parser from the ArchiveReaderFactory we have to give it the name of the
                //uncompressed file.
                final String inflatedName = fileName.replace(".gz", "");
                ArchiveReader archiveReader = ArchiveReaderFactory.get(inflatedName, iStr, atFirst);
                ArchiveRecord archiveRecord = archiveReader.get();
                BitarchiveRecord reply = new BitarchiveRecord(archiveRecord, fileName);
                log.debug("reply: " + reply.toString());
                return reply;
            } else {
                log.warn("Received null response entity for request for {}, {}", uri, offset);
                return null;
            }
        }
    }

    /**
     * Retrieves a single BitarchiveRecord from the repository from a given file and offset. If the operation fails for
     * any reason, this method returns null.
     *
     * @param arcfileName Name of the arcfile/warcfile to retrieve.
     * @param index offset to fetch specific record from warc or arc file
     */
    public BitarchiveRecord getBitarchiveRecord(String arcfileName, long index) {

        BitarchiveRecord bitarchiveRecord = null;
        try {
            ArgumentNotValid.checkNotNullOrEmpty(arcfileName, "arcfile");
            ArgumentNotValid.checkNotNegative(index, "index");

            log.debug("Requesting get of record '{}:{}'", arcfileName, index);

            String strUri = this.getBaseUri().toString() + "/" + arcfileName;

            URI uri = new URI(strUri);
            bitarchiveRecord = this.fetchBitarchiveRecord(uri, index);
        } catch (Exception e) {
            log.error("Failed to retrieve record at offset {} from file {}.", index, arcfileName, e);
        }
        return bitarchiveRecord;
    }


}
