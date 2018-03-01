/*
 * #%L
 * Netarchivesuite - harvester
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
package dk.netarkivet.harvester.scheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Provider;

import org.junit.Test;
import org.mockito.Matchers;

import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;

public class JobSupervisorTest {
    private JobSupervisor jobSupervisor;
    private JobDAO jobDaoMock = mock(JobDAO.class);
    // JAVA 8 required
    //private Provider<JobDAO> jobDAOProvider = () -> jobDaoMock;
    private Provider<JobDAO> jobDAOProvider = new Provider<JobDAO>() {
        @Override
        public JobDAO get() {
            return jobDaoMock;
        }

    };

    @Test
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
        verify(pastObsoleteJobMock).appendHarvestErrors(Matchers.any(String.class));
        verifyNoMoreInteractions(pastActiveMock, futureActiveMock);

        verify(jobDaoMock).update(pastObsoleteJobMock);
        verifyNoMoreInteractions(jobDaoMock);
    }

    @Test
    public void testCleanOldJobsNoJobs() {
        Long jobTimeoutTime = 1L;
        jobSupervisor = new JobSupervisor(jobDAOProvider, jobTimeoutTime);

        List<Long> jobIDs = Arrays.asList(new Long[] {});
        when(jobDaoMock.getAllJobIds(JobStatus.STARTED)).thenReturn(jobIDs.iterator());

        jobSupervisor.cleanOldJobs();

        verify(jobDaoMock).getAllJobIds(JobStatus.STARTED);
        verifyNoMoreInteractions(jobDaoMock);
    }

    @Test
    public void testRescheduleMultipleSubmittedJobs() {
        Long jobTimeoutTime = 1L;
        jobSupervisor = new JobSupervisor(jobDAOProvider, jobTimeoutTime);

        List<Long> jobIDs = Arrays.asList(1L, 3L);
        when(jobDaoMock.getAllJobIds(JobStatus.SUBMITTED)).thenReturn(jobIDs.iterator());

        jobSupervisor.rescheduleLeftOverJobs();

        verify(jobDaoMock).getAllJobIds(JobStatus.SUBMITTED);
        verify(jobDaoMock).rescheduleJob(jobIDs.get(0));
        verify(jobDaoMock).rescheduleJob(jobIDs.get(1));
        verifyNoMoreInteractions(jobDaoMock);
    }

    @Test
    public void testRescheduleNoSubmittedJobs() {
        Long jobTimeoutTime = 1L;
        jobSupervisor = new JobSupervisor(jobDAOProvider, jobTimeoutTime);

        List<Long> jobIDs = Arrays.asList(new Long[] {});
        when(jobDaoMock.getAllJobIds(JobStatus.SUBMITTED)).thenReturn(jobIDs.iterator());

        jobSupervisor.rescheduleLeftOverJobs();

        verify(jobDaoMock).getAllJobIds(JobStatus.SUBMITTED);
        verifyNoMoreInteractions(jobDaoMock);
    }
}
