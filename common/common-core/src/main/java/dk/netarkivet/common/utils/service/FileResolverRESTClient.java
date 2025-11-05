package dk.netarkivet.common.utils.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.HttpsClientBuilder;
import dk.netarkivet.common.utils.Settings;

import javax.net.ssl.SSLHandshakeException;

/**
 * A FileResolver client to communicate with a service implementing the FileResolver API
 * e.g. url's like http://some.url.dk/555-.*
 */
public class FileResolverRESTClient implements FileResolver {

    private static final Logger log = LoggerFactory.getLogger(FileResolverRESTClient.class);
    private static final HttpsClientBuilder clientBuilder;

    static {
        String privateKeyFile = Settings.get(CommonSettings.FILE_RESOLVER_KEYFILE);
        log.info("Building FileResolverRESTClient with private key file: {}", privateKeyFile);
        clientBuilder = new HttpsClientBuilder(privateKeyFile);
    }

    /**
     * Base url for the API endpoint
     */
    private final URL baseUrl;

    public FileResolverRESTClient() {
        baseUrl = getBaseURL();
    }

    private URL getBaseURL() {
        final URL baseUrl;
        String url = Settings.get(CommonSettings.FILE_RESOLVER_BASE_URL);
        try {
            baseUrl = new URL(url);
        } catch (MalformedURLException e) {
            log.error("Malformed Url for FileResolver", e);
            throw new RuntimeException(e);
        }
        return baseUrl;
    }

    @Override public List<Path> getPaths(Pattern filepattern) {
        return getPaths(filepattern, false);
    }

    private List<Path> getPaths(Pattern filepattern, boolean exactfilename) {
        int retries = Settings.getInt(CommonSettings.FILE_RESOLVER_RETRIES);
        int retryWaitSeconds = Settings.getInt(CommonSettings.FILE_RESOLVER_RETRY_WAIT);
        for (int i = 0;  i <= retries; i++) {
            List<Path> results = getPathsSingleAttempt(filepattern, exactfilename);
            if (results == null || results.isEmpty()) {
                if (i == retries) break;
                try {
                    log.debug("FileResolver call on {} gave empty result, retrying.", filepattern);
                    Thread.sleep(retryWaitSeconds*1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                log.debug("FileResolver call on {} gave non-empty result with {} match(es).", filepattern, results.size());
                return results;
            }
        }
        log.debug("No matching results for FileResolver for {}" + filepattern);
        return new ArrayList<>();
    }

    private List<Path> getPathsSingleAttempt(Pattern filepattern, boolean exactfilename) {
        try {
            String pattern = filepattern.pattern();
            URI uri = new URL(baseUrl + "/" + URLEncoder.encode(pattern, StandardCharsets.UTF_8.toString())).toURI().normalize();
            log.debug("Calling FileResolver: {}", uri);
            CGIRequestBuilder requestBuilder = new CGIRequestBuilder(uri);
            HttpUriRequest request = requestBuilder.buildFileResolverRequest(exactfilename);
            CloseableHttpClient httpClient = clientBuilder.getHttpsClient();
            log.debug("Request: {}, client {}", request, httpClient);
            try (CloseableHttpResponse httpResponse = httpClient.execute(request)) {
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    log.warn("FileResolver call to {} returned status code {}.", uri, statusCode);
                    return new ArrayList<>();
                }
                InputStream istr = httpResponse.getEntity().getContent();
                List<String> results = IOUtils.readLines(istr);
                return results.stream()
                        .filter(path -> !"".equals(path.trim()))   //remove empties and whitespace
                        .map(pathString -> Paths.get(pathString.trim()))   //convert to Path
                        .collect(Collectors.toList());
            }
        } catch (IOException | URISyntaxException e) {
            log.error("Problem resolving file " + filepattern, e);
            return new ArrayList<>();
        }
    }

    /**
     * Note that the input to this method should be a literal filename but no checking or escaping is
     * done to prevent the inclusion of regex directives.
     * @param filename The filename to resolve.
     * @return The first Path to a matching file or null if no such file is found
     */
    @Override public Path getPath(String filename) {
        final List<Path> paths = getPaths(Pattern.compile(filename), true);
        if (!paths.isEmpty()) {
            return paths.get(0);
        } else {
            return null;
        }
    }
}
