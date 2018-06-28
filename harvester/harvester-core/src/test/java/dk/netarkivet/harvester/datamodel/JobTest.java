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
package dk.netarkivet.harvester.datamodel;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import ch.qos.logback.classic.Level;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.harvesting.report.Heritrix1Constants;
import dk.netarkivet.harvester.test.utils.OrderXmlBuilder;
import dk.netarkivet.testutils.LogbackRecorder;
import dk.netarkivet.testutils.TestFileUtils;

public class JobTest {
    private static final HarvestChannel FOCUSED_CHANNEL = new HarvestChannel("FOCUSED", false, true, "");

    private LogbackRecorder logRecorder;

    @Before
    public void initialise() {
        logRecorder = LogbackRecorder.startRecorder();
    }

    public void cleanup() {
        logRecorder.stopRecorder();
    }

    @Test
    public void testSeedList() throws IOException {
        Job job = createDefaultJob();

        String seed1 = "seed1.org", seed2 = "seed2.org", seed3 ="seed3.org";
        Set<String> seedListAsSet = Sets.newSet(seed1, seed2, seed3);
        String seedListAsString = seed1 + "\n" + seed2 + "\n" + seed3;
        job.setSeedList(seedListAsString);

        Set<String> seedsFromJob = new HashSet<>();
        BufferedReader reader = new BufferedReader(new StringReader(job.getSeedListAsString()));
        String s;
        while ((s = reader.readLine()) != null) {
            seedsFromJob.add(s);
        }
        assertEquals(seedListAsSet, seedsFromJob);
    }

    /**
     * Tests that seedlists have ascii versions added.
     */
    @Test
    public void testInternationalCharsInSeedList() throws Exception {
        String seed1 = "http://www.pølse.dk/enfil";
        String seed2 = "http://www.pølse.dk/enpølse";
        String seed3 ="http://www.uden.dk/enpølse";
        SeedList seedList = new SeedList("pølse.dk", asList(new String[] {seed1, seed2, seed3}));
        DomainConfiguration domainConfiguration = new DomainConfiguration(
                "pølse.dk", "pølse.dk", new DomainHistory(),
                new ArrayList<String>(), asList(new SeedList[] {seedList}), new ArrayList<Password>());
        Job job = createDefaultJob(domainConfiguration);

        Set<String> internationalizedSeeds = Sets.newSet(
                "http://www.pølse.dk/enfil",
                "http://www.xn--plse-gra.dk/enfil",
                "http://www.pølse.dk/enpølse",
                "http://www.xn--plse-gra.dk/enpølse",
                "http://www.uden.dk/enpølse");

        Set<String> seedsFromJob = new HashSet<>();
        BufferedReader reader = new BufferedReader(new StringReader(job.getSeedListAsString()));
        String s;
        while ((s = reader.readLine()) != null) {
            seedsFromJob.add(s);
        }
        assertEquals(internationalizedSeeds, seedsFromJob);
    }

    @Test
    public void testActualStart() {
        Job job = createDefaultJob();
        assertNull(job.getActualStart());

        Date startDate = new Date(0);
        job.setActualStart(startDate);
        assertEquals(startDate, job.getActualStart());

        startDate = new Date(1);
        job.setActualStart(startDate);
        assertEquals(startDate, job.getActualStart());
    }

    /** The setting of the stop date requires that a actual start is defined, so we have to do this initially */
    @Test
    public void testActualStop() {
        Job job = createDefaultJob();
        job.setActualStart(new Date(0));
        assertNull(job.getActualStop());

        Date stopDate = new Date(1);
        job.setActualStop(stopDate);
        assertEquals(stopDate, job.getActualStop());

        stopDate = new Date(2);
        job.setActualStop(stopDate);
        assertEquals(stopDate, job.getActualStop());
    }



    @Test
    public void testLegalNew2FailedStatusChange() {
        Job job = createDefaultJob();
        job.setStatus(JobStatus.FAILED);
    }

    @Test
    public void testLegalNew2DoneStatusChange() {
        Job job = createDefaultJob();
        job.setStatus(JobStatus.DONE);
    }

