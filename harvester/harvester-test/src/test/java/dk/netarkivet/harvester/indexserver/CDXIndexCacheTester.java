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

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.FileAsserts;

/**
 * Unit test(s) for the CDXIndexCache class.
 */
public class CDXIndexCacheTester extends CacheTestCase {
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
        FileAsserts.assertFileNumberOfLines(
                "Should have files 3 and 4",
                cacheFile,
                (int) FileUtils.countLines(TestInfo.METADATA_FILE_3)
                        + (int) FileUtils.countLines(TestInfo.METADATA_FILE_4));
        // Checks that lines are sorted: The original metadata3 file has a
        // metadatb line after the file 3 block 2 line.
        FileAsserts.assertFileContains("Must have lines sorted", "metadata file 3 block 2\nmetadata file 4 block 1",
                cacheFile);
    }
}
