package dk.netarkivet.archive.arcrepository;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jms.Message;
import javax.jms.MessageListener;

import junit.framework.TestCase;
import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.arcrepository.bitpreservation.ChecksumJob;
import dk.netarkivet.archive.arcrepository.distribute.StoreMessage;
import dk.netarkivet.archive.arcrepositoryadmin.Admin;
import dk.netarkivet.archive.arcrepositoryadmin.AdminFactory;
import dk.netarkivet.archive.arcrepositoryadmin.ReplicaCacheDatabase;
import dk.netarkivet.archive.bitarchive.distribute.BitarchiveClient;
import dk.netarkivet.archive.bitarchive.distribute.BitarchiveMonitorServer;
import dk.netarkivet.archive.bitarchive.distribute.BitarchiveServer;
import dk.netarkivet.archive.bitarchive.distribute.GetFileMessage;
import dk.netarkivet.archive.bitarchive.distribute.RemoveAndGetFileMessage;
import dk.netarkivet.archive.checksum.distribute.ChecksumClient;
import dk.netarkivet.archive.distribute.ReplicaClient;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.PreservationArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.MD5;
import dk.netarkivet.common.utils.PrintNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.ClassAsserts;
import dk.netarkivet.testutils.DatabaseTestUtils;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.TestUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

public class ArcRepositoryDatabaseTester extends TestCase {
    /** A repeatedly used reflected method, used across method calls. */
    Method readChecksum;
    ReloadSettings rs = new ReloadSettings();
    private UseTestRemoteFile rf = new UseTestRemoteFile();
    static boolean first = true;

    /**
     * BATCH
     */
    private static File[] testFiles;

    private static final File TEST_DIR =
            new File("tests/dk/netarkivet/archive/arcrepository/data/batch");
    private static final File ORIGINALS_DIR = new File(TEST_DIR, "originals");
    private static final File WORKING_DIR = new File(TEST_DIR, "working");
    private static final File BITARCHIVE_DIR = new File(WORKING_DIR,
                                                        "bitarchive1");
    private static final File CLOG_DIR = new File(WORKING_DIR,
                                                  "log/controller");
    private static final File SERVER_DIR = new File(WORKING_DIR, "server1");
    private static final File OUTPUT_FILE = new File(WORKING_DIR,
                                                     "checksum_output");
    static BitarchiveServer archiveServer1 = null;
    static ChannelID SERVER_ID1 = null;
    static ChannelID THE_BAMON = null;
    static ArcRepository arcRepos = null;
    static PreservationArcRepositoryClient arClient = null;
    static BitarchiveMonitorServer bam_server = null;

    /**
     * These are for Get (ArcRepositoryTesterGet)
     */
    private static final File GET_DIR = new File(
            "tests/dk/netarkivet/archive/arcrepository/data/get");

    private static final File GET_ORIGINALS_DIR = new File(GET_DIR, "originals");

    private static final File GET_WORKING_DIR = new File(GET_DIR, "working");

    private static final File GET_BITARCHIVE_DIR = new File(WORKING_DIR,
            "bitarchive1");

    private static final File GET_SERVER_DIR = new File(WORKING_DIR, "server1");

    // List of files that can be used in the scripts (content of the ORIGINALS_DIR)
    private static final List<String> GETTABLE_FILES
        = Arrays.asList(new String[] {"get1.ARC", "get2.ARC" });

    /**
     * LOG - NO LOG!!!!
     */
    
    /**
     * STORE - NO STORE!!!!
     */
    
    /**
     * STORE CHECKSUM
     */
    private static final File STORE_CHECKSUM_DIR =
        new File("tests/dk/netarkivet/archive/arcrepository/data/store/originals");
    private static final String[] STORABLE_FILES = new String[]{
            "NetarchiveSuite-store1.arc", "NetarchiveSuite-store2.arc"};


    public void setUp() throws Exception {
        super.setUp();
        rf.setUp();
        rs.setUp();
        ChannelsTester.resetChannels();
        JMSConnectionMockupMQ.clearTestQueues();

        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        // Database admin test.
        DatabaseTestUtils.takeDatabase(TestInfo.DATABASE_FILE, 
                TestInfo.WORKING_DIR);
        TestFileUtils.copyDirectoryNonCVS(
                TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
        Settings.set(ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN,
                     TestInfo.WORKING_DIR.getAbsolutePath());
        Settings.set(CommonSettings.NOTIFICATIONS_CLASS, 
                PrintNotifications.class.getName());
        
        
        Settings.set(ArchiveSettings.BASEURL_ARCREPOSITORY_ADMIN_DATABASE, 
                "jdbc:derby:" + TestInfo.WORKING_DIR.getAbsolutePath());
        Settings.set(ArchiveSettings.MACHINE_ARCREPOSITORY_ADMIN_DATABASE,
                "");
        Settings.set(ArchiveSettings.PORT_ARCREPOSITORY_ADMIN_DATABASE,
                "");
        Settings.set(ArchiveSettings.DIR_ARCREPOSITORY_ADMIN_DATABASE,
                "");

        Settings.set(ArchiveSettings.ADMIN_CLASS, 
                dk.netarkivet.archive.arcrepositoryadmin.DatabaseAdmin.class.getName());
        
        // Batch
        TestFileUtils.copyDirectoryNonCVS(ORIGINALS_DIR, WORKING_DIR);
        Settings.set(CommonSettings.REMOTE_FILE_CLASS,
                     "dk.netarkivet.common.distribute.TestRemoteFile");
        FileUtils.createDir(CLOG_DIR);

        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, BITARCHIVE_DIR.getAbsolutePath());
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, SERVER_DIR.getAbsolutePath());
        archiveServer1 = BitarchiveServer.getInstance();
        arcRepos = ArcRepository.getInstance();

        arClient = ArcRepositoryClientFactory.getPreservationInstance();

        bam_server = BitarchiveMonitorServer.getInstance();

