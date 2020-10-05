package dk.netarkivet.common.utils;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

// TODO: Will fail on moving files/restructuring
public class SimpleFileResolverTester {
    String PARENT_DIR = "src/test/resources/"; //TODO make somewhere more meaningful??
    Path dirPath = Paths.get(PARENT_DIR);
    FileResolver fileResolver = new SimpleFileResolver(dirPath);
    PathMatcher matcher;
    String filePattern;

    /** getPath() on an existing filename in PARENT_DIR should return the resolved Path */
    @Test
    public void testGetPathOnExistingFilename() {
        Path path = fileResolver.getPath("logback-test.xml");
        Assert.assertNotNull(path);
        Assert.assertTrue(path.toFile().exists());
    }

    /** getPath() on non-existing filename should return null */
    @Test
    public void testGetPathOnNonexistingFilename() {
        Path path = fileResolver.getPath("nice-filename.warc");
        Assert.assertNull(path);
    }

    /** getPaths() with a matching pattern should return a list of paths - one for each file in PARENT_DIR */
    @Test
    public void testGetPathsOnMatchingPattern() {
        filePattern = ".*\\.xml";
        matcher = FileSystems.getDefault().getPathMatcher("regex:" + filePattern);
        List<Path> paths = fileResolver.getPaths(matcher);
        Assert.assertTrue(paths.size() >= 2);
        paths.forEach(Assert::assertNotNull);
    }

    /** Should give an empty list on getPaths() with non-matching pattern */
    @Test
    public void testGetPathsOnNonMatchingPattern() {
        filePattern = "non(-matching)?.*\\.xml";
        matcher = FileSystems.getDefault().getPathMatcher("regex:" + filePattern);
        List<Path> paths = fileResolver.getPaths(matcher);
        Assert.assertTrue(paths.isEmpty());
    }
}
