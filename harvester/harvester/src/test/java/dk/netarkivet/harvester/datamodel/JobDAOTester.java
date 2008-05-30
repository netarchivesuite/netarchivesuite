/*$Id$
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
package dk.netarkivet.harvester.datamodel;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Node;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.common.utils.IteratorUtils;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.testutils.TestUtils;

/**
 * For purposes of DB tests in this class, create a table
 * CREATE TABLE tests ( name varchar, value int );
 *
 */


public class JobDAOTester extends DataModelTestCase {
    Connection c;
    /** We start out with one job in status DONE */
    private static final int INITIAL_JOB_COUNT = 1;

    public JobDAOTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
        TestUtils.resetDAOs();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        TestUtils.resetDAOs();
    }

    public void testGetCountJobs() throws Exception {
        JobDAO dao = JobDAO.getInstance();
        assertEquals("Must have " + INITIAL_JOB_COUNT + " jobs from the beginning",
                     INITIAL_JOB_COUNT, dao.getCountJobs());
        HarvestDefinitionDAO hdDao = HarvestDefinitionDAO.getInstance();
        HarvestDefinition hd = hdDao.read(new Long(42));
        int jobsMade = hd.createJobs();
        assertEquals("Must find same number of jobs as we created",
                     jobsMade + INITIAL_JOB_COUNT, dao.getCountJobs());
        jobsMade = hd.createJobs();
        assertEquals("Must find all the jobs we have created",
                     2 * jobsMade + INITIAL_JOB_COUNT,
                     dao.getCountJobs());
    }

    public void testGenerateNextID() {
        JobDAO dao = JobDAO.getInstance();
        assertEquals("Must get id 2 with " + INITIAL_JOB_COUNT + " jobs",
                     new Long(2), dao.generateNextID());
        HarvestDefinitionDAO hdDao = HarvestDefinitionDAO.getInstance();
        HarvestDefinition hd = hdDao.read(new Long(42));
        int jobsMade = hd.createJobs();
        assertEquals("Must get correct id after making some jobs",
                     new Long(INITIAL_JOB_COUNT + 1 + jobsMade),
                     dao.generateNextID());
        int moreJobsMade = hd.createJobs();
        assertEquals("Must get correct id after making more jobs",
                     new Long(INITIAL_JOB_COUNT + 1 + jobsMade + moreJobsMade),
                     dao.generateNextID());
    }


    /**
     * This test creates (and stores) a new job and reads it back again
     * Verifies that state of stored job equals state of original job
     */
    public void testJobRead() {
        JobDAO dao = JobDAO.getInstance();
        DomainConfiguration dc = TestInfo.getDRConfiguration();
        Job job = Job.createJob(TestInfo.HARVESTID, dc, 0);

        dao.create(job);

        try {
            dao.read(null);
            fail("Failed to throw ArgumentNotValid exception on null-argument to constructor");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            Long unknownID = new Long(42424242);
            dao.read(unknownID);
            fail("Failed to throw Unknown ID exception on jobID " + unknownID);
        } catch (UnknownID e) {
            // expected
        }

        Job readJob = dao.read(job.getJobID());
        assertEquals("Id of read Job should equal id of original Job", job.getJobID(), readJob.getJobID());
        assertEquals("Status of read Job should equal status of original Job", job.getStatus(),
                     readJob.getStatus());
        assertEquals("Seedlist of read Job should equal seedlist of original Job", job.getSeedListAsString(),
                     readJob.getSeedListAsString());
        assertEquals("Order.xml of read Job should equal order.xml of original Job", job.getOrderXMLdoc().getText(),
                     readJob.getOrderXMLdoc().getText());
        assertEquals("Filename of order.xml of read Job should equal filename of order.xml of original Job",
                     job.getOrderXMLName(), readJob.getOrderXMLName());
        assertEquals("List of settings.xml's of read Job should equal list of settings.xml's of original Job",
                     job.getSettingsXMLdocs(), readJob.getSettingsXMLdocs());
        assertEquals("OrigHarvestDefinitionID of read Job should equal OrigHarvestDefinitionID of original Job",
                     job.getOrigHarvestDefinitionID(), readJob.getOrigHarvestDefinitionID());

        assertEquals("DomainConfigurationMap of read Job should equal DomainConfigurationMap of original Job",
                     job.getDomainConfigurationMap(), readJob.getDomainConfigurationMap());

        // Job.getSettingsXMLfiles() is probably obsolete
        // No decided if we need Job.getActualStart() and Job.getActualStop() - but we probably do (at least nice to have)
    }


    /**
     * This test creates (and stores) a new job, modifies it, and checks that the modified job can be retrieved
     */
    public void testJobUpdate() {
        JobDAO dao = JobDAO.getInstance();

        /* Create Job to update */
        DomainConfiguration dc = TestInfo.getDRConfiguration();
        Job job = Job.createJob(TestInfo.HARVESTID, dc, 0);
        dao.create(job);

        try {
            dao.update(null);
            fail("Failed to throw ArgumentNotValid exception on null argument");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            Long unknownID = new Long(42424242);
            Job jobUknownID = Job.createJob(TestInfo.HARVESTID, dc, 0);
            jobUknownID.setJobID(unknownID);
            dao.update(jobUknownID);
            fail("Failed to throw Unknown ID exception on jobID " + unknownID);
        } catch (UnknownID e) {
            // expected
        }

        /* Modify the job and update */

        // a simple modification:
        job.setStatus(JobStatus.DONE);

        // modify the list of configuration aggregated to this job:
        DomainConfiguration anotherConfiguration = TestInfo.getNetarkivetConfiguration();
        job.addConfiguration(anotherConfiguration);

        dao.update(job);

        // check that the modified job can be retrieved
        JobDAO dao2 = JobDAO.getInstance();
        Job jobUpdated = dao2.read(job.getJobID());

        assertTrue("The retrieved job should have status " + JobStatus.DONE
                   + ", but has status " + jobUpdated.getStatus(), jobUpdated.getStatus() == JobStatus.DONE);

        Map domainConfigurationMap = jobUpdated.getDomainConfigurationMap();

        assertTrue("The DomainConfigurationMap of the retrieved job does not match that of the original job "
                   + " - domain name " + dc.getDomain().getName() + " not found",
                   domainConfigurationMap.containsKey(dc.getDomain().getName()));
        assertTrue("The DomainConfigurationMap of the retrieved job does not match that of the original job"
                   + " - domain name " + anotherConfiguration.getDomain().getName() + " not found",
                   domainConfigurationMap.containsKey(anotherConfiguration.getDomain().getName()));

        assertEquals("The DomainConfigurationMap of the retrieved job does not match that of the original job"
                     + " - domainConfiguration name " + dc.getName() + " not found",
                     domainConfigurationMap.get(dc.getDomain().getName()),
                     dc.getName());

        assertEquals("The DomainConfigurationMap of the retrieved job does not match that of the original job"
                     + " - domainConfiguration name " + anotherConfiguration.getName() + " not found",
                     domainConfigurationMap.get(anotherConfiguration.getDomain().getName()),
                     anotherConfiguration.getName());
    }

    /**
     * Test that the max objects per domain attribute can be updated in persistent storage.
     * @throws IOException
     */
    public void testJobUpdateForceMaxObjectsPerDomain() throws IOException {
        JobDAO dao = JobDAO.getInstance();

        /* Create Job to update */
        DomainConfiguration dc = TestInfo.getDRConfiguration();
        Job job = Job.createSnapShotJob(TestInfo.HARVESTID, dc, TestInfo.MAX_OBJECTS_PER_DOMAIN, -1, 0);
        dao.create(job);

        // check that the modified job can be retrieved
        JobDAO dao2 = JobDAO.getInstance();
        Job jobUpdated = dao2.read(job.getJobID());

        assertTrue("The retrieved job should have max object per domain = " +
                   TestInfo.MAX_OBJECTS_PER_DOMAIN + ", but it is equal to "
                   + jobUpdated.getForceMaxObjectsPerDomain(),
                   jobUpdated.getForceMaxObjectsPerDomain() == TestInfo.MAX_OBJECTS_PER_DOMAIN);

        // check that the job-specific order.xml is modified accordingly:

        final Document orderXMLdoc = jobUpdated.getOrderXMLdoc();
        String xpath =
                "/crawl-order/controller"
                + "/newObject[@name='frontier']/long[@name='queue-total-budget']";
        Node queueTotalBudgetNode = orderXMLdoc.selectSingleNode(xpath);
        assertEquals("OrderXML value should equals set value",
                     TestInfo.MAX_OBJECTS_PER_DOMAIN,
                     Integer.parseInt(queueTotalBudgetNode.getText()));

    }


    /*
     * Check that an apropriate number of jobs of various statuses are found
     * with getAll()
     */
    private void assertJobsFound(String msg,
                                 int c_new, int c_submitted, int c_started,
                                 int c_failed, int c_done) {
        JobDAO jdao = JobDAO.getInstance();
        assertEquals(c_new + " jobs with status NEW should be present " + msg,
                     c_new, IteratorUtils.toList(jdao.getAll(JobStatus.NEW)).size());
        assertEquals(c_started + " jobs with status STARTED should be present " + msg,
                     c_started, IteratorUtils.toList(jdao.getAll(JobStatus.STARTED)).size());
        assertEquals(c_submitted + " jobs with status SUBMITTED should be present " + msg,
                     c_submitted, IteratorUtils.toList(jdao.getAll(JobStatus.SUBMITTED)).size());
        assertEquals(c_failed + " jobs with status FAILED should be present " + msg,
                     c_failed, IteratorUtils.toList(jdao.getAll(JobStatus.FAILED)).size());
        assertEquals((INITIAL_JOB_COUNT + c_done) + " jobs with status DONE should be present " + msg,
                     INITIAL_JOB_COUNT + c_done, IteratorUtils.toList(jdao.getAll(JobStatus.DONE)).size());
    }

    /**
     * Test getting jobs with various statuses
     * @throws Exception
     */
    public void testGetAll() throws Exception {
        JobDAO jdao = JobDAO.getInstance();
        assertJobsFound("at start", 0, 0, 0, 0, 0);
        // Now add some jobs.
        Domain domain = DomainDAO.getInstance().read("netarkivet.dk");
        DomainConfiguration cfg = domain.getDefaultConfiguration();
        int num_jobs = 5;
        List<Job> jobs = new ArrayList<Job>(num_jobs);
        for (int i = 0; i < num_jobs; i++) {
            Job j = Job.createJob(new Long(42), cfg, 0);
            jdao.create(j);
            jobs.add(j);
        }
        // Check that they all exist
        assertJobsFound("after adding jobs", num_jobs, 0, 0, 0, 0);
        setJobStatus(jobs, 0, JobStatus.NEW);
        setJobStatus(jobs, 1, JobStatus.STARTED);
        setJobStatus(jobs, 2, JobStatus.SUBMITTED);
        setJobStatus(jobs, 3, JobStatus.DONE);
        setJobStatus(jobs, 4, JobStatus.FAILED);
        assertJobsFound("after setting one of each", 1, 1, 1, 1, 1);
        setJobStatus(jobs, 0, JobStatus.STARTED);
        setJobStatus(jobs, 1, JobStatus.STARTED);
        setJobStatus(jobs, 2, JobStatus.FAILED);
        setJobStatus(jobs, 3, JobStatus.FAILED);
        setJobStatus(jobs, 4, JobStatus.FAILED);
        assertJobsFound("only started and failed jobs", 0, 0, 2, 3, 0);
    }

    public void testPersistenseOfPriority() {
        //create two jobs with different priority
        Domain d = Domain.getDefaultDomain("testdomain.dk");
        DomainDAO.getInstance().create(d);
        Job job0 = Job.createJob(new Long(1), d.getDefaultConfiguration(), 0);
        assertEquals("A new job should have high priority", JobPriority.HIGHPRIORITY,
                     job0.getPriority());
        Job job1 = Job.createSnapShotJob(new Long(1),
                                         d.getDefaultConfiguration(), 2000, -1, 0);
        assertEquals("A new job should have low priority", JobPriority.LOWPRIORITY,
                     job1.getPriority());

        //save them
        JobDAO jobDAO = JobDAO.getInstance();
        jobDAO.create(job0);
        jobDAO.create(job1);

        //read them again
        Job job2 = jobDAO.read(job0.getJobID());
        Job job3 = jobDAO.read(job1.getJobID());

        //check the priorities
        assertEquals("Jobs should preserve priority", job0.getPriority(),
                     job2.getPriority());

        //check the priorities
        assertEquals("Jobs should preserve priority", job1.getPriority(),
                     job3.getPriority());
    }

    private void setJobStatus(List<Job> jobs, int i, JobStatus status) {
        (jobs.get(i)).setStatus(status);
        JobDAO jdao = JobDAO.getInstance();
        jdao.update((jobs.get(i)));
    }

    /** Test that the job error info is stored correctly. */
    public void testPersistenceOfJobErrors() {
        Domain d = Domain.getDefaultDomain("testdomain.dk");
        DomainDAO.getInstance().create(d);
        Job j = Job.createJob(new Long(1), d.getDefaultConfiguration(), 0);
        JobDAO dao = JobDAO.getInstance();
        dao.create(j);
        Job j2 = dao.read(j.getJobID());
        assertNull("Should have no harvest error by default", j2.getHarvestErrors());
        assertNull("Should have no harvest error details by default", j2.getHarvestErrorDetails());
        assertNull("Should have no upload error by default", j2.getUploadErrors());
        assertNull("Should have no upload error details by default", j2.getUploadErrorDetails());
        j2.appendHarvestErrors("str1");
        j2.appendHarvestErrorDetails("str2");
        j2.appendUploadErrors("str3");
        j2.appendUploadErrorDetails("str4");
        dao.update(j2);
        Job j3 = dao.read(j2.getJobID());
        assertEquals("Should have new harvest error string",
                     "str1", j3.getHarvestErrors());
        assertEquals("Should have new harvest error detail string",
                     "str2", j3.getHarvestErrorDetails());
        assertEquals("Should have new upload error string",
                     "str3", j3.getUploadErrors());
        assertEquals("Should have new upload error detail string",
                     "str4", j3.getUploadErrorDetails());
    }
    /**
     * Reset the job dao.
     */
    public static void resetDAO() {
        JobDAO.reset();
    }

    /** Test that we can get reasonable status info about jobs.
     * @throws Exception
     */
    public void testGetStatusInfo() throws Exception {
        TemplateDAO.getInstance();
        DomainDAO ddao = DomainDAO.getInstance();
        ScheduleDAO.getInstance();
        HarvestDefinitionDAO.getInstance();
        JobDAO dao = JobDAO.getInstance();
        Job j = dao.read(1L);
        List<JobStatusInfo> infos = dao.getStatusInfo();
        assertEquals("Should get info on one job", 1, infos.size());
        JobStatusInfo info = infos.get(0);
        checkInfoCorrect(j, info);

        Domain d = TestInfo.getDefaultDomain();
        DomainConfiguration dc = TestInfo.getDefaultConfig(d);
        d.addConfiguration(dc);
        d.setDefaultConfiguration(dc.getName());
        ddao.create(d);
        Job j2 = new Job(43L, dc, JobPriority.HIGHPRIORITY, 0L, -1, 3);
        dao.create(j2);
        j2.appendUploadErrors("Bad stuff");
        j2.appendHarvestErrors("Good harvest");
        j2.setActualStart(new Date());
        dao.update(j2);

        infos = dao.getStatusInfo();
        assertEquals("Should get info on two jobs", 2, infos.size());
        info = infos.get(0);
        JobStatus statusj = info.getStatus();
        checkInfoCorrect(j, info);
        info = infos.get(1);
        JobStatus statusj2 = info.getStatus();
        checkInfoCorrect(j2, info);
        assertEquals("Status DONE for first job", statusj, JobStatus.DONE);
        assertEquals("Status NEW for second job", statusj2, JobStatus.NEW);
        

        infos = dao.getStatusInfo(false);
        assertEquals("Should get info on two jobs", 2, infos.size());
        info = infos.get(0);
        statusj = info.getStatus();
        checkInfoCorrect(j2, info);
        info = infos.get(1);
        statusj2 = info.getStatus();
        checkInfoCorrect(j, info);
        assertEquals("Status NEW for first job", statusj, JobStatus.NEW);
        assertEquals("Status DONE for second job", statusj2, JobStatus.DONE);

        
        infos = dao.getStatusInfo(JobStatus.DONE);
        assertEquals("Should get info on one job with status DONE", 1, infos.size());
        infos = dao.getStatusInfo(JobStatus.DONE, true);
        assertEquals("Should get info on one job with status DONE (ascending)", 1, infos.size());
        infos = dao.getStatusInfo(JobStatus.DONE, false);
        assertEquals("Should get info on one job with status DONE (descending)", 1, infos.size());
        infos = dao.getStatusInfo(JobStatus.NEW);
        assertEquals("Should get info on one job with status NEW", 1, infos.size());
        infos = dao.getStatusInfo(JobStatus.FAILED);
        assertEquals("Should get info on no job with status FAILED", 0, infos.size());
    }

    /** Test that we can get reasonable status info about jobs from specific
     * harvest runs.
     * @throws Exception
     */
    public void testGetStatusInfoForHarvest() throws Exception {
        DomainDAO ddao = DomainDAO.getInstance();
        JobDAO dao = JobDAO.getInstance();

        Domain d = TestInfo.getDefaultDomain();
        DomainConfiguration dc = TestInfo.getDefaultConfig(d);
        d.addConfiguration(dc);
        d.setDefaultConfiguration(dc.getName());
        ddao.create(d);
        Job j2 = new Job(43L, dc, JobPriority.HIGHPRIORITY, 0L, -1, 3);
        dao.create(j2);

        Job j3 = new Job(43L, dc, JobPriority.HIGHPRIORITY, 0L, -1, 4);
        dao.create(j3);

        Job j4 = new Job(43L, dc, JobPriority.HIGHPRIORITY, 0L, -1, 4);
        dao.create(j4);

        List<JobStatusInfo> infos = dao.getStatusInfo(43L, 0);
        assertEquals("Should get info on no jobs", 0, infos.size());

        infos = dao.getStatusInfo(117L, 23);
        assertEquals("Should get info on no jobs", 0, infos.size());

        infos = dao.getStatusInfo(43L, 3);
        assertEquals("Should get info on one job", 1, infos.size());
        JobStatusInfo info = infos.get(0);
        checkInfoCorrect(j2, info);

        infos = dao.getStatusInfo(43L, 4);
        assertEquals("Should get info on two jobs", 2, infos.size());
        info = infos.get(0);
        checkInfoCorrect(j3, info);
        info = infos.get(1);
        checkInfoCorrect(j4, info);
    }

    private void checkInfoCorrect(Job j, JobStatusInfo info) {
        HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        assertEquals("Info should be for job " + j.getJobID(),
                     j.getJobID(), j.getJobID());
        assertEquals("Status for job " + j.getJobID(),
                     j.getStatus(), info.getStatus());
        assertEquals("HarvestID for job " + j.getJobID(),
                     (long)j.getOrigHarvestDefinitionID(), info.getHarvestDefinitionID());
        assertEquals("HarvestNum for job " + j.getJobID(),
                     j.getHarvestNum(), info.getHarvestNum());
        assertEquals("HarvestName for job " + j.getJobID(),
                     hddao.read(j.getOrigHarvestDefinitionID()).getName(),
                     info.getHarvestDefinition());
        assertEquals("HarvestError for job " + j.getJobID(),
                     j.getHarvestErrors(), info.getHarvestErrors());
        assertEquals("UploadError for job " + j.getJobID(),
                     j.getUploadErrors(), info.getUploadErrors());
        assertEquals("OrderXML name for job  " + j.getJobID(),
                     j.getOrderXMLName(), info.getOrderXMLname());
        assertEquals("Domain count for job " + j.getJobID(),
                     j.getDomainConfigurationMap().size(), info.getConfigCount());
        assertEquals("Start date for job " + j.getJobID(),
                     j.getActualStart(), info.getStartDate());
        assertEquals("End date for job " + j.getJobID(),
                     j.getActualStop(), info.getEndDate());
    }

    /** Check that start and end dates are created and stored correctly. */
    public void testSetDates() {
        JobDAO jdao = JobDAO.getInstance();
        DomainDAO ddao = DomainDAO.getInstance();
        Domain d = ddao.read("netarkivet.dk");
        DomainConfiguration dc = d.getDefaultConfiguration();
        Date startDate = new Date();
        Job newJob1 = new Job(42L, dc, JobPriority.HIGHPRIORITY, 10L, -1, 2);
        newJob1.setStatus(JobStatus.SUBMITTED);
        jdao.create(newJob1);
        assertNull("Should have null start date at start, but was "
                   + newJob1.getActualStart(), newJob1.getActualStart());
        assertNull("Should have null stop date at start, but was "
                   + newJob1.getActualStop(), newJob1.getActualStop());
        newJob1.setStatus(JobStatus.STARTED);
        assertNotNull("Should have non-null start date after starting",
                      newJob1.getActualStart());
        assertFalse("Should have updated start date after starting (>= before)",
                    startDate.after(newJob1.getActualStart()));
        assertFalse("Should have updated start date after starting (<= now)",
                    new Date().before(newJob1.getActualStart()));
        assertNull("Should have null stop date after starting, but was "
                   + newJob1.getActualStop(), newJob1.getActualStop());
        jdao.update(newJob1);
        Job newJob2 = jdao.read(newJob1.getJobID());
        assertNotNull("Should have non-null start date after rereading",
                      newJob2.getActualStart());
        assertEquals("Should have same start date after rereading",
                     newJob1.getActualStart(), newJob2.getActualStart());
        assertNull("Should have null stop date after rereading, but was "
                   + newJob2.getActualStop(),  newJob2.getActualStop());
        try {
            // Make sure new time is different
            Thread.sleep(1);
        } catch (InterruptedException e) {
            // Ignored
        }
        Date stopDate = new Date();
        newJob2.setStatus(JobStatus.DONE);
        assertNotNull("Should have non-null start date after finishing",
                      newJob2.getActualStart());
        assertEquals("Should have same start date after rereading",
                     newJob1.getActualStart(), newJob2.getActualStart());
        assertNotNull("Should have non-null stop date after finishing",
                      newJob2.getActualStop());
        assertFalse("Should have updated stop date after finishing (>= before)",
                    stopDate.after(newJob2.getActualStop()));
        assertFalse("Should have updated stop date after finishing (<= now)",
                    new Date().before(newJob2.getActualStop()));
        jdao.update(newJob2);
        Job newJob3 = jdao.read(newJob2.getJobID());
        assertNotNull("Should have non-null start date after rerereading",
                      newJob3.getActualStart());
        assertEquals("Should have same start date after rereading",
                     newJob2.getActualStart(), newJob3.getActualStart());
        assertNotNull("Should have non-null stop date after rerereading",
                      newJob3.getActualStop());
        assertEquals("Should have same stop date after rereading",
                     newJob2.getActualStop(), newJob3.getActualStop());
        // Also check that you can't mess with the dates.
        try {
            // Make sure new time is different
            Thread.sleep(1);
        } catch (InterruptedException e) {
            // Ignored
        }
        
        // It is now possible to set start date to after end date '
        // without any exceptions being thrown. However, a notification is emitted.
        Settings.set(Settings.NOTIFICATIONS_CLASS,
                RememberNotifications.class.getName());
        RememberNotifications.resetSingleton();
        
        try {
            newJob3.setActualStart(new Date());
            assertTrue("Setting start date to after end date should result in notification", 
                    RememberNotifications.getInstance().message.length() > 0);
        } catch (ArgumentNotValid e) {
            fail ("Setting start date to after end date should not throw exception: " + e);
        }
        RememberNotifications.resetSingleton();        
        newJob3.setActualStart(stopDate);
    
        try {
            newJob3.setActualStop(startDate);
            assertTrue("Setting stop date to before end date should result in notification",
                    RememberNotifications.getInstance().message.length() > 0);
        } catch (ArgumentNotValid e) {
            fail ("Setting stop date to before end date should not throw exception: " + e);
        } 
    }

    /** Tests the retrieval of jobs to use for duplicate reduction.
     *
     * The following cases are tested:
     *
     * Unknown job ID.
     * Partial harvests with no previous harvest is present should return empty
     * list.
     * Partial harvests with two previous harvests should return jobs from first
     * harvest.
     * Full harvest with no previous full harvests should return empty list.
     * Full harvest not based on anything but with previous chain should return
     * that.
     * Full harvest based on something but with no previous chain should return
     * that.
     * Full harvest based on something AND with previous chains should return
     * that.
     *
     *
     * @throws Exception
     */
    public void testGetJobIDsForDuplicateReduction() throws Exception {
        createTestJobs();
        JobDAO dao = JobDAO.getInstance();

        try {
            dao.getJobIDsForDuplicateReduction(9999L);
            fail("Expected UnknownID on job ID not in database");
        } catch (UnknownID e) {
            //expected
        }

        List<Long> result;
        List<Long> expected;

        result = dao.getJobIDsForDuplicateReduction(2L);
        assertEquals("Should get empty list on no previous harvest",
                     0, result.size());

        result = dao.getJobIDsForDuplicateReduction(7L);
        expected = Arrays.asList(new Long[]{4L, 5L});
        Collections.sort(result);
        Collections.sort(expected);
        assertEquals("Should get previous harvests' job ids in list",
                     expected, result);

        result = dao.getJobIDsForDuplicateReduction(8L);
        assertEquals("Should get empty list on no previous harvest",
                     0, result.size());

        result = dao.getJobIDsForDuplicateReduction(10L);
        expected = Arrays.asList(new Long[]{8L, 9L});
        Collections.sort(result);
        Collections.sort(expected);
        assertEquals("Should get originating harvests' job ids in list",
                     expected, result);

        result = dao.getJobIDsForDuplicateReduction(12L);
        expected = Arrays.asList(new Long[]{8L, 9L, 10L, 11L});
        Collections.sort(result);
        Collections.sort(expected);
        assertEquals("Should get previous full harvests' job ids in list",
                     expected, result);

        result = dao.getJobIDsForDuplicateReduction(14L);
        expected = Arrays.asList(new Long[]{8L, 9L, 10L, 11L, 12L, 13L});
        Collections.sort(result);
        Collections.sort(expected);
        assertEquals("Should get previous full harvests' job ids in list",
                     expected, result);
    }

    private void compareCopiedJob(Job oldJob1, Job newJob1, Long newID) {
        assertEquals("Should have same domain count",
                oldJob1.getCountDomains(), newJob1.getCountDomains());
        assertEquals("Should have same domain config map",
                oldJob1.getDomainConfigurationMap(),
                newJob1.getDomainConfigurationMap());
        assertEquals("Should have same forceMaxObjects",
                oldJob1.getForceMaxObjectsPerDomain(),
                newJob1.getForceMaxObjectsPerDomain());
        assertEquals("Should have same alias info",
                oldJob1.getJobAliasInfo(), newJob1.getJobAliasInfo());
        assertEquals("Should have same max bytes",
                oldJob1.getMaxBytesPerDomain(), newJob1.getMaxBytesPerDomain());
        assertEquals("Should have same max objects",
                oldJob1.getMaxObjectsPerDomain(),
                newJob1.getMaxObjectsPerDomain());
        assertEquals("Should have same order.xml",
                oldJob1.getOrderXMLdoc().asXML(), newJob1.getOrderXMLdoc().asXML());
        assertEquals("Should have same order xml name",
                oldJob1.getOrderXMLName(), newJob1.getOrderXMLName());
        assertEquals("Should have same original harvest id",
                oldJob1.getOrigHarvestDefinitionID(),
                newJob1.getOrigHarvestDefinitionID());
        assertEquals("Should have same priority",
                oldJob1.getPriority(), newJob1.getPriority());
        assertEquals("Should have same seedlist",
                oldJob1.getSeedListAsString(), newJob1.getSeedListAsString());
        assertEquals("Should have same settingsxml docs",
                oldJob1.getSettingsXMLdocs(), newJob1.getSettingsXMLdocs());
        assertEquals("Should have same settingsxml files",
                oldJob1.getSettingsXMLfiles(), newJob1.getSettingsXMLfiles());
        assertEquals("Should have new status",
                     JobStatus.NEW, newJob1.getStatus());
        assertEquals("Should have new edition",
                1L, newJob1.getEdition());
        assertEquals("Should have new ID", newID, newJob1.getJobID());
    }

    public static void changeStatus(long jobID, JobStatus newStatus) {
        PreparedStatement s = null;
        Connection c = DBConnect.getDBConnection();
        try {
            s = c.prepareStatement("update jobs set status=? where job_id=?");
            s.setLong(1, newStatus.ordinal());
            s.setLong(2, jobID);
            s.executeUpdate();
        } catch (SQLException e) {
            String message = "SQL error changing job state for job with id="
                + jobID + " in database";
            throw new IOFailure(message, e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }

    /**
     * Tests method in JobDBDAO.rescheduleJob
     * Now verifies, that the new job has startdate and enddate set to null.
     */
    public void testRescheduleJob() {
        createTestJobs();
        JobDAO dao = JobDAO.getInstance();

        for (long i = 1; i < 16; i++) {
            Job oldJob = dao.read(i);
            if (oldJob.getStatus() != JobStatus.SUBMITTED &&
                    oldJob.getStatus() != JobStatus.FAILED) {
                try {
                    dao.rescheduleJob(i);
                    fail("Should not have been able to resubmit job " + oldJob);
                } catch (IllegalState e) {
                    // expected;
                }
            }
        }

        for (long i = 1; i < 16; i++) {
            changeStatus(i, i % 2 == 0 ? JobStatus.SUBMITTED : JobStatus.FAILED);
            long newJobID = dao.rescheduleJob(i);
            Job oldJob = dao.read(i);
            Job newJob = dao.read(newJobID);
            long newID = i+15;
            compareCopiedJob(oldJob, newJob, newID);
            assertEquals("Old job should have resubmitted status",
                         JobStatus.RESUBMITTED, oldJob.getStatus());
            assertTrue("New job must have null startdate", newJob.getActualStart() == null);
            assertTrue("New job must have null enddate", newJob.getActualStop() == null);
        }

        try {
            dao.rescheduleJob(42L);
            fail("Should not have been able to resubmit non-existing job");
        } catch (UnknownID e) {
            // expected
        }
    }
}