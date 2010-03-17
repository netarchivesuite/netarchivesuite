/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;
import dk.netarkivet.testutils.ReflectUtils;

/**
 * Testclass for class CrawlLogIndexCache.
 */
public class CrawlLogIndexCacheTester extends CacheTestCase {
    public CrawlLogIndexCacheTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }
    
    /**
     * Test of private method sortCrawlLog.
     * @throws Exception
     */
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
                    fail(msg + ": " + f + " unsorted at line " + i
                            + ":\n" + url + "\n" + prev);
                }
            }
            prev = url;
            i++;
        }
    }
    
    /**
     * Test of preparecombine.
     * @throws Exception
     */
    public void testPrepareCombine()
            throws NoSuchFieldException, IllegalAccessException {
        // Currently only tests that a log message is written
        CrawlLogIndexCache cache = new FullCrawlLogIndexCache();
        ReflectUtils.getPrivateField(CrawlLogIndexCache.class,
                                     "cdxcache").set(cache,
                                                     new CDXDataCache() {
                                                         public Long cache(long ID) {
                                                             if (ID % 3 == 0) {
                                                                 return null;
                                                             } else {
                                                                 return ID;
                                                             }
                                                         }
                                                     });
        ReflectUtils.getPrivateField(CombiningMultiFileBasedCache.class,
                                     "rawcache").set(cache,
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
        LogUtils.flushLogs(CrawlLogIndexCache.class.getName());
        FileAsserts.assertFileContains("Should have info about starting index",
                                       "Starting to generate fullcrawllogindex for jobs: " + jobIDs,
                                       TestInfo.LOG_FILE);
    }
}