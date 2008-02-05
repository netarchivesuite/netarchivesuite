/* File:     $Id$
 * Revision: $Revision$
 * Author:   $Author$
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
package dk.netarkivet.archive.arcrepository.bitpreservation;

import javax.jms.Message;
import javax.jms.MessageListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import dk.netarkivet.archive.arcrepository.distribute.JMSArcRepositoryClient;
import dk.netarkivet.archive.arcrepositoryadmin.AdminData;
import dk.netarkivet.archive.arcrepositoryadmin.ArcRepositoryEntry;
import dk.netarkivet.archive.arcrepositoryadmin.ArchiveStoreState;
import dk.netarkivet.archive.arcrepositoryadmin.MyArcRepositoryEntry;
import dk.netarkivet.archive.arcrepositoryadmin.UpdateableAdminData;
import dk.netarkivet.archive.bitarchive.BitarchiveAdmin;
import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.archive.bitarchive.distribute.BatchReplyMessage;
import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionTestMQ;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.distribute.StringRemoteFile;
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.BitArchiveStoreState;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.Location;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.arc.BatchLocalFiles;
import dk.netarkivet.common.utils.arc.FileBatchJob;
import dk.netarkivet.testutils.CollectionUtils;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.TestUtils;
import dk.netarkivet.testutils.preconfigured.MockupJMS;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/**
 */
public class FileBasedActiveBitPreservationTester extends TestCase {
    private UseTestRemoteFile rf = new UseTestRemoteFile();

