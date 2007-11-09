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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import dk.netarkivet.archive.arcrepository.distribute.JMSArcRepositoryClient;
import dk.netarkivet.archive.arcrepositoryadmin.AdminData;
import dk.netarkivet.archive.arcrepositoryadmin.ArchiveStoreState;
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
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.BitArchiveStoreState;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.Location;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.MD5;
import dk.netarkivet.common.utils.arc.BatchLocalFiles;
import dk.netarkivet.common.utils.arc.FileBatchJob;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.TestUtils;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/**
 */
public class ActiveBitPreservationTester extends TestCase {
    private UseTestRemoteFile rf = new UseTestRemoteFile();

    static boolean to_fail;
    ActiveBitPreservation abp;
    private static final File REFERENCE_DIR = new File(TestInfo.ORIGINALS_DIR,
                                                       "referenceFiles");

    protected void setUp() throws Exception {
        super.setUp();
        Settings.reload();
        ChannelsTester.resetChannels();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                                          TestInfo.WORKING_DIR);
        //FileUtils.removeRecursively(TestInfo.THE_ARCHIVE_DIR);
        to_fail = false;

        JMSConnectionTestMQ.useJMSConnectionTestMQ();
        JMSConnectionTestMQ.clearTestQueues();
        rf.setUp();

