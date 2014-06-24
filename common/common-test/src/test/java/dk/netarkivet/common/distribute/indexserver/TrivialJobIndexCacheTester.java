package dk.netarkivet.common.distribute.indexserver;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Unit tests for the TrivialJobIndexCache class. 
 */
public class TrivialJobIndexCacheTester extends TestCase {
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR,
            TestInfo.WORKING_DIR);
    ReloadSettings rs = new ReloadSettings();

    public TrivialJobIndexCacheTester(String s) {
        super(s);
    }

    public void setUp() {
        rs.setUp();
        Settings.set(CommonSettings.CACHE_DIR, TestInfo.WORKING_DIR.getAbsolutePath());
        mtf.setUp();
    }

    public void tearDown() {
        mtf.tearDown();
        rs.tearDown();
    }
    public void testCacheData() throws Exception {
        JobIndexCache cache = new TrivialJobIndexCache(RequestType.DEDUP_CRAWL_LOG);
        try {
            cache.getIndex(Collections.singleton(1L)).getIndexFile().getName();
            fail("Expected IOFailure on non-existing cache file");                    
        } catch (IOFailure e) {
            //expected
        }

        Set<Long> jobs = new HashSet<Long>();
        jobs.add(2L);
        jobs.add(3L);
        assertEquals("Should give the expected cache with the right jobs",
                "2-3-DEDUP_CRAWL_LOG-cache", cache.getIndex(jobs).getIndexFile().getName());

    }
}