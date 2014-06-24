package dk.netarkivet.harvester.indexserver;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.harvester.indexserver.CombiningMultiFileBasedCache;
import dk.netarkivet.harvester.indexserver.FileBasedCache;


/**
 * Unit test(s) for the CombiningMultiFileBasedCache class.
 */
public class CombiningMultiFileBasedCacheTester extends CacheTestCase {
    public CombiningMultiFileBasedCacheTester(String s) {
        super(s);
    }

    public void testCacheData() throws Exception {
        final Map<Integer, File> combined = new HashMap<Integer, File>();
        final Set<Integer> rawgotten = new HashSet<Integer>();
        CombiningMultiFileBasedCache<Integer> cache =
                new CombiningMultiFileBasedCache<Integer>("test1",
                        new FileBasedCache<Integer>("rawtest1") {
                    
                            public File getCacheFile(Integer id) {
                                return new File(TestInfo.CRAWLLOGS_DIR,
                                        "crawl-" + id + ".log");
                            }

                            public Integer cache(Integer id) {
                                rawgotten.add(id);
                                return super.cache(id);
                            }

                            // Our "cache" always gives null for other files
                            protected Integer cacheData(Integer id) {
                                return null;
                            }
                        }) {

                    protected void combine(Map<Integer, File> filesFound) {
                        File resultFile = getCacheFile(filesFound.keySet());
                        combined.clear();
                        combined.putAll(filesFound);
                        try {
                            resultFile.createNewFile();
                        } catch (IOException e) {
                            throw new IOFailure("Exception touching file", e);
                        }
                    }
                  
                };

        // Test that combine is call on all ids (1,4) in set.
        Set<Integer> ids = new HashSet<Integer>();
        ids.add(1);
        ids.add(4);
        checkContainsExactly("Should have both elements",
                list(1, 4), cache.cache(ids));
        checkContainsExactly("Cached two existinging elements",
                list(1, 4), combined.keySet());

        // Should not call combine again if file still exists.
        combined.clear();
        checkContainsExactly("Should get two elements for existing cache",
                list(1, 4), cache.cache(ids));
        assertTrue("Should have nothing now", combined.isEmpty());

        // If asking for a set with an ungettable file, combine should not
        // be called on the full set, but on the existing subset.
        ids.remove(4);
        ids.add(5);
        rawgotten.clear();
        checkContainsExactly("Should only get the existing element",
                list(1), cache.cache(ids));
        checkContainsExactly("Should have asked for both elements",
                list(1, 5), rawgotten);
        assertEquals("Should not have called combine when parts were missing",
                0, combined.size());
    }

    static <T> void checkContainsExactly(String msg, Collection<T> wanted,
                                         Collection<T> gotten) {
        assertEquals(msg + ": Sizes must be equal", wanted.size(), gotten.size());
        for (T x : wanted) {
            assertTrue(msg + ": Should have element " + x,
                    gotten.contains(x));
        }
    }

    static <T> List<T> list(T... args) {
        return Arrays.asList(args);
    }
}