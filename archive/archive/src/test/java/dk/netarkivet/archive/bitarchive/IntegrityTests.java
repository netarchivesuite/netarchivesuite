/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.archive.bitarchive;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.bitarchive.distribute.BitarchiveServer;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionTestMQ;
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * lc forgot to comment this!
 *
 */

public class IntegrityTests extends TestCase {
    /** The archive directory to work on.
     */
    private static final File ARCHIVE_DIR =
            new File("tests/dk/netarkivet/bitarchive/data/upload/working/");
    /** The archive that this test queries.
     */
    private static Bitarchive archive;

    /** The external interface */
    private static BitarchiveServer server;

    /** The directory from where we upload the ARC files.
     *
     */
    private static final File ORIGINALS_DIR =
            new File("tests/dk/netarkivet/bitarchive/data/upload/originals/");
    /** The files that are uploaded during the tests and that must be removed
     * afterwards.
     */
    private static final List<String> UPLOADED_FILES =
            Arrays.asList(new String[] { "Upload1.ARC",
                                         "Upload2.ARC",
                                         "Upload3.ARC" });
    ReloadSettings rs = new ReloadSettings();


    /** Construct a new tester object. */
    public IntegrityTests(final String sTestName) {
        super(sTestName);
    }

    /** At start of test, set up an archive we can run against.
     *
     */
    public void setUp() {
        rs.setUp();
        FileUtils.removeRecursively(ARCHIVE_DIR);
        Settings.set(CommonSettings.REMOTE_FILE_CLASS, TestRemoteFile.class.getName());
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, ARCHIVE_DIR.getAbsolutePath());
        archive = Bitarchive.getInstance();
        JMSConnectionTestMQ.useJMSConnectionTestMQ();
        server = BitarchiveServer.getInstance();
    }

    /** At end of test, remove any files we managed to upload.
     *
     */
    public void tearDown() {
        archive.close();
        FileUtils.removeRecursively(ARCHIVE_DIR);
        if (server != null) {
            server.close();
        }
        rs.tearDown();
        //FileUtils.removeRecursively(new File(ARCHIVE_DIR));
    }

    private void setupBitarchiveWithDirs(final String[] dirpaths) {
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, dirpaths);
        // Don't like the archive made in setup, try again:)
        archive.close();
        archive = Bitarchive.getInstance();
    }


    /** Verify that we spill into the next directory
     * This test requires special setup to run.
     */
    public void testUploadChangesDirectory() {
        final File dir1 = new File(ARCHIVE_DIR, "dir1");
        final File dir2 = new File(ARCHIVE_DIR, "dir2");
        setupBitarchiveWithDirs(new String[] {
            dir1.getAbsolutePath(),
            dir2.getAbsolutePath()
        });
        archive.upload(new TestRemoteFile(new File(ORIGINALS_DIR,
                (String)UPLOADED_FILES.get(0)), false, false, false),
                (String)UPLOADED_FILES.get(0));
        assertTrue("Should place first file in first directory",
                new File(new File(dir1, "filedir"), (String)UPLOADED_FILES.get(0)).exists());
        archive.upload(new TestRemoteFile(new File(ORIGINALS_DIR,
                (String)UPLOADED_FILES.get(2)), false, false, false),
                (String)UPLOADED_FILES.get(2));
        assertTrue("Should place second file in second directory",
                new File(new File(dir2, "filedir"), (String)UPLOADED_FILES.get(2)).exists());
    }

    /**
     * Verify that we get appropriate errors when we don't have enough space.
     * This test requires a special setup before actual out of disk space
     * errors will occur.
     */
    public void testUploadNoSpace() {
        long freeSpace = FileUtils.getBytesFree(ARCHIVE_DIR);
        final File localFile2 = new File(ORIGINALS_DIR,
                (String)UPLOADED_FILES.get(2));
        Settings.set(ArchiveSettings.BITARCHIVE_MIN_SPACE_LEFT, "" + (freeSpace - localFile2.length() - 1));
        final File dir1 = new File(ARCHIVE_DIR, "dir1");
        setupBitarchiveWithDirs(new String[] {
            dir1.getAbsolutePath(),
        });
        JMSConnectionTestMQ con = (JMSConnectionTestMQ) JMSConnectionTestMQ.getInstance();
        assertEquals("We should listen to ANY_BA at the start",
                1, con.getListeners(Channels.getAnyBa()).size());
        // Try big file
        try {
            final File localFile1 = new File(ORIGINALS_DIR,
                    (String)UPLOADED_FILES.get(1));
            archive.upload(new TestRemoteFile(localFile1, false, false, false), localFile1.getName());
            fail("Should have thrown IOFailure when uploading " + localFile1.length() + " bytes");
        } catch (IOFailure e) {
            // Expected
        }
        // Upload small file
        archive.upload(new TestRemoteFile(localFile2, false, false, false), localFile2.getName());
        assertTrue("Should have room for file of " + localFile2 + " bytes in first directory",
                new File(new File(dir1, "filedir"), (String)UPLOADED_FILES.get(2)).exists());
        // Try another small file, big enough to die.
        try {
            final File localFile0 = new File(ORIGINALS_DIR,
                    (String)UPLOADED_FILES.get(0));
            archive.upload(new TestRemoteFile(localFile0, false, false, false), localFile0.getName());
            fail("Should have thrown IOFailure when second file, " + localFile0.length());
        } catch (IOFailure e) {
            // Expected
        }
    }
}
