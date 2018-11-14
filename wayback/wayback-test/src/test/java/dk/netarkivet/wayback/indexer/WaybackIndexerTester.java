/*
 * #%L
 * Netarchivesuite - wayback - test
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
package dk.netarkivet.wayback.indexer;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.wayback.WaybackSettings;

public class WaybackIndexerTester extends IndexerTestCase {

    File originals = new File("tests/dk/netarkivet/wayback/indexer/data/originals");
    File working = new File("tests/dk/netarkivet/wayback/indexer/data/working");

    @Before
    public void setUp() {
        super.setUp();
        FileUtils.removeRecursively(working);
        TestFileUtils.copyDirectoryNonCVS(originals, working);
    }

    @After
    public void tearDown() {
        super.tearDown();
        FileUtils.removeRecursively(working);
    }

    /**
     * ingestInitialFiles should return without doing anything if the specified file is an empty string.
     */
    @Test
    public void testIngestInitialFilesBlankSetting() throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        System.setProperty(WaybackSettings.WAYBACK_INDEXER_INITIAL_FILES, "");
        Method ingestMethod = WaybackIndexer.class.getDeclaredMethod("ingestInitialFiles");
        ingestMethod.setAccessible(true);
        ingestMethod.invoke(null);
    }

    @Test
    public void testIngestInitialFiles() throws NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        String file = (new File(working, "initialfiles")).getAbsolutePath();
        System.setProperty(WaybackSettings.WAYBACK_INDEXER_INITIAL_FILES, file);
        Method ingestMethod = WaybackIndexer.class.getDeclaredMethod("ingestInitialFiles");
        ingestMethod.setAccessible(true);
        ingestMethod.invoke(null);
        ArchiveFileDAO dao = new ArchiveFileDAO();
        assertTrue("Three file should have been ingested",
                dao.exists("1.arc") && dao.exists("2.arc") && dao.exists("3.arc"));
        assertTrue("Should be no files awaiting indexing", dao.getFilesAwaitingIndexing().isEmpty());

    }
}