    static boolean to_fail;
    FileBasedActiveBitPreservation abp;
    private static final File REFERENCE_DIR = new File(TestInfo.ORIGINALS_DIR,
                                                       "referenceFiles");
    // Dummy value to test, that constructor for FilePreservationStatus
    // does not allow null arguments, so the null second argument to DummyFPS is replaced by
    // fooAdmindatum.
    private ArcRepositoryEntry fooAdmindatum = new MyArcRepositoryEntry("filename", "md5", null);

    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR,
                                                  TestInfo.WORKING_DIR);
    private ReloadSettings rs = new ReloadSettings();
    private MockupJMS mj = new MockupJMS();

    private static final Location SB = Location.get("SB");
    private static final Location KB = Location.get("KB");

    protected void setUp() throws Exception {
        super.setUp();
        rs.setUp();
        ChannelsTester.resetChannels();
        mtf.setUp();
        mj.setUp();
        rf.setUp();

        to_fail = false;

        Settings.set(Settings.DIRS_ARCREPOSITORY_ADMIN,
                     TestInfo.WORKING_DIR.getAbsolutePath());
        Settings.set(Settings.DIR_ARCREPOSITORY_BITPRESERVATION,
                     TestInfo.WORKING_DIR.getAbsolutePath());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        // Make sure admin data instance is closed.
        UpdateableAdminData.getInstance().close();

        // Make sure the ArcRepositoryClient is closed.
        ArcRepositoryClientFactory.getPreservationInstance().close();

        // Close the ActiveBitPreservation if it was instantiated.
        if (abp != null) {
            abp.close();
        }

        rf.tearDown();
        mtf.tearDown();
        mj.tearDown();
        rs.tearDown();

    }

    private static Map<Location, List<String>> getChecksumMap(String filename) {
        Map<Location, List<String>> map = new HashMap<Location, List<String>>();
        for (Location location : Location.getKnown()) {
            map.put(location,
                    getChecksums(filename, location));
        }
        return map;

    }

    private static List<String> getChecksums(String filename, Location loc) {
        return FileBasedActiveBitPreservation.getInstance()
                            .getFilePreservationStatus(filename)
                            .getBitarchiveChecksum(loc);
    }

    /**
     * Tests the normal behaviour of findWrongFiles.
     *
     * This should check:
     *
     * That the two files are created with the expected output. What happens if
     * the expected input isn't there.
     *
     * @throws IOException
     */
    public void testFindWrongFiles() throws IOException {
        if (!TestUtils.runningAs("KFC")) {
            //Excluded while restructuring
            return;
        }
        File dir = REFERENCE_DIR;

        // We check the following four cases:
        // integrity1 is marked as failed, but is correct in bitarchives
        // integrity2 is missing from admin data, but exists in bitarchives
        // integrity7 is marked as completed, but is not in bitarchives
        // integrity11 has no state and wrong checksum
        // integrity12 is marked as completed but has wrong checksum
        // Note that generalState is ignored:)
        UpdateableAdminData ad = UpdateableAdminData.getUpdateableInstance();
        ad.addEntry("integrity1.ARC", null, "44ddf7a30f7fabb838e43a8505f927c2",
                    new ArchiveStoreState(BitArchiveStoreState.UPLOAD_FAILED));
        ad.addEntry("integrity11.ARC", null, "4236be8e67e0c10da2902764ff4b954a",
                    new ArchiveStoreState(
                            BitArchiveStoreState.UPLOAD_COMPLETED));
        ad.addEntry("integrity7.ARC", null, "44ddf7a30f7fabb838e43a8505f927c2",
                    new ArchiveStoreState(
                            BitArchiveStoreState.UPLOAD_COMPLETED));
        ad.addEntry("integrity12.ARC", null, "4236be8e67e0c10da2902764ff4b954a",
                    new ArchiveStoreState(BitArchiveStoreState.UPLOAD_FAILED));
        Location locationKB = Location.get("KB");
        ad.setState("integrity1.ARC",
                    locationKB.getChannelID().getName(),
                    BitArchiveStoreState.UPLOAD_FAILED);
        ad.setState("integrity7.ARC",
                    locationKB.getChannelID().getName(),
                    BitArchiveStoreState.UPLOAD_COMPLETED);
        ad.setState("integrity12.ARC",
                    locationKB.getChannelID().getName(),
                    BitArchiveStoreState.UPLOAD_COMPLETED);

        // First try an error case
        FileBasedActiveBitPreservation abp = FileBasedActiveBitPreservation.getInstance();
        try {
            abp.getChangedFiles(locationKB);
            fail("Should have thrown exception on missing files.");
        } catch (IllegalState e) {
            // Expected
        }
        abp.close();

        // Set up like we've run a checksum job
        File originals = new File(dir, "checksums");
        TestFileUtils.copyDirectoryNonCVS(new File(originals, "unsorted.txt"),
                                          WorkFiles.getFile(locationKB,
                                                            WorkFiles.CHECKSUMS_ON_BA));
        abp = FileBasedActiveBitPreservation.getInstance();
        abp.getChangedFiles(locationKB);

        // Check that wrong-files file exists and has correct content
        List<String> expectedContent = Arrays.asList
                ("integrity11.ARC", "integrity12.ARC");
        File wrong = WorkFiles.getFile(locationKB, WorkFiles.WRONG_FILES);
        List<String> actualContent = FileUtils.readListFromFile(wrong);
        Collections.sort(actualContent);
        assertEquals("Wrong state list should be as expected.\n"
                     + "Expected " + expectedContent
                     + " but was " + actualContent,
                     expectedContent, actualContent);

        // Check that wrong-state file exists and has correct content
        List<String> expectedContent2
                = Arrays.asList("integrity1.ARC");
        List<String> actualContent2 =
                WorkFiles.getLines(locationKB, WorkFiles.WRONG_STATES);
        Collections.sort(actualContent2);
        assertEquals("Wrong state list should be as expected.\n"
                     + "Expected " + expectedContent2
                     + " but was " + actualContent2,
                     expectedContent2, actualContent2);
        abp.close();
    }

    /**
     * Tests the normal behaviour of findMissingFiles.
     *
     * @throws IOException
     */
    public void testFindMissingFiles() throws IOException {
        if (!TestUtils.runningAs("KFC")) {
            //Excluded while restructuring
            return;
        }
        File dir = new File(TestInfo.WORKING_DIR, "referenceFiles");

        /* Set it up to look like we've run a listFiles job */
        File listingDir = new File(dir, "filelistOutput");
        Location locationSB = Location.get("SB");
        File listingFile = WorkFiles.getFile(locationSB,
                                             WorkFiles.FILES_ON_BA);
        TestFileUtils.copyDirectoryNonCVS(new File(listingDir, "unsorted.txt"),
                listingFile);

        FileBasedActiveBitPreservation abp = FileBasedActiveBitPreservation.getInstance();
        abp.findMissingFiles(locationSB);

        // Check that missing-files file exists and has correct content
        File missing = WorkFiles.getFile(locationSB,
                                         WorkFiles.MISSING_FILES_BA);
        String[] expectedContent = {"g.arc", "h.arc"};
        String[] actualContent = FileUtils.readFile(missing).split("\n");
        for (int i = 0; i < actualContent.length; i++) {
            actualContent[i] = actualContent[i].trim();
        }
        Arrays.sort(expectedContent);
        Arrays.sort(actualContent);
        // Compare sorted arrays
        assertTrue("Missing files list should be as expected."
                   + "\nExpected " + Arrays.toString(expectedContent)
                   + " but was " + Arrays.toString(actualContent),
                   Arrays.equals(expectedContent, actualContent));
        abp.close();
    }

    /**
     * Tests that the correct setup is done when running a checksum batch job
     * and that the checksum batch job generates the expected output.
     *
     * Tests requirements of Assigment 3.1.2
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void testRunChecksumJob()
            throws FileNotFoundException, IOException, NoSuchMethodException,
                   InvocationTargetException, IllegalAccessException {
        Method runChecksumJob = ReflectUtils.getPrivateMethod(
                FileBasedActiveBitPreservation.class, "runChecksumJob", String.class);

        DummyBatchMessageReplyServer dummy = new DummyBatchMessageReplyServer();

        FileBasedActiveBitPreservation acp = FileBasedActiveBitPreservation.getInstance();

        // Test valid parameters:
        try {
            runChecksumJob.invoke(acp, null);
            fail("Argument 'location' must not be null");
        } catch (ArgumentNotValid e) {
            // expected
        }

        Location location = Location.get(TestInfo.VALID_LOCATION);
        runChecksumJob.invoke(acp, location);

        File unsortedOutput = WorkFiles.getFile(location,
                                                WorkFiles.CHECKSUMS_ON_BA);
        assertTrue("No output file generated for unsorted output",
                   unsortedOutput.exists());

        /* No longer automatically sorting when not needed
        File sortedOutput = new File(outputSubDir, Constants.SORTED_OUTPUT_FILE);
        assertTrue("No output file generated for sorted output", sortedOutput.exists());
        */

        assertEquals("Unsorted output file should have been deleted from FTP " +
                     "server", 0, TestRemoteFile.remainingFiles().size());

        /*
        assertEquals("The two files containing unsorted output and sorted output do not have the same size",
                unsortedOutput.length(), sortedOutput.length());

        FileInputStream fis = new FileInputStream(sortedOutput);
        BufferedReader in = new BufferedReader(new InputStreamReader(fis));
        String line = null;
        String prevArcFileName = null;
        while ((line = in.readLine()) != null) {
            String arcFileName = line.substring(0, line.indexOf(Constants.STRING_FILENAME_SEPARATOR));
            if (prevArcFileName != null) {
                assertTrue("Batch output is not sorted (lexicographically) according to ARC file name",
                        arcFileName.compareTo(prevArcFileName) > 0);
            }
            prevArcFileName = arcFileName;
        }
        in.close();
        fis.close();
        */

        // ChecksumJobTester tests that the correct MD5 checksums are generated.
        acp.close();
    }

    public void testRunBatchJob() throws NoSuchMethodException,
                                         IllegalAccessException,
                                         InvocationTargetException {
        Method runBatchJob = ReflectUtils.getPrivateMethod(
                FileBasedActiveBitPreservation.class,
                "runBatchJob",
                FileBatchJob.class, Location.class, List.class, File.class);

        DummyBatchMessageReplyServer dummy = new DummyBatchMessageReplyServer();
        abp = FileBasedActiveBitPreservation.getInstance();
        FileListJob job = new FileListJob();
        File outputFile = new File(TestInfo.WORKING_DIR, "outputFile");
        runBatchJob.invoke(abp, job, Location.get("KB"),
                           null, outputFile);
        assertTrue("Output file should exist after successfull run",
                   outputFile.exists());
        to_fail = true;
        outputFile.delete();
        runBatchJob.invoke(abp, job, Location.get("KB"),
                           null, outputFile);
        assertFalse("Output file should not exist after failed run",
                    outputFile.exists());
    }

    public void testGetFilePreservationStatus()
            throws NoSuchFieldException, IllegalAccessException {
        // Must be able to get replies, even though we don't use them
        DummyBatchMessageReplyServer dummy = new DummyBatchMessageReplyServer();
        FileUtils.copyFile(TestInfo.CORRECT_ADMIN_DATA, TestInfo.ADMIN_DATA);
        // Ensure that the admin data are read from the file
        AdminData dummyad = AdminData.getUpdateableInstance();
        abp = FileBasedActiveBitPreservation.getInstance();
        FilePreservationStatus fps
                = abp.getFilePreservationStatus(TestInfo.FILE_IN_ADMIN_DATA);
        assertNotNull("Should get FilePreservationStatus for existing file",
                fps);
        Field fpsFilename = ReflectUtils.getPrivateField(FilePreservationStatus.class,
                "filename");
        assertEquals("Should get FPS for correct file",
                TestInfo.FILE_IN_ADMIN_DATA, fpsFilename.get(fps));

        fps = abp.getFilePreservationStatus(TestInfo.FILE_NOT_IN_ADMIN_DATA);
        assertNull("Should get null for non-existing file", fps);
    }

    /**
     * Test for bug #462: Should be able to run checksum jobs in either place.
     */
    public void testRunChecksumJobElsewhere() throws NoSuchFieldException,
                                                     IllegalAccessException,
                                                     NoSuchMethodException,
                                                     InvocationTargetException {
        Method runChecksumJob = ReflectUtils.getPrivateMethod(
                FileBasedActiveBitPreservation.class, "runChecksumJob", String.class);

        final FileBasedActiveBitPreservation abp = FileBasedActiveBitPreservation.getInstance();
        // Make a dummy arcrepclient that just drops the name into an array.
        final String[] location = new String[1];
        final Field f = JMSArcRepositoryClient.class.getDeclaredField(
                "instance");
        f.setAccessible(true);
        ArcRepositoryClient arc = new JMSArcRepositoryClient() {
            public BatchStatus batch(FileBatchJob job, String locationName) {
                location[0] = locationName;
                File file = new File(
                        new File(TestInfo.WORKING_DIR, "checksums"),
                        "unsorted.txt");
                file.getParentFile().mkdirs();
                try {
                    new FileWriter(file).close();
                } catch (IOException e) {
                    throw new IOFailure("Can't make empty file " + file, e);
                }
                return new BatchStatus(locationName, new HashSet<File>(), 0,
                                       new TestRemoteFile(file, to_fail,
                                                           to_fail, to_fail));
            }

            public void close() {
                try {
                    f.set(abp, null);
                } catch (IllegalAccessException e) {
                    throw new PermissionDenied("Can't reset arcrep client");
                }
            }
        };
        f.set(abp, arc);
        // Try to run "checksumjobs" on both allowable locations
        runChecksumJob.invoke(abp, Location.get("SB"));

        assertEquals("Checksum job should have run on SB", "SB",
                     location[0]);
        runChecksumJob.invoke(abp, Location.get("KB"));
        assertEquals("Checksum job should have run on KB", "KB",
                     location[0]);
        abp.close();
    }


    /**
     * Test normal behaviour of runFileListJob():
     *
     * It should pick normal or reference dir. It should generate the correct
     * files. It should restrict itself to specified files. It should check the
     * number of lines. It should remove the temporary file.
     *
     * Note that we don't need to test if the expected files are found, as the
     * file scanning is done in submethods, but it comes automatically when we
     * check for restriction.
     *
     * @throws IOException
     */
    public void testRunFileListJob() throws IOException, NoSuchMethodException,
                                            InvocationTargetException,
                                            IllegalAccessException {
        Method runFilelistJob = ReflectUtils.getPrivateMethod(
                FileBasedActiveBitPreservation.class, "runFilelistJob", String.class);

        FileBasedActiveBitPreservation abp
                = FileBasedActiveBitPreservation.getInstance();

        BitarchiveMonitorServerStub monitor = new BitarchiveMonitorServerStub();
        JMSConnectionTestMQ con
                = (JMSConnectionTestMQ) JMSConnectionFactory.getInstance();
        con.setListener(Channels.getTheArcrepos(), monitor);

        // Check normal run
        final String locationName = TestInfo.LOCATION_NAME;
        Location location = Location.get(locationName);
        final String otherLocationName = TestInfo.OTHER_LOCATION_NAME;
        Location otherLocation = Location.get(otherLocationName);
        runFilelistJob.invoke(abp, location);
        File normalOutputFile =
                WorkFiles.getFile(location, WorkFiles.FILES_ON_BA);
        File referenceOutputFile =
                WorkFiles.getFile(location, WorkFiles.FILES_ON_REFERENCE_BA);
        assertTrue("Output should exist", normalOutputFile.exists());
        assertFalse("Reference output should not exist",
                    referenceOutputFile.exists());
        normalOutputFile.delete();
        assertTrue("Last temp file should be gone",
                   monitor.lastRemoteFile.isDeleted());

        // Check that wrong counts are caught
        monitor.fakeCount = 17;
        runFilelistJob.invoke(abp, location);
        LogUtils.flushLogs(FileBasedActiveBitPreservation.class.getName());
        FileAsserts.assertFileContains("Should have warning about wrong count",
                                       "Number of files found (" + 4
                                       + ") does not"
                                       + " match with number reported by job (17)",
                                       TestInfo.LOG_FILE);

        abp.close();
    }

    public static class BitarchiveMonitorServerStub implements MessageListener {

        public TestRemoteFile lastRemoteFile;

        public int fakeCount = -1;

        public void onMessage(Message message) {
            BatchMessage baMsg = (BatchMessage) JMSConnection.unpack(message);
            assertEquals("", FileListJob.class, baMsg.getJob().getClass());
            File resultFile = null;
            try {
                resultFile = File.createTempFile("BAM_stub_result", "");
            } catch (IOException e) {
                throw new IOFailure("Error making temp file", e);
            }
            File[] arcfiles = new File(TestInfo.GOOD_ARCHIVE_DIR, "filedir").
                    listFiles(FileUtils.ARCS_FILTER);
            String outputString = "";
            int count = 0;
            for (File arcfile : arcfiles) {
                String name = arcfile.getName();
                if (baMsg.getJob().getFilenamePattern().matcher(
                        name).matches()) {
                    outputString += name + "\n";
                    count++;
                }
            }
            if (fakeCount != -1) {
                count = fakeCount;
            }
            FileUtils.writeBinaryFile(resultFile, outputString.getBytes());
            // Send reply
            lastRemoteFile = (TestRemoteFile) RemoteFileFactory
                    .getInstance(resultFile, true, true, true);
            BatchReplyMessage rep = new BatchReplyMessage(baMsg.getReplyTo(),
                                                          Channels.getError(),
                                                          baMsg.getReplyOfId(),
                                                          count,
                                                          null, lastRemoteFile);
            JMSConnectionFactory.getInstance().send(rep);
        }

    }

    public void testGetBitarchiveChecksum() throws Exception {
        if (!TestUtils.runningAs("KFC")) {
            //Excluded while restructuring
            return;
        }
        DummyChecksumBatchMessageReplyServer replyServerChecksum = new DummyChecksumBatchMessageReplyServer();

        // Test standard case
        replyServerChecksum.batchResult.put(SB, "foobar##md5-1");
        replyServerChecksum.batchResult.put(KB, "foobar##md5-2");
        FilePreservationStatus fps = new FilePreservationStatus("foobar", fooAdmindatum,
                                                                getChecksumMap(
                                                                        "foobar"));
        assertEquals("Should have expected size for SB",
                1, getChecksumMap("foobar").get(SB).size());
        assertEquals("Should have expected number of keys",
                     2, getChecksumMap("foobar").size());
        assertEquals("Should have expected value for SB",
                "md5-1", getChecksumMap("foobar").get(SB).get(0));
        assertEquals("Should have expected size for KB",
                1, getChecksumMap("foobar").get(KB).size());
        assertEquals("Should have expected value for KB",
                "md5-2", getChecksumMap("foobar").get(KB).get(0));

        // Test fewer checksums
        replyServerChecksum.batchResult.clear();
        replyServerChecksum.batchResult.put(SB, "");
        fps = new FilePreservationStatus("foobar", fooAdmindatum,
                                         getChecksumMap("foobar"));
        assertEquals("Should have expected number of keys",
                2, getChecksumMap("foobar").size());
        assertEquals("Should have expected size for SB",
                0, getChecksumMap("foobar").get(SB).size());
        assertEquals("Should have expected size for KB",
                0, getChecksumMap("foobar").get(KB).size());
        LogUtils.flushLogs(getClass().getName());
        FileAsserts.assertFileNotContains("Should have no warning about SB",
                TestInfo.LOG_FILE, "while asking Location SB");
        FileAsserts.assertFileNotContains("Should have no warning about KB",
                TestInfo.LOG_FILE, "while asking Location KB");

        // Test malformed checksums
        replyServerChecksum.batchResult.clear();
        replyServerChecksum.batchResult.put(SB, "foobar#klaf");
        replyServerChecksum.batchResult.put(KB, "foobarf##klaff");
        fps = new FilePreservationStatus("foobar", fooAdmindatum,
                                         getChecksumMap("foobar"));
        String s = "foobar";
        assertEquals("Should have expected number of keys",
                2, getChecksumMap(s).size());
        assertEquals("Should have expected size for SB",
                0, getChecksumMap("foobar").get(SB).size());
        assertEquals("Should have expected size for KB",
                0, getChecksumMap("foobar").get(KB).size());
        LogUtils.flushLogs(getClass().getName());
        FileAsserts.assertFileContains("Should have warning about SB",
                "while asking Location SB", TestInfo.LOG_FILE);
        FileAsserts.assertFileContains("Should have warning about KB",
                "while asking Location KB", TestInfo.LOG_FILE);

        // Test extra checksums
        replyServerChecksum.batchResult.clear();
        replyServerChecksum.batchResult.put(SB, "barfu#klaf\nbarfu##klyf\nbarfu##knof");
        replyServerChecksum.batchResult.put(KB, "barfuf##klaff\nbarfu##klof\nbarfu##klof\nbarfu##klof");
        fps = new FilePreservationStatus("barfu", fooAdmindatum,
                                         getChecksumMap("barfu"));
        assertEquals("Should have expected number of keys",
                2, getChecksumMap("foobar").size());
        assertEquals("Should have expected size for SB",
                2, getChecksumMap("foobar").get(SB).size());
        assertEquals("Should have expected size for KB",
                3, getChecksumMap("foobar").get(KB).size());
        LogUtils.flushLogs(getClass().getName());
        FileAsserts.assertFileContains("Should have warning about SB",
                "while asking Location SB", TestInfo.LOG_FILE);
        FileAsserts.assertFileContains("Should have warning about KB",
                "while asking Location KB", TestInfo.LOG_FILE);

        // TODO: More funny cases
    }

    public void testGetChecksums() {
        DummyChecksumBatchMessageReplyServer replyServerChecksum = new DummyChecksumBatchMessageReplyServer();

        // Test standard case
        replyServerChecksum.batchResult.put(SB, "foobar##md5-1");
        replyServerChecksum.batchResult.put(KB, "foobar##md5-2");
        FilePreservationStatus fps = new FilePreservationStatus("foobar", fooAdmindatum,
                                                                getChecksumMap(
                                                                        "foobar"));
        List<String> checksums = getChecksums("foobar", Location.get("KB"));
        assertEquals("Should have one checksum for known file",
                1, checksums.size());
        assertEquals("Should have the right checksum",
                "md5-2", checksums.get(0));

        replyServerChecksum.batchResult.clear();
        fps = new FilePreservationStatus("fobar", fooAdmindatum,
                                         getChecksumMap("fobar"));
        checksums = getChecksums("foobar", Location.get("KB"));
        assertEquals("Should have no checksums for unknown file",
                0, checksums.size());
    }

    /** This is a subclass that overrides the getChecksum method to avoid
     * calling batch().
     */
    static class DummyFPS extends FilePreservationStatus {
        static Map<Location, List<String>> arcrepresults =
                new HashMap<Location, List<String>>();
        public DummyFPS(String filename, ArcRepositoryEntry entry) {
            super(filename, entry, getChecksumMap(filename));
        }

        public List<String> getChecksums(Location ba, String filename) {
            if (arcrepresults.containsKey(ba)) {
                return arcrepresults.get(ba);
            } else {
                return CollectionUtils.list();
            }
        }
    }

    private static class DummyBatchMessageReplyServer
            implements MessageListener {

        JMSConnection conn = JMSConnectionFactory.getInstance();
        private byte[] encodedKey;
        private BitarchiveRecord bar;

        public DummyBatchMessageReplyServer() {
            conn.setListener(Channels.getTheArcrepos(), this);
        }

        public void close() {
            conn.removeListener(Channels.getTheArcrepos(), this);
        }

        public void onMessage(Message msg) {
            try {
                BatchMessage bMsg = (BatchMessage) JMSConnection.unpack(msg);
                BatchStatus lbs =
                        batch(bMsg.getJob(), bMsg.getLocationName(), null);
                conn.reply(
                        new BatchReplyMessage(bMsg.getTo(), bMsg.getReplyTo(),
                                              bMsg.getID(), 4, new ArrayList(),
                                              lbs.getResultFile()));
            } catch (IOFailure e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * The following three methods are used by the ActiveBitPreservation.
         *
         * @param job
         * @param file
         * @return BatchStatus
         * @throws IOException
         */
        public BatchStatus batch(FileBatchJob job, String locationName,
                                 RemoteFile file) throws IOException {
            BatchStatus lbs = null;
            File tmpfile = File.createTempFile("DummyBatch", "");
            OutputStream os = new FileOutputStream(tmpfile);
            // The file name
            File bitarchive_dir = new File(TestInfo.WORKING_DIR, locationName);
            Settings.set(Settings.BITARCHIVE_SERVER_FILEDIR,
                         bitarchive_dir.getAbsolutePath());
            BitarchiveAdmin admin = BitarchiveAdmin.getInstance();
            BatchLocalFiles localBatchRunner = new BatchLocalFiles(
                    admin.getFiles());
            localBatchRunner.run(job, os);
            os.close();
            RemoteFile resultFile = RemoteFileFactory.getInstance(tmpfile, true,
                                                                  false, true);
            if (to_fail) {
                File artificial_failure = new File(bitarchive_dir,
                                                   TestInfo.REFERENCE_FILES[0]);
                List<File> l = new ArrayList<File>();
                l.add(artificial_failure);
                lbs = new BatchStatus(bitarchive_dir.getName(),
                                      l, job.getNoOfFilesProcessed(),
                                      null);
            } else {

                lbs = new BatchStatus(bitarchive_dir.getName(),
                                      job.getFilesFailed(),
                                      job.getNoOfFilesProcessed(),
                                      resultFile);
            }
            return lbs;
        }
    }

    private static class DummyChecksumBatchMessageReplyServer implements MessageListener {
        public Map<Location, String> batchResult = new HashMap<Location, String>();
        JMSConnection conn = JMSConnectionFactory.getInstance();

        public DummyChecksumBatchMessageReplyServer() {
            conn.setListener(Channels.getTheArcrepos(), this);
        }

        public void close() {
            conn.removeListener(Channels.getTheArcrepos(), this);
        }

        public void onMessage(Message msg) {
            try {
                BatchMessage bMsg = (BatchMessage) JMSConnection.unpack(msg);
                String res = batchResult.get(Location.get(bMsg.getLocationName()));
                if (res != null) {
                    conn.reply(new BatchReplyMessage(bMsg.getTo(), bMsg.getReplyTo(),
                            bMsg.getID(), res.split("\n").length, new ArrayList<File>(),
                            new StringRemoteFile(res)));
                } else {
                    conn.reply(new BatchReplyMessage(bMsg.getTo(), bMsg.getReplyTo(),
                            bMsg.getID(), 0, new ArrayList<File>(), null));
                }
            } catch (IOFailure e) {}
        }
    }

}
