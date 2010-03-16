/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import dk.netarkivet.common.utils.FileUtils;
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