    @Test
    public void testLegalNew2SubmittedStatusChange() {
        Job job = createDefaultJob();
        job.setStatus(JobStatus.SUBMITTED);
    }

    @Test
    public void testLegalNew2ResubmittedStatusChange() {
        Job job = createDefaultJob();
        job.setStatus(JobStatus.RESUBMITTED);
    }

    @Test
    public void testLegalNew2StartedStatusChange() {
        Job job = createDefaultJob();
        job.setStatus(JobStatus.STARTED);
    }

    @Test
    public void testLegalNew2FailedRejectedStatusChange() {
        Job job = createDefaultJob();
        job.setStatus(JobStatus.FAILED_REJECTED);
    }

    /** Note as the legal status changes is based on the JobStatus class logic only a single sanity test is included */
    @Test (expected = ArgumentNotValid.class)
    public void testIllegalDone2NewInStatusChange() {
        Job job = createDefaultJob();
        job.setStatus(JobStatus.DONE);
        job.setStatus(JobStatus.NEW);
    }

    @Test
    public void testAddConfiguration() {
        Job job = createDefaultJob();

        DomainConfiguration anotherConfig = DomainConfigurationTest.createDefaultDomainConfiguration("kaarefc.dk");
        job.addConfiguration(anotherConfig);
        assertEquals(2, job.getDomainConfigurationMap().size());
    }

    @Test
    public void testExceedLimitsInInitialConfiguration() {
        DomainConfiguration domainConfiguration = DomainConfigurationTest.createDefaultDomainConfiguration();

        HarvestInfo hi = new HarvestInfo(1L, domainConfiguration.getDomainName(), domainConfiguration.getName(),
                new Date(), 10000L, 10000L, StopReason.DOWNLOAD_COMPLETE);
        domainConfiguration.getDomainhistory().addHarvestInfo(hi);

        Job job = createDefaultJob(domainConfiguration);
        assertEquals("First configuration should be accepted", 1, job.getCountDomains());
    }

    @Test(expected = ArgumentNotValid.class)
    public void testAddNullConfiguration() {
        createDefaultJob().addConfiguration(null);
    }

    @Test(expected = ArgumentNotValid.class)
    public void testAddDuplicateConfiguration() {
        DomainConfiguration defaultConfig = DomainConfigurationTest.createDefaultDomainConfiguration();
        Job job = createDefaultJob(defaultConfig);
        job.addConfiguration(defaultConfig);
    }

    /**
     * Tests that job status fields are defined and are distinct
     */
    @Test
    public void testStatusFields() {
        JobStatus s = JobStatus.NEW;
        assertEquals("Error implementing status code names for NEW", s, JobStatus.valueOf(s.name()));
        assertEquals("Error implementing status ordinal for NEW", s, JobStatus.fromOrdinal(s.ordinal()));
        s = JobStatus.SUBMITTED;
        assertEquals("Error implementing status code names for " + s.name(), s, JobStatus.valueOf(s.name()));
        assertEquals("Error implementing status ordinal for " + s.name(), s, JobStatus.fromOrdinal(s.ordinal()));
        s = JobStatus.STARTED;
        assertEquals("Error implementing status code names for " + s.name(), s, JobStatus.valueOf(s.name()));
        assertEquals("Error implementing status ordinal for " + s.name(), s, JobStatus.fromOrdinal(s.ordinal()));
        s = JobStatus.DONE;
        assertEquals("Error implementing status code names for " + s.name(), s, JobStatus.valueOf(s.name()));
        assertEquals("Error implementing status ordinal for " + s.name(), s, JobStatus.fromOrdinal(s.ordinal()));
        s = JobStatus.FAILED;
        assertEquals("Error implementing status code names for " + s.name(), s, JobStatus.valueOf(s.name()));
        assertEquals("Error implementing status ordinal for " + s.name(), s, JobStatus.fromOrdinal(s.ordinal()));
        s = JobStatus.RESUBMITTED;
        assertEquals("Error implementing status code names for " + s.name(), s, JobStatus.valueOf(s.name()));
        assertEquals("Error implementing status ordinal for " + s.name(), s, JobStatus.fromOrdinal(s.ordinal()));
    }

