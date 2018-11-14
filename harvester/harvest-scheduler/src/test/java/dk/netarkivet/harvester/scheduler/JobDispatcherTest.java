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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.dom4j.DocumentException;
import org.dom4j.tree.DefaultDocument;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.verification.Times;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.AliasInfo;
import dk.netarkivet.harvester.datamodel.H1HeritrixTemplate;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.HeritrixTemplate;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.SparsePartialHarvest;
import dk.netarkivet.harvester.distribute.HarvesterChannels;
import dk.netarkivet.harvester.harvesting.distribute.DoOneCrawlMessage;
import dk.netarkivet.harvester.harvesting.metadata.MetadataEntry;
import dk.netarkivet.harvester.test.utils.OrderXmlBuilder;

/**
 * Test JobDispatcher class.
 */
public class JobDispatcherTest {
    private JobDispatcher jobDispatcher;
    private HarvestDefinitionDAO harvestDefinitionDAO;
    private JobDAO jobDAO;
    private JMSConnection jmsConnection;
    private HarvestChannel SELECTIVE_HARVEST_CHANNEL = new HarvestChannel("FOCUSED", false, true, "");
    private final ArgumentCaptor<DoOneCrawlMessage> crawlMessageCaptor = ArgumentCaptor
            .forClass(DoOneCrawlMessage.class);
    private Job jobMock = createJob(1);
    private SparsePartialHarvest harvest = createDefaultSparsePartialHarvest();

    @Before
    public void setUp() {
        harvestDefinitionDAO = mock(HarvestDefinitionDAO.class);
        jobDAO = mock(JobDAO.class);
        jmsConnection = mock(JMSConnection.class);
        jobDispatcher = new JobDispatcher(jmsConnection, harvestDefinitionDAO, jobDAO);
    }

    /**
     * Simple test of new job submitting.
     */
    @Test
    public void testSubmitNewJobs() throws DocumentException {
        prepareDefaultMockAnswers(SELECTIVE_HARVEST_CHANNEL, jobMock, false);

        jobDispatcher.submitNextNewJob(SELECTIVE_HARVEST_CHANNEL);

        verify(jobMock).setStatus(JobStatus.SUBMITTED);
        verify(jobMock).setSubmittedDate(any(Date.class));

        verify(jobDAO, new Times(1)).update(jobMock);
        
        verify(jmsConnection).send(crawlMessageCaptor.capture());
        
        assertTrue(jobMock == crawlMessageCaptor.getValue().getJob());
        assertEquals(HarvesterChannels.getHarvestJobChannelId(SELECTIVE_HARVEST_CHANNEL), crawlMessageCaptor.getValue()
                .getTo());
        assertEquals(harvest.getName(), crawlMessageCaptor.getValue().getOrigHarvestInfo().getOrigHarvestName());
    }

    /**
     * Test that runNewJobs generates correct alias information for the job.s
     */
    @Test
    public void testSubmitNewJobsMakesAliasInfo() throws SQLException {
        prepareDefaultMockAnswers(SELECTIVE_HARVEST_CHANNEL, jobMock, false);
        String originalDomain = "netarkiv.dk";
        String aliasDomain = "netatarkivalias.dk";
        when(jobDAO.getJobAliasInfo(any(Job.class))).thenReturn(
                Arrays.asList(new AliasInfo[] {new AliasInfo("netatarkivalias.dk", "netarkiv.dk", new Date())}));

        jobDispatcher.submitNextNewJob(SELECTIVE_HARVEST_CHANNEL);

        verify(jmsConnection).send(crawlMessageCaptor.capture());
        assertEquals("Should have 1 metadata entry in last received message", 1, crawlMessageCaptor.getValue()
                .getMetadata().size());
        assertEquals(aliasDomain + " is an alias for " + originalDomain + "\n", new String(crawlMessageCaptor
                .getValue().getMetadata().get(0).getData()));
    }

