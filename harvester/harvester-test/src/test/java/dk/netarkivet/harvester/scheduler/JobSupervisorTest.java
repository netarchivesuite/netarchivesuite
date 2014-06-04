/* File:    $Id$
 * Revision: $Revision$
 * Author:   $Author$
 * Date:     $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.datamodel.DatabaseTestUtils;
import dk.netarkivet.harvester.datamodel.Domain;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.datamodel.HarvestDAOUtils;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.JobStatusInfo;
import dk.netarkivet.harvester.webinterface.HarvestStatusQuery;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import java.io.File;
import java.io.FileInputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.LogManager;
import javax.inject.Provider;
import junit.framework.TestCase;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class JobSupervisorTest extends TestCase {
    private ReloadSettings reloadSettings = new ReloadSettings();
    private JobSupervisor jobSupervisor;
    private JobDAO jobDaoMock = mock(JobDAO.class);
    private Provider<JobDAO> jobDAOProvider;

    public void setUp() throws Exception {
        reloadSettings.setUp();

        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestInfo.WORKING_DIR.mkdirs();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
        FileInputStream testLogPropertiesStream = new FileInputStream(TestInfo.TESTLOGPROP);
        LogManager.getLogManager().readConfiguration(testLogPropertiesStream);
        testLogPropertiesStream.close();
        Settings.set(CommonSettings.DB_BASE_URL, "jdbc:derby:" + TestInfo.WORKING_DIR.getCanonicalPath() + "/fullhddb");
        DatabaseTestUtils.getHDDB(new File(TestInfo.BASEDIR, "fullhddb.jar"), "fullhddb", TestInfo.WORKING_DIR);

        // HarvestDAOUtils.resetDAOs();

        Settings.set(CommonSettings.NOTIFICATIONS_CLASS, RememberNotifications.class.getName());

        HarvestDefinitionDAO.getInstance();

        jobDAOProvider = new Provider<JobDAO>() {
            @Override
            public JobDAO get() {
                return jobDaoMock;
            }
        };
    }

    /**
     * After test is done close test-objects.
     */
    public void tearDown() throws SQLException, IllegalAccessException, NoSuchFieldException {
        DatabaseTestUtils.dropHDDB();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        HarvestDAOUtils.resetDAOs();
        reloadSettings.tearDown();
        // HarvestJobGenerator.clearGeneratingJobs();
    }

    public void testCleanOldJobsMultipleJobs() {
        Long jobTimeoutTime = 1L;
        jobSupervisor = new JobSupervisor(jobDAOProvider, jobTimeoutTime);

        List<Long> jobIDs = Arrays.asList(1L, 2L, 3L);
        when(jobDaoMock.getAllJobIds(JobStatus.STARTED)).thenReturn(jobIDs.iterator());
        Job pastObsoleteJobMock = mock(Job.class);
        Job pastActiveMock = mock(Job.class);
        Job futureActiveMock = mock(Job.class);
        when(jobDaoMock.read(1L)).thenReturn(pastObsoleteJobMock);
        when(jobDaoMock.read(2L)).thenReturn(pastActiveMock);
        when(jobDaoMock.read(3L)).thenReturn(futureActiveMock);

        Date inTheObsoletePast = new Date(System.currentTimeMillis() - 10000);
        Date inTheActivePast = new Date(System.currentTimeMillis() - 1);
        Date inTheActiveFuture = new Date(System.currentTimeMillis() + 10000);

        when(pastObsoleteJobMock.getActualStart()).thenReturn(inTheObsoletePast);
        when(pastActiveMock.getActualStart()).thenReturn(inTheActivePast);
        when(futureActiveMock.getActualStart()).thenReturn(inTheActiveFuture);

        jobSupervisor.cleanOldJobs();

        verify(jobDaoMock).getAllJobIds(JobStatus.STARTED);

        verify(jobDaoMock).read(jobIDs.get(0));
        verify(jobDaoMock).read(jobIDs.get(1));
        verify(jobDaoMock).read(jobIDs.get(2));

        verify(pastObsoleteJobMock).getActualStart();
        verify(pastActiveMock).getActualStart();
        verify(futureActiveMock).getActualStart();
        verify(pastObsoleteJobMock).setStatus(JobStatus.FAILED);
        verify(pastObsoleteJobMock).appendHarvestErrors(any(String.class));
        verifyNoMoreInteractions(pastActiveMock, futureActiveMock);

        verify(jobDaoMock).update(pastObsoleteJobMock);
        verifyNoMoreInteractions(jobDaoMock);

        // Fixme Consider testing the logging
    }

    public void testCleanOldJobsNoJobs() {
        Long jobTimeoutTime = 1L;
        jobSupervisor = new JobSupervisor(jobDAOProvider, jobTimeoutTime);

        List<Long> jobIDs = Arrays.asList(new Long[] {});
        when(jobDaoMock.getAllJobIds(JobStatus.STARTED)).thenReturn(jobIDs.iterator());

        jobSupervisor.cleanOldJobs();

        verify(jobDaoMock).getAllJobIds(JobStatus.STARTED);
        verifyNoMoreInteractions(jobDaoMock);
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
        assertTrue("Should be at least one domain in domains table", domainsIterator.hasNext());
        DomainConfiguration cfg = domainsIterator.next().getDefaultConfiguration();
        final JobDAO jdao = JobDAO.getInstance();

        final Long harvestID = 1L;
        // Verify that harvestDefinition with ID=1L exists
        assertTrue("harvestDefinition with ID=" + harvestID + " does not exist, but should have", HarvestDefinitionDAO
                .getInstance().exists(harvestID));
        // Create 7 jobs, one in each JobStatus:
        // (NEW, SUBMITTED, STARTED, DONE, FAILED, FAILED_REJECTED, RESUBMITTED)
        for (JobStatus status : JobStatus.values()) {
            Job newJob = Job.createJob(harvestID, new HarvestChannel("test", false, true, ""), cfg, 1);
            newJob.setStatus(status);
            jdao.create(newJob);
        }

        List<JobStatusInfo> oldInfos = jdao.getStatusInfo(new HarvestStatusQuery()).getJobStatusInfo();
        // Since initial DB contains one NEW job, we now have one of each
        // status plus one extra NEW (i.e. 8 jobs).

        assertTrue("There should have been 8 jobs now, but there was " + oldInfos.size(), oldInfos.size() == 7);

        jobSupervisor.rescheduleLeftOverJobs();
        List<JobStatusInfo> newInfos = jdao.getStatusInfo(new HarvestStatusQuery()).getJobStatusInfo();
        // Check that all old jobs are there, with one changed status
        OLDS: for (JobStatusInfo oldInfo : oldInfos) {
            for (JobStatusInfo newInfo : newInfos) {
                if (newInfo.getJobID() == oldInfo.getJobID()) {
                    if (oldInfo.getStatus() == JobStatus.SUBMITTED) {
                        assertEquals("SUBMITTED job should be RESUBMITTED", JobStatus.RESUBMITTED, newInfo.getStatus());
                    } else {
                        assertEquals("Non-SUBMITTED job should be unchanged", oldInfo.getStatus(), newInfo.getStatus());
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
            assertEquals("Newly created job should be in status NEW", JobStatus.NEW, newInfo.getStatus());
        }
        assertTrue("Should have found new job", foundNewJob);
    }
}
