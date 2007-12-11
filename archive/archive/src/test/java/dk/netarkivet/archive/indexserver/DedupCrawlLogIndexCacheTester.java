/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package dk.netarkivet.archive.indexserver;
/**
 * lc forgot to comment this!
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.StringAsserts;


public class DedupCrawlLogIndexCacheTester extends CacheTestCase {
    public DedupCrawlLogIndexCacheTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCombine() throws Exception {
        // These are the origins of job #4 and #1
        Map<String, String> origins = new HashMap<String, String>(8);

        // "job" #4
        origins.put("http://www.kb.dk/bevarbogen/images/menu_03.gif", "54-8-20050620183552-00016-kb-prod-har-001.kb.dk.arc,92248220");
        origins.put("http://www.kb.dk/bevarbogen/images/menu_06.gif", "54-8-20050620183552-00016-kb-prod-har-001.kb.dk.arc,95056820");
        origins.put("http://www.kb.dk/bevarbogen/images/menu_07.gif", "54-8-20050620183552-00016-kb-prod-har-001.kb.dk.arc,95468220");
        origins.put("http://www.kb.dk/bevarbogen/images/menutop.gif", "54-8-20050620183552-00016-kb-prod-har-002.kb.dk.arc,42");
        origins.put("http://www.kb.dk/bevarbogen/script.js", "check-arc,42");

        // "job" #1
        origins.put("http://www.kb.dk/clear.gif", "54-8-20050620183552-00016-kb-prod-har-001.kb.dk.arc,55983420");
        origins.put("http://www.kb.dk/dither.gif", "54-8-20050620183552-00016-kb-prod-har-001.kb.dk.arc,53985420");
        origins.put("http://www.kb.dk/dither_blaa.gif", "54-8-20050620183552-00016-kb-prod-har-001.kb.dk.arc,58593420");

        Map<Long, File> files = new HashMap<Long, File>();
        files.put(1L, TestInfo.CRAWL_LOG_1);
        files.put(4L, TestInfo.CRAWL_LOG_4);

        DedupCrawlLogIndexCache cache = new DedupCrawlLogIndexCache();
        File resultFile = cache.getCacheFile(files.keySet());
        setDummyCDXCache(cache);

        cache.combine(files);

        assertTrue("Result file should have contents after combining",
                resultFile.length() > 0);
        assertFalse("Should not have left an unzipped lucene index",
                new File(resultFile.getAbsolutePath().substring(0,
                        resultFile.getAbsolutePath().length() - 4)).exists());
        File unzipDir = new File(TestInfo.WORKING_DIR, "luceneindex");
        unzipDir.mkdir();
        File[] resultFiles = resultFile.listFiles();
        for (File f : resultFiles) {
            if (f.getName().endsWith(".gz")) {
                InputStream in = new GZIPInputStream(new FileInputStream(f));
                FileUtils.writeStreamToFile(in, new File(unzipDir,
                        f.getName().substring(0, f.getName().length() - ".gz".length())));
                in.close();
            }
        }
        String indexName = unzipDir.getAbsolutePath();

        IndexSearcher index = new IndexSearcher(indexName);
        QueryParser queryParser = new QueryParser("url", new WhitespaceAnalyzer());
        Query q = queryParser.parse("http\\://www.kb.dk*");

        Hits hits = index.search(q);
        // Crawl log 1 has five entries for www.kb.dk, but two are robots
        // and /, which the indexer ignores, leaving 3
        // Crawl log 4 has five entries for www.kb.dk
        for (int i = 0; i < hits.length(); i++) {
            Document doc = hits.doc(i);
            String url = doc.get("url");
            String origin = doc.get("origin");

            assertEquals("Should have correct origin for url " + url,
                    origins.get(url), origin);
            // Ensure that each occurs only once.
            origins.remove(url);
        }
        assertTrue("Should have hit all origins, but have " + origins,
                origins.isEmpty());
    }

    public void testGetSortedCDX() throws Exception {
        Method getSortedCDX = ReflectUtils.getPrivateMethod(CrawlLogIndexCache.class,
                "getSortedCDX", Long.TYPE);
        DedupCrawlLogIndexCache logcache = new DedupCrawlLogIndexCache();

        setDummyCDXCache(logcache);

        File reader = (File)getSortedCDX.invoke(logcache, 4L);
        assertNotNull("Should get a file for an existing job", reader);
        assertEquals("CDX file returned should contain same as presorted file",
                FileUtils.readListFromFile(TestInfo.CDX_CACHE_4),
                FileUtils.readListFromFile(reader));

        try {
            getSortedCDX.invoke(logcache, 2L);
            fail("Should have had exception on unknown ID 2");
        } catch (InvocationTargetException e) {
            // Real exception gets wrapped in the invoke call
            UnknownID cause = (UnknownID)e.getCause();
            StringAsserts.assertStringContains("Should have job ID mentioned in message",
                    "2", cause.getMessage());
        }
    }

    /** Sets up a dummy CDX index cache that just serves the files existing
     * It even just assumes that you only ask for one id at a time.
    */
    private void setDummyCDXCache(DedupCrawlLogIndexCache logcache) throws
            NoSuchFieldException, IllegalAccessException {
        CDXDataCache dummyindexcache = new CDXDataCache() {
            public Long cache(Long id) {
                File cacheFile = getCacheFile(id);
                if (cacheFile.exists()) {
                    return id;
                }
                return null;
            }
        };
        Field indexcachefield = ReflectUtils.getPrivateField(CrawlLogIndexCache.class,
                "cdxcache");
        indexcachefield.set(logcache, dummyindexcache);
    }

    public void testGetCacheFile() throws Exception {
        DedupCrawlLogIndexCache cache = new DedupCrawlLogIndexCache();
        File test = cache.getCacheFile(Collections.singleton((1L)));
        assertEquals("Should have gzip dir as the cache file name",
                "1-cache", test.getName());
    }
}