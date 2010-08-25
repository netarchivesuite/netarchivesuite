/* File:        $Id$
 * Revision:    $Revision$
 * Date:        $Date$
 * Author:      $Author$
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
package dk.netarkivet.harvester.harvesting.distribute;

import javax.jms.JMSException;
import javax.jms.ObjectMessage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;

import junit.framework.TestCase;

import dk.netarkivet.archive.arcrepository.distribute.JMSArcRepositoryClient;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.Constants;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.BatchLocalFiles;
import dk.netarkivet.common.utils.cdx.CDXRecord;
import dk.netarkivet.common.utils.cdx.ExtractCDXJob;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobPriority;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.harvesting.HarvestController;
import dk.netarkivet.harvester.harvesting.HarvestDocumentation;
import dk.netarkivet.harvester.harvesting.IngestableFiles;
import dk.netarkivet.testutils.ClassAsserts;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.GenericMessageListener;
import dk.netarkivet.testutils.LogUtils;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.MockupArcRepositoryClient;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/**
 * Test HarvestControllerServer.
 */
public class HarvestControllerServerTester extends TestCase {
    private UseTestRemoteFile utrf = new UseTestRemoteFile();

    TestInfo info = new TestInfo();

    /** The message to write to log when starting the server. */
    private static final String START_MESSAGE = "Starting HarvestControllerServer.";

    /** The message to write to log when stopping the server. */
    private static final String CLOSE_MESSAGE = "Closing HarvestControllerServer.";

    HarvestControllerServer hcs;

    /* variables only used by the two harvestInfo test-methods */
    Job theJob;

    File jobTempDir;

    ReloadSettings rs = new ReloadSettings();

    /**
     * Constants used by writeOtherFilesToArc().
     */
    final static String RECORD_PREFIX = "www.netarkivet.dk/crawlerdata/job/";

    // TODO: this should be set in the Constants class (and maybe in
    // settings.xml).
    final static int maxSize = 524288000; // 500 MB

    final static boolean compress = false;

    public HarvestControllerServerTester(String sTestName) {
        super(sTestName);
    }

