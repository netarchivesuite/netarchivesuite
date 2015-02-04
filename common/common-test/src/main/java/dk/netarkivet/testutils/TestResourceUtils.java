package dk.netarkivet.testutils;

import java.io.File;
import java.net.URL;

import org.junit.Assert;

public class TestResourceUtils {

	public static final String OUTPUT_DIR = "target/test-output";

    protected static ClassLoader clsLdr = TestResourceUtils.class.getClassLoader();

    public static String getFilePath(String filename) {
    	System.out.println(filename);
        String path = null;
    	try {
            URL url = clsLdr.getResource(filename);
            if (url != null) {
                path = url.getFile();
                path = path.replaceAll("%5b", "[");
                path = path.replaceAll("%5d", "]");
            }
    	} catch (Throwable t) {
    		t.printStackTrace();
    	}
    	if (path == null) {
    		Assert.fail("Didn't find resource '" + filename + "' on the classpath.");
    	}
    	return path;
    }

    public static File getFile(String filename) {
    	File file = new File(getFilePath(filename));
    	if (!file.exists()) {
    		Assert.fail("Resource '" + filename + "' does not exist.");
    	}
    	return file;
    }

}
