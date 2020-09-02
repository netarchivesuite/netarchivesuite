package dk.netarkivet.common.utils.warc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.bitrepository.Bitrepository;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.archive.io.warc.WARCReaderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dk.netarkivet.common.utils.FileUtils;

public class WarcRecordClient {
    public static final String BITREPOSITORY_COLLECTIONID = "settings.common.arcrepositoryClient.bitrepository.collectionID";
    /** Logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(WarcRecordClient.class);
    /** The amount of milliseconds in a second. 1000. */
    private static final int MILLISECONDS_PER_SECOND = 1000;

    public URI getBaseUri() {
        return baseUri;
    }

    private URI baseUri;

    /** The length of time to wait for a get reply before giving up. */
    private long getTimeout;
    private final static int STREAM_ALL = -1;
    private static final String USER_AGENT = "Mozilla/5.0";
    private static int timeout = 1000;
    private String collectionId;
    private Bitrepository bitrep;
    private URI uri;
    private  CloseableHttpClient httpClient = null;
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
        private static Singleton instance;
        private static CloseableHttpClient closeableHttpClient;

        private Singleton() {
        }

        public static CloseableHttpClient getCloseableHttpClient() {
            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
            cm.setMaxTotal(200);           // Increase max total connections to 200
            cm.setDefaultMaxPerRoute(20);  // Increase  default max connections per route to 20
            closeableHttpClient  = HttpClients.custom()
                    .setConnectionManager(cm)
                    .build();
            return closeableHttpClient;
        }

        public static Singleton getInstance() {
            return instance;
        }
    }

    public WarcRecordClient(URI baseUri) throws URISyntaxException {
        this.baseUri = baseUri;
    }

    public BitarchiveRecord getWarc( URI uri, long offset) throws IOException {
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
            RequestConfig.Builder requestBuilder = RequestConfig.custom();
            requestBuilder.setConnectTimeout(timeout);
            requestBuilder.setConnectionRequestTimeout(timeout);

            String fileName = Paths.get(uri.getPath()).getFileName().toString();
            log.debug("fileName: " + fileName);

            HttpUriRequest request = RequestBuilder.get()
                    .setUri(uri)
                    .addHeader("User-Agent",USER_AGENT)
                    .addHeader("Range", "bytes=" + offset + "-")   // offset + 1?? might require <= -1 check and > 1 check
                    .build();
            log.debug("Executing request " + request.getRequestLine());

            CloseableHttpClient closableHttpClient = WarcRecordClient.Singleton.getCloseableHttpClient();
            HttpResponse httpResponse = closableHttpClient.execute(request);
            log.debug("httpResponse status: " + httpResponse.getStatusLine().toString());
            if (httpResponse.getStatusLine().getStatusCode() != 200) {
                log.error("Http request error " + httpResponse.getStatusLine().getStatusCode());
                return null;
            }

           BitarchiveRecord reply = null;
           try {
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
           }
           catch (ClassCastException e) {
               log.error("Received invalid argument reply: '" + reply + "'", e);
               throw new IOFailure("Received invalid argument reply: '" + reply + "'", e);
           }

        return reply;
    }

    private  HttpGet getHttpfrom(String uri) {
        return new HttpGet(uri);
    }

    public BitarchiveRecord get(String arcfileName, long index) throws ArgumentNotValid, IOFailure, IOException, URISyntaxException {
        /**
         * Retrieves a file from a repository and places it in a local file.
         *
         * @param arcfilename Name of the arcfile to retrieve.
         * @param index offset to fetch specific record from warc file
         *              index must be the same as the offset that ends up in the range header
         * @throws ArgumentNotValid if arcfilename is null or empty, or if toFile is null
         * @throws IOFailure Specific IOException from Netarkivet used if there are problems reading or writing file, or the file with the given arcfilename could not
         * be found.
         * @throws IOException if reading file fails
         * @throws URISyntaxException if URI i not composed of baseUri + "/" + long name of file
         */
        ArgumentNotValid.checkNotNullOrEmpty(arcfileName, "arcfile");
        ArgumentNotValid.checkNotNegative(index, "index");
        log.debug("Requesting get of record '{}:{}'", arcfileName, index);
        long start = System.currentTimeMillis();

        // call WarcRecordService to get the Warc record in the file on the given index
        // and to parse it to a BitArchiveRecord
        String strUri = this.getBaseUri().toString() + "/" + arcfileName;
        URI uri = new URI(strUri);
        BitarchiveRecord warcInstance = this.getWarc(uri, index);

        return warcInstance;
    }

    /**
     * Retrieves a file from a repository and places it in a local file.
     *
     * @param arcfilename Name of the arcfile to retrieve.
     * @param toFile Filename of a place where the file fetched can be put.
     * @throws ArgumentNotValid if arcfilename is null or empty, or if toFile is null
     * @throws IOFailure if there are problems reading or writing file, or the file with the given arcfilename could not
     * be found.
     */

    public void getFile(String arcfilename,  File toFile) {
        ArgumentNotValid.checkNotNullOrEmpty(arcfilename, "String arcfilename");
        ArgumentNotValid.checkNotNull(toFile, "File toFile");

        if (!bitrep.existsInCollection(arcfilename, collectionId)) {
            log.warn("The file '{}' is not in collection '{}'.", arcfilename, collectionId);
            throw new IOFailure("File '" + arcfilename + "' does not exist");
        } else {
            File f = bitrep.getFile(arcfilename, collectionId, null);
            FileUtils.copyFile(f, toFile);
        }
    }

}
