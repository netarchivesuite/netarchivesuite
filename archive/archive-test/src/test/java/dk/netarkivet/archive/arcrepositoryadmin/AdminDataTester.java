/*
 * #%L
 * Netarchivesuite - archive - test
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
package dk.netarkivet.archive.arcrepositoryadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ch.qos.logback.classic.Level;
import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.arcrepository.distribute.StoreMessage;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.ClassAsserts;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogbackRecorder;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * This class tests AdminData save/store methods in general.
 */
@SuppressWarnings({"deprecation"})
public class AdminDataTester {

    /**
     * Test instance.
     */
    UpdateableAdminData ad;

    /**
     * A dummy file name.
     */
    private String myFile;
    ReloadSettings rs = new ReloadSettings();

    @Before
    public void setUp() throws IOException {
        rs.setUp();
        Settings.set(CommonSettings.REMOTE_FILE_CLASS, TestRemoteFile.class.getName());
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        FileUtils.removeRecursively(TestInfo.TEST_DIR);
        FileUtils.createDir(TestInfo.TEST_DIR);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.ARCHIVE_DIR1);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.ARCHIVE_DIR2);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.NON_EMPTY_ADMIN_DATA_DIR_ORIG, TestInfo.NON_EMPTY_ADMIN_DATA_DIR);
        Settings.set(ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN, TestInfo.TEST_DIR.getAbsolutePath());
        myFile = "arcfileNameForTests";
    }

    @After
    public void tearDown() {
        if (ad != null) {
            ad.close();
        }
        FileUtils.removeRecursively(TestInfo.TEST_DIR);
        rs.tearDown();
    }

    @Test
    public void testSingleton() {
        ClassAsserts.assertSingleton(UpdateableAdminData.class);
        // init ad to make sure it is closed in teardown.
        ad = UpdateableAdminData.getUpdateableInstance();
    }

    /**
     * Verifies that setReplyInfo(), hasReplyInfo() and getAndRemoveReplyInfo() work as expected.
     *
     * @throws IOException
     */
    @Test
    public void testReplyInfoOperations() throws IOException {
        LogbackRecorder lr = LogbackRecorder.startRecorder();
        ad = UpdateableAdminData.getUpdateableInstance();
        assertFalse("No replyInfo has been set", ad.hasReplyInfo(myFile));
        StoreMessage myReplyInfo = new StoreMessage(Channels.getError(), Files.createTempFile("dummy", "dummy").toFile());
        // ArchiveStoreState dummyGeneralState = new ArchiveStoreState(BitArchiveStoreState.UPLOAD_STARTED);
        ad.addEntry(myFile, myReplyInfo, "checksum");
        assertTrue("replyInfo has been set", ad.hasReplyInfo(myFile));
        assertEquals("Wrong replyInfo returned", myReplyInfo, ad.removeReplyInfo(myFile));
        try {
            ad.removeReplyInfo(myFile);
            fail("Should not be able to remove replyInfo twice");
        } catch (UnknownID e) {
            // Expected
        }
        try {
            ad.removeReplyInfo("Some other file");
            fail("replyInfo not set for this file");
        } catch (UnknownID e) {
            // Expected
        }
        ad.setReplyInfo(myFile, myReplyInfo);
        ad.setReplyInfo(myFile, myReplyInfo);
        // Should give a warning in log.
        lr.assertLogContains("Should be a warning in the log", "Overwriting replyInfo");
        lr.stopRecorder();
    }

    /**
     * Verifies that setChecksum(), getChecksum() work as expected.
     */
    @Test
    public void testChecksumOperations() {
        ad = UpdateableAdminData.getUpdateableInstance();
        assertFalse("Entry for file: " + myFile + " already exists", ad.hasEntry(myFile));
        String myChecksum = "Dummy checksum";
        // ArchiveStoreState dummyGeneralState = new ArchiveStoreState(BitArchiveStoreState.UPLOAD_STARTED);
        ad.addEntry(myFile, null, myChecksum);
        assertTrue("Entry for File: " + myFile + " should exist now.", ad.hasEntry(myFile));
        assertEquals("Should get just set checksum", myChecksum, ad.getCheckSum(myFile));
        try {
            ad.getCheckSum("some other file");
            fail("Checksum not set for this file");
        } catch (UnknownID e) {
            // Expected
        }
        // Resetting a wrong checksum is allowed:
        ad.setCheckSum(myFile, "some other checksum");
        assertEquals("Should get just set checksum", "some other checksum", ad.getCheckSum(myFile));
    }

    /**
     * Verifies that setState() and getState() work as expected.
     *
     * @throws FileNotFoundException
     */
    @Test
    public void testStoreStateOperations() throws FileNotFoundException {
        ad = UpdateableAdminData.getUpdateableInstance();
        String myBA = "Test ID of bit archive";
        // TODO: Needs to incorporate the timestamps, and generalstate into this test
        // ArchiveStoreState dummyGeneralState = new ArchiveStoreState(BitArchiveStoreState.UPLOAD_STARTED);
        ad.addEntry(myFile, null, "checksum");
        assertFalse("No state set for " + myFile + " " + myBA, ad.hasState(myFile, myBA));
        ad.setState(myFile, myBA, ReplicaStoreState.UPLOAD_STARTED);
        assertEquals("Wrong store state returned", ReplicaStoreState.UPLOAD_STARTED, ad.getState(myFile, myBA));
        ad.setState(myFile, myBA, ReplicaStoreState.UPLOAD_FAILED);
        assertEquals("Wrong store state returned", ReplicaStoreState.UPLOAD_FAILED, ad.getState(myFile, myBA));
        assertTrue("State has been set for " + myFile + " " + myBA, ad.hasState(myFile, myBA));
        try {
            ad.getState("some other file", myBA);
            fail("State not set for this file");
        } catch (UnknownID e) {
            // Expected
        }
    }

    /**
     * Verifies that after closing an AdminData and constructing a new one: - Checksums are the same - Store states are
     * the same - replyInfoObjects are removed.
     *
     * @throws IOException
     */
    @Test
    public void testPersistence() throws IOException {
        ad = UpdateableAdminData.getInstance();
        StoreMessage myReplyInfo = new StoreMessage(Channels.getError(), Files.createTempFile("dummy", "dummy").toFile());
        String myChecksum = "Dummychecksum";
        ad.addEntry(myFile, myReplyInfo, myChecksum);
        String myBA = "TestIDofbitarchive";
        ad.setState(myFile, myBA, ReplicaStoreState.UPLOAD_STARTED);
        ad.close();
        ad = UpdateableAdminData.getInstance();
        assertFalse("replyInfos should not be persistent", ad.hasReplyInfo(myFile));
        try {
            ad.removeReplyInfo(myFile);
            fail("replyInfos should be nulled after reload");
        } catch (UnknownID e) {
            // Expected
        }
        assertTrue("Checksum should be persistent", ad.hasEntry(myFile));
        assertEquals("Checksum should be persistent", myChecksum, ad.getCheckSum(myFile));
        assertEquals("Store state should be persistent", ReplicaStoreState.UPLOAD_STARTED, ad.getState(myFile, myBA));
    }

    /**
     * Tests that admin data starts with an empty or no log.
     *
     * @throws IOException
     */
    @Test
    public void testAdminDataEmptylog() throws IOException {
        LogbackRecorder lr = LogbackRecorder.startRecorder();
        ad = UpdateableAdminData.getUpdateableInstance();
        if (!lr.isEmpty()) {
            int index = lr.logIndexOf("AdminData created", 0);
            Assert.assertTrue("Should contain starting entry", index > -1);
            int index2 = lr.logIndexOf("Starting AdminData", index + 1);
            Assert.assertFalse("Should contain starting entry", index2 > -1);
            index2 = lr.logIndexOf("<record>", index + 1);
            assertFalse("Log contains further entries - it should not at this point !", index2 > -1);
        }
        /*
         * LogUtils.flushLogs(UpdateableAdminData.class.getName()); final File logfile = TestInfo.LOG_DIR; if
         * (logfile.exists()) { String logtxt = FileUtils.readFile(logfile);
         * FileAsserts.assertFileContains("Should contain starting entry", "AdminData created", logfile); int index =
         * logtxt.indexOf("Starting AdminData");
         * assertFalse("Log contains further entries - it should not at this point !", logtxt.indexOf("<record>", index)
         * > -1); }
         */
        lr.stopRecorder();
    }

    /**
     * Test that admin state transitions work correctly.
     */
    @Test
    public void testBitArchiveStoreState() {
        // TODO: incorporate the timestamps, and generalState into this unit-test
        ad = UpdateableAdminData.getUpdateableInstance();
        File file = new File(TestInfo.ORIGINALS_DIR, (String) TestInfo.GETTABLE_FILENAMES.get(0));
        String filename = file.getName();
        String bitArchiveID = "bitArchiveID";
        // ArchiveStoreState dummyGeneralState = new ArchiveStoreState(BitArchiveStoreState.UPLOAD_STARTED);
        ad.addEntry(filename, null, "checksum");

        try {
            // no state has been set yet:
            ad.getState(filename, bitArchiveID);
            fail("An UnknownID should have been thrown - no state has been set yet");
        } catch (UnknownID unknown) {
            // expected
        }

        ad.setState(filename, bitArchiveID, ReplicaStoreState.UPLOAD_STARTED);
        assertEquals("BitArchiveStoreState set - expected ", ReplicaStoreState.UPLOAD_STARTED,
                ad.getState(filename, bitArchiveID));

        ad.setState(filename, bitArchiveID, ReplicaStoreState.DATA_UPLOADED);
        assertEquals("BitArchiveStoreState set - expected ", ReplicaStoreState.DATA_UPLOADED,
                ad.getState(filename, bitArchiveID));

        ad.setState(filename, bitArchiveID, ReplicaStoreState.UPLOAD_COMPLETED);
        assertEquals("BitArchiveStoreState set - expected ", ReplicaStoreState.UPLOAD_COMPLETED,
                ad.getState(filename, bitArchiveID));

        ad.setState(filename, bitArchiveID, ReplicaStoreState.UPLOAD_FAILED);
        assertEquals("BitArchiveStoreState set - expected ", ReplicaStoreState.UPLOAD_FAILED,
                ad.getState(filename, bitArchiveID));
    }

    /**
     * Verify that constructing an AdminData does not fail.
     */
    @Test
    public void testCTOR() {
        // Test invalid settings:
        Settings.set(ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN, "/foo/bar/nonExistingDir");
        try {
            ad = UpdateableAdminData.getUpdateableInstance();
            fail("Should have thrown PermissionDenied");
        } catch (PermissionDenied ioFailure) {
            // expected
        }
    }

    /**
     * Test that we can read the set of all files stored in admin.data.
     */
    @Test
    public void testGetAllFiles() {
        Settings.set(ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN, TestInfo.NON_EMPTY_ADMIN_DATA_DIR.getAbsolutePath());
        ad = UpdateableAdminData.getUpdateableInstance();
        Set<String> allFiles = ad.getAllFileNames();
        Set<String> expectedFiles = new HashSet<String>();
        expectedFiles.addAll(Arrays.asList(TestInfo.files));
        assertEquals("List of files read in should be as expected", expectedFiles, allFiles);
    }

    /**
     * Test that the admin data is written in a journalling style. Fixed bug #324.
     *
     * @throws IOException
     * @throws FileNotFoundException
     */
    @Test
    public void testWriteJournalling() throws IOException, FileNotFoundException {
        // ArchiveStoreState dummyGeneralState = new ArchiveStoreState(BitArchiveStoreState.UPLOAD_STARTED);
        File datafile = new File(Settings.get(ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN), "admin.data");
        ad = UpdateableAdminData.getUpdateableInstance();
        // System.out.println("datafile at step 0: " + FileUtils.readFile(datafile));
        final String filename = TestInfo.files[0];
        FileAsserts.assertFileNotContains("File " + filename + " should not be in admin data before adding", datafile,
                filename);
        ad.addEntry(filename, null, "foobar");
        FileAsserts
                .assertFileContains("File " + filename + " should be in admin data after adding", filename, datafile);
        ad.setState(filename, "DummyBA1", ReplicaStoreState.UPLOAD_COMPLETED);
        FileAsserts.assertFileNumberOfLines("AdminData should have an extra " + "line after changing state", datafile,
                3);
        ad.setState(filename, "DummyBA1", ReplicaStoreState.UPLOAD_FAILED);
        FileAsserts.assertFileNumberOfLines("AdminData should have an extra " + "line after changing state", datafile,
                4);
        ad.setCheckSum(filename, "otherChecksum");
        FileAsserts.assertFileNumberOfLines("AdminData should be reduced after " + "changing checksum", datafile, 2);
        FileAsserts.assertFileContains("Should have new checksum only after changing", "otherChecksum", datafile);
        ad.setState(filename, "DummyBA2", ReplicaStoreState.UPLOAD_COMPLETED);
        FileAsserts.assertFileNumberOfLines("AdminData should have an extra " + "line after changing state", datafile,
                3);
        // System.out.println("datafile before closing " + FileUtils.readFile(datafile));
        UpdateableAdminData.getUpdateableInstance().close();
        ad = UpdateableAdminData.getUpdateableInstance();
        // System.out.println("datafile at step 2: " + FileUtils.readFile(datafile));
        FileAsserts.assertFileNumberOfLines("AdminData should be reduced after " + "making a new AdminData", datafile,
                2);
        FileAsserts.assertFileContains("Should have state for bitarchive1", "DummyBA1 UPLOAD_FAILED", datafile);
        FileAsserts.assertFileContains("Should have state for bitarchive2", "DummyBA2 UPLOAD_COMPLETED", datafile);
        String filename2 = TestInfo.files[1];
        ad.addEntry(filename2, null, "aChecksum");
        ad.setState(filename2, "DummyBA1", ReplicaStoreState.UPLOAD_STARTED);
        ad.setState(filename, "DummyBA1", ReplicaStoreState.UPLOAD_COMPLETED);
        ad.setState(filename2, "DummyBA2", ReplicaStoreState.DATA_UPLOADED);
        FileAsserts.assertFileNumberOfLines("Must have 6 lines when having two" + " files and 3 changes", datafile, 6);
        // close to force new instance
        ad.close();
        ad = UpdateableAdminData.getUpdateableInstance();
        FileAsserts.assertFileNumberOfLines("AdminData should be reduced after " + "making a new AdminData", datafile,
                3);

        // Check that the admin data is still aware of the files.
        assertTrue("Should have the first test filename: " + TestInfo.files[0],
                ad.toString().contains(TestInfo.files[0]));
        assertTrue("Should have the first test filename: " + TestInfo.files[1],
                ad.toString().contains(TestInfo.files[1]));
    }

    /**
     * Test that a valid file can be read, and that an invalid file gives appropriate log entries and invalid entries.
     */
    @Test
    public void testReadCurrentVersion() throws Exception {
        LogbackRecorder lr = LogbackRecorder.startRecorder();
        File datafile = new File(Settings.get(ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN), "admin.data");
        // Note: the file 'datafile' does not exist at this point in time.

        ad = UpdateableAdminData.getUpdateableInstance();
        // close to force new instance
        ad.close();
        // Now datafile contains one line with contents "0.4"
        FileAsserts.assertFileNumberOfLines("Should only contain only one line now, i.e. the version number line",
                datafile, 1);
        FileAsserts.assertFileContains("Should contain the versionnumber", "0.4", datafile);

        final String filename1 = "foobar";
        String checksum1 = "xxx";
        ArchiveStoreState dummyStoreState = new ArchiveStoreState(ReplicaStoreState.UPLOAD_STARTED);
        addLineToFile(datafile, filename1 + " " + checksum1 + " " + dummyStoreState.toString());

        lr.reset();

        ad = UpdateableAdminData.getUpdateableInstance();
        assertTrue("New entry should turn up", ad.hasEntry(filename1));
        assertEquals("New entry should have stated checksum", ad.getCheckSum(filename1), checksum1);
        // Can't assume log file exists, but don't want to check it. So just
        // ensure it's there if it wasn't already.
        // FileAsserts.assertFileNotContains("Should have no warning in log", TestInfo.LOG_DIR, "WARNING");
        lr.assertLogNotContainsLevel("Should have no warning in log", Level.WARN);
        final String ba1 = "ba1";
        // close to force new instance
        ad.close();
        ArchiveStoreState dummyGeneralStoreState = new ArchiveStoreState(ReplicaStoreState.UPLOAD_COMPLETED, new Date(
                1126627010110L));
        ArchiveStoreState dummyBitarchiveStoreState = new ArchiveStoreState(ReplicaStoreState.UPLOAD_COMPLETED,
                new Date(1126627010132L));

        addLineToFile(datafile, filename1 + " " + checksum1 + " " + dummyGeneralStoreState + " , " + ba1 + " "
                + dummyBitarchiveStoreState);

        ad = UpdateableAdminData.getUpdateableInstance();
        // close to force new instance
        ad.close();
        assertTrue("Changed entry should turn up", ad.hasEntry(filename1));
        assertEquals("Changed entry should have right state", ReplicaStoreState.UPLOAD_COMPLETED,
                ad.getState(filename1, ba1));
        // LogUtils.flushLogs(UpdateableAdminData.class.getName());
        /*
         * FileAsserts.assertFileNotContains("Should have no warning in log", TestInfo.LOG_DIR, "WARNING");
         */
        lr.assertLogNotContainsLevel("Should have no warning in log", Level.WARN);
        String filename2 = "barfu";

        String checksum2 = "yyy";
        addLineToFile(datafile, filename2 + " " + checksum2 + " " + dummyGeneralStoreState + " , " + ba1 + " "
                + dummyBitarchiveStoreState);
        ad = UpdateableAdminData.getUpdateableInstance();
        assertTrue("Old entry should turn up", ad.hasEntry(filename1));
        assertEquals("Old entry should have right state", ReplicaStoreState.UPLOAD_COMPLETED,
                ad.getState(filename1, ba1));

        /*
         * LogUtils.flushLogs(AdminData.class.getName()); FileAsserts.assertFileContains("Should have a warning in log",
         * TestInfo.LOG_DIR, "WARNING: Corrupt admin");
         * 
         * try { ad.setState(filename2, ba1, BitArchiveStoreState.DATA_UPLOADED);
         * fail("Should have thrown ArgumentNotValid, because the entry exists but is invalid"); } catch
         * (ArgumentNotValid e) { // expected }
         */

        ad.setCheckSum(filename1, checksum1);
        // close to force new instance
        ad.close();
        ad = UpdateableAdminData.getUpdateableInstance();
        assertEquals("Old invalid entry should have same checksum", checksum2, ad.getCheckSum(filename2));
        // close to force new instance
        ad.close();
        ad = UpdateableAdminData.getUpdateableInstance();
        FileAsserts.assertFileNumberOfLines("There should be two entries and one version number in the file", datafile,
                2 + 1);
        lr.stopRecorder();
    }

    @Test
    public void testMigrateOldToCurrentVersion() throws Exception {

        File old_version_admindata = new File(TestInfo.VERSION_03_ADMIN_DATA_DIR_ORIG, "admin.data");
        File datafile = new File(Settings.get(ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN), "admin.data");

        FileUtils.copyFile(old_version_admindata, datafile);
        ad = UpdateableAdminData.getUpdateableInstance();
        assertEquals("We should now have 2 entries", 2, ad.getAllFileNames().size());
        ad.close();

        // We now should have migrated the old 0.3 admindata to 3 lines of 0.4 admindata
        // with one version-number line, and 2 normal entries

        FileAsserts.assertFileNumberOfLines("Should contain 3 lines now, including the version number line", datafile,
                3);
    }

    private void addLineToFile(File datafile, String s) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(datafile, true));
        writer.println(s);
        writer.close();
    }

}