    @Test(expected = ArgumentNotValid.class)
    public void testNullHarvestIDInConstructor() {
        Long nullHarvestID = null;
        HeritrixTemplate ht = new H1HeritrixTemplate(OrderXmlBuilder.createDefault().getDoc());
        new Job(nullHarvestID,
                DomainConfigurationTest.createDefaultDomainConfiguration(),
                ht,
                FOCUSED_CHANNEL, -1, -1, -1, 1);
    }

    @Test(expected = ArgumentNotValid.class)
    public void testNegativeHarvestIDInConstructor() {
        Long negativHarvestID = -1L;
        HeritrixTemplate ht = new H1HeritrixTemplate(OrderXmlBuilder.createDefault().getDoc());
        new Job(negativHarvestID,
                DomainConfigurationTest.createDefaultDomainConfiguration(),
                ht,
                FOCUSED_CHANNEL, -1, -1, -1, 1);
    }

    @Test(expected = ArgumentNotValid.class)
    public void testNullDomainConfigurationInConstructor() {
        DomainConfiguration nullDomainConfiguration = null;
        HeritrixTemplate ht = new H1HeritrixTemplate(OrderXmlBuilder.createDefault().getDoc());
        new Job(1L,
                nullDomainConfiguration,
                ht,
                FOCUSED_CHANNEL, -1, -1, -1, 1);
    }

    @Test(expected = ArgumentNotValid.class)
    public void testNullChannelInConstructor() {
        HarvestChannel nullHarvestChannel = null;
        HeritrixTemplate ht = new H1HeritrixTemplate(OrderXmlBuilder.createDefault().getDoc());
        new Job(1L,
                DomainConfigurationTest.createDefaultDomainConfiguration(),
                ht,
                nullHarvestChannel, -1, -1, -1, 1);
    }

    @Test(expected = ArgumentNotValid.class)
    public void testNullStartDate() {
        Job job = createDefaultJob();
        job.setActualStart(null);
    }

    @Test(expected = ArgumentNotValid.class)
    public void testNullEndDate() {
        Job job = createDefaultJob();
        job.setActualStop(null);
    }

    @Test
    public void testActualStopBeforeActualStart() {
        Job job = createDefaultJob();
        job.setActualStart(new Date(1));
        job.setActualStop(new Date(0));
        logRecorder.assertLogContains(Level.WARN, "actualStop");
    }

    @Test
    public void testActualStopAndNullActualStart() {
        Job job = createDefaultJob();
        job.setActualStop(new Date(0));
        logRecorder.assertLogContains(Level.WARN, "actualStop");
    }

    /**
     * Tests that it is possible to set the maximum number of objects to be retrieved per domain i.e. that the order.xml
     * that is used as base for this Job is edited accordingly.
     */
    @Test
    public void testForceMaxObjectsPerDomain() {
        // test capping of forceMaxObjectsPerDomain:
        DomainConfiguration domainConfiguration = DomainConfigurationTest.createDefaultDomainConfiguration();
        Job job = createDefaultJob(domainConfiguration);
        assertEquals("forceMaxObjectsPerDomain not capped to domain config",
                domainConfiguration.getMaxObjects(),
                job.getForceMaxObjectsPerDomain());

        // check if updated in the Document object that the Job object holds:
        // xpath-expression that selects the appropriate node in order.xml:
        final String xpath = "/crawl-order/controller/map[@name='pre-fetch-processors']"
                + "/newObject[@name='QuotaEnforcer']" + "/long[@name='group-max-fetch-successes']";
        // FIXME only works for H1
//        Document orderXML = job.getOrderXMLdoc();
//        Node groupMaxFetchSuccessNode = orderXML.selectSingleNode(xpath);
//
//        long maxObjectsXML = Long.parseLong(groupMaxFetchSuccessNode.getText());
//        assertEquals("The order.xml Document should have been updated", domainConfiguration.getMaxObjects(),
//                maxObjectsXML);
    }

