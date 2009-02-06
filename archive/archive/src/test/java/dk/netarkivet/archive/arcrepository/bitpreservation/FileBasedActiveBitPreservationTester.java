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
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.arcrepositoryadmin.AdminData;
import dk.netarkivet.archive.arcrepositoryadmin.UpdateableAdminData;
import dk.netarkivet.archive.bitarchive.BitarchiveAdmin;
import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.archive.bitarchive.distribute.BatchReplyMessage;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.distribute.StringRemoteFile;
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.BitArchiveStoreState;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.BatchLocalFiles;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.preconfigured.MockupJMS;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/**
 * Unit test for the class FileBasedActiveBitPreservation.
 */
public class FileBasedActiveBitPreservationTester extends TestCase {
    private UseTestRemoteFile rf = new UseTestRemoteFile();

    static boolean to_fail;
    FileBasedActiveBitPreservation abp;
    
    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR,
                                                  TestInfo.WORKING_DIR);
    private ReloadSettings rs = new ReloadSettings();
    private MockupJMS mj = new MockupJMS();

    private static final Replica ONE = Replica.getReplicaFromId("ONE");
    private static final Replica TWO = Replica.getReplicaFromId("TWO");

    protected void setUp() throws Exception {
        super.setUp();
        rs.setUp();
        ChannelsTester.resetChannels();
        mtf.setUp();
        mj.setUp();
        rf.setUp();

        to_fail = false;

        Settings.set(CommonSettings.ARC_REPOSITORY_CLIENT,
                     MockupArcRepositoryClient.class.getName());
        Settings.set(ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN, TestInfo.WORKING_DIR.getAbsolutePath());
        Settings.set(ArchiveSettings.DIR_ARCREPOSITORY_BITPRESERVATION, TestInfo.WORKING_DIR.getAbsolutePath());
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
        
        MockupArcRepositoryClient.instance = null;

        rf.tearDown();
        mtf.tearDown();
        mj.tearDown();
        rs.tearDown();
    }

    /**
     * Tests the normal behaviour of findChangedFiles.
     *
     * This should check:
     *
     * That the two files are created with the expected output. What happens if
     * the expected input isn't there.
     *
     * @throws IOException
     */
    public void testFindChangedFiles() throws IOException {
        
        // We check the following four cases:
        // integrity1 is marked as failed, but is correct in bitarchives
        // integrity2 is missing from admin data, but exists in bitarchives
        // integrity7 is marked as completed, but is not in bitarchives
        // FIXME: integrity7 is discovered nowhere!
        // integrity11 has no state and wrong checksum
        // integrity12 is marked as completed but has wrong checksum

        // Set up admin data
        UpdateableAdminData ad = UpdateableAdminData.getUpdateableInstance();
        ad.addEntry("integrity1.ARC", null,
                    "708afc1b7aebc12f7e65ecf1be054d23");
        ad.addEntry("integrity7.ARC", null,
                    "44ddf7a30f7fabb838e43a8505f927c2");
        ad.addEntry("integrity11.ARC", null,
                    "4236be8e67e0c10da2902764ff4b954a");
        ad.addEntry("integrity12.ARC", null,
                    "4236be8e67e0c10da2902764ff4b954a");
        Replica replicaTwo = Replica.getReplicaFromId("TWO");
        ad.setState("integrity1.ARC",
                replicaTwo.getChannelID().getName(),
                    BitArchiveStoreState.UPLOAD_FAILED);
        ad.setState("integrity7.ARC",
                replicaTwo.getChannelID().getName(),
                    BitArchiveStoreState.UPLOAD_COMPLETED);
        ad.setState("integrity12.ARC",
                replicaTwo.getChannelID().getName(),
                    BitArchiveStoreState.UPLOAD_COMPLETED);

        abp = FileBasedActiveBitPreservation.getInstance();
        abp = FileBasedActiveBitPreservation.getInstance();
        abp.findChangedFiles(replicaTwo);

        // Check that wrong-files file exists and has correct content
        List<String> expectedContent = Arrays.asList(
                "integrity11.ARC", "integrity12.ARC");
        File wrong = WorkFiles.getFile(replicaTwo, WorkFiles.WRONG_FILES);
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
                WorkFiles.getLines(replicaTwo, WorkFiles.WRONG_STATES);
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

        /* Set it up so fileListJob will return expected results. */
        File listingDir = new File(new File(dir, "filelistOutput"),
                                   "unsorted.txt");
        MockupArcRepositoryClient.getInstance().overrideBatch = new BatchStatus(
                "AP1", Collections.<File>emptySet(), 5,
                RemoteFileFactory.getMovefileInstance(listingDir),
                new ArrayList<FileBatchJob.ExceptionOccurrence>(0));
        Replica replicaOne = Replica.getReplicaFromId("ONE");

        //Run method.
        FileBasedActiveBitPreservation abp 
            = FileBasedActiveBitPreservation.getInstance();
        abp.findMissingFiles(replicaOne);

        //Clean up
        MockupArcRepositoryClient.getInstance().overrideBatch = null;

        // Check that missing-files file exists and has correct content
        File missing = WorkFiles.getFile(replicaOne,
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
     * Tests requirements of Assignment 3.1.2
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void testRunChecksumJob()
            throws FileNotFoundException, IOException, NoSuchMethodException,
                   InvocationTargetException, IllegalAccessException {
        Method runChecksumJob = ReflectUtils.getPrivateMethod(
                FileBasedActiveBitPreservation.class, "runChecksumJob",
                Replica.class);

        FileBasedActiveBitPreservation acp 
            = FileBasedActiveBitPreservation.getInstance();

        // Test valid parameters:
        try {
            runChecksumJob.invoke(acp, (Replica) null);
            fail("Argument 'replica' must not be null");
        } catch (InvocationTargetException e) {
            assertEquals("Should have thrown ANV",
                         ArgumentNotValid.class, e.getCause().getClass());
            // expected
        }

        Replica replica = Replica.getReplicaFromId(TestInfo.VALID_REPLICA_ID);
        runChecksumJob.invoke(acp, replica);

        File unsortedOutput = WorkFiles.getFile(replica,
                                                WorkFiles.CHECKSUMS_ON_BA);
        assertTrue("No output file generated for unsorted output",
                   unsortedOutput.exists());

        /* No longer automatically sorting when not needed
        File sortedOutput = new File(outputSubDir,
            Constants.SORTED_OUTPUT_FILE);
        assertTrue("No output file generated for sorted output",
            sortedOutput.exists());
        */

        assertEquals("Unsorted output file should have been deleted from FTP "
                + "server", 0, TestRemoteFile.remainingFiles().size());

        /*
        assertEquals("The two files containing unsorted output"
            + " and sorted output do not have the same size",
                unsortedOutput.length(), sortedOutput.length());

        FileInputStream fis = new FileInputStream(sortedOutput);
        BufferedReader in = new BufferedReader(new InputStreamReader(fis));
        String line = null;
        String prevArcFileName = null;
        while ((line = in.readLine()) != null) {
            String arcFileName = line.substring(0, 
                line.indexOf(Constants.STRING_FILENAME_SEPARATOR));
            if (prevArcFileName != null) {
                assertTrue("Batch output is not sorted (lexicographically) "
                + "according to ARC file name",
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
                FileBatchJob.class, Replica.class, List.class, File.class);

        abp = FileBasedActiveBitPreservation.getInstance();
        FileListJob job = new FileListJob();
        File outputFile = new File(TestInfo.WORKING_DIR, "outputFile");
        runBatchJob.invoke(abp, job, Replica.getReplicaFromId("TWO"),
                           null, outputFile);
        assertTrue("Output file should exist after successfull run",
                   outputFile.exists());

        // Mimic failure
        File artificialFailure = new File(TestInfo.GOOD_ARCHIVE_FILE_DIR,
                                           TestInfo.REFERENCE_FILES[0]);
                List<File> l = new ArrayList<File>();
                l.add(artificialFailure);
                MockupArcRepositoryClient.getInstance().overrideBatch
                        = new BatchStatus("AP1", l, 1, null,
                                new ArrayList<FileBatchJob
                                    .ExceptionOccurrence>(0));
        outputFile.delete();
        runBatchJob.invoke(abp, job, Replica.getReplicaFromId("TWO"),
                           null, outputFile);
        assertFalse("Output file should not exist after failed run",
                    outputFile.exists());
    }

    public void testGetFilePreservationStatus()
            throws NoSuchFieldException, IllegalAccessException {

        FileUtils.copyFile(TestInfo.CORRECT_ADMIN_DATA, TestInfo.ADMIN_DATA);
        // Ensure that the admin data are read from the file
        AdminData.getUpdateableInstance();
        abp = FileBasedActiveBitPreservation.getInstance();
        FilePreservationState fps
                = abp.getFilePreservationState(TestInfo.FILE_IN_ADMIN_DATA);
        assertNotNull("Should get FilePreservationStatus for existing file",
                fps);

        assertEquals("Should get FPS for correct file",
                TestInfo.FILE_IN_ADMIN_DATA, fps.getFilename());

        fps = abp.getFilePreservationState(TestInfo.FILE_NOT_IN_ADMIN_DATA);
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
                FileBasedActiveBitPreservation.class,
                "runChecksumJob", Replica.class);

        final FileBasedActiveBitPreservation abp
            = FileBasedActiveBitPreservation.getInstance();
        // Make a dummy archive repository client that just drops the name
        // into an array.
        final String[] replica = new String[1];
        MockupArcRepositoryClient.instance = new MockupArcRepositoryClient() {
            public BatchStatus batch(FileBatchJob job, String replicaId) {
                replica[0] = replicaId;
                File file = new File(
                        new File(TestInfo.WORKING_DIR, "checksums"),
                        "unsorted.txt");
                file.getParentFile().mkdirs();
                try {
                    new FileWriter(file).close();
                } catch (IOException e) {
                    throw new IOFailure("Can't make empty file " + file, e);
                }
                return new BatchStatus(replicaId, new HashSet<File>(), 0,
                                       new TestRemoteFile(file, to_fail,
                                                           to_fail, to_fail),
                                                           job.getExceptions());
            }

            public void close() {
                MockupArcRepositoryClient.instance = null;
            }
        };
        // Try to run "checksumjobs" on both allowable locations
        runChecksumJob.invoke(abp, Replica.getReplicaFromId("ONE"));

        assertEquals("Checksum job should have run on ONE", "ONE",
                     replica[0]);
        runChecksumJob.invoke(abp, Replica.getReplicaFromId("TWO"));
        assertEquals("Checksum job should have run on TWO", "TWO",
                replica[0]);
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
                FileBasedActiveBitPreservation.class, "runFileListJob",
                Replica.class);

        FileBasedActiveBitPreservation abp
                = FileBasedActiveBitPreservation.getInstance();

        // Check normal run
        final String replicaId = TestInfo.REPLICA_ID;
        Replica replica = Replica.getReplicaFromId(replicaId);
        //final String otherLocationName = TestInfo.OTHER_LOCATION_NAME;
        // Location otherLocation = Location.get(otherLocationName);
        runFilelistJob.invoke(abp, replica);
        File normalOutputFile =
                WorkFiles.getFile(replica, WorkFiles.FILES_ON_BA);
        File referenceOutputFile =
                WorkFiles.getFile(replica, WorkFiles.FILES_ON_REFERENCE_BA);
        assertTrue("Output should exist", normalOutputFile.exists());
        assertFalse("Reference output should not exist",
                    referenceOutputFile.exists());
        normalOutputFile.delete();

        // Check that wrong counts are caught
        MockupArcRepositoryClient.getInstance().overrideBatch
                = new BatchStatus("AP1", Collections.<File>emptyList(), 17,
                        RemoteFileFactory.getMovefileInstance(
                                new File(TestInfo.WORKING_DIR, 
                                        "test_file_list_output/"
                                        + "filelistOutput/unsorted.txt")),
                                        new ArrayList<FileBatchJob
                                            .ExceptionOccurrence>(0));
        runFilelistJob.invoke(abp, replica);
        LogUtils.flushLogs(FileBasedActiveBitPreservation.class.getName());
        FileAsserts.assertFileContains("Should have warning about wrong count",
                                       "Number of files found (" + 6
                                       + ") does not"
                                       + " match with number reported by job (17)",
                                       TestInfo.LOG_FILE);

        abp.close();
    }

    public void testGetBitarchiveChecksum() throws Exception {
        AdminData.getUpdateableInstance().addEntry("foobar", null, "md5-1");
        AdminData.getUpdateableInstance().addEntry("barfu", null, "klaf");
        final Map<Replica, String> results = new HashMap<Replica, String>();
        // Test standard case
        MockupArcRepositoryClient.instance = new MockupArcRepositoryClient() {
            public BatchStatus batch(FileBatchJob job, String replicaId) {
                if (job.getClass().equals(ChecksumJob.class)) {

                    if (results.containsKey(Replica.getReplicaFromId(replicaId))) {
                        return new BatchStatus("AP1",
                                Collections.<File>emptyList(), 1,
                                new StringRemoteFile(
                                        results.get(Replica.getReplicaFromId(replicaId))),
                                        new ArrayList<FileBatchJob.ExceptionOccurrence>(0));
                    } else {
                        return new BatchStatus("AP1",
                                    Collections.<File>emptyList(), 0, null,
                                    new ArrayList<FileBatchJob.ExceptionOccurrence>(0));
                    }
                } else {
                    return super.batch(job,
                            replicaId);
                }
            }
        };
        results.put(ONE, "foobar##md5-1");
        results.put(TWO, "foobar##md5-2");
        FilePreservationState fps 
            = FileBasedActiveBitPreservation.getInstance()
            .getFilePreservationState("foobar");
        assertFalse("Should have received result non-null result for ONE",
                fps.getBitarchiveChecksum(ONE) == null);        
        assertEquals("Should have expected size for ONE",
                1, fps.getBitarchiveChecksum(ONE).size());
        assertEquals("Should have expected value for ONE",
                "md5-1", fps.getBitarchiveChecksum(ONE).get(0));
        
        assertFalse("Should have received result non-null result for TWO",
                fps.getBitarchiveChecksum(TWO) == null);
        assertEquals("Should have expected size for TWO",
                1, fps.getBitarchiveChecksum(TWO).size());
        assertEquals("Should have expected value for TWO",
                "md5-2", fps.getBitarchiveChecksum(TWO).get(0));

        // Test fewer checksums
        results.clear();
        results.put(ONE, "");

        fps = FileBasedActiveBitPreservation.getInstance()
            .getFilePreservationState("foobar");
        
        assertFalse("Should have received result non-null result for ONE",
                fps.getBitarchiveChecksum(ONE) == null);
        assertEquals("Should have expected size for ONE",
                0, fps.getBitarchiveChecksum(ONE).size());
        assertEquals("Should have expected size for TWO",
                0, fps.getBitarchiveChecksum(TWO).size());

        LogUtils.flushLogs(getClass().getName());
        FileAsserts.assertFileNotContains("Should have no warning about ONE",
                TestInfo.LOG_FILE, "while asking 'Replica ONE'");
        FileAsserts.assertFileNotContains("Should have no warning about TWO",
                TestInfo.LOG_FILE, "while asking 'Replica TWO'");

        // Test malformed checksums
        results.clear();
        results.put(ONE, "foobar#klaf");
        results.put(TWO, "foobarf##klaff");
        fps = FileBasedActiveBitPreservation.getInstance()
            .getFilePreservationState("foobar");
        assertEquals("Should have expected size for ONE",
                0, fps.getBitarchiveChecksum(ONE).size());
        assertEquals("Should have expected size for TWO",
                0, fps.getBitarchiveChecksum(TWO).size());
        LogUtils.flushLogs(getClass().getName());
        
        FileAsserts.assertFileContains("Should have warning about ONE in logfile: "
                + FileUtils.readFile(TestInfo.LOG_FILE),
                //Before: "while asking replica 'Replica One'", 
                "while asking replica 'BITARCHIVEReplica (ONE) ReplicaOne'",
                TestInfo.LOG_FILE);
        FileAsserts.assertFileContains("Should have warning about TWO in logfile: "
                + FileUtils.readFile(TestInfo.LOG_FILE),
                //Before: "while asking replica 'Replica TWO'",
                "while asking replica 'BITARCHIVEReplica (TWO) ReplicaTwo'",
                TestInfo.LOG_FILE);

        // Test extra checksums
        results.clear();
        results.put(ONE, "barfu#klaf\nbarfu##klyf\nbarfu##knof");
        results.put(TWO, "barfuf##klaff\nbarfu##klof\nbarfu##klof\nbarfu##klof");
        fps = FileBasedActiveBitPreservation.getInstance()
            .getFilePreservationState("barfu");
        assertEquals("Should have expected size for ONE",
                2, fps.getBitarchiveChecksum(ONE).size());
        assertEquals("Should have expected size for TWO",
                3, fps.getBitarchiveChecksum(TWO).size());
        LogUtils.flushLogs(getClass().getName());
        FileAsserts.assertFileContains("Should have warning about ONE in logfile: "
                + FileUtils.readFile(TestInfo.LOG_FILE),
                //Before: "while asking replica 'Replica ONE'",
                "while asking replica 'BITARCHIVEReplica (ONE) ReplicaOne'",
                TestInfo.LOG_FILE);
        FileAsserts.assertFileContains("Should have warning about TWO in logfile: "
                + FileUtils.readFile(TestInfo.LOG_FILE),
                //Before: "while asking replica 'Replica TWO'",
                "while asking replica 'BITARCHIVEReplica (TWO) ReplicaTwo'",
                TestInfo.LOG_FILE);

        // TODO: More funny cases
    }

    private static class DummyBatchMessageReplyServer
            implements MessageListener {

        JMSConnection conn = JMSConnectionFactory.getInstance();
        private byte[] encodedKey;
        private BitarchiveRecord bar;

        public DummyBatchMessageReplyServer() {
            conn.setListener(Channels.getTheRepos(), this);
        }

        public void close() {
            conn.removeListener(Channels.getTheRepos(), this);
        }

        public void onMessage(Message msg) {
            try {
                BatchMessage bMsg = (BatchMessage) JMSConnection.unpack(msg);
                BatchStatus lbs =
                        batch(bMsg.getJob(), bMsg.getReplicaId(), null);
                conn.reply(
                        new BatchReplyMessage(bMsg.getTo(), bMsg.getReplyTo(),
                                              bMsg.getID(), 4, 
                                              new ArrayList<File>(),
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
         * @param job a given FileBatchjob
         * @param file a given RemoteFile (ignored)
         * @return BatchStatus 
         * @throws IOException If trouble creating temporary files and
         * writing to this temporary files.
         */
        public BatchStatus batch(FileBatchJob job, String locationName,
                                 RemoteFile file) throws IOException {
            BatchStatus lbs = null;
            File tmpfile = File.createTempFile("DummyBatch", "");
            OutputStream os = new FileOutputStream(tmpfile);
            // The file name
            File bitarchive_dir = new File(TestInfo.WORKING_DIR, locationName);
            Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR,
                         bitarchive_dir.getAbsolutePath());
            BitarchiveAdmin admin = BitarchiveAdmin.getInstance();
            BatchLocalFiles localBatchRunner = new BatchLocalFiles(
                    admin.getFiles());
            localBatchRunner.run(job, os);
            os.close();
            RemoteFile resultFile = RemoteFileFactory.getInstance(tmpfile, true,
                                                                  false, true);
            if (to_fail) {
                File artificialFailure = new File(bitarchive_dir,
                                                   TestInfo.REFERENCE_FILES[0]);
                List<File> l = new ArrayList<File>();
                l.add(artificialFailure);
                lbs = new BatchStatus(bitarchive_dir.getName(),
                                      l, job.getNoOfFilesProcessed(),
                                      null, null);
            } else {

                lbs = new BatchStatus(bitarchive_dir.getName(),
                                      job.getFilesFailed(),
                                      job.getNoOfFilesProcessed(),
                                      resultFile, null);
            }
            return lbs;
        }
    }

    public static class MockupArcRepositoryClient implements ArcRepositoryClient {
        private static MockupArcRepositoryClient instance;
        private BitarchiveRecord overrideGet;
        private File overrideGetFile;
        private File overrideStore;
        private BatchStatus overrideBatch;
        private File overrideRemoveAndGetFile;

        public static MockupArcRepositoryClient getInstance() {
            if (instance == null) {
                instance = new MockupArcRepositoryClient();
            }
            return instance;
        }

        public void close() {
            instance = null;
        }

        public BitarchiveRecord get(String arcfile, long index)
        throws ArgumentNotValid {
            if (overrideGet != null) {
                return overrideGet;
            }
            File file = new File(TestInfo.GOOD_ARCHIVE_FILE_DIR, arcfile);
            try {
                if (!file.exists()) {
                    return null;
                } else {
                    return new BitarchiveRecord(
                            (ARCRecord) ARCReaderFactory.get(file).get(index));
                }
            } catch (IOException e) {
                fail("Test failure while reading file '" + file + "'");
                return null;
            }
        }

        public void getFile(String arcfilename, Replica replica, File toFile) {
            if (overrideGetFile != null) {
                FileUtils.copyFile(overrideGetFile, toFile);
                return;
            }
            File file = new File(TestInfo.GOOD_ARCHIVE_FILE_DIR, arcfilename);
            FileUtils.copyFile(file, toFile);
        }

        public void store(File file) throws IOFailure, ArgumentNotValid {
            File f = new File(TestInfo.GOOD_ARCHIVE_FILE_DIR, file.getName());
            if (overrideStore != null) {
                FileUtils.copyFile(overrideStore, f);
                return;
            }
            FileUtils.copyFile(file, f);
        }

        public BatchStatus batch(FileBatchJob job, String locationName) {
            if (overrideBatch != null) {
                return overrideBatch;
            }
            try {
                File output = File.createTempFile("Batch", ".dat", TestInfo.WORKING_DIR);
                File[] in_files = TestInfo.GOOD_ARCHIVE_FILE_DIR.listFiles();
                FileOutputStream os = new FileOutputStream(output);
                new BatchLocalFiles(in_files).run(job, os);
                os.close();
                return new BatchStatus("BA1", Collections.<File>emptyList(),
                        in_files.length,
                        RemoteFileFactory.getMovefileInstance(output),
                        new ArrayList<FileBatchJob.ExceptionOccurrence>(0));
            } catch (IOException e) {
                fail("IO error during test");
                return null;
            }
        }

        public void updateAdminData(String fileName, String bitarchiveId,
                                    BitArchiveStoreState newval) {
            UpdateableAdminData adminData
                    = AdminData.getUpdateableInstance();
            if (!adminData.hasEntry(fileName)) {
                adminData.addEntry(fileName, null, "xx");
            }
            adminData.setState(fileName, bitarchiveId, newval);
        }

        public void updateAdminChecksum(String filename, String checksum) {
            UpdateableAdminData adminData
                    = AdminData.getUpdateableInstance();
            adminData.setCheckSum(filename, checksum);
        }

        public File removeAndGetFile(String fileName, String bitarchiveName,
                                     String checksum, String credentials) {
            if (overrideRemoveAndGetFile != null) {
                return overrideRemoveAndGetFile;
            }
            File output = null;
            try {
                output = File.createTempFile(fileName, ".removed", TestInfo.WORKING_DIR);
            } catch (IOException e) {
                fail("IO error during test.");
            }
            if (!credentials.equals("XX")) {
                throw new PermissionDenied("Credentials are not XX");
            }
            FileUtils.copyFile(new File(TestInfo.GOOD_ARCHIVE_FILE_DIR, fileName), output);
            return output;
        }
    }

}
