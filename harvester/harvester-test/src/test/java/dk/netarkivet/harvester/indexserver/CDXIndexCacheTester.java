package dk.netarkivet.harvester.indexserver;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.harvester.indexserver.CDXIndexCache;
import dk.netarkivet.testutils.FileAsserts;

/**
 * Unit test(s) for the CDXIndexCache class.
 */
public class CDXIndexCacheTester extends CacheTestCase {
    public CDXIndexCacheTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCombine() throws Exception {
        // Check that items are collected, null entries ignored, and all
        // is sorted.
        CDXIndexCache cache = new CDXIndexCache();
        Map<Long, File> files = new HashMap<Long, File>();
        files.put(4L, TestInfo.METADATA_FILE_4);
        files.put(3L, TestInfo.METADATA_FILE_3);
        Set<Long> requiredSet = new HashSet<Long>();
        requiredSet.add(3L);
        requiredSet.add(4L);
        cache.combine(files);
        File cacheFile = cache.getCacheFile(files.keySet());
        FileAsserts.assertFileNumberOfLines("Should have files 3 and 4",
                cacheFile,
                (int)FileUtils.countLines(TestInfo.METADATA_FILE_3)
                + (int)FileUtils.countLines(TestInfo.METADATA_FILE_4));
        // Checks that lines are sorted:  The original metadata3 file has a
        // metadatb line after the file 3 block 2 line.
        FileAsserts.assertFileContains("Must have lines sorted",
                "metadata file 3 block 2\nmetadata file 4 block 1", cacheFile);
    }
}