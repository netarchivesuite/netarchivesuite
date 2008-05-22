package dk.netarkivet.archive.indexserver;
/**
 * lc forgot to comment this!
 */

import java.io.File;

import junit.framework.TestCase;

import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;


public class FileBasedCacheTester extends TestCase {
    public FileBasedCacheTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }
    public void testGetIndex() throws Exception {
        FileBasedCache cache = new FileBasedCache<String>("Test") {

            /**
             * Get the file that caches content for the given ID.
             *
             * @param id Some sort of id that uniquely identifies the item within the
             *           cache.
             * @return A file (possibly nonexistant or empty) that can cache the data
             *         for the id.
             */
            public File getCacheFile(String id) {
                return TestInfo.ARC_FILE_1;
            }

            /**
             * Fill in actual data in the file in the cache.  This is the workhorse
             * method that is allowed to modify the cache.  When this method is called,
             * the cache can assume that getCacheFile(id) does not exist.
             *
             * @param id Some identifier for the item to be cached.
             * @return An id of content actually available.  In most cases, this will
             *         be the same as id, but for complex I it could be a subset (or null if
             *         the type argument I is a simple type).  If the return value is not the
             *         same as id, the file will not contain cached data, and may not even
             *         exist.
             */
            protected String cacheData(String id) {
                return null;
            }

            String nextId = "";
            /**
             * Fill in actual data in the file in the cache.  This is the workhorse
             * method that is allowed to modify the cache.  When this method is called,
             * the cache can assume that getCacheFile(id) does not exist.
             *
             * @param id Some identifier for the item to be cached.
             * @return An id of content actually available.  In most cases, this will
             *         be the same as id, but for complex I it could be a subset (or null if
             *         the type argument I is a simple type).  If the return value is not the
             *         same as id, the file will not contain cached data, and may not even
             *         exist.
             */
            public String cache(String id) {
                if (nextId.length() < 4) {
                    nextId += "X";
                }
                return nextId;
            }
        };
        cache.getIndex("A");
        LogUtils.flushLogs(FileBasedCache.class.getName());
        FileAsserts.assertFileNotContains("Should not give warning on cache retry",
                TestInfo.LOG_FILE, "WARNING");
    }
}