        testFiles = new File(BITARCHIVE_DIR, "filedir").listFiles(
                FileUtils.ARCS_FILTER);

    }
    
    public void tearDown() throws Exception {
        // BATCH
        arcRepos.close(); //Close down ArcRepository controller
        bam_server.close();
        arClient.close();
        archiveServer1.close();
        ReplicaCacheDatabase.getInstance().cleanup();
        FileUtils.removeRecursively(WORKING_DIR);
        
        ArcRepository.getInstance().close();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        // Empty the log file.
        new FileOutputStream(TestInfo.LOG_FILE).close();
        rs.tearDown();
        rf.tearDown();
        super.tearDown();
    }

    /** Test that ArcRepository is a singleton. */
    public void testIsSingleton() {
        ClassAsserts.assertSingleton(ArcRepository.class);
        ArcRepository.getInstance().close();
    }


    /** Verify that calling the protected no-arg constructor does not fail. */
    public void testConstructor() {
        ArcRepository.getInstance().close();
    }

    /** Test parameters. */
    public void testGetReplicaClientFromReplicaNameParameters() {
        ArcRepository a = ArcRepository.getInstance();
        /**
         * test with null parameter.
         */
        try {
            a.getReplicaClientFromReplicaId(null);
            fail("ArgumentNotValid should have been thrown");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        /**
         * Test with invalid parameter.
         */
        try {
            a.getReplicaClientFromReplicaId("-1");
            fail("ArgumentNotValid should have been thrown");
        } catch (UnknownID e) {
            // Expected
        }
    }

    /** Test a valid BitarchiveClient is returned. */
    public void testGetReplicaClientFromReplicaName() {
        ArcRepository a = ArcRepository.getInstance();
        
        for (Replica rep : Replica.getKnown()) {
            ReplicaClient rc = a.getReplicaClientFromReplicaId(
                    rep.getId());
            assertNotNull("Should return a valid ReplicaClient", rc);
            
            if(rep.getType() == ReplicaType.BITARCHIVE) {
                assertTrue("A Bitarchive replica should have BitarchiveClient", 
                        rc instanceof BitarchiveClient);
            }
            if(rep.getType() == ReplicaType.CHECKSUM) {
                assertTrue("A Bitarchive replica should have BitarchiveClient", 
                        rc instanceof ChecksumClient);
            }
        }
    }

    /**
     * Test that the readChecksum() method works as required.
     *
     * @throws Throwable if something are thrown
     */
    public void testReadChecksum() throws Throwable {
        readChecksum = ArcRepository.class.getDeclaredMethod("readChecksum",
                new Class[]{File.class, String.class});
        readChecksum.setAccessible(true);

        try {
            assertFalse("Testfile should not exist", TestInfo.TMP_FILE.exists());
            // Missing file
            String result = (String) readChecksum.invoke(
                    ArcRepository.getInstance(),
                    TestInfo.TMP_FILE, "foobar");
            fail("Should get failure on missing file, not " + result);
        } catch (InvocationTargetException e) {
            assertEquals("Should throw IOFailure",
                         IOFailure.class, e.getCause().getClass());
        }

        assertEquals("Should get empty output from empty file",
                     "", callReadChecksum("", "foobar"));

        assertEquals("Should get empty output from other-file file",
                     "", callReadChecksum("bazzoon##klaf", "foobar"));

        assertEquals("Should not match checksum with filename",
                     "", callReadChecksum("bar##foo", "foo"));

        assertEquals("Should get right checksum when matching",
                     "foo", callReadChecksum("bar##foo", "bar"));

        assertEquals("Should get right checksum if not on first line",
                     "bonk", callReadChecksum("bar##baz\nfoo##bonk", "foo"));
        LogUtils.flushLogs(ArcRepository.class.getName());
        FileAsserts.assertFileContains(
                "Should have warning about unwanted line",
                "There were an unexpected arc-file name in checksum result for arc-file 'foo'(line: 'bar##baz')",
                TestInfo.LOG_FILE);
        FileAsserts.assertFileNotContains(
                "Should have no warning about wanted line",
                TestInfo.LOG_FILE, "Read unexpected line 'foo##bonk");

        assertEquals("Should get right checksum if not on last line",
                     "baz", callReadChecksum("bar##baz\nfoo##bonk", "bar"));

        assertEquals("Should get right checksum if empty lines",
                     "bar", callReadChecksum("foo##bar\n\n", "foo"));

        // Check that the lines are validated correctly.
        try {
            callReadChecksum("barf", "foobar");
            fail("A checksum output file only containing 'barf' should through IllegalState");
        } catch (IllegalState e) {
            assertEquals("Not expected error message!",
                         "Read checksum line had unexpected format 'barf'",
                         e.getMessage());
            // This is expected!
        }
        // Check that a entry may not have two different checksums.
        try {
            callReadChecksum("foo##bar\nfoo##notBar", "foo");
            fail("A checksum output file containing two entries with for same "
                 + "name with different checksums should through IllegalState");
        } catch (IllegalState e) {
            assertEquals("Not expected error message!",
                         "The arc-file 'foo' was found with two different checksums: "
                         + "bar and notBar. Last line: 'foo##notBar'.",
                         e.getMessage());
            // This is expected!
        }

        LogUtils.flushLogs(ArcRepository.class.getName());
        FileAsserts.assertFileContains(
                "Should have warning about two different checksums",
                "The arc-file 'foo' was found with two different checksums: bar and notBar.",
                TestInfo.LOG_FILE);

    }


    /**
     * Call the readChecksum method with some input and a file to look for.
     *
     * @param input       Will be written to a file that readChecksum reads.
     *                    Valid input is of the form <arcfilename>##<checksum>,
     *                    but invalid input is part of the test.
     * @param arcfilename The name of the arcfile that readChecksum should look
     *                    for.
     *
     * @return The string found for the given filename.
     *
     * @throws IOFailure when readChecksum does.
     */
    public String callReadChecksum(String input, String arcfilename)
            throws Throwable {
        FileUtils.writeBinaryFile(TestInfo.TMP_FILE, input.getBytes());
        try {
            return (String) readChecksum.invoke(ArcRepository.getInstance(),
                                                TestInfo.TMP_FILE, arcfilename);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /**
     * Tests that ordinary, non-failing execution of a batch job writes output
     * back to reply message.
     */
    public void testNoOfFilesProcessed() {
        assertTrue("Should have more than zero files in the test directory!",
                   testFiles.length != 0);
        ChecksumJob jobTest = new ChecksumJob();
        BatchStatus batchStatus = arClient.batch(jobTest,
                Settings.get(CommonSettings.USE_REPLICA_ID));
        int processed = batchStatus.getNoOfFilesProcessed();
        assertEquals("Number of files processed: " + processed
                     + " does not equal number of given files",
                     testFiles.length, processed);
    }

    /**
     * Tests that a checkSum job can write output via a RemoteFile, one line of
     * output per file.
     */
    public void testOrdinaryRunRemoteOutput() {
        ChecksumJob jobTest = new ChecksumJob();
        BatchStatus lbs = arClient.batch(jobTest,
                Settings.get(CommonSettings.USE_REPLICA_ID));
        lbs.getResultFile().copyTo(OUTPUT_FILE);
        assertEquals("No exceptions should have happened", 0,
                     jobTest.getFilesFailed().size());
        assertEquals("2 files should have been processed", 2,
                     jobTest.getNoOfFilesProcessed());
        FileAsserts.assertFileNumberOfLines("Output file should have two lines",
                                            OUTPUT_FILE, 2);
    }

    /**
     * Check that null arguments provoke exceptions.
     */
    public void testNullArgumentsToBatch() {
        try {
            arClient.batch(null,
                           Settings.get(
                                   CommonSettings.USE_REPLICA_ID));
            fail("Failed to throw exception on null batch-job argument to Controller.batch()");
        } catch (ArgumentNotValid e) {
            //expected
        }
    }

    /**
     * Check that a batch job can be executed twice sequentially.
     */
    public void testSequentialRuns() {
        ChecksumJob jobTest = new ChecksumJob();
        BatchStatus batchStatus = arClient.batch(jobTest,
                                                 Settings.get(
                                                         CommonSettings.USE_REPLICA_ID));
        assertEquals("First batch should work",
                     testFiles.length, batchStatus.getNoOfFilesProcessed());

        batchStatus = arClient.batch(jobTest,
                                     Settings.get(
                                             CommonSettings.USE_REPLICA_ID));
        assertEquals("Second batch should work",
                     testFiles.length, batchStatus.getNoOfFilesProcessed());
    }

    /**
     * Test that correct checksums are generated in ChecksumJob, i.e. that the
     * expected checksums are written to the remote file for a given set of
     * files.
     *
     * @throws IOException
     */

    public void testGeneratedChecksum() throws IOException {
        ChecksumJob checkJob = new ChecksumJob();
        BatchStatus batchStatus = arClient.batch(checkJob,
                                                 Settings.get(
                                                         CommonSettings.USE_REPLICA_ID));
        batchStatus.getResultFile().copyTo(OUTPUT_FILE);
        List<String> jobChecksums = new ArrayList<String>();

        assertTrue("Output file should exist", OUTPUT_FILE.exists());
        BufferedReader reader = new BufferedReader(
                new FileReader(OUTPUT_FILE));

        String line;
        while ((line = reader.readLine()) != null) {
            jobChecksums.add(line.split(
                    dk.netarkivet.archive.arcrepository.bitpreservation.Constants.STRING_FILENAME_SEPARATOR)[1]);
        }

        reader.close();
        FileUtils.removeRecursively(OUTPUT_FILE);

        String[] refChecksums = new String[testFiles.length];
        for (int i = 0; i < refChecksums.length; i++) {
            refChecksums[i] = MD5.generateMD5onFile(testFiles[i]);
        }


        for (int i = 0; i < testFiles.length; i++) {
            assertTrue(
                    "The checksums return from checksum job do not contain the reference checksum: "
                    + refChecksums[i], jobChecksums.contains(refChecksums[i]));
        }

    }


    /**
     * Verify that ChecksumJob objects can be serialized and deserialized
     * without harm.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void testSerializability()
            throws IOException, ClassNotFoundException {

        ChecksumJob job1 = new ChecksumJob();
        ChecksumJob job2 = null;

        //We should probably change state of job1 to something else than default state...

        //Now serialize and deserialize the study object:
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream ous = null;
        ous = new ObjectOutputStream(baos);
        ous.writeObject(job1);
        ous.close();
        baos.close();
        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(baos.toByteArray()));
        job2 = (ChecksumJob) ois.readObject();
        //ois.close();
        //Finally, compare their visible states:
        assertEquals("The two jobs should have same number of processed files",
                     job1.getNoOfFilesProcessed(),
                     job2.getNoOfFilesProcessed());
        assertEquals("The two jobs should have identical lists of failed files",
                     job1.getFilesFailed(), job2.getFilesFailed());
        assertEquals(
                "The two jobs should have identical String representations",
                job1.toString(), job2.toString());
    }

    /**
     * This tests the get()-method for a non-existing-file.
     */
    public void testGetNonExistingFile() {
        BitarchiveRecord bar = arClient.get("nosuchfile.arc", (long) 0);
        assertNull("Should have retrieved null, not " + bar, bar);
    }

    /**
     * this tests the get()-method for an existing file.
     */
    public void testGetExistingFile() {
        BitarchiveRecord bar = arClient.get((String) GETTABLE_FILES.get(1),
                (long) 0);

        // not really much of a test as we just check for no exception
        // better tests follow later

        if (null == bar) {
            fail("Nullpointer should not be returned for existing file");
        }
    }

    /**
     * this tests get get()-method for an existing file - getting get File-name
     * out of the BitarchiveRecord.
     */
    public void testGetFile() throws IOException {
        arcRepos.close();
        DummyGetFileMessageReplyServer dServer
                = new DummyGetFileMessageReplyServer();
        File result = new File(FileUtils.createUniqueTempDir(
                        WORKING_DIR, "testGetFile"), (String) GETTABLE_FILES.get(1));
        Replica replica = Replica.getReplicaFromId(Settings.get(
                CommonSettings.USE_REPLICA_ID));
        arClient.getFile(GETTABLE_FILES.get(1), replica, result);
        byte[] buffer = FileUtils.readBinaryFile(result);
        ((JMSConnectionMockupMQ) JMSConnectionFactory.getInstance())
                .waitForConcurrentTasksToFinish();
        assertNotNull("Buffer should not be null", buffer);
        byte targetbuffer[] = FileUtils.readBinaryFile(new File(new File(
                GET_BITARCHIVE_DIR, "filedir"), (String) GETTABLE_FILES.get(1)));
        assertTrue("Received data and uploaded must be equal", Arrays
                .equals(buffer, targetbuffer));
    }

    /**
     * this tests get get()-method for an existing file - getting get File-name
     * out of the BitarchiveRecord.
     */
    public void testRemoveAndGetFile() throws IOException {
        arcRepos.close();
        arClient.close();
        arClient = ArcRepositoryClientFactory.getPreservationInstance();
        new DummyRemoveAndGetFileMessageReplyServer();
        final File bitarchiveFiledir = new File(
                Settings.get(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR),
                        "filedir");
        arClient.removeAndGetFile((String) GETTABLE_FILES.get(1),
                                Settings.get(
                                        CommonSettings.USE_REPLICA_ID),
                                "42",
                                MD5.generateMD5onFile(
                                                new File(bitarchiveFiledir,
                                        (String) GETTABLE_FILES.get(1)))
                                        );
        File copyOfFile = new File(FileUtils.getTempDir(),
                                   (String) GETTABLE_FILES.get(1));
        assertTrue("Must have copied file to commontempdir",
                   copyOfFile.exists());

        byte[] buffer = FileUtils.readBinaryFile(copyOfFile);
        ((JMSConnectionMockupMQ) JMSConnectionFactory.getInstance())
                .waitForConcurrentTasksToFinish();
        assertNotNull("Buffer should not be null", buffer);
        byte targetbuffer[] = FileUtils.readBinaryFile(new File(new File(
                GET_BITARCHIVE_DIR, "filedir"), (String) GETTABLE_FILES.get(1)));
        assertTrue("Received data and uploaded must be equal", Arrays
                .equals(buffer, targetbuffer));

        assertEquals("Should have no remote files left on the server",
                     0, TestRemoteFile.remainingFiles().size());
    }

    /**
     * this tests the getting of actual data (assuming that the length is not
     * null) is the length of getData() > 0 the next test checks the first 55
     * chars !
     */
    public void testGetData() {
        BitarchiveRecord bar = arClient.get((String) GETTABLE_FILES.get(1),
                (long) 0);

        if (bar.getLength() == 0L) {
            fail("No data in BitarchiveRecord");
        } else {
            // BitarchiveRecord.getData() now returns a InputStream 
                // instead of a byte[]
            String data = new String(TestUtils.inputStreamToBytes(bar.getData(),
                        (int) bar.getLength())).substring(0, 55);
            assertEquals("First 55 chars of data should be correct", data,
                    "<?xml version=\"1.0\" "
                        + "encoding=\"UTF-8\" standalone=\"yes\"?>");
        }
    }

    /**
     * Test for index out of bounds.
     */
    public void testGetIndexOutOfBounds() {
        try {
            BitarchiveRecord bar = arClient.get((String) GETTABLE_FILES.get(1),
                    (long) 50000000);
            fail("Index out of bounds should throw exception, but " +
                    "gave " + bar);
        } catch (Exception e) {
            //expected
        }
    }

    /**
     * Test for index not pointing on ARC-record.
     */
    public void testGetIllegalIndex() {
        try {
            BitarchiveRecord bar = arClient.get((String) GETTABLE_FILES.get(1),
                    (long) 5000);
            fail("Illegal index should return null, not given " + bar);
        } catch (Exception e) {
            //expected
        }
    }

    public static class DummyGetFileMessageReplyServer implements
            MessageListener {

        JMSConnection conn = JMSConnectionFactory.getInstance();

        public DummyGetFileMessageReplyServer() {
            conn.setListener(Channels.getTheRepos(), this);
        }

        public void close() {
            conn.removeListener(Channels.getTheRepos(), this);
        }

        public void onMessage(Message msg) {
            GetFileMessage netMsg = (GetFileMessage) JMSConnection
                    .unpack(msg);
            netMsg.setFile(new File(new File(GET_BITARCHIVE_DIR, "filedir"),
                    (String) GETTABLE_FILES.get(1)));
            conn.reply(netMsg);
        }
    }

    public static class DummyRemoveAndGetFileMessageReplyServer implements
            MessageListener {

        JMSConnection conn = JMSConnectionFactory.getInstance();

        public DummyRemoveAndGetFileMessageReplyServer() {
            conn.setListener(Channels.getTheRepos(), this);
        }

        public void close() {
            conn.removeListener(Channels.getTheRepos(), this);
        }

        public void onMessage(Message msg) {
            RemoveAndGetFileMessage netMsg 
                = (RemoveAndGetFileMessage) JMSConnection.unpack(msg);
            netMsg.setFile(new File(new File(GET_BITARCHIVE_DIR, "filedir"),
                    (String) GETTABLE_FILES.get(1)));
            conn.reply(netMsg);
        }
    }

    /**
     * Tests that Controller.getCheckSum() behaves as expected when using a
     * reference to a non-stored file.
     */
    public void testGetChecksumNotStoredFile() {
        File file = new File(STORE_CHECKSUM_DIR, STORABLE_FILES[0]);
        // do nothing with file - e.g. not storing it
        // thus checksum reference table should not contain an entry for
        // the file, i.e. getCheckSum() should return null:
        try {
            AdminFactory.getInstance().getCheckSum(file.getName());
            fail("Should throw UnknownID when getting non-existing checksum");
        } catch (UnknownID e) {
            //Expected
        }
    }

    /**
     * Tests if an attempt to store an already uploaded/stored file produces
     * the expected behavior: a PermissionDenied should be thrown,
     * and the original entry in checksum reference table remains unaffected.
     */
    public void testStoreFailedAlreadyUploadedChecksum() {
        File file = null;
        String orgCheckSum = null;
        String storedCheckSum = null;
        Admin admin = AdminFactory.getInstance();
        try {
            file = new File(STORE_CHECKSUM_DIR, STORABLE_FILES[0]);
            try {
                orgCheckSum = MD5.generateMD5onFile(file);
                admin.addEntry(file.getName(), new StoreMessage(
                        Channels.getThisReposClient(), file), MD5.generateMD5onFile(
                                file));
                admin.setState(file.getName(),
                        Channels.retrieveReplicaChannelFromReplicaId("TWO").getName(),
                        ReplicaStoreState.UPLOAD_COMPLETED);
                admin.setState(file.getName(),
                        Channels.retrieveReplicaChannelFromReplicaId("THREE").getName(),
                        ReplicaStoreState.UPLOAD_COMPLETED);

            } catch (IOException e) {
                e.printStackTrace();
                fail("Unexpected IOException thrown at generateMD5onFile()");
            }
            //JMSConnection con = JMSConnectionFactory.getInstance();
            StoreMessage msg = new StoreMessage(Channels.getError(), file);
            arcRepos.store(msg.getRemoteFile(), msg);
            UploadWaiting.waitForUpload(file, this);
            String refTableSum = admin.getCheckSum(file.getName());
            assertEquals("Stored checksum and reference checksum should be equal", 
                                refTableSum, orgCheckSum);
            storedCheckSum = refTableSum;
            // attempting to upload/store the file again:
            msg = new StoreMessage(Channels.getError(), file);
            arcRepos.store(msg.getRemoteFile(), msg);
            UploadWaiting.waitForUpload(file, this);
            fail("Should throw an PermissionDenied here!");
        } catch (dk.netarkivet.common.exceptions.PermissionDenied e) {
            String refTableSum = admin.getCheckSum(file.getName());
            // the checksum stored in reference table (during first store
            // operation) should be unaffected
            // by this second attempt to store the file:
            assertEquals(
                        "Stored checksum and reference checksum should be equal", 
                        refTableSum, storedCheckSum);
        } catch (IOFailure e) {
            e.printStackTrace();
            fail("Unexpected IOException thrown "
                        + "while trying to re-upload file: " + e);
        }
    }
}
