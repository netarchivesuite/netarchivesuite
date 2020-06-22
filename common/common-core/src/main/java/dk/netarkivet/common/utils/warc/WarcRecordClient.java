package dk.netarkivet.common.utils.warc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
// import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.valueOf;

public class WarcRecordClient {

    /** Logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(WarcRecordClient.class);
    /** The amount of milliseconds in a second. 1000. */
    private static final int MILLISECONDS_PER_SECOND = 1000;
    /** The length of time to wait for a get reply before giving up. */
    private long getTimeout;
    private final static int STREAM_ALL = -1;
    private static final String USER_AGENT = "Mozilla/5.0";
   //  private int connectTimeout = 10000;
    private static int timeout = 1000;
    // private int readTimeout = 10000;
    private final static long offset = 3442;
    final static String SAMPLE_HOST = "http://localhost:8883/cgi-bin2/py1.cgi/10-4-20161218234343407-00000-kb-test-har-003.kb.dk.warc.gz";

    public static void main(String args[]) throws Exception {
        // Header header = new BasicHeader( name,value);
        // Use URI from OpenWayback example
        // curl:
        // test_curl: -r "3442-" "http://localhost:8883/cgi-bin2/py1.cgi/10-4-20161218234343407-00000-kb-test-har-003.kb.dk.warc.gz?foo=bar&x=y"
        // httpGet.addHeader("Range", "bytes=3442-");  // or httpGet.addHeader("Range", "bytes=3442-3442"); or open range converted to closed range to make it work

        // Creating a HttpClient object
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        try {
          WarcRecordClient warcRecordClient = new WarcRecordClient();
          BitarchiveRecord warcRecord  = warcRecordClient.getWarc(httpClient, SAMPLE_HOST);
        } finally {
            httpClient.close();
        }
    }
        public void sendGET() throws IOException {

        }

public BitarchiveRecord getWarc(CloseableHttpClient httpClient, String uri) throws IOException {                     // should return warcRecord??
            RequestConfig.Builder requestBuilder = RequestConfig.custom();
            requestBuilder.setConnectTimeout(timeout);
            requestBuilder.setConnectionRequestTimeout(timeout);

            HttpUriRequest request = RequestBuilder.get()
                    .setUri(uri)
                    .addHeader("User-Agent",USER_AGENT)
                    .setHeader(HttpHeaders.CONTENT_TYPE, "application/warc")   // STREAM_ALL = -1;
                    .setHeader("Range", "bytes=" + offset + "-")   // offset + 1?? might require <= -1 check and > 1 check
                    .addParameter("foo", "bar")  // first querey parameter
                    .addParameter("x", "y")      // second query parameter
                    .build();
            // client.execute(request);
            System.out.println("Executing request " + request.getRequestLine());

            // Create custom response handler
            ResponseHandler<String> responseHandler = WarcRecordClient::handleResponse;

            //(HttpHeaders.CONTENT_TYPE, "application/warc")
            HttpResponse httpResponse = httpClient.execute(request, (HttpContext) responseHandler);
           // httpResponse.getEntity().getContent();

           // return responseHandler.toString();
           BitarchiveRecord reply = null;
           try {
               reply = (BitarchiveRecord) httpResponse.getEntity();
           }
           catch (ClassCastException e) {
            throw new IOFailure("Received invalid argument reply: '" + reply + "'", e);
           }
           return reply;
    }
/*
        // If usiong PoolingHttpClient
        PoolingHttpClientConnectionManager cm = getPoolingHttpClientConnectionManager();

        // CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpClient httpclient = createClosableHttpClient(cm);

        //Creating a HttpGet object
        HttpGet httpget1 = getHttpfrom(SAMPLE_URI1);

        //Printing the method used
        System.out.println("Request Type: " + httpget1.getMethod());

        //Executing the Get request
        HttpResponse httpresponse = httpclient.execute(httpget1);

        Scanner sc = new Scanner(httpresponse.getEntity().getContent());

        //Printing the status line
        System.out.println(httpresponse.getStatusLine());
        while(sc.hasNext()) {
            System.out.println(sc.nextLine());
        }
*/


