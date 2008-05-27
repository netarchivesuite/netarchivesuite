/* $Id$
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
package dk.netarkivet.common.distribute.arcrepository;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import dk.netarkivet.archive.arcrepository.bitpreservation.FileListJob;
import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;


/**
 * Unit-tests for the class
 * LocalArcRepositoryClient.
 */
public class LocalArcRepositoryClientTester extends TestCase {
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR,
                                          TestInfo.WORKING_DIR);
    ReloadSettings rs = new ReloadSettings();
    UseTestRemoteFile utrf = new UseTestRemoteFile();

    public LocalArcRepositoryClientTester(String s) {
        super(s);
    }

    public void setUp() {
        rs.setUp();
        utrf.setUp();

        Settings.set(Settings.DIR_COMMONTEMPDIR,
                     TestInfo.WORKING_DIR.getAbsolutePath());
        mtf.setUp();
    }

    public void tearDown() {
        mtf.tearDown();
        utrf.tearDown();
        rs.tearDown();
    }

    public void testStore() throws Exception {
        File dir1 = new File(TestInfo.WORKING_DIR, "dir1");
        File dir2 = new File(TestInfo.WORKING_DIR, "dir2");
        Settings.create("settings.common.arcrepositoryClient.fileDir",
                        dir1.getAbsolutePath(), dir2.getAbsolutePath());
        ArcRepositoryClient arcrep = new LocalArcRepositoryClient();

        FileUtils.copyFile(TestInfo.SAMPLE_FILE, TestInfo.SAMPLE_FILE_COPY);
        arcrep.store(TestInfo.SAMPLE_FILE_COPY);
        assertEquals("Should have stored file in one dir only",
                     1, dir1.list().length + dir2.list().length);
        assertTrue("Dir1 should contain file",
                   new File(dir1, TestInfo.SAMPLE_FILE_COPY.getName()).exists());
        // Try to store the same file again. This should provoke an IllegalState 
        // exception
        try {
            FileUtils.copyFile(TestInfo.SAMPLE_FILE, TestInfo.SAMPLE_FILE_COPY);
            arcrep.store(TestInfo.SAMPLE_FILE_COPY);
        } catch (IllegalState e) {
            // Expected
        }
        assertEquals("Should not have stored file twice",
                1, dir1.list().length + dir2.list().length);
        
        assertTrue("Dir1 should contain file",
                   new File(dir1, TestInfo.SAMPLE_FILE_COPY.getName()).exists());
        FileUtils.removeRecursively(dir1);
        FileUtils.copyFile(TestInfo.SAMPLE_FILE, TestInfo.SAMPLE_FILE_COPY);
        arcrep.store(TestInfo.SAMPLE_FILE_COPY);
        assertEquals("Should have stored file in other dir",
                     1, dir2.list().length);
        assertTrue("Dir1 should contain file",
                   new File(dir2, TestInfo.SAMPLE_FILE_COPY.getName()).exists());
    }

    public void testGetFile() throws IOException {
        File dir1 = new File(TestInfo.WORKING_DIR, "dir1");
        File dir2 = new File(TestInfo.WORKING_DIR, "dir2");
        Settings.create("settings.common.arcrepositoryClient.fileDir",
                        dir1.getAbsolutePath(), dir2.getAbsolutePath());
        ArcRepositoryClient arcrep = new LocalArcRepositoryClient();
        FileUtils.copyFile(TestInfo.SAMPLE_FILE, TestInfo.SAMPLE_FILE_COPY);
        arcrep.store(TestInfo.SAMPLE_FILE_COPY);
        assertFalse("Should have removed sample file original",
                    TestInfo.SAMPLE_FILE_COPY.exists());
        arcrep.getFile(TestInfo.SAMPLE_FILE_COPY.getName(),
                       Location.get(Settings.get(Settings.ENVIRONMENT_THIS_LOCATION)),
                       TestInfo.SAMPLE_FILE_COPY);
        assertTrue("Should have fetched sample file",
                   TestInfo.SAMPLE_FILE_COPY.exists());
        assertEquals("Should have same contents as original",
                     FileUtils.readFile(TestInfo.SAMPLE_FILE),
                     FileUtils.readFile(TestInfo.SAMPLE_FILE_COPY));
        try {
            arcrep.getFile("No Such File",
                           Location.get(Settings.get(Settings.ENVIRONMENT_THIS_LOCATION)),
                           TestInfo.SAMPLE_FILE_COPY);
            fail("Should have died on missing file");
        } catch (IOFailure e) {
            // expected
        }
    }

    public void testBatch() {
        File dir1 = new File(TestInfo.WORKING_DIR, "dir1");
        File dir2 = new File(TestInfo.WORKING_DIR, "dir2");
        Settings.create("settings.common.arcrepositoryClient.fileDir",
                        dir1.getAbsolutePath(), dir2.getAbsolutePath());
        ArcRepositoryClient arcrep = new LocalArcRepositoryClient();

        BatchStatus status = arcrep.batch(new FileListJob(), "BA");
        assertEquals("Should have no files processed at outset",
                     0, status.getNoOfFilesProcessed());
        FileUtils.copyFile(TestInfo.SAMPLE_FILE, TestInfo.SAMPLE_FILE_COPY);
        arcrep.store(TestInfo.SAMPLE_FILE_COPY);
        status = arcrep.batch(new FileListJob(), "BA");
        assertEquals("Should have one file processed at end",
                     1, status.getNoOfFilesProcessed());
    }
}
