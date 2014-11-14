package dk.netarkivet.testutils;

import java.net.URL;

public class TestResourceUtils {
    public static final String OUTPUT_DIR = "target/test-output";

    public static String getFilePath(String fileName) {
        URL url = TestResourceUtils.class.getClassLoader().getResource(fileName);
        if (url == null) {
            throw new IllegalArgumentException("Didn't find resource '" + fileName + "'on the classpath");
        }
        return getUrlPath(url);
    }

    private static String getUrlPath(URL url) {
        String path = url.getFile();
        path = path.replaceAll("%5b", "[");
        path = path.replaceAll("%5d", "]");
        return path;
    }
}
