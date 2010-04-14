/* $Id$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *   USA
 */
package dk.netarkivet.wayback.indexer;

import java.io.File;

import junit.framework.TestCase;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.LocalArcRepositoryClient;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.wayback.TestInfo;
import dk.netarkivet.wayback.WaybackSettings;
import dk.netarkivet.testutils.TestFileUtils;

public class ArchiveFileTester extends IndexerTestCase {
   /* private String oldClient = System.getProperty(CommonSettings.ARC_REPOSITORY_CLIENT);
    private String oldFileDir = System.getProperty("settings.common.arcrepositoryClient.fileDir");

    public void setUp() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
        System.setProperty(CommonSettings.ARC_REPOSITORY_CLIENT, "dk.netarkivet.common.distribute.arcrepository.LocalArcRepositoryClient");
        System.setProperty("settings.common.arcrepositoryClient.fileDir", TestInfo.WORKING_DIR.getAbsolutePath());
        assertTrue(ArcRepositoryClientFactory.getPreservationInstance() instanceof LocalArcRepositoryClient);
        FileUtils.removeRecursively(tempdir);
        FileUtils.createDir(tempdir);
    }


    public void tearDown() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        FileUtils.removeRecursively(tempdir);
        FileUtils.remove(TestInfo.LOG_FILE);
        if (oldClient != null) {
            System.setProperty(CommonSettings.ARC_REPOSITORY_CLIENT, oldClient);
        } else {
            System.setProperty(CommonSettings.ARC_REPOSITORY_CLIENT, "");
        }
        if (oldFileDir != null ) {
            System.setProperty("settings.common.arcrepositoryClient.fileDir", oldFileDir);
        } else {
            System.setProperty("settings.common.arcrepositoryClient.fileDir", "");
        }
    }
*/

    @Override
    public void setUp() {
        super.setUp();
        File destDir = new File(Settings.get(WaybackSettings.WAYBACK_INDEX_TEMPDIR) );
        FileUtils.removeRecursively(destDir);
    }

    @Override
    public void tearDown() {
        super.tearDown();
        File destDir = new File(Settings.get(WaybackSettings.WAYBACK_INDEX_TEMPDIR) );
        FileUtils.removeRecursively(destDir);
    }

    /**
     * Test indexing on an archive arcfile
     */
    public void testIndexerArc() {
        ArchiveFile file = new ArchiveFile();
        file.setFilename("arcfile_withredirects.arc");
        (new ArchiveFileDAO()).create(file);
        file.index();
        File outputFile = new File(tempdir,
                                   file.getOriginalIndexFileName());
        assertTrue("Should have a resonable numer of lines in output file",
                   FileUtils.countLines(outputFile)>5);
    }

    /**
     * Test indexing on a metadata arcfile
     */
    public void testIndexerMetadata() {
        ArchiveFile file = new ArchiveFile();
        file.setFilename("duplicate.metadata.arc");
        (new ArchiveFileDAO()).create(file);
        file.index();
        File outputFile = new File(tempdir,
                                   file.getOriginalIndexFileName());
        assertTrue("Should have a resonable numer of lines in output file",
                   FileUtils.countLines(outputFile) == 15);
    }


}
