/* File:    $Id$
 * Revision: $Revision$
 * Author:   $Author$
 * Date:     $Date$
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
 * Foundation,dk.netarkivet.harvester.schedulerFloor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.harvester.scheduler;

import java.io.File;
import java.io.FileInputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.LogManager;

import junit.framework.TestCase;
import dk.netarkivet.TestUtils;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.datamodel.DatabaseTestUtils;
import dk.netarkivet.harvester.datamodel.Domain;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.JobStatusInfo;
import dk.netarkivet.harvester.webinterface.HarvestStatusQuery;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

public class JobSupervisorTest extends TestCase  {
    private ReloadSettings reloadSettings = new ReloadSettings();
    private JobSupervisor jobSupervisor;
    
    public void setUp() throws Exception {
        reloadSettings.setUp();
        jobSupervisor = new JobSupervisor();
        
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestInfo.WORKING_DIR.mkdirs();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                TestInfo.WORKING_DIR);
        FileInputStream testLogPropertiesStream = new FileInputStream(
                TestInfo.TESTLOGPROP);
        LogManager.getLogManager().readConfiguration(testLogPropertiesStream);
        testLogPropertiesStream.close();
        Settings.set(CommonSettings.DB_BASE_URL, "jdbc:derby:"
                + TestInfo.WORKING_DIR.getCanonicalPath() + "/fullhddb");
        DatabaseTestUtils.getHDDB(new File(TestInfo.BASEDIR, "fullhddb.jar"),
                "fullhddb", TestInfo.WORKING_DIR);

        //TestUtils.resetDAOs();

        Settings.set(CommonSettings.NOTIFICATIONS_CLASS,
                RememberNotifications.class.getName());

        HarvestDefinitionDAO.getInstance();
    }
    
    /**
     * After test is done close test-objects.
     */
    public void tearDown() throws SQLException, IllegalAccessException,
            NoSuchFieldException {
        DatabaseTestUtils.dropHDDB();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestUtils.resetDAOs();
        reloadSettings.tearDown();
        //HarvestJobGenerator.clearGeneratingJobs();
    }

    public void testCleanOldJobs() throws Exception {
        final DomainDAO dao = DomainDAO.getInstance();
        Iterator<Domain> domainsIterator = dao.getAllDomains();
        assertTrue("Should be at least one domain in domains table",
                domainsIterator.hasNext());
        DomainConfiguration cfg = domainsIterator.next()
                .getDefaultConfiguration();
        final JobDAO jdao = JobDAO.getInstance();

        final Long harvestID = 1L;
        // Verify that harvestDefinition with ID=1L exists
        assertTrue("harvestDefinition with ID=" + harvestID
                + " does not exist, but should have", HarvestDefinitionDAO
                .getInstance().exists(harvestID));
        // Create 6 jobs - with start time minus 60*60*24*7 +1
        // (one week plus one second)
        for (int i = 0; i < 6; i++) {
            Job newJob = Job.createJob(harvestID, cfg, 1);
            newJob.setActualStart(new Date((new Date()).getTime()
                    - (604801 * 1000)));
            newJob.setStatus(JobStatus.STARTED);
            jdao.create(newJob);
        }
        // Create 6 new jobs with now
        for (int i = 0; i < 6; i++) {
            Job newJob = Job.createJob(harvestID, cfg, 1);
            newJob.setActualStart(new Date());
            newJob.setStatus(JobStatus.STARTED);
            jdao.create(newJob);
        }
        List<JobStatusInfo> oldInfos = jdao.getStatusInfo(
                new HarvestStatusQuery()).getJobStatusInfo();
        // Since initial DB contains one NEW job, we now have one of each
        // status plus one extra NEW (i.e. 7 jobs).
        assertEquals("Wrong number of job statuses", oldInfos.size(), 12);

        Iterator<Long> ids = jdao.getAllJobIds(JobStatus.STARTED);
        int size = 0;
        while (ids.hasNext()) {
            ids.next();
            size++;
        }
        assertTrue("There should be 12 jobs with status STARTED, there are "
                + size, size == 12);
        jobSupervisor.cleanOldJobs();

        // check that we have 6 failed and 6 submitted job after we have stopped
        // old jobs
        ids = jdao.getAllJobIds(JobStatus.STARTED);
        size = 0;
        while (ids.hasNext()) {
            ids.next();
            size++;
        }
        assertTrue("There should be 6 jobs with status STARTED, there are "
                + size, size == 6);
        ids = jdao.getAllJobIds(JobStatus.FAILED);
        size = 0;
        while (ids.hasNext()) {
            ids.next();
            size++;
        }
        assertTrue("There should be 6 jobs with status FAILED, there are "
                + size, size == 6);

    }

    /**
     * Unit test testing the private method rescheduleJob.
     *
     * @throws Exception
     *             if HarvestScheduler throws exception
     */
    public void testRescheduleSubmittedJobs() throws Exception {
        final DomainDAO dao = DomainDAO.getInstance();
        Iterator<Domain> domainsIterator = dao.getAllDomains();
        assertTrue("Should be at least one domain in domains table",
                domainsIterator.hasNext());
        DomainConfiguration cfg = domainsIterator.next()
                .getDefaultConfiguration();
        final JobDAO jdao = JobDAO.getInstance();

        final Long harvestID = 1L;
        // Verify that harvestDefinition with ID=1L exists
        assertTrue("harvestDefinition with ID=" + harvestID
                + " does not exist, but should have", HarvestDefinitionDAO
                .getInstance().exists(harvestID));
        // Create 7 jobs, one in each JobStatus:
        // (NEW, SUBMITTED, STARTED, DONE, FAILED, FAILED_REJECTED, RESUBMITTED)
        for (JobStatus status : JobStatus.values()) {
            Job newJob = Job.createJob(harvestID, cfg, 1);
            newJob.setStatus(status);
            jdao.create(newJob);
        }

        List<JobStatusInfo> oldInfos = jdao.getStatusInfo(
                new HarvestStatusQuery()).getJobStatusInfo();
        // Since initial DB contains one NEW job, we now have one of each
        // status plus one extra NEW (i.e. 8 jobs).

        assertTrue("There should have been 8 jobs now, but there was "
                + oldInfos.size(), oldInfos.size() == 7);

        jobSupervisor.rescheduleLeftOverJobs();
        List<JobStatusInfo> newInfos = jdao.getStatusInfo(
                new HarvestStatusQuery()).getJobStatusInfo();
        // Check that all old jobs are there, with one changed status
        OLDS: for (JobStatusInfo oldInfo : oldInfos) {
            for (JobStatusInfo newInfo : newInfos) {
                if (newInfo.getJobID() == oldInfo.getJobID()) {
                    if (oldInfo.getStatus() == JobStatus.SUBMITTED) {
                        assertEquals("SUBMITTED job should be RESUBMITTED",
                                JobStatus.RESUBMITTED, newInfo.getStatus());
                    } else {
                        assertEquals("Non-SUBMITTED job should be unchanged",
                                oldInfo.getStatus(), newInfo.getStatus());
                    }
                    continue OLDS;
                }
            }
            fail("Job " + oldInfo + " has disappeared!");
        }

        // Check that a new job is there, in status submitted
        boolean foundNewJob = false;
        NEWS: for (JobStatusInfo newInfo : newInfos) {
            for (JobStatusInfo oldInfo : oldInfos) {
                if (newInfo.getJobID() == oldInfo.getJobID()) {
                    continue NEWS;
                }
            }
            // This new job was not found in old jobs list.
            foundNewJob = true;
            assertEquals("Newly created job should be in status NEW",
                    JobStatus.NEW, newInfo.getStatus());
        }
        assertTrue("Should have found new job", foundNewJob);
    }
}