    private static synchronized String handleResponse(HttpResponse response) throws IOException {
        int status = response.getStatusLine().getStatusCode();
        if (status >= 200 && status < 300) {
            HttpEntity entity = response.getEntity();
            System.out.println("Entity:" + entity);
            return entity != null ? EntityUtils.toString(entity) : null;
        } else {
            throw new ClientProtocolException("Unexpected response status: " + status);
        }
    }


    private  HttpGet getHttpfrom(String uri) {
        return new HttpGet(uri);
    }

   // Make it singleton?
    private static synchronized HttpClient createClosableHttpClient(PoolingHttpClientConnectionManager cm) throws IOException {
            CloseableHttpClient httpClient = HttpClients.custom()
                    .setConnectionManager(cm)
                    .build();
            return httpClient;
    }

    private static PoolingHttpClientConnectionManager getPoolingHttpClientConnectionManager() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        // Increase max total connection to 200
        cm.setMaxTotal(200);
        // Increase default max connection per route to 20
        cm.setDefaultMaxPerRoute(20);
        // Increase max connections for localhost:80 to 50
        HttpHost localhost = new HttpHost("localhost", 80);
        cm.setMaxPerRoute(new HttpRoute(localhost), 50);
        return cm;
    }


    public BitarchiveRecord get(String arcfileName, long index) throws ArgumentNotValid, IOFailure, IOException {
        ArgumentNotValid.checkNotNullOrEmpty(arcfileName, "arcfile");
        ArgumentNotValid.checkNotNegative(index, "index");
        log.debug("Requesting get of record '{}:{}'", arcfileName, index);
        long start = System.currentTimeMillis();

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        String uri = getUriFrom(arcfileName, index);
        // sendGet()
        BitarchiveRecord replyNetMsg = getWarc(httpClient, uri);  // check result

        long timePassed = System.currentTimeMillis() - start;
        //  Rewrite for Apache httpclient:
        //     GetMessage requestGetMsg = new GetMessage(Channels.getTheRepos(), replyQ, arcfile, index);
        if (replyNetMsg != null) {
            log.debug("Reply received after {} seconds", (timePassed / MILLISECONDS_PER_SECOND));
        } else if (replyNetMsg == null) {
            log.info("Request for record({}:{}) timed out after {} seconds. Returning null BitarchiveRecord", arcfileName,
                    index, (getTimeout / MILLISECONDS_PER_SECOND));
            return null;
        }

        return replyNetMsg;
    }


    private String getUriFrom(String arcfileName, long index) {
       // ToDo: Get Uri from request with arcfileName, index and more...
        String uri = null;
        return uri;
    }


    /**
     * Synchronously retrieves a file from a bitarchive and places it in a local file. This is the interface for sending
     * GetFileMessage on the "TheArcrepos" queue. This is a blocking call.
     *
     * @param arcfilename Name of the arcfile to retrieve.
     * @param replica The bitarchive to retrieve the data from.
     * @param toFile Filename of a place where the file fetched can be put.
     * @throws ArgumentNotValid If the arcfilename are null or empty, or if either replica or toFile is null.
     * @throws IOFailure if there are problems getting a reply or the file could not be found.
     */
    public void getFile(String arcfilename, Replica replica, File toFile) throws ArgumentNotValid, IOFailure {
        ArgumentNotValid.checkNotNullOrEmpty(arcfilename, "arcfilename");
        ArgumentNotValid.checkNotNull(replica, "replica");
        ArgumentNotValid.checkNotNull(toFile, "toFile");

        log.debug("Requesting get of file '{}' from '{}'", arcfilename, replica);
        // ArgumentNotValid.checkNotNull(replyQ, "replyQ must not be null");
        /* ToDo: rewrite to take bitmag API call
     //   GetFileMessage gfMsg = new GetFileMessage(Channels.getTheRepos(), replyQ, arcfilename, replica.getId());
     //   GetFileMessage getFileMessage = (GetFileMessage) sendAndWaitForOneReply(gfMsg, 0);
        if (getFileMessage == null) {
            throw new IOFailure("GetFileMessage timed out before returning." + "File not found?");
        } else if (!getFileMessage.isOk()) {
            throw new IOFailure("GetFileMessage failed: " + getFileMessage.getErrMsg());
        } else {
            getFileMessage.getData(toFile);
        }

         */
    }

}
