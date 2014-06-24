package dk.netarkivet.harvester.scheduler;

import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import javax.inject.Provider;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class JobSupervisorTest {
    private JobSupervisor jobSupervisor;
    private JobDAO jobDaoMock = mock(JobDAO.class);
    private Provider<JobDAO> jobDAOProvider;

    @Before
    public void setUp() {
        jobDAOProvider = new Provider<JobDAO>() {
            @Override
            public JobDAO get() {
                return jobDaoMock;
            }
        };
    }

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