    public void setUp() throws Exception {
        rs.setUp();
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        JMSConnectionMockupMQ.clearTestQueues();
        FileUtils.removeRecursively(TestInfo.SERVER_DIR);
        TestInfo.WORKING_DIR.mkdirs();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                TestInfo.WORKING_DIR);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.TEST_CRAWL_DIR,
                TestInfo.CRAWL_DIR_COPY);
        FileInputStream fis = new FileInputStream("tests/dk/netarkivet/testlog.prop");
        LogManager.getLogManager().reset();
        FileUtils.removeRecursively(TestInfo.LOG_FILE);
        LogManager.getLogManager().readConfiguration(fis);
        fis.close();
        ChannelsTester.resetChannels();
        utrf.setUp();
        Settings.set(JMSArcRepositoryClient.ARCREPOSITORY_STORE_RETRIES, "1");
        Settings.set(CommonSettings.NOTIFICATIONS_CLASS, RememberNotifications.class.getName());
        Settings.set(HarvesterSettings.HARVEST_CONTROLLER_SERVERDIR, TestInfo.WORKING_DIR.getAbsolutePath());
        Settings.set(HarvesterSettings.HARVEST_CONTROLLER_OLDJOBSDIR,
                     TestInfo.WORKING_DIR.getAbsolutePath() + "/oldjobs");
    }

    /**
     * After test is done close test-objects.
     * @throws SQLException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    public void tearDown() throws SQLException, IllegalAccessException, NoSuchFieldException {
        if (hcs != null) {
            hcs.close();
        }
        JMSConnectionMockupMQ.clearTestQueues();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        FileUtils.removeRecursively(TestInfo.SERVER_DIR);
        FileUtils.removeRecursively(TestInfo.CRAWL_DIR_COPY);

        FileUtils.removeRecursively(new File(Settings.get(
                HarvesterSettings.HARVEST_CONTROLLER_OLDJOBSDIR)));
        FileUtils.removeRecursively(new File(Settings.get(
                HarvesterSettings.HARVEST_CONTROLLER_SERVERDIR)));
        ChannelsTester.resetChannels();
        utrf.tearDown();
        RememberNotifications.resetSingleton();
        rs.tearDown();
    }

    /**
     * Test singletonicity.
     */
    public void testIsSingleton() {
        hcs = ClassAsserts.assertSingleton(HarvestControllerServer.class);
    }

    /**
     * Testing that server starts and log-file logs this !
     * @throws IOException
     */
    public void testServerStarting() throws IOException {
        Settings.set(HarvesterSettings.HARVEST_CONTROLLER_SERVERDIR, TestInfo.SERVER_DIR
                .getAbsolutePath());
        hcs = HarvestControllerServer.getInstance();
        LogUtils.flushLogs(HarvestControllerServer.class.getName());
        FileAsserts.assertFileContains("Log should contain start message.",
                START_MESSAGE, TestInfo.LOG_FILE);
    }

    /** Test that if the harvestcontrollerserver cannot start, the HACO listener
     * will not be added
     */
    public void testFailingArcRepositoryClient() {
        Settings.set(HarvesterSettings.HARVEST_CONTROLLER_SERVERDIR, "/fnord");
        try {
            hcs = HarvestControllerServer.getInstance();
            fail("HarvestControllerServer should have thrown an exception");
        } catch (PermissionDenied e) {
            //expected
        }
        JobPriority priority = JobPriority.valueOf(
                Settings.get(HarvesterSettings.HARVEST_CONTROLLER_PRIORITY));
        ChannelID channel = JobChannelUtil.getChannel(priority);
        assertEquals("Should have no listeners to the HACO queue",
                     0, ((JMSConnectionMockupMQ) JMSConnectionFactory
                .getInstance()).getListeners(channel).size());
    }

    /**
     * Tests resolution of Bug68 which prevents of creation of server-directory.
     * if it is located more than one level below an existing directory in the
     * hierarchy
     */
    public void testCreateServerDir() {
        File server_dir = new File(TestInfo.SERVER_DIR + "/server/server");
        Settings.set(HarvesterSettings.HARVEST_CONTROLLER_SERVERDIR, server_dir
                .getAbsolutePath());
        hcs = HarvestControllerServer.getInstance();
        assertTrue("Server Directory not created " + server_dir, server_dir
                .exists());
    }

    /**
     * Check that we receive the expected CrawlStatusMessages when we send a
     * broken job to a HarvestControllerServer. The case of a
     * correctly-functioning job is more-or-less identical and is to be included
     * in the IntegrityTester suite
     * @throws InterruptedException
     */
    public synchronized void testMessagesSentByFailedJob()
            throws InterruptedException {
        Settings.set(HarvesterSettings.HARVEST_CONTROLLER_SERVERDIR, TestInfo.SERVER_DIR
                .getAbsolutePath());
        hcs = HarvestControllerServer.getInstance();
        // make a dummy job
        Job j = TestInfo.getJob();
        j.setJobID(1L);
        //
        // Break the job by setting its status to something other than SUBMITTED
        // so
        // that no harvest will actually be started
        //
        j.setStatus(JobStatus.DONE);
        NetarkivetMessage nMsg = new DoOneCrawlMessage(j, TestInfo.SERVER_ID,
                                                                                                  TestInfo.emptyMetadata);
        JMSConnectionMockupMQ.updateMsgID(nMsg, "UNIQUE_ID");
        JMSConnectionMockupMQ con = (JMSConnectionMockupMQ) JMSConnectionFactory
                .getInstance();
        CrawlStatusMessageListener listener = new CrawlStatusMessageListener();
        con.setListener(Channels.getTheSched(), listener);
        ObjectMessage msg = JMSConnectionMockupMQ.getObjectMessage(nMsg);
        hcs.onMessage(msg);
        con.waitForConcurrentTasksToFinish();
        //
        // should have received two messages - one with status started and one
        // one with status failed
        //
        assertEquals("Should have received two messages", 2,
                listener.status_codes.size());
        //
        // Expect to receive two messages, although possibly out of order
        //
        JobStatus status_0 = listener.status_codes.get(0);
        JobStatus status_1 = listener.status_codes.get(1);

        assertTrue(
                "Message statuses are " + status_0 + " and " + status_1,
                (status_0 == JobStatus.STARTED
                        && status_1 == JobStatus.FAILED)
                        || (status_1 == JobStatus.STARTED
                                && status_0 == JobStatus.FAILED));
        //
        // Check that JobIDs are corrects
        //
        assertEquals("JobIDs do not match for first message:", j.getJobID()
                .longValue(), (listener.jobids.get(0)).longValue());
        assertEquals("JobIDs do not match for second message:", j.getJobID()
                .longValue(), (listener.jobids.get(1)).longValue());
    }

    /**
     * Testing close().
     * @throws IOException
     */
    public void testClose() throws IOException {
        Settings.set(HarvesterSettings.HARVEST_CONTROLLER_SERVERDIR, TestInfo.SERVER_DIR
                .getAbsolutePath());
        hcs = HarvestControllerServer.getInstance();
        hcs.close();
        hcs = null; // so that tearDown does not try to close again !!
        String logtxt = FileUtils.readFile(TestInfo.LOG_FILE);
        StringAsserts.assertStringContains("HarvestControllerServer not stopped !",
                CLOSE_MESSAGE, logtxt);
    }

    /**
     * Tests that sending a doOneCrawlMessage with a value other than submitted
     * results in a job-failed message being sent back.
     * @throws JMSException
     */
    public void testJobFailedOnBadMessage() throws JMSException {
        GenericMessageListener listener = new GenericMessageListener();
        JMSConnection con = JMSConnectionFactory.getInstance();
        con.setListener(Channels.getTheSched(), listener);
        hcs = HarvestControllerServer.getInstance();
        theJob = TestInfo.getJob();
        theJob.setStatus(JobStatus.DONE);
        theJob.setJobID(new Long(42L));
        JobPriority priority = JobPriority.valueOf(
                Settings.get(HarvesterSettings.HARVEST_CONTROLLER_PRIORITY));
        NetarkivetMessage naMsg = new DoOneCrawlMessage(
                theJob, JobChannelUtil.getChannel(priority), TestInfo.emptyMetadata);
        JMSConnectionMockupMQ.updateMsgID(naMsg, "id1");
        ObjectMessage oMsg = JMSConnectionMockupMQ.getObjectMessage(naMsg);
        hcs.onMessage(oMsg);
        ((JMSConnectionMockupMQ) con).waitForConcurrentTasksToFinish();
        // Should send job-started and job-failed messages
        assertEquals("Should have received two messages", 2,
                listener.messagesReceived.size());
        JobStatus code0 = ((CrawlStatusMessage) listener.messagesReceived.get(0))
                .getStatusCode();
        JobStatus code1 = ((CrawlStatusMessage) listener.messagesReceived.get(1))
                .getStatusCode();
        assertTrue("Should have sent a STATUS_FAILED message",
                code0 == JobStatus.FAILED || code1 == JobStatus.FAILED);
    }

    public void harvestInfoSetup() {
        Settings.set(HarvesterSettings.HARVEST_CONTROLLER_SERVERDIR, TestInfo.SERVER_DIR
                .getAbsolutePath());
        hcs = HarvestControllerServer.getInstance();
        // TODO check that new clean Haco does not send any CrawlStatusMessages
        theJob = TestInfo.getJob();
        theJob.setJobID(new Long(42L)); // Hack: because j.getJobID() == null

        jobTempDir = new File(TestInfo.SERVER_DIR, "jobTempDir");
        if (!jobTempDir.mkdir()) {
            fail("Unable to create dir: " + jobTempDir.getAbsolutePath());
        }
        /* Use if need for better testdata arises */
        // Long harvestId = new Long((long) Math.random() * 100.0D);
        // DomainConfiguration cfg
        // Job aJob = Job.createJob(harvestID, DomainConfiguration cfg)
    }

    /**
     * Test that starts (and stops) the HarvestControllerServer
     * and verifies that found "old jobs" are treated as expected.
     * Thus, an "indirect" test of method processHarvestInfoFile().
     * @param crawlDir the location of the crawldir
     * @param numberOfStoreMessagesExpected The number of sotre messages
     * expected. Usually number of files in dir + 1 for metadata arc file.
     * @param storeFailFile If not null, simulate failure on upload of this file
     * @return The CrawlStatusMessage returned by the HarvestControllerServer
     * for the found job.
     */
    public CrawlStatusMessage testProcessingOfLeftoverJobs(
            File crawlDir,
            int numberOfStoreMessagesExpected,
            String storeFailFile) {
        //System.out.println("StoreFailFile: " + storeFailFile);
        final JMSConnectionMockupMQ con = (JMSConnectionMockupMQ) JMSConnectionFactory
                .getInstance();
        Settings.set(HarvesterSettings.HARVEST_CONTROLLER_SERVERDIR, crawlDir.getParentFile()
                .getAbsolutePath());
        MockupArcRepositoryClient marc = new MockupArcRepositoryClient();
        marc.setUp();
        marc.failOnFile(storeFailFile);
        // Scheduler stub to check for crawl status messages
        GenericMessageListener sched = new GenericMessageListener();
        con.setListener(Channels.getTheSched(), sched);
        con.waitForConcurrentTasksToFinish();
        assertEquals("Should not have received any messages yet", 0,
                sched.messagesReceived.size());
        //Start and close HCS, thus attempting to upload all ARC files found in arcsDir
        HarvestControllerServer hcs = HarvestControllerServer.getInstance();
        con.waitForConcurrentTasksToFinish();
        hcs.close();
        con.removeListener(Channels.getTheSched(), sched);
        marc.tearDown();
        //The HCS should try to upload all original ARC files + 1 metadata ARC file
        assertEquals("Should have received store messages for all arc files",
                numberOfStoreMessagesExpected, marc.getMsgCount());
        /* The test serverDirs always contain excatly one job with one or more ARC files.
         * Therefore, starting up the HCS should generate exactly one FAILED status msg. */
        assertEquals("Should have received one crawl status message", 1,
                sched.messagesReceived.size());
        assertEquals("Job status should be FAILED", JobStatus.FAILED,
                ((CrawlStatusMessage) sched.messagesReceived.get(0))
                        .getStatusCode());
        //The HCS should move found crawlDir to oldjobsdir
        assertFalse("Crawl directory should have been moved", crawlDir.exists());
        File expected_new_crawl_dir =
            new File(Settings.get(HarvesterSettings.HARVEST_CONTROLLER_OLDJOBSDIR),
                    crawlDir.getName());
        File expected_new_arcs_dir = new File(expected_new_crawl_dir, "arcs");
        assertTrue("Should find crawl directory moved to "
                + expected_new_crawl_dir, expected_new_crawl_dir.exists());
        //The moved dir should only contain ARC files that couldn't be uploaded.
        int filesInCrawlDirAfterUpload = ("".equals(storeFailFile) ? 0 : 1);
        assertEquals("The moved dir should only contain ARC files that couldn't be uploaded.",
                filesInCrawlDirAfterUpload,
                expected_new_arcs_dir.listFiles(FileUtils.ARCS_FILTER).length);
        //Return the CrawlStatusMessage for further analysis.
        return (CrawlStatusMessage) sched.messagesReceived.get(0);
    }

    /**
     * Tests processing of leftover jobs in the case where all uploads go well.
     */
    public void testProcessHarvestInfoFile() {
        CrawlStatusMessage message
                = testProcessingOfLeftoverJobs(
                TestInfo.LEFTOVER_CRAWLDIR_1,
                TestInfo.FILES_IN_LEFTOVER_JOB_DIR_1 + 1,
                "");
        assertEquals("Message should be for right job", 42L, message.getJobID());
    }

    /**
     * Tests processing of leftover jobs in the case where some uploads fail.
     */
    public void testProcessHarvestInfoFileFails() {
        CrawlStatusMessage crawlStatusMessage =
            testProcessingOfLeftoverJobs(
                    TestInfo.LEFTOVER_CRAWLDIR_2,
                    TestInfo.FILES_IN_LEFTOVER_JOB_DIR_2.length + 1,
                    TestInfo.FILES_IN_LEFTOVER_JOB_DIR_2[1]);
        assertEquals("Job upload message should detail number of failures",
                    "No hosts report found, 1 files failed to upload",
                    crawlStatusMessage.getUploadErrors());
        StringAsserts.assertStringMatches(
                "Detailed upload message should declare which files failed",
                "Error uploading.*" + TestInfo.LEFTOVER_JOB_DIR_2_SOME_FILE_PATTERN,
                crawlStatusMessage.getUploadErrorDetails());
        StringAsserts.assertStringContains("Harvest should seem interrupted",
                "Crawl probably interrupted", crawlStatusMessage.getHarvestErrors());
        StringAsserts.assertStringMatches("Harvest should seem interrupted",
                "Crawl probably interrupted.*HarvestControllerServer",
                crawlStatusMessage.getHarvestErrorDetails());
        assertTrue("Failed CrawlStatusMessage should also be OK",
                crawlStatusMessage.isOk());

        String oldjobsdir = Settings.get(
                HarvesterSettings.HARVEST_CONTROLLER_OLDJOBSDIR);
        FileUtils.removeRecursively(new File(
                oldjobsdir));

        crawlStatusMessage =
            testProcessingOfLeftoverJobs(
                    TestInfo.LEFTOVER_CRAWLDIR_3,
                    0,
                    null);
        assertTrue("Failed CrawlStatusMessage should also be OK",
                crawlStatusMessage.isOk());

        assertTrue("Crawl.log must not have been deleted on error",
                   new File(new File(oldjobsdir, TestInfo.LEFTOVER_CRAWLDIR_3.getName()), "logs/crawl.log").exists());

        assertTrue("Progress-statistics log must not have been deleted on error",
                   new File(new File(oldjobsdir, TestInfo.LEFTOVER_CRAWLDIR_3.getName()), "logs/progress-statistics.log").exists());
    }

    /**
     * Test bug 852. the system property
     * org.archive.crawler.frontier.AbstractFrontier.queue-assignment-policy
     * must be set by the HarvestControllerServer
     * and include dk.netarkivet.harvester.harvesting.DomainnameQueueAssignmentPolicy
     * Also tests, that heritrix.version is set to Constants.getHeritrixVersion()
     */
     public void testBug852() {
         hcs = HarvestControllerServer.getInstance();
         if (!System.getProperties().containsKey("org.archive.crawler.frontier.AbstractFrontier.queue-assignment-policy")) {
             fail ("org.archive.crawler.frontier.AbstractFrontier.queue-assignment-policy is not defined!!");
         }
         String assignmentPolicyList =
             System.getProperties().getProperty(
                     "org.archive.crawler.frontier.AbstractFrontier.queue-assignment-policy");
         if (!assignmentPolicyList.contains("dk.netarkivet.harvester.harvesting.DomainnameQueueAssignmentPolicy")) {
             fail("NetarchiveSuite assignment policy not included in queue-assignment-policy");
         }
         if (!System.getProperties().containsKey("heritrix.version")) {
             fail ("heritrix.version is not set");
         }
         String heritrixVersion = System.getProperties().getProperty("heritrix.version");
         if (!heritrixVersion.equals(Constants.getHeritrixVersionString())) {
             fail ("The 'heritrix.version' property is not set to: "
                     + Constants.getHeritrixVersionString());
         }

     }

     /**
     * Verify that preharvest metadata is found in the final metadata file.
     * See also bug #738.
     * @throws NoSuchMethodException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public void testCopyPreharvestMetadata() throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        //Set up harvest controller, a job some metadata and a crawlDir
        hcs = HarvestControllerServer.getInstance();
        Job job = TestInfo.getJob();
        long jobId = 42L;
        job.setJobID(jobId);
        List<MetadataEntry> meta = new ArrayList<MetadataEntry>();
        meta.add(TestInfo.sampleEntry);
        File crawlDir = TestInfo.WORKING_DIR;
        File arcsDir = new File(crawlDir,Constants.ARCDIRECTORY_NAME);
        arcsDir.mkdir();
        //Write preharvest metadata file
        final HarvestController hc = HarvestController.getInstance();
        Method writePreharvestMetadata = ReflectUtils.getPrivateMethod(
                hc.getClass(), "writePreharvestMetadata",
                Job.class, List.class, File.class);
        writePreharvestMetadata.invoke(hc, job, meta, crawlDir);
        //Write final metadata file - should copy the preharvest metadata
        HarvestDocumentation.documentHarvest(crawlDir,jobId, job.getOrigHarvestDefinitionID());
        //Verify that metadata file has been generated
        IngestableFiles inf = new IngestableFiles(crawlDir,jobId);
        assertTrue("documentHarvest() should have generated final metadata",
                inf.isMetadataReady());
        assertEquals("Expected just one metadata arc file",
                1,inf.getMetadataArcFiles().size());
        File mf = inf.getMetadataArcFiles().get(0);
        //Verify that no surprises were found in the final metadata
        List<CDXRecord> mfContent = getCdx(mf);
        // After implementation of C.2.2 (Write harvest details)
        // we now get 3 records instead of just one:
        // the last 2 are records for the order.xml and seeds.txt
        assertEquals("Expected no records except our 3 metadata samples",
                3, mfContent.size());
        //Verify that sampleEntry is in the final metadata
        CDXRecord rec = mfContent.get(0);
        assertEquals("The first record should be our metadata example record",
                TestInfo.sampleEntry.getURL(),rec.getURL());
        assertEquals("The first record should be our metadata example record",
                TestInfo.sampleEntry.getMimeType(),rec.getMimetype());
        assertEquals("The first record should be our metadata example record",
                TestInfo.sampleEntry.getData().length,rec.getLength());
    }
    /**
     * Runs an ExtractCDXJob on the given, local arc-file and formats the output.
     * Everything stored in RAM - don't use on large files!
     * @param arcFile An arc-file present on the local system.
     * @return The full CDX index as List of CDXRecords.
     */
    private List<CDXRecord> getCdx(File arcFile) {
        List<CDXRecord> result = new ArrayList<CDXRecord>();
        ByteArrayOutputStream cdxBaos = new ByteArrayOutputStream();
        BatchLocalFiles batchRunner =
            new BatchLocalFiles(new File[]{ arcFile });
        batchRunner.run(new ExtractCDXJob(),cdxBaos);
        for(String cdxLine : cdxBaos.toString().split("\n")) {
            result.add(new CDXRecord(cdxLine.split("\\s+")));
        }
        return result;
    }

}
