package dk.netarkivet.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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

public class FileResolverRESTClient implements FileResolver {

    private static final Logger log = LoggerFactory.getLogger(FileResolverRESTClient.class);

    private final URL baseUrl;

    private static final PoolingHttpClientConnectionManager cManager = new PoolingHttpClientConnectionManager();



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



    @Override public List<Path> getPaths(String filepattern) {
        try {
            CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cManager).build();
            URL url = new URL(baseUrl + "/" + filepattern).toURI().normalize().toURL();
            HttpUriRequest request = RequestBuilder.get()
                    .setUri(url.toString())
                    .build();
            try (CloseableHttpResponse httpResponse = httpClient.execute(request)) {
                InputStream istr = httpResponse.getEntity().getContent();
                List<String> results = IOUtils.readLines(istr);
                return results.stream()
                        .filter(path -> !"".equals(path.trim()))
                        .map(pathString -> Paths.get(pathString.trim()))
                        .collect(Collectors.toList());
            }
        } catch (IOException | URISyntaxException e) {
            log.error("Problem resolving file " + filepattern, e);
            return new ArrayList<>();
        }
    }

    @Override public Path getPath(String filename) {
        final List<Path> paths = getPaths(filename);
        if (!paths.isEmpty()) {
            return paths.get(0);
        } else {
            return null;
        }
    }
}
