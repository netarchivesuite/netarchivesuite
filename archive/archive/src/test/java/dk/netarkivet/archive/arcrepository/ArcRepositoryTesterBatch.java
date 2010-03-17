/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.archive.arcrepository;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.arcrepository.bitpreservation.ChecksumJob;
import dk.netarkivet.archive.bitarchive.distribute.BitarchiveMonitorServer;
import dk.netarkivet.archive.bitarchive.distribute.BitarchiveServer;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.PreservationArcRepositoryClient;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.MD5;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;


/**
 * NB: This class was formerly known as ChecksumJobTester, but it tests
 * batch-functionality more than it tests the class ChecksumJob.
 * Therefore it was moved to this package from ??, but some adjusting still
 * needs to be done. Specifically,some of the test data for this class are still
 * located in the bitpreservation package.
 */
public class ArcRepositoryTesterBatch extends TestCase {
    private UseTestRemoteFile rf = new UseTestRemoteFile();

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
    private static final File ALOG_DIR = new File(WORKING_DIR, "log/admindata");

    static BitarchiveServer archiveServer1 = null;
    static ChannelID SERVER_ID1 = null;
    static ChannelID THE_BAMON = null;
    static ArcRepository c = null;
    static PreservationArcRepositoryClient arClient = null;
    static BitarchiveMonitorServer bam_server = null;

    ReloadSettings rs = new ReloadSettings();

    /**
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        rs.setUp();
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        ChannelsTester.resetChannels();
        FileUtils.removeRecursively(WORKING_DIR);
        TestFileUtils.copyDirectoryNonCVS(ORIGINALS_DIR, WORKING_DIR);
        Settings.set(CommonSettings.REMOTE_FILE_CLASS,
                     "dk.netarkivet.common.distribute.TestRemoteFile");
        FileUtils.createDir(CLOG_DIR);
        FileUtils.createDir(ALOG_DIR);

        Settings.set(ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN, ALOG_DIR.getAbsolutePath());
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, BITARCHIVE_DIR.getAbsolutePath());
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, SERVER_DIR.getAbsolutePath());
        archiveServer1 = BitarchiveServer.getInstance();
        c = ArcRepository.getInstance();

        arClient = ArcRepositoryClientFactory.getPreservationInstance();

        bam_server = BitarchiveMonitorServer.getInstance();

        testFiles = new File(BITARCHIVE_DIR, "filedir").listFiles(
                FileUtils.ARCS_FILTER);
        rf.setUp();
    }

    /**
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        c.close(); //Close down ArcRepository controller
        bam_server.close();
        arClient.close();
        archiveServer1.close();
        FileUtils.removeRecursively(WORKING_DIR);
        rf.tearDown();
        rs.tearDown();
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
                                                 Settings.get(
                                                         CommonSettings.USE_REPLICA_ID));
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
                                         Settings.get(
                                                 CommonSettings.USE_REPLICA_ID));
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


}
