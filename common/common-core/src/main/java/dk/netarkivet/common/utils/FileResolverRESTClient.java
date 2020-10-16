package dk.netarkivet.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
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

    private URL baseUrl;

    private static PoolingHttpClientConnectionManager cManager = new PoolingHttpClientConnectionManager();



    public FileResolverRESTClient() {
        String url = Settings.get(CommonSettings.FILE_RESOLVER_BASE_URL);
        try {
            baseUrl = new URL(url);
        } catch (MalformedURLException e) {
            log.error("Malformed Url for FileResolver", e);
            throw new RuntimeException(e);
        }
        cManager.setMaxTotal(20);
        cManager.setDefaultMaxPerRoute(20);
    }



    @Override public List<Path> getPaths(String filepattern) {
        try {
            CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cManager).build();
            HttpUriRequest request = RequestBuilder.get()
                    .setUri(baseUrl + filepattern)
                    .build();
            CloseableHttpResponse httpResponse = httpClient.execute(request);
            InputStream istr = httpResponse.getEntity().getContent();
            List<String> results = IOUtils.readLines(istr);
            httpResponse.close();
            return results.stream().map(pathString -> Paths.get(pathString)).collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    @Override public Path getPath(String filename) {
        return getPaths(filename).get(0);
    }
}