    @Test(expected = ArgumentNotValid.class)
    public void testForceNegativeMaxBytesPerDomain() {
        long harvestId = 1;
        long forceMaxObjectsPerDomain = -2, forceMaxBytesPerDomain = -1, forceMaxJobRunningTime = -1;
        int harvestNum = 1;
        HeritrixTemplate ht = new H1HeritrixTemplate(OrderXmlBuilder.createDefault().getDoc());
        new Job(
                harvestId,
                DomainConfigurationTest.createDefaultDomainConfiguration(),
                ht,
                FOCUSED_CHANNEL,
                forceMaxObjectsPerDomain,
                forceMaxBytesPerDomain,
                forceMaxJobRunningTime,
                harvestNum);
    }

    @Test(expected = ArgumentNotValid.class)
    public void testForceNegativeMaxObjectsPerDomain() {
        long harvestId = 1;
        long forceMaxObjectsPerDomain = -1, forceMaxBytesPerDomain = -2, forceMaxJobRunningTime = -1;
        int harvestNum = 1;
        HeritrixTemplate ht = new H1HeritrixTemplate(OrderXmlBuilder.createDefault().getDoc());
        new Job(
                harvestId,
                DomainConfigurationTest.createDefaultDomainConfiguration(),
                ht,
                FOCUSED_CHANNEL,
                forceMaxObjectsPerDomain,
                forceMaxBytesPerDomain,
                forceMaxJobRunningTime,
                harvestNum);
    }

