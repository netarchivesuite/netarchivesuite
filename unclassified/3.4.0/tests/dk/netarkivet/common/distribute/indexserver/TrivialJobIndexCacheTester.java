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
package dk.netarkivet.common.distribute.indexserver;
/**
 * lc forgot to comment this!
 */

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import dk.netarkivet.common.Settings;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;


public class TrivialJobIndexCacheTester extends TestCase {
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR,
            TestInfo.WORKING_DIR);
    ReloadSettings rs = new ReloadSettings();

    public TrivialJobIndexCacheTester(String s) {
        super(s);
    }

    public void setUp() {
        rs.setUp();
        Settings.set(Settings.CACHE_DIR,
                TestInfo.WORKING_DIR.getAbsolutePath());
        mtf.setUp();
    }

    public void tearDown() {
        mtf.tearDown();
        rs.tearDown();
    }
    public void testCacheData() throws Exception {
        JobIndexCache cache = new TrivialJobIndexCache(RequestType.DEDUP_CRAWL_LOG);
        assertEquals("Should give empty pseudo-cache for 1 file",
                "1-DEDUP_CRAWL_LOG-cache", cache.getIndex(Collections.singleton(1L)).getName());

        TestFileUtils.copyDirectoryNonCVS(new File(TestInfo.WORKING_DIR, "2-3-cache"),
                new File(new File(Settings.get(Settings.CACHE_DIR), "TrivialJobIndexCache"),
                        "2-3-cache"));
        Set<Long> jobs = new HashSet<Long>();
        jobs.add(2L);
        jobs.add(3L);
        assertEquals("Should give the expected cache with the right jobs",
                "2-3-DEDUP_CRAWL_LOG-cache", cache.getIndex(jobs).getName());

    }
}