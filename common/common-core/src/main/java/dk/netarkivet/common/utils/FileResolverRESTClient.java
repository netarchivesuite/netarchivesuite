package dk.netarkivet.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;

/**
 * A FileResolver client to communicate with a service implementing the FileResolver API
 * e.g. url's like http://some.url.dk/555-.*
 */
public class FileResolverRESTClient implements FileResolver {

    private static final Logger log = LoggerFactory.getLogger(FileResolverRESTClient.class);

    /**
     * Base url for the API endpoint
     */
    private final URL baseUrl;

    /**
     * Pool of http connections
     */
    private static final PoolingHttpClientConnectionManager cManager = new PoolingHttpClientConnectionManager();

    /**
     * Whether or not to prepend a "^" to any regex patterns which do not already start with one. This defaults to true.
     */
    private boolean doPrependCircumflex = true;

    public FileResolverRESTClient() {
        String url = Settings.get(CommonSettings.FILE_RESOLVER_BASE_URL);
        try {
            baseUrl = new URL(url);
        } catch (MalformedURLException e) {
            log.error("Malformed Url for FileResolver", e);
            throw new RuntimeException(e);
        }
        cManager.setMaxTotal(Settings.getInt(CommonSettings.MAX_TOTAL_CONNECTIONS));
        cManager.setDefaultMaxPerRoute(Settings.getInt(CommonSettings.MAX_CONNECTIONS_PER_ROUTE));
    }

    /**
     * Whether or not to prepend a "^" to any regex patterns which do not already start with one.
     */
    public void setDoPrependCircumflex(boolean doPrependCircumflex) {
        this.doPrependCircumflex = doPrependCircumflex;
    }

    @Override public List<Path> getPaths(Pattern filepattern) {
        try {
            CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cManager).build();
            String pattern = filepattern.pattern();
            if (doPrependCircumflex && !pattern.startsWith("^")) {
                pattern = "^" + pattern;
            }
            URL url = new URL(baseUrl + "/" + URLEncoder.encode(pattern)).toURI().normalize().toURL();
            HttpUriRequest request = RequestBuilder.get()
                    .setUri(url.toString())
                    .build();
            try (CloseableHttpResponse httpResponse = httpClient.execute(request)) {
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
        final List<Path> paths = getPaths(Pattern.compile("^"+filename+"$"));
        if (!paths.isEmpty()) {
            return paths.get(0);
        } else {
            return null;
        }
    }
}
