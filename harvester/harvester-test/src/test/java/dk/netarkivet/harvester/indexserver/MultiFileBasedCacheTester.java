package dk.netarkivet.harvester.indexserver;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.indexserver.MultiFileBasedCache;

/**
 * Tests for multifilebasedcache, i.e. file name generation.
 */
public class MultiFileBasedCacheTester extends TestCase {

    private MultiFileBasedCache<Long> multiFileBasedCache
    = new MultiFileBasedCache<Long>("Test") {

        protected Set<Long> cacheData(Set<Long> id) {
            //Not used
            return null;
        }
    };

    public MultiFileBasedCacheTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    public void testGetCacheFile() throws Exception {
        try {
            multiFileBasedCache.getCacheFile(null);
            fail("Should throw exception");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        assertEquals("Expect value on 1 id",
                "1-cache",
                multiFileBasedCache.getCacheFile(
                        new HashSet<Long>(
                                Arrays.asList(new Long[]{1L})))
                                .getName());
        assertEquals("Expect value on 2 ids",
                     "1-2-cache",
                     multiFileBasedCache.getCacheFile(
                             new HashSet<Long>(
                                     Arrays.asList(new Long[]{1L, 2L})))
                                     .getName());
        assertEquals("Expect value on 8 ids",
                "1-2-3-4-d28f2f51ffe4b6d42b3e46a7cd7a72da-cache",
                multiFileBasedCache.getCacheFile(
                        new HashSet<Long>(
                                Arrays.asList(
                                        new Long[]{1L, 2L, 3L, 4L,
                                                5L, 6L, 7L, 8L}))).getName());
        assertEquals("Expect value on 9 ids",
                     "1-2-3-4-ef8be5d11d2348e3cf689ae6bf0dd6ef-cache",
                     multiFileBasedCache.getCacheFile(
                             new HashSet<Long>(
                                     Arrays.asList(
                                             new Long[]{1L, 2L, 3L, 4L,
                                                     5L, 6L,7L, 8L, 9L})))
                                                     .getName());
    }
}
