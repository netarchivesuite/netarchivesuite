/*
 * #%L
 * Netarchivesuite - harvester - test
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

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import javax.inject.Provider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ch.qos.logback.classic.Level;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.utils.Notifications;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.JobStub;
import dk.netarkivet.harvester.distribute.HarvesterChannels;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;
import dk.netarkivet.harvester.harvesting.distribute.CrawlStatusMessage;
import dk.netarkivet.harvester.harvesting.distribute.JobEndedMessage;
import dk.netarkivet.harvester.harvesting.report.HarvestReport;
import dk.netarkivet.testutils.LogbackRecorder;

public class HarvestSchedulerMonitorServerTest {
    private final JMSConnection jmsConnectionMock = mock(JMSConnection.class);
    //private final Provider<JMSConnection> jmsConnectionProvider = () -> jmsConnectionMock;
    private final Provider<JMSConnection> jmsConnectionProvider = new Provider<JMSConnection>() {
        @Override
        public JMSConnection get() {
            return jmsConnectionMock;
        }

    };
    
    private JobDAO jobDAOMock = mock(JobDAO.class);
    //private final Provider<JobDAO> jobDAOProvider = () -> jobDAOMock;
    private final Provider<JobDAO> jobDAOProvider = new Provider<JobDAO>() {
        @Override
        public JobDAO get() {
            return jobDAOMock;
        }

    };

    private final HarvestDefinitionDAO harvestDefinitionDAOMock = mock(HarvestDefinitionDAO.class);
    //private final Provider<HarvestDefinitionDAO> harvestDefinitionDAOProvider = () -> harvestDefinitionDAOMock;
    private final Provider<HarvestDefinitionDAO> harvestDefinitionDAOProvider = new Provider<HarvestDefinitionDAO>(){

        @Override
        public HarvestDefinitionDAO get() {
            return harvestDefinitionDAOMock;
        }

    };

    private final Notifications notificationsMock = mock(Notifications.class);
    //private final Provider<Notifications> notificationsProvider = () -> notificationsMock;
    private final Provider<Notifications> notificationsProvider = new Provider<Notifications>() {

        @Override
        public Notifications get() {

            return notificationsMock;
        }

    };

    private final HarvestSchedulerMonitorServer harvestStatusMonitor = new HarvestSchedulerMonitorServer(
            jmsConnectionProvider, jobDAOProvider, harvestDefinitionDAOProvider, notificationsProvider
    );

    private LogbackRecorder logRecorder;
    private JobStub job1;

    @Before
    public void setup() {
        logRecorder = LogbackRecorder.startRecorder();

        job1 = new JobStub();
        job1.setJobID(1L);
        job1.setStatus(JobStatus.NEW);
        job1.setOrigHarvestDefinitionID(1L);
        when(jobDAOMock.read(job1.getJobID())).thenReturn(job1);
    }

    @After
    public void teardown() {
        logRecorder.stopRecorder();
    }

    /**
     * Test that harvestStatusMonitor actually listens to the HarvesterChannels.THE_SCHED Channel and removes the
     * listener
     * on shutdown.
     */
    @Test
    public void testListenerSetRemove() {
        harvestStatusMonitor.start();
        verify(jmsConnectionMock).setListener(HarvesterChannels.getTheSched(), harvestStatusMonitor);
        harvestStatusMonitor.shutdown();
        verify(jmsConnectionMock).removeListener(HarvesterChannels.getTheSched(), harvestStatusMonitor);
    }

    /**
     * Test that the reception of a 'Started' crawl status is handled correctly for a job in initial 'New' state.
     */
    @Test
    public void testStatusNewToStarted() {
        harvestStatusMonitor.start();
        harvestStatusMonitor.visit(new CrawlStatusMessage(job1.getJobID(), JobStatus.STARTED));

        ArgumentCaptor<Job> jobArgumentCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobDAOMock).update(jobArgumentCaptor.capture());
        assertEquals(JobStatus.STARTED, jobArgumentCaptor.getValue().getStatus());

        ArgumentCaptor<CrawlProgressMessage> crawlProgressMessageArgumentCaptor =
                ArgumentCaptor.forClass(CrawlProgressMessage.class);
        verify(jmsConnectionMock).send(crawlProgressMessageArgumentCaptor.capture());
        assertEquals((long) job1.getJobID(), crawlProgressMessageArgumentCaptor.getValue().getJobID());
    }

    /**
     * Test that the reception of a 'Started' crawl status is handled correctly for a job in initial 'Submitted' state .
     */
    @Test
    public void testStatusSubmittedToStarted() {
        long jobID = 1;
        JobStub job1 = new JobStub();
        job1.setJobID(jobID);
        job1.setStatus(JobStatus.SUBMITTED);
        job1.setOrigHarvestDefinitionID(1L);
        when(jobDAOMock.read(jobID)).thenReturn(job1);

        harvestStatusMonitor.start();
        harvestStatusMonitor.visit(new CrawlStatusMessage(job1.getJobID(), JobStatus.STARTED));

        ArgumentCaptor<Job> jobArgumentCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobDAOMock).update(jobArgumentCaptor.capture());
        assertEquals(JobStatus.STARTED, jobArgumentCaptor.getValue().getStatus());

        ArgumentCaptor<CrawlProgressMessage> crawlProgressMessageArgumentCaptor =
                ArgumentCaptor.forClass(CrawlProgressMessage.class);
        verify(jmsConnectionMock).send(crawlProgressMessageArgumentCaptor.capture());
        assertEquals((long) job1.getJobID(), crawlProgressMessageArgumentCaptor.getValue().getJobID());
    }

    /**
     * Test that the reception of a 'Started' crawl status is ignored for a job already in 'Started' state.
     */
    @Test
    public void testStatusStartedToStarted() {
        long jobID = 1;
        JobStub job1 = new JobStub();
        job1.setJobID(jobID);
        job1.setStatus(JobStatus.STARTED);
        job1.setOrigHarvestDefinitionID(1L);
        when(jobDAOMock.read(jobID)).thenReturn(job1);

        harvestStatusMonitor.start();
        harvestStatusMonitor.visit(new CrawlStatusMessage(job1.getJobID(), JobStatus.STARTED));

        verify(jobDAOMock).read(jobID);
        verify(jmsConnectionMock).setListener(HarvesterChannels.getTheSched(), harvestStatusMonitor);
        verifyNoMoreInteractions(jobDAOMock, jmsConnectionMock);
    }

    /**
     * Test that the reception of a 'Started' crawl status is handled correctly for a job in initial 'Submitted' state .
     */
    @Test
    public void testStatusStartedToDone() {
        long jobID = 1;
        JobStub job1 = new JobStub();
        job1.setJobID(jobID);
        job1.setStatus(JobStatus.STARTED);
        job1.setOrigHarvestDefinitionID(1L);
        when(jobDAOMock.read(jobID)).thenReturn(job1);

        harvestStatusMonitor.start();
        harvestStatusMonitor.visit(new CrawlStatusMessage(job1.getJobID(), JobStatus.DONE));

        ArgumentCaptor<Job> jobArgumentCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobDAOMock).update(jobArgumentCaptor.capture());
        assertEquals(JobStatus.DONE, jobArgumentCaptor.getValue().getStatus());

        ArgumentCaptor<JobEndedMessage> jobEndedMessageArgumentCaptor =
                ArgumentCaptor.forClass(JobEndedMessage.class);
        verify(jmsConnectionMock).send(jobEndedMessageArgumentCaptor.capture());
        assertEquals((long) job1.getJobID(), jobEndedMessageArgumentCaptor.getValue().getJobId());
        assertEquals(JobStatus.DONE, jobEndedMessageArgumentCaptor.getValue().getJobStatus());
    }

    /**
     * Test that receiving a "DONE" for a "SUBMITTED" job runs as normal, but is logged.
     */
    @Test
    public void testSubmittedToDone() {
        job1.setStatus(JobStatus.SUBMITTED);

        harvestStatusMonitor.start();
        harvestStatusMonitor.visit(new CrawlStatusMessage(job1.getJobID(), JobStatus.DONE));

        ArgumentCaptor<Job> jobArgumentCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobDAOMock).update(jobArgumentCaptor.capture());
        assertEquals(JobStatus.DONE, jobArgumentCaptor.getValue().getStatus());

        ArgumentCaptor<JobEndedMessage> jobEndedMessageArgumentCaptor =
                ArgumentCaptor.forClass(JobEndedMessage.class);
        verify(jmsConnectionMock).send(jobEndedMessageArgumentCaptor.capture());
        assertEquals((long) job1.getJobID(), jobEndedMessageArgumentCaptor.getValue().getJobId());
        assertEquals(JobStatus.DONE, jobEndedMessageArgumentCaptor.getValue().getJobStatus());

        logRecorder.assertLogContains(Level.WARN, "Received unexpected CrawlStatusMessage for job 1 with new status DONE, current state is " +
                "SUBMITTED");
    }

    /**
     * If DONE arrives after FAILED, the job should be marked FAILED and error info should be added to the job.
     * Note: this is not what is tested here. Currently tested is:  
     * If FAILED arrives after DONE, the job should be marked FAILED and error info should be added to the job.
     */
    @Test
    public void testDoneToFailed() {
        job1.setStatus(JobStatus.DONE);
        final String HARVEST_ERRORS = "Some harvesterrors";
        CrawlStatusMessage crawlStatusMessage = new CrawlStatusMessage(job1.getJobID(), JobStatus.FAILED);
        crawlStatusMessage.setHarvestErrors(HARVEST_ERRORS);

        harvestStatusMonitor.start();
        harvestStatusMonitor.visit(crawlStatusMessage);

        final String ERROR_MESSAGE = "Received unexpected CrawlStatusMessage for job 1 with new status FAILED, current " +
                "state is DONE. Marking job as DONE. Reported harvestErrors on job: Some harvesterrors"; 
        
        ArgumentCaptor<Job> jobArgumentCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobDAOMock).update(jobArgumentCaptor.capture());
        Job actualJob = jobArgumentCaptor.getValue();
        assertEquals(job1.getJobID(), actualJob.getJobID());
        assertEquals(JobStatus.DONE, actualJob.getStatus());
        assertThat(actualJob.getHarvestErrors(), containsString(HARVEST_ERRORS));
        assertThat(actualJob.getHarvestErrors(), containsString(ERROR_MESSAGE));
        assertThat(actualJob.getHarvestErrorDetails(), containsString(HARVEST_ERRORS));
        assertThat(actualJob.getHarvestErrorDetails(), containsString(ERROR_MESSAGE));

        ArgumentCaptor<JobEndedMessage> jobEndedMessageArgumentCaptor =
                ArgumentCaptor.forClass(JobEndedMessage.class);
        verify(jmsConnectionMock).send(jobEndedMessageArgumentCaptor.capture());
        assertEquals((long) job1.getJobID(), jobEndedMessageArgumentCaptor.getValue().getJobId());
        assertEquals(JobStatus.FAILED, jobEndedMessageArgumentCaptor.getValue().getJobStatus()); 

        logRecorder.assertLogContains(Level.WARN, ERROR_MESSAGE);
    }

    /**
     * Send a STARTED CrawlStatusMessage for a Failed job. This STARTED message should be ignored.
     */
    @Test
    public void testDoneToStarted() {
        job1.setStatus(JobStatus.DONE);

        harvestStatusMonitor.start();
        harvestStatusMonitor.visit(new CrawlStatusMessage(job1.getJobID(), JobStatus.STARTED));

        verify(jobDAOMock).read(job1.getJobID());
        verify(jmsConnectionMock).setListener(HarvesterChannels.getTheSched(), harvestStatusMonitor);
        verifyNoMoreInteractions(jobDAOMock, jmsConnectionMock);
    }

    /**
     * Send a STARTED CrawlStatusMessage after a Failed message. This STARTED message should be ignored.
     */
    @Test
    public void testFailedToStarted() {
        job1.setStatus(JobStatus.FAILED);
        ;

        harvestStatusMonitor.start();
        harvestStatusMonitor.visit(new CrawlStatusMessage(job1.getJobID(), JobStatus.STARTED));

        verify(jobDAOMock).read(job1.getJobID());
        verify(jmsConnectionMock).setListener(HarvesterChannels.getTheSched(), harvestStatusMonitor);
        verifyNoMoreInteractions(jobDAOMock, jmsConnectionMock);
    }

    /**
     * Validate that DomainHarvestReport's are postprocessed for a job if the report is contained in a received Done
     * or Failed CrawlStatusMessage.
     */
    @Test
    public void testDomainHarvestReportPostProcessing() {
        List<JobStatus> crawlMessageStatusesToTest = Arrays.asList(new JobStatus[]
                {JobStatus.DONE, JobStatus.FAILED});
 
        //crawlMessageStatusesToTest.forEach((status) -> { // java 8 required
        for (JobStatus status: crawlMessageStatusesToTest) {
            job1.setStatus(JobStatus.STARTED);
            HarvestReport harvestReport = mock(HarvestReport.class);
            CrawlStatusMessage crawlStatusMessage = mock(CrawlStatusMessage.class);
            when(crawlStatusMessage.getJobID()).thenReturn(job1.getJobID());
            when(crawlStatusMessage.getStatusCode()).thenReturn(status);
            when(crawlStatusMessage.getDomainHarvestReport()).thenReturn(harvestReport);

            harvestStatusMonitor.start();
            harvestStatusMonitor.visit(crawlStatusMessage);

            ArgumentCaptor<Job> jobArgumentCaptor = ArgumentCaptor.forClass(Job.class);
            verify(harvestReport).postProcess(jobArgumentCaptor.capture());
            assertEquals(job1.getJobID(), jobArgumentCaptor.getValue().getJobID());
        }
        //});
    }

    /**
     * The following tests validate that the related job is updated with error details for jobs in state
     * NEW, SUBMITTED, RESUBMITTED or STARTED when receiving a DONE or FAILED message.
     */
    @Test
    public void testJobIsUpdatedWithErrorInfoNewToDone() {
        testJobIsUpdatedWithErrorInfo(JobStatus.NEW, JobStatus.DONE);
    }
    @Test
    public void testJobIsUpdatedWithErrorInfoSubmittedToDone() {
        testJobIsUpdatedWithErrorInfo(JobStatus.SUBMITTED, JobStatus.DONE);
    }
    @Test
    public void testJobIsUpdatedWithErrorInfoResubmittedToDone() {
        testJobIsUpdatedWithErrorInfo(JobStatus.RESUBMITTED, JobStatus.DONE);
    }
    @Test
    public void testJobIsUpdatedWithErrorInfoStartedToDone() {
        testJobIsUpdatedWithErrorInfo(JobStatus.STARTED, JobStatus.DONE);
    }
    @Test
    public void testJobIsUpdatedWithErrorInfoNewToFailed() {
        testJobIsUpdatedWithErrorInfo(JobStatus.NEW, JobStatus.FAILED);
    }
    @Test
    public void testJobIsUpdatedWithErrorInfoSubmittedToFailed() {
        testJobIsUpdatedWithErrorInfo(JobStatus.SUBMITTED, JobStatus.FAILED);
    }
    @Test
    public void testJobIsUpdatedWithErrorInfoResubmittedToFailed() {
        testJobIsUpdatedWithErrorInfo(JobStatus.RESUBMITTED, JobStatus.FAILED);
    }
    @Test
    public void testJobIsUpdatedWithErrorInfoStartedToFailed() {
        testJobIsUpdatedWithErrorInfo(JobStatus.STARTED, JobStatus.FAILED);
    }
    private void testJobIsUpdatedWithErrorInfo(JobStatus initialJobStatus, JobStatus crawlMessageStatus) {
        job1.setStatus(initialJobStatus);
        final String HARVEST_ERRORS = "Some harvesterrors";
        final String HARVEST_ERROR_DETAILS = "Some harvestErrorDetails";
        final String UPLOADER_ERRORS = "Some uploadErrors";
        final String UPLOAD_ERROR_DETAILS = "Some uploadErrorDetails";
        CrawlStatusMessage crawlStatusMessage = mock(CrawlStatusMessage.class);
        when(crawlStatusMessage.getJobID()).thenReturn(job1.getJobID());
        when(crawlStatusMessage.getStatusCode()).thenReturn(crawlMessageStatus);
        when(crawlStatusMessage.getHarvestErrors()).thenReturn(HARVEST_ERRORS);
        when(crawlStatusMessage.getHarvestErrorDetails()).thenReturn(HARVEST_ERROR_DETAILS);
        when(crawlStatusMessage.getUploadErrors()).thenReturn(UPLOADER_ERRORS);
        when(crawlStatusMessage.getUploadErrorDetails()).thenReturn(UPLOAD_ERROR_DETAILS);

        harvestStatusMonitor.start();
        harvestStatusMonitor.visit(crawlStatusMessage);

        ArgumentCaptor<Job> jobArgumentCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobDAOMock).update(jobArgumentCaptor.capture());
        assertEquals(job1.getJobID(), jobArgumentCaptor.getValue().getJobID());
        assertEquals(HARVEST_ERRORS, jobArgumentCaptor.getValue().getHarvestErrors());
        assertEquals(HARVEST_ERROR_DETAILS, jobArgumentCaptor.getValue().getHarvestErrorDetails());
        assertEquals(UPLOADER_ERRORS, jobArgumentCaptor.getValue().getUploadErrors());
        assertEquals(UPLOAD_ERROR_DETAILS, jobArgumentCaptor.getValue().getUploadErrorDetails());
    }
}
