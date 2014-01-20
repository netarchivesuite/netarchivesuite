/* File: $Id$
 * Revision: $Revision$
 * Author: $Author$
 * Date: $Date$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.harvester.scheduler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.tools.OrderXmlBuilder;
import dk.netarkivet.harvester.datamodel.AliasInfo;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.SparsePartialHarvest;
import dk.netarkivet.harvester.distribute.HarvesterChannels;
import dk.netarkivet.harvester.harvesting.distribute.DoOneCrawlMessage;
import dk.netarkivet.harvester.harvesting.metadata.MetadataEntry;
import junit.framework.TestCase;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.tree.DefaultDocument;
import org.mockito.ArgumentCaptor;

import static org.mockito.Mockito.*;

/**
 * Test JobDispatcher class.
 */
public class JobDispatcherTester extends TestCase {
    /** The JobDispatcher used for testing. */
    private JobDispatcher jobDispatcher;
    private HarvestDefinitionDAO harvestDefinitionDAO;
    private JobDAO jobDAO;
    private JMSConnection jmsConnection;
    private HarvestChannel SELECTIVE_HARVEST_CHANNEL = new HarvestChannel("FOCUSED", "", true);
    private final ArgumentCaptor<DoOneCrawlMessage> crawlMessageCaptor =
            ArgumentCaptor.forClass(DoOneCrawlMessage.class);
    private Job jobMock = createJob(1);
    private SparsePartialHarvest harvest = createDefaultSparsePartialHarvest();


    public JobDispatcherTester(String sTestName) {
        super(sTestName);
    }

    public void setUp() {
        harvestDefinitionDAO = mock(HarvestDefinitionDAO.class);
        jobDAO = mock(JobDAO.class);
        jmsConnection = mock(JMSConnection.class);
        jobDispatcher = new JobDispatcher(jmsConnection, harvestDefinitionDAO, jobDAO);
    }

    /**
     * Simple test of new job submitting.
     */
    public void testSubmitNewJobs() throws DocumentException {
        prepareDefaultMockAnswers(SELECTIVE_HARVEST_CHANNEL, jobMock);

        jobDispatcher.submitNextNewJob(SELECTIVE_HARVEST_CHANNEL);

        verify(jobMock).setStatus(JobStatus.SUBMITTED);
        verify(jobMock).setSubmittedDate(any(Date.class));
        verify(jobDAO).update(jobMock);
        verify(jmsConnection).send(crawlMessageCaptor.capture());
        assertTrue(jobMock == crawlMessageCaptor.getValue().getJob());
        assertEquals(
                HarvesterChannels.getHarvestJobChannelId(SELECTIVE_HARVEST_CHANNEL),
                crawlMessageCaptor.getValue().getTo());
        assertEquals(harvest.getName(),crawlMessageCaptor.getValue().getOrigHarvestInfo().getOrigHarvestName());
    }

    /**
     * Test that runNewJobs generates correct alias information for the job.s
     */
    public void testSubmitNewJobsMakesAliasInfo() throws SQLException {
        prepareDefaultMockAnswers(SELECTIVE_HARVEST_CHANNEL, jobMock);
        String originalDomain = "netarkiv.dk";
        String aliasDomain = "netatarkivalias.dk";
        when(jobMock.getJobAliasInfo()).thenReturn(Arrays.asList(new AliasInfo[] {
                new AliasInfo("netatarkivalias.dk", "netarkiv.dk", new Date())}));

        jobDispatcher.submitNextNewJob(SELECTIVE_HARVEST_CHANNEL);

        verify(jmsConnection).send(crawlMessageCaptor.capture());
        assertEquals("Should have 1 metadata entry in last received message",
                1, crawlMessageCaptor.getValue().getMetadata().size());
        assertEquals(aliasDomain +" is an alias for " + originalDomain + "\n",
                new String(crawlMessageCaptor.getValue().getMetadata().get(0).getData()));
    }

