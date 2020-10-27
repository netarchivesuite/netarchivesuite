/*
 * #%L
 * Netarchivesuite - harvester - test
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.harvester.indexserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermRangeFilter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.BytesRef;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.utils.AllDocsCollector;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.StringUtils;
import is.hi.bok.deduplicator.DigestIndexer;

/**
 * Unit test(s) for the DedupCrawlLogIndexCache class.
 */
public class DedupCrawlLogIndexCacheTester extends CacheTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testCombine() throws Exception {
        // These are the origins of job #4 and #1
        Map<String, String> origins = new HashMap<String, String>(8);

        // "job" #4
        origins.put("http://www.kb.dk/bevarbogen/images/menu_03.gif",
                "54-8-20050620183552-00016-kb-prod-har-001.kb.dk.arc,92248220,20050506114818000");
        origins.put("http://www.kb.dk/bevarbogen/images/menu_06.gif",
                "54-8-20050620183552-00016-kb-prod-har-001.kb.dk.arc,95056820,20050506114822000");
        origins.put("http://www.kb.dk/bevarbogen/images/menu_07.gif",
                "54-8-20050620183552-00016-kb-prod-har-001.kb.dk.arc,95468220,20050506114816000");
        origins.put("http://www.kb.dk/bevarbogen/images/menutop.gif",
                "54-8-20050620183552-00016-kb-prod-har-002.kb.dk.arc,42,20050506114820000");
        origins.put("http://www.kb.dk/bevarbogen/script.js", "check-arc,42");

        // "job" #1
        origins.put("http://www.kb.dk/clear.gif", "54-8-20050620183552-00016-kb-prod-har-001.kb.dk.arc,55983420,20050506114732000");
        origins.put("http://www.kb.dk/dither.gif", "54-8-20050620183552-00016-kb-prod-har-001.kb.dk.arc,53985420,20050506114736000");
        origins.put("http://www.kb.dk/dither_blaa.gif", "54-8-20050620183552-00016-kb-prod-har-001.kb.dk.arc,58593420,20050506114734000");

        Map<Long, File> files = new HashMap<Long, File>();
        files.put(1L, TestInfo.CRAWL_LOG_1);
        files.put(4L, TestInfo.CRAWL_LOG_4);

        Set<Long> requiredSet = new HashSet<Long>();
        requiredSet.add(1L);
        requiredSet.add(4L);

        DedupCrawlLogIndexCache cache = new DedupCrawlLogIndexCache();
        File resultFile = cache.getCacheFile(files.keySet());

        cache.combine(files);

        assertTrue("Result file should have contents after combining", resultFile.length() > 0);
        assertFalse("Should not have left an unzipped lucene index",
                new File(resultFile.getAbsolutePath().substring(0, resultFile.getAbsolutePath().length() - 4)).exists());
        File unzipDir = new File(TestInfo.WORKING_DIR, "luceneindex");
        if (!unzipDir.mkdir()) {
            fail("Unable to create unzipDir '" + unzipDir.getAbsolutePath() + "' for luceneindex: ");
        }
        File[] resultFiles = resultFile.listFiles();
        for (File f : resultFiles) {
            if (f.getName().endsWith(".gz")) {
                InputStream in = new GZIPInputStream(new FileInputStream(f));
                FileUtils.writeStreamToFile(in,
                        new File(unzipDir, f.getName().substring(0, f.getName().length() - ".gz".length())));
                in.close();
            }
        }

        Directory luceneDirectory = new MMapDirectory(unzipDir);
        IndexReader reader = DirectoryReader.open(luceneDirectory);

        // System.out.println("doc-count: " + reader.maxDoc());
        IndexSearcher index = new IndexSearcher(reader);
        // QueryParser queryParser = new QueryParser("url",
        // new WhitespaceAnalyzer(dk.netarkivet.common.constants.LUCENE_VERSION));
        // QueryParser queryParser = new QueryParser(dk.netarkivet.common.Constants.LUCENE_VERSION, "url",
        // new WhitespaceAnalyzer(dk.netarkivet.common.Constants.LUCENE_VERSION));
        // Query q = queryParser.parse("http\\://www.kb.dk*");

        // Crawl log 1 has five entries for www.kb.dk, but two are robots
        // and /, which the indexer ignores, leaving 3
        // Crawl log 4 has five entries for www.kb.dk

        // System.out.println("Found hits: " + hits.size());
        // for (ScoreDoc hit : hits) {
        // int docID = hit.doc;
        // Document doc = index.doc(docID);
        //
        // String url = doc.get("url");
        // String origin = doc.get("origin");
        // System.out.println("url,origin = " + url + ", " + origin);
        // }

        verifySearchResult(origins, index);

        assertTrue("Should have found all origins, but have still " + origins.size() + " left: " + origins,
                origins.isEmpty());
    }

    private void verifySearchResult(Map<String, String> origins, IndexSearcher index) throws IOException {
        Set<String> urls = new HashSet<String>(origins.keySet());
        List<String> errors = new ArrayList<String>();
        for (String urlValue : urls) {
            BytesRef uriRef = new BytesRef(urlValue);
            Query q = new ConstantScoreQuery(new TermRangeFilter(DigestIndexer.FIELD_URL, uriRef, uriRef, true, true));
            AllDocsCollector collector = new AllDocsCollector();
            index.search(q, collector);
            List<ScoreDoc> hits = collector.getHits();
            for (ScoreDoc hit : hits) {
                int docID = hit.doc;
                Document doc = index.doc(docID);
                String url = doc.get("url");
                String origin = doc.get("origin");
                if (!origins.get(url).equals(origin)) {
                	errors.add("Should have correct origin for url '" + url + "' but was: '" + origin + "'");
                }
                
                // Ensure that each occurs only once.
                String removedValue = origins.remove(url);
                if (removedValue == null) {
                    // System.out.println("'" + url + "' not found in origins map");
                } else {
                    // System.out.println("'" + url + "' was found in origins map");
                }
            }
        }
        if (errors.size() > 0) {
        	fail(errors.size() + " unexpected origins found: " + StringUtils.conjoin(",", errors)); 
        }
    }

    @Test
    public void testGetSortedCDX() throws Exception {
        CDXDataCache dummyindexcache = new CDXDataCache();
        File cdxUnsorted = dummyindexcache.getCacheFile(4L);

        File reader = DedupCrawlLogIndexCache.getSortedCDX(cdxUnsorted);
        assertNotNull("Should get a file for an existing job", reader);
        assertEquals("CDX file returned should have same content as presorted file",
                FileUtils.readListFromFile(TestInfo.CDX_CACHE_4), FileUtils.readListFromFile(reader));
    }

    @Test
    public void testGetCacheFile() throws Exception {
        DedupCrawlLogIndexCache cache = new DedupCrawlLogIndexCache();
        File test = cache.getCacheFile(Collections.singleton((1L)));
        assertEquals("Should have gzip dir as the cache file name", "1-cache", test.getName());
    }
}
