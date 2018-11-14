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

import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.LogbackRecorder;
import dk.netarkivet.testutils.ReflectUtils;

/**
 * Testclass for class CrawlLogIndexCache.
 */
public class CrawlLogIndexCacheTester extends CacheTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test of private method sortCrawlLog.
     *
     * @throws Exception
     */
    @Test
    public void testSortCrawlLog() throws Exception {
        File sortedFile = new File(TestInfo.CRAWL_LOG_1.getAbsolutePath() + ".sorted");
        FileUtils.sortCrawlLog(TestInfo.CRAWL_LOG_1, sortedFile);

        assertIsSortedCrawlLog("Should be sorted", sortedFile);
    }

    private void assertIsSortedCrawlLog(String msg, File f) {
        int urlField = 3;
        String prev = null;
        List<String> strings = FileUtils.readListFromFile(f);
        int i = 1;
        for (String s : strings) {
            String[] split = s.split("\\s+");
            String url = split[urlField];
            if (prev != null) {
                if (url.compareTo(prev) < 0) {
                    fail(msg + ": " + f + " unsorted at line " + i + ":\n" + url + "\n" + prev);
                }
            }
            prev = url;
            i++;
        }
    }

    /**
     * Test of preparecombine.
     *
     * @throws Exception
     */
    @Test
    public void testPrepareCombine() throws NoSuchFieldException, IllegalAccessException {
        LogbackRecorder lr = LogbackRecorder.startRecorder();
        // Currently only tests that a log message is written
        CrawlLogIndexCache cache = new FullCrawlLogIndexCache();
        ReflectUtils.getPrivateField(CrawlLogIndexCache.class, "cdxcache").set(cache, new CDXDataCache() {
            public Long cache(Long ID) {
                if (ID % 3 == 0) {
                    return null;
                } else {
                    return ID;
                }
            }
        });
        ReflectUtils.getPrivateField(CombiningMultiFileBasedCache.class, "rawcache").set(cache,
                new CrawlLogDataCache() {
                    public File getCacheFile(Long id) {
                        return new File(TestInfo.WORKING_DIR, "cache-" + id);
                    }

                    protected Long cacheData(Long id) {
                        return null;
                    }
                });
        Set<Long> jobIDs = new HashSet<Long>();
        jobIDs.add(1L);
        cache.prepareCombine(jobIDs);
        lr.assertLogContains("Should have info about starting index", "Starting to generate fullcrawllogindex for the "
                + jobIDs.size() + " jobs: " + jobIDs);
        lr.stopRecorder();
    }

}