    /**
     * Test that runNewJobs makes correct duplication reduction information.
     */
    public void testSubmitNewJobsMakesDuplicateReductionInfo() throws DocumentException {
        prepareDefaultMockAnswers(SELECTIVE_HARVEST_CHANNEL, jobMock);
        Document doc = OrderXmlBuilder.create().enableDeduplication().getOrderXml();
        when(jobMock.getOrderXMLdoc()).thenReturn(doc);
        List<Long> jobIDsForDuplicateReduction = Arrays.asList(new Long[]{1L});
        when(jobDAO.getJobIDsForDuplicateReduction(jobMock.getJobID())).thenReturn(jobIDsForDuplicateReduction);

        jobDispatcher.submitNextNewJob(SELECTIVE_HARVEST_CHANNEL);

        verify(jmsConnection).send(crawlMessageCaptor.capture());


        DoOneCrawlMessage crawlMessage = crawlMessageCaptor.getValue();
        assertEquals("Should have 1 metadata entry in the crawl request",
                1, crawlMessage.getMetadata().size());
        MetadataEntry metadataEntry = crawlMessage.getMetadata().get(0);
        assertNotNull("Should have 1 metadata entry", metadataEntry);
        assertEquals("Should have mimetype text/plain", "text/plain",
                metadataEntry.getMimeType());
        assertEquals("Should have right url",
                "metadata://netarkivet.dk/crawl/setup/duplicatereductionjobs" +
                        "?majorversion=1" +
                        "&minorversion=0" +
                        "&harvestid=" + harvest.getOid() +
                        "&harvestnum=" + jobMock.getHarvestNum() +
                        "&jobid=" + jobMock.getJobID(),
                metadataEntry.getURL());
        assertEquals("Should have right data", jobIDsForDuplicateReduction.get(0) +"", new String(
                metadataEntry.getData()));
    }

    /**
     * Test sending + check that we send a message
     * Uses MessageTestHandler()
     */
    public void testSendingToCorrectQueue() {
        prepareDefaultMockAnswers(SELECTIVE_HARVEST_CHANNEL, jobMock);

        jobDispatcher.submitNextNewJob(SELECTIVE_HARVEST_CHANNEL);
        verify(jmsConnection).send(crawlMessageCaptor.capture());
        assertTrue(jobMock == crawlMessageCaptor.getValue().getJob());
        assertEquals(
                HarvesterChannels.getHarvestJobChannelId(SELECTIVE_HARVEST_CHANNEL),
                crawlMessageCaptor.getValue().getTo());
        reset(jmsConnection);

        Job snapshotJob = createJob(2);
        prepareDefaultMockAnswers(HarvestChannel.SNAPSHOT, snapshotJob);
        jobDispatcher.submitNextNewJob(HarvestChannel.SNAPSHOT);
        verify(jmsConnection).send(crawlMessageCaptor.capture());
        assertTrue(snapshotJob == crawlMessageCaptor.getValue().getJob());

        assertEquals(
                HarvesterChannels.getHarvestJobChannelId(HarvestChannel.SNAPSHOT),
                crawlMessageCaptor.getValue().getTo());
    }

    /**
     * Verify handling of NULL value for Job
     * Uses MessageTestHandler()
     */
    public void testNullJob() {
        try {
            jobDispatcher.doOneCrawl((Job)null, "test", "test", "test", SELECTIVE_HARVEST_CHANNEL,
                    "unittesters",
                    new ArrayList<MetadataEntry>());
            fail("Should throw ArgumentNotValid on NULL job");
        } catch (ArgumentNotValid e) {
            // expected case
        }
    }

    private SparsePartialHarvest createDefaultSparsePartialHarvest() {
        SparsePartialHarvest harvest = new SparsePartialHarvest(
                9L, "TestHarvest", "A comment", 2, new Date(), true, 3, "schedule", new Date(), "The audience",
                SELECTIVE_HARVEST_CHANNEL.getId());
        return harvest;
    }

    private Job createJob(long jobID) {
        Job job = mock(Job.class);
        when(job.getJobID()).thenReturn(jobID);
        when(job.getOrigHarvestDefinitionID()).thenReturn(9L);
        when(job.getOrderXMLdoc()).thenReturn(new DefaultDocument());
        return job;
    }

    private void prepareDefaultMockAnswers(HarvestChannel channel, Job job) {
        Iterator<Long> jobIDIterator = Arrays.asList(new Long[]{job.getJobID()}).iterator();
        when(jobDAO.getAllJobIds(JobStatus.NEW, channel)).thenReturn(jobIDIterator);
        when(jobDAO.read(job.getJobID())).thenReturn(job);
        when(harvestDefinitionDAO.getHarvestName(harvest.getOid())).thenReturn(harvest.getName());
        when(harvestDefinitionDAO.getSparsePartialHarvest(harvest.getName())).thenReturn(harvest);
    }
}
