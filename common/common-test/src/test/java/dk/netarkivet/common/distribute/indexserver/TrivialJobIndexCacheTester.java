/*
 * #%L
 * Netarchivesuite - common - test
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
package dk.netarkivet.common.distribute.indexserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Unit tests for the TrivialJobIndexCache class.
 */
public class TrivialJobIndexCacheTester {
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
    ReloadSettings rs = new ReloadSettings();

    @Before
    public void setUp() {
        rs.setUp();
        Settings.set(CommonSettings.CACHE_DIR, TestInfo.WORKING_DIR.getAbsolutePath());
        mtf.setUp();
    }

    @After
    public void tearDown() {
        mtf.tearDown();
        rs.tearDown();
    }

    @Test
    public void testCacheData() throws Exception {
        JobIndexCache cache = new TrivialJobIndexCache(RequestType.DEDUP_CRAWL_LOG);
        try {
            cache.getIndex(Collections.singleton(1L)).getIndexFile().getName();
            fail("Expected IOFailure on non-existing cache file");
        } catch (IOFailure e) {
            // expected
        }

        Set<Long> jobs = new HashSet<Long>();
        jobs.add(2L);
        jobs.add(3L);
        assertEquals("Should give the expected cache with the right jobs", "2-3-DEDUP_CRAWL_LOG-cache",
                cache.getIndex(jobs).getIndexFile().getName());

    }
}