    /**
     * Test that runNewJobs makes correct duplication reduction information.
     */
    @Test
    public void testSubmitNewJobsMakesDuplicateReductionInfo() throws DocumentException {
        prepareDefaultMockAnswers(SELECTIVE_HARVEST_CHANNEL, jobMock, true);
        // FIXME Which is correct? Does this fail currently?
        // when(jobMock.getOrderXMLdoc()).thenReturn(doc);
        List<Long> jobIDsForDuplicateReduction = Arrays.asList(new Long[] {1L});
        when(jobDAO.getJobIDsForDuplicateReduction(jobMock.getJobID())).thenReturn(jobIDsForDuplicateReduction);

        jobDispatcher.submitNextNewJob(SELECTIVE_HARVEST_CHANNEL);

        verify(jmsConnection).send(crawlMessageCaptor.capture());

        DoOneCrawlMessage crawlMessage = crawlMessageCaptor.getValue();
        assertEquals("Should have 1 metadata entry in the crawl request", 1, crawlMessage.getMetadata().size());
        MetadataEntry metadataEntry = crawlMessage.getMetadata().get(0);
        assertNotNull("Should have 1 metadata entry", metadataEntry);
        assertEquals("Should have mimetype text/plain", "text/plain", metadataEntry.getMimeType());
        assertEquals("Should have right url",
                "metadata://netarkivet.dk/crawl/setup/duplicatereductionjobs" + "?majorversion=1" + "&minorversion=0"
                        + "&harvestid=" + harvest.getOid() + "&harvestnum=" + jobMock.getHarvestNum() + "&jobid="
                        + jobMock.getJobID(), metadataEntry.getURL());
        assertEquals("Should have right data", jobIDsForDuplicateReduction.get(0) + "",
                new String(metadataEntry.getData()));
    }

    private static final HarvestChannel SNAPSHOT = new HarvestChannel("SNAPSHOT", true, true, "");

    /**
     * Test sending + check that we send a message Uses MessageTestHandler()
     */
    @Test
    public void testSendingToCorrectQueue() {
        prepareDefaultMockAnswers(SELECTIVE_HARVEST_CHANNEL, jobMock, false);

        jobDispatcher.submitNextNewJob(SELECTIVE_HARVEST_CHANNEL);
        verify(jmsConnection).send(crawlMessageCaptor.capture());
        assertTrue(jobMock == crawlMessageCaptor.getValue().getJob());
        assertEquals(HarvesterChannels.getHarvestJobChannelId(SELECTIVE_HARVEST_CHANNEL), crawlMessageCaptor.getValue()
                .getTo());
        reset(jmsConnection);

        Job snapshotJob = createJob(2);
        prepareDefaultMockAnswers(SNAPSHOT, snapshotJob, false);
        jobDispatcher.submitNextNewJob(SNAPSHOT);
        verify(jmsConnection).send(crawlMessageCaptor.capture());
        assertTrue(snapshotJob == crawlMessageCaptor.getValue().getJob());

        assertEquals(HarvesterChannels.getHarvestJobChannelId(SNAPSHOT), crawlMessageCaptor.getValue().getTo());
    }

    /**
     * Verify handling of NULL value for Job Uses MessageTestHandler()
     */
    @Test
    public void testNullJob() {
        try {
            jobDispatcher.doOneCrawl((Job) null, "test", "test", "test", SELECTIVE_HARVEST_CHANNEL, "unittesters",
            		new ArrayList<MetadataEntry>());
            fail("Should throw ArgumentNotValid on NULL job");
        } catch (ArgumentNotValid e) {
            // expected case
        }
    }

    private SparsePartialHarvest createDefaultSparsePartialHarvest() {
        SparsePartialHarvest harvest = new SparsePartialHarvest(9L, "TestHarvest", "A comment", 2, new Date(), true, 3,
                "schedule", new Date(), "The audience", SELECTIVE_HARVEST_CHANNEL.getId());
        return harvest;
    }

    private Job createJob(long jobID) {
        Job job = mock(Job.class);
        when(job.getJobID()).thenReturn(jobID);
        when(job.getOrigHarvestDefinitionID()).thenReturn(9L);
        when(job.getOrderXMLdoc()).thenReturn(
        		new H1HeritrixTemplate(new DefaultDocument(), false)); //FIXME only works for H1 templates
        return job;
    }

    private void prepareDefaultMockAnswers(HarvestChannel channel, Job job, boolean dedup) {
        Iterator<Long> jobIDIterator = Arrays.asList(new Long[] {job.getJobID()}).iterator();
        when(jobDAO.getAllJobIds(JobStatus.NEW, channel)).thenReturn(jobIDIterator);
        when(jobDAO.read(job.getJobID())).thenReturn(job);
        when(harvestDefinitionDAO.getHarvestName(harvest.getOid())).thenReturn(harvest.getName());
        when(harvestDefinitionDAO.getSparsePartialHarvest(harvest.getName())).thenReturn(harvest);
        OrderXmlBuilder builder = OrderXmlBuilder.createDefault();
        builder = builder.setDeduplication(dedup);
        HeritrixTemplate h1temp = new H1HeritrixTemplate(builder.getDoc(), false);
        when(job.getOrderXMLdoc()).thenReturn(h1temp);
        when(job.getChannel()).thenReturn(SELECTIVE_HARVEST_CHANNEL.getName());
        when(job.getOrderXMLName()).thenReturn("What is the purpose of this test?");
        when(job.getHarvestFilenamePrefix()).thenReturn("1-bla-");
    }
}
