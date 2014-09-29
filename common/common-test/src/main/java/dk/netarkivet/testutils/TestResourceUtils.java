package dk.netarkivet.testutils;

import java.net.URL;

public class TestResourceUtils {

    public static String getFilePath(String fileName) {
        URL url = TestResourceUtils.class.getClassLoader().getResource(fileName);
        return getUrlPath(url);
    }

    private static String getUrlPath(URL url) {
        String path = url.getFile();
        path = path.replaceAll("%5b", "[");
        path = path.replaceAll("%5d", "]");
        return path;
    }
}
