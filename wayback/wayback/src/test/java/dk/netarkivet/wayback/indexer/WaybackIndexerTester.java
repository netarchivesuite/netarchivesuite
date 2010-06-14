/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package dk.netarkivet.wayback.indexer;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.wayback.TestInfo;
import dk.netarkivet.wayback.WaybackSettings;

import junit.framework.TestCase;

public class WaybackIndexerTester extends IndexerTestCase {

    File originals = new File("tests/dk/netarkivet/wayback/indexer/data/originals");
    File working = new File("tests/dk/netarkivet/wayback/indexer/data/working");

     public void setUp() {
         super.setUp();
        FileUtils.removeRecursively(working);
        TestFileUtils.copyDirectoryNonCVS(originals, working);
      }


    public void tearDown() {
        super.tearDown();
        FileUtils.removeRecursively(working);
    }

    /**
     * ingestInitialFiles should return without doing anything if the
     * specified file is an empty string.
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
     public void testIngestInitialFilesBlankSetting()
            throws NoSuchMethodException, InvocationTargetException,
                   IllegalAccessException {
        System.setProperty(WaybackSettings.WAYBACK_INDEXER_INITIAL_FILES, "");
        Method ingestMethod = WaybackIndexer.class.getDeclaredMethod("ingestInitialFiles");
        ingestMethod.setAccessible(true);
        ingestMethod.invoke(null);
    }

    public void testIngestInitialFiles()
            throws NoSuchMethodException, InvocationTargetException,
                   IllegalAccessException {
        String file = (new File(working, "initialfiles"))
                .getAbsolutePath();
        System.setProperty(WaybackSettings.WAYBACK_INDEXER_INITIAL_FILES, file);
        Method ingestMethod = WaybackIndexer.class.getDeclaredMethod("ingestInitialFiles");
        ingestMethod.setAccessible(true);
        ingestMethod.invoke(null);
        ArchiveFileDAO dao = new ArchiveFileDAO();
        assertTrue("Three file should have been ingested", dao.exists("1.arc") &&
                    dao.exists("2.arc") && dao.exists("3.arc"));
        assertTrue("Should be no files awaiting indexing",
                   dao.getFilesAwaitingIndexing().isEmpty());

    }
}