        TestInfo.REPORT_DIR.mkdirs();
        TestInfo.CLOG_DIR.mkdirs();
        Settings.set(Settings.DIRS_ARCREPOSITORY_ADMIN,
                     TestInfo.CLOG_DIR.getAbsolutePath());
        Settings.set(Settings.DIR_ARCREPOSITORY_BITPRESERVATION,
                     TestInfo.CLOG_DIR.getAbsolutePath());
        //c = new TestController();
        for (String aREFERENCE_FILES : TestInfo.REFERENCE_FILES) {
            File file = new File(new File(TestInfo.GOOD_ARCHIVE_DIR, "filedir"),
                                 aREFERENCE_FILES);
            String orgCheckSum = MD5.generateMD5onFile(file);
            //c.setCheckSum(file.getName(), orgCheckSum);
        }
    }

    protected void tearDown() throws Exception {
        UpdateableAdminData.getInstance().close();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        if (abp != null) {
            abp.close();
        }
        ArcRepositoryClientFactory.getPreservationInstance().close();
        JMSConnectionTestMQ.clearTestQueues();
        rf.tearDown();
        Settings.reload();
        super.tearDown();
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
        File dir = REFERENCE_DIR;
        Settings.set(Settings.DIRS_ARCREPOSITORY_ADMIN,
                     TestInfo.WORKING_DIR.getAbsolutePath());
        Settings.set(Settings.DIR_ARCREPOSITORY_BITPRESERVATION,
                     TestInfo.WORKING_DIR.getAbsolutePath());

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
        ActiveBitPreservation abp = ActiveBitPreservation.getInstance();
        try {
            abp.findWrongFiles(locationKB);
            fail("Should have thrown exception on missing files.");
        } catch (IOFailure e) {
            // Expected
        }
        abp.close();

        // Set up like we've run a checksum job
        File originals = new File(dir, "checksums");
        TestFileUtils.copyDirectoryNonCVS(new File(originals, "unsorted.txt"),
                                          WorkFiles.getFile(locationKB,
                                                            WorkFiles.CHECKSUMS_ON_BA));
        abp = ActiveBitPreservation.getInstance();
        abp.findWrongFiles(locationKB);

        // Check that wrong-files file exists and has correct content
        List<String> expectedContent = Arrays.asList
                (new String[]{"integrity11.ARC", "integrity12.ARC"});
        File wrong = WorkFiles.getFile(locationKB, WorkFiles.WRONG_FILES);
        List<String> actualContent = FileUtils.readListFromFile(wrong);
        Collections.sort(actualContent);
        assertEquals("Wrong state list should be as expected.\n"
                     + "Expected " + expectedContent
                     + " but was " + actualContent,
                     expectedContent, actualContent);

        // Check that wrong-state file exists and has correct content
        List<String> expectedContent2
                = Arrays.asList(new String[]{"integrity1.ARC"});
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
        File dir = new File(TestInfo.WORKING_DIR, "referenceFiles");
        Settings.set(Settings.DIRS_ARCREPOSITORY_ADMIN,
                     TestInfo.WORKING_DIR.getAbsolutePath());
        Settings.set(Settings.DIR_ARCREPOSITORY_BITPRESERVATION,
                     TestInfo.WORKING_DIR.getAbsolutePath());

        /* Set it up to look like we've run a listFiles job */
        File listingDir = new File(dir, "filelistOutput");
        Location locationSB = Location.get("SB");
        File listingFile = WorkFiles.getFile(locationSB,
                                             WorkFiles.FILES_ON_BA);
        TestFileUtils.copyDirectoryNonCVS(new File(listingDir, "unsorted.txt"),
                listingFile);

        ActiveBitPreservation abp = ActiveBitPreservation.getInstance();
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
    public void testRunChecksumJob() throws FileNotFoundException, IOException {
        Settings.set(Settings.DIR_ARCREPOSITORY_BITPRESERVATION,
                     TestInfo.WORKING_DIR.getAbsolutePath());
        DummyBatchMessageReplyServer dummy = new DummyBatchMessageReplyServer();

        ActiveBitPreservation acp = ActiveBitPreservation.getInstance();

        // Test valid parameters:
        try {
            acp.runChecksumJob(null);
            fail("Argument 'location' must not be null");
        } catch (ArgumentNotValid e) {
            // expected
        }

        Location location = Location.get(TestInfo.VALID_LOCATION);
        acp.runChecksumJob(location);

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
                ActiveBitPreservation.class,
                "runBatchJob",
                FileBatchJob.class, Location.class, List.class, File.class);

        Settings.set(Settings.DIR_ARCREPOSITORY_BITPRESERVATION,
                     TestInfo.WORKING_DIR.getAbsolutePath());
        DummyBatchMessageReplyServer dummy = new DummyBatchMessageReplyServer();
        abp = ActiveBitPreservation.getInstance();
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
        Settings.set(Settings.DIRS_ARCREPOSITORY_ADMIN,
                TestInfo.WORKING_DIR.getAbsolutePath());
        FileUtils.copyFile(TestInfo.CORRECT_ADMIN_DATA, TestInfo.ADMIN_DATA);
        // Ensure that the admin data are read from the file
        AdminData dummyad = AdminData.getUpdateableInstance();
        abp = ActiveBitPreservation.getInstance();
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

    /**
     * Test for bug #462: Should be able to run checksum jobs in either place.
     */
    public void testRunChecksumJobElsewhere() throws NoSuchFieldException,
                                                     IllegalAccessException {
        final ActiveBitPreservation abp = ActiveBitPreservation.getInstance();
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
        abp.runChecksumJob(Location.get("SB"));
        assertEquals("Checksum job should have run on SB", "SB",
                     location[0]);
        abp.runChecksumJob(Location.get("KB"));
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
    public void testRunFileListJob() throws IOException {
        Settings.set(Settings.DIR_ARCREPOSITORY_BITPRESERVATION,
                     TestInfo.WORKING_DIR.getAbsolutePath());
        ActiveBitPreservation abp = ActiveBitPreservation.getInstance();

        BitarchiveMonitorServerStub monitor = new BitarchiveMonitorServerStub();
        JMSConnectionTestMQ con
                = (JMSConnectionTestMQ) JMSConnectionFactory.getInstance();
        con.setListener(Channels.getTheArcrepos(), monitor);

        // Check normal run
        final String locationName = TestInfo.LOCATION_NAME;
        Location location = Location.get(locationName);
        final String otherLocationName = TestInfo.OTHER_LOCATION_NAME;
        Location otherLocation = Location.get(otherLocationName);
        abp.runFileListJob(location);
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

        // Check normal run restricted to nothing
        abp.runFileListJob(location, null,
                           Arrays.asList(new String[]{}));
        assertTrue("Output should exist", normalOutputFile.exists());
        assertEquals("Output file should be empty",
                     0, normalOutputFile.length());
        assertFalse("Reference output should not exist",
                    referenceOutputFile.exists());
        normalOutputFile.delete();
        assertTrue("Last temp file should be gone",
                   monitor.lastRemoteFile.isDeleted());

        // Check normal run restricted to nothing
        List<String> specifiedFiles = Arrays.asList(new String[]{
                "integrity1.ARC", "integrity12.ARC"});
        abp.runFileListJob(location, null, specifiedFiles);
        assertTrue("Output should exist", normalOutputFile.exists());
        List<String> fileList = WorkFiles.getLines(location,
                                                   WorkFiles.FILES_ON_BA);
        assertEquals("Output file should have two lines", 2, fileList.size());
        assertTrue("Output should contain both files",
                   fileList.containsAll(specifiedFiles));
        assertFalse("Reference output should not exist",
                    referenceOutputFile.exists());
        normalOutputFile.delete();
        assertTrue("Last temp file should be gone",
                   monitor.lastRemoteFile.isDeleted());

        // Check reference run
        abp.runFileListJob(otherLocation, location,
                           null);
        assertTrue("Output should exist", referenceOutputFile.exists());
        assertFalse("Reference output should not exist",
                    normalOutputFile.exists());
        referenceOutputFile.delete();
        assertTrue("Last temp file should be gone",
                   monitor.lastRemoteFile.isDeleted());

        // Check reference run restricted to nothing
        abp.runFileListJob(otherLocation, location,
                           Arrays.asList(new String[]{}));
        assertTrue("Output should exist", referenceOutputFile.exists());
        assertEquals("Output file should be empty",
                     0, referenceOutputFile.length());
        assertFalse("Reference output should not exist",
                    normalOutputFile.exists());
        referenceOutputFile.delete();
        assertTrue("Last temp file should be gone",
                   monitor.lastRemoteFile.isDeleted());

        // Check that wrong counts are caught
        monitor.fakeCount = 17;
        abp.runFileListJob(location);
        LogUtils.flushLogs(ActiveBitPreservation.class.getName());
        FileAsserts.assertFileContains("Should have warning about wrong count",
                                       "Number of files found (" + 4
                                       + ") does not"
                                       + " match with number reported by job (17)",
                                       TestInfo.LOG_FILE);

        abp.close();
    }

    public void testGenerateActionListForMissingFiles()
            throws IOException {
        Settings.set(Settings.DIR_ARCREPOSITORY_BITPRESERVATION,
                     TestInfo.WORKING_DIR.getAbsolutePath());
        ActiveBitPreservation abp = ActiveBitPreservation.getInstance();

        File origsDir = new File(REFERENCE_DIR, "missingFiles");

        File outputDir = new File(TestInfo.WORKING_DIR, TestInfo.LOCATION_NAME);
        FileUtils.createDir(outputDir);

        Location workloc = Location.get(TestInfo.LOCATION_NAME);
        File list1 = WorkFiles.getFile(workloc, WorkFiles.DELETE_FROM_ADMIN);
        File list2 = WorkFiles.getFile(workloc, WorkFiles.INSERT_IN_ADMIN);
        File list3 = WorkFiles.getFile(workloc, WorkFiles.DELETE_FROM_BA);
        File list4 = WorkFiles.getFile(workloc, WorkFiles.UPLOAD_TO_BA);

        TestFileUtils.copyDirectoryNonCVS(origsDir,
                                          new File(outputDir, "missingFiles"));
        BitarchiveMonitorServerStub monitor = new BitarchiveMonitorServerStub();
        JMSConnectionTestMQ con
                = (JMSConnectionTestMQ) JMSConnectionFactory.getInstance();
        con.setListener(Channels.getTheArcrepos(), monitor);
        Location referenced_by = Location.get(TestInfo.LOCATION_NAME);
        abp.generateActionListForMissingFiles(referenced_by,
                                              Location.get(
                                                      TestInfo.OTHER_LOCATION_NAME));

        //First check the output from the reference bitarchive

        // Check that both output files exist
        assertTrue("Unsorted output should exist",
                   WorkFiles.getFile(referenced_by,
                                     WorkFiles.FILES_ON_REFERENCE_BA).exists());
        // Check that output files have same content set
        Set<String> made = new HashSet<String>(WorkFiles.getLines(referenced_by,
                                                                  WorkFiles.FILES_ON_REFERENCE_BA));
        File refDir = new File(REFERENCE_DIR, "referenceFiles");
        File refFile = new File(refDir, "unsorted.txt");

        Set<String> ref = new HashSet<String>(
                FileUtils.readListFromFile(refFile));
        assertEquals("Output file should have correct content", ref, made);

        //Now check the four actionlists
        assertEquals(
                "The insert in admin data file should contain expected content",
                "integrity0.ARC", FileUtils.readFile(list1).trim());
        assertEquals(
                "The remove from admin data file should contain expected content",
                "integrity1.ARC", FileUtils.readFile(list2).trim());
        assertEquals("The upload to BA file should contain expected content",
                     "integrity3.ARC", FileUtils.readFile(list3).trim());
        assertEquals("The delete from BA file should contain expected content",
                     "integrity2.ARC", FileUtils.readFile(list4).trim());
        abp.close();
    }


    /**
     * stub for BitarchiveMonitorServer which receives a file-list batch message
     * intended for KB and replies by uploading a listing of
     * TestInfo.GOOD_ARCHIVE_DIR
     */
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

}