    @Test
    public void testSerializability() throws IOException, ClassNotFoundException {
        // make a job:
        Job job = createDefaultJob();

        // Write and read
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream ous = new ObjectOutputStream(baos);
        ous.writeObject(job);
        ous.close();
        baos.close();

        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));

        Job job2 = (Job) ois.readObject();

        // Finally, compare their visible states:

        // This requires, that class Job needs to override methods hashCode, and equals.
        // assertTrue("Job2 should equal job1", job.equals(job2));

        assertEquals(
                "After serialization the states differed:\n"
                        + TestFileUtils.getDifferences(relevantState(job), relevantState(job2)), relevantState(job),
                relevantState(job2));

        assertTrue(job.getSeedListAsString().equals(job2.getSeedListAsString()));

        // Call a method that logs - this will fail if the transient logger
        // has not been reinitialised
        job2.setSeedList("www.netarkivet.dk");
    }

    /**
     * Tests that crawlertraps are correctly merged with the order.xml. Crawlertraps are expressed using
     * MatchesListRegExpDecideRule each listing the crawlertraps specified for a single domain, e.g. <newObject
     * name="dr.dk" class="org.archive.crawler.deciderules.MatchesListRegExpDecideRule"> <string
     * name="decision">REJECT</string> <string name="list-logic">OR</string> <stringList name='regexp-list'>
     * <string>xyz.*</string> <string>.*[a-z]+</string> </stringList> </newObject>
     */
    @Test
    public void testAddConfigurationUpdatesOrderXml() {
        // Make a configuration with no crawlertraps
        DomainConfiguration dc1 = DomainConfigurationTest.createDefaultDomainConfiguration();

        // Make a configuration with two crawlertraps
        DomainConfiguration dc2 = DomainConfigurationTest.createDefaultDomainConfiguration("otherDomain.org");
        List<String> traps = new ArrayList<>();

        String crawlerTrap1 = "xyz.*";
        String crawlerTrap2 = ".*[a-z]+";
        traps.add(crawlerTrap1);
        traps.add(crawlerTrap2);
        dc2.setCrawlertraps(traps);

        // Make a job with the two configurations
        Job job = createDefaultJob(dc1);
        job.addConfiguration(dc2);

        // Check that there are no crawlertraps for the first domain and exactly
        // the right two in the second domain
        String domain1CrawlerTrapsXpath = "/crawl-order/controller/newObject[@name='scope']/newObject[@class='"
                + Heritrix1Constants.DECIDERULESEQUENCE_CLASSNAME + "']/map[@name='rules']/"
                + "/newObject[@name='" + dc1.getDomainName()
                + "'][@class='" + Heritrix1Constants.MATCHESLISTREGEXPDECIDERULE_CLASSNAME + "']";

        String domain2CrawlerTrapsXpath = "/crawl-order/controller/newObject[@name='scope']/newObject[@class='"
                + Heritrix1Constants.DECIDERULESEQUENCE_CLASSNAME + "']/map[@name='rules']/"
                + "/newObject[@name='" + dc2.getDomainName()
                + "'][@class='" + Heritrix1Constants.MATCHESLISTREGEXPDECIDERULE_CLASSNAME + "']";
        //FIXME test only appropriate for H1 templates 
        /*
        job.getOrderXMLdoc().normalize();
        List<Node> nodes = job.getOrderXMLdoc().selectNodes(domain1CrawlerTrapsXpath);
        assertFalse("There shouldn't be any crawler traps for domain '" + dc1.getDomainName() + "'", nodes.size() != 0);
        nodes = job.getOrderXMLdoc().selectNodes(domain2CrawlerTrapsXpath);
        assertTrue("There should be crawler traps for domain '" + dc2.getDomainName() + "'", nodes.size() == 1);
        Node regexpListDeciderule = nodes.get(0);
        */
        // TODO add checks for
        // <string name="decision">REJECT</string>
        // <string name="list-logic">OR</string>

        /*
         * FIXME test only appropriate for H1 templates
     
        nodes = regexpListDeciderule.selectNodes("stringList[@name='regexp-list']/string");

        Set<String> regexpFound = new HashSet<String>();

        for (Node n : nodes) {
            String regexp = n.getText();
            regexpFound.add(regexp);
        }

        int found = regexpFound.size();
        assertTrue("Must only contain two regexp, but found " + found + "(" + regexpFound.toArray().toString() + ")",
                found == 2);
        assertTrue("Must contain regexp '" + crawlerTrap1 + "'.", regexpFound.contains(crawlerTrap1));
        assertTrue("Must contain regexp '" + crawlerTrap2 + "'.", regexpFound.contains(crawlerTrap2));
        */
        
        
    }

    private String relevantState(Job job) {
        return "Job:" + "\nJob ID: " + job.getJobID() + "\nHarvest ID: " + job.getOrigHarvestDefinitionID()
                + "\nOrder XML name: " + job.getOrderXMLName() + "\nOrder XML contents: "
                //+ job.getOrderXMLdoc().asXML() + "\nSetting XML files" + job.getSettingsXMLfiles()
                //+ "\nSetting XML docs" + job.getSettingsXMLdocs() + "\nStatus: " + job.getStatus().toString()
                + "\nEdition" + job.getEdition() + "\nDomain->Configuration map" + job.getDomainConfigurationMap()
                + "\nExpected maxObjects: " + job.getMaxObjectsPerDomain()
                + "\nForced maxObjects: "
                + job.getForceMaxObjectsPerDomain()
                + "\nExpected maxBytes: "
                + job.getMaxBytesPerDomain()
                // Checked outside this method, because order of seeds is random
                // + "\nSeedlist: " + job.getSeedListAsString()
                + "\nSubmitted tid: " + job.getSubmittedDate() + "\nCreation tid: " + job.getCreationDate()
                + "\nActual Start: " + job.getActualStart() + "\nActual Stop: " + job.getActualStop() + "\nChannel: "
                + job.getChannel();
    }

    /**
     * Test fields are set correctly, especially harvestNum (after bug 544) and max-bytes since that now uses limit from
     * configuration if present.
     */
    @Test
    public void testCreateJob() {
        DomainConfiguration domainConfiguration = DomainConfigurationTest.createDefaultDomainConfiguration();
        domainConfiguration.setMaxBytes(-1);
        final int harvestNum = 4;
        long harvestId = 1;
        long forceMaxObjectsPerDomain = -1, forceMaxBytesPerDomain = -1, forceMaxJobRunningTime = -1;
        HeritrixTemplate ht = new H1HeritrixTemplate(OrderXmlBuilder.createDefault().getDoc());
        Job j = new Job(harvestId,
                domainConfiguration,
                ht,
                FOCUSED_CHANNEL,
                forceMaxObjectsPerDomain,
                forceMaxBytesPerDomain,
                forceMaxJobRunningTime,
                harvestNum);
        assertEquals("Job should have harvest num set", harvestNum, j.getHarvestNum());
        assertEquals("Job should have harvest id set", Long.valueOf(1L), j.getOrigHarvestDefinitionID());
        assertEquals("Job should have right prio", "FOCUSED", j.getChannel());
        assertEquals("Job should have configuration object limit", domainConfiguration.getMaxObjects(),
                j.getMaxObjectsPerDomain());
        assertEquals("Job should have no byte limit (neither config nor hd sets it)", -1, j.getMaxBytesPerDomain());
        assertEquals("Job should currently have one config", 1, j.getCountDomains());
        assertEquals("Should be the right one", domainConfiguration.getName(),
                j.getDomainConfigurationMap().values().iterator().next());
    }

    @Test
    public void testEditOrderXML_maxBytesPerDomain() throws Exception {
        // Check that order.xml for the job is updated after calling setMaxBytesPerDomain()
        // analogous with what is done in testForceMaxObjectsPerDomain()
        // Should be able to find the value maxBytes2 (333 * 1024 * 1024)
        // in the group-max-success-kb node.
        // xpath-expression that selects the appropriate node in order.xml:
        Job job = createDefaultJob();
        
        //FIXME only appropiate for H1
        /*
        Document orderXML = job.getOrderXMLdoc();
        final String xpath = "/crawl-order/controller/map[@name='pre-fetch-processors']"
                + "/newObject[@name='QuotaEnforcer']" + "/long[@name='group-max-success-kb']";
        Node groupMaxSuccessKbNode = orderXML.selectSingleNode(xpath);

        long maxBytesXML = Long.parseLong(groupMaxSuccessKbNode.getText());
        assertEquals("The group-max-success-kb field should have been updated in the order.xml Document", -1L,
                maxBytesXML);
                */
    }

    @Test
    public void testSetSeedlist() {
        Job job = new Job();
        job.setSeedList("http://www.netarkivet.dk\nhttp://www.kb.dk");
    }

    @Test(expected = ArgumentNotValid.class)
    public void testSetSeedlistWithNull() {
        Job job = new Job();
        job.setSeedList(null);
    }

    @Test(expected = ArgumentNotValid.class)
    public void testSetSeedlistWithEmptySeedlist() {
        Job job = new Job();
        job.setSeedList("");
    }

    public static Job createDefaultJob() {
        long harvestId = 1;
        long forceMaxObjectsPerDomain = -1, forceMaxBytesPerDomain = -1, forceMaxJobRunningTime = -1;
        int harvestNum = 1;
        HeritrixTemplate ht = new H1HeritrixTemplate(OrderXmlBuilder.createDefault().getDoc());
        return new Job(
                harvestId,
                DomainConfigurationTest.createDefaultDomainConfiguration(),
                ht,
                FOCUSED_CHANNEL,
                forceMaxObjectsPerDomain,
                forceMaxBytesPerDomain,
                forceMaxJobRunningTime,
                harvestNum);
    }

    public static Job createDefaultJob(DomainConfiguration domainConfiguration) {
        long harvestId = 1;
        long forceMaxObjectsPerDomain = -1, forceMaxBytesPerDomain = -1, forceMaxJobRunningTime = -1;
        int harvestNum = 1;
        HeritrixTemplate ht = new H1HeritrixTemplate(OrderXmlBuilder.createDefault().getDoc());
        return new Job(
                harvestId,
                domainConfiguration,
                ht,
                FOCUSED_CHANNEL,
                forceMaxObjectsPerDomain,
                forceMaxBytesPerDomain,
                forceMaxJobRunningTime,
                harvestNum);
    }
}