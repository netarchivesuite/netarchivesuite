/*
 * #%L
 * Netarchivesuite - harvester - test
 * %%
 * Copyright (C) 2005 - 2017 The Royal Danish Library, 
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
package dk.netarkivet.harvester.harvesting.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.StopReason;
import dk.netarkivet.harvester.harvesting.HeritrixFiles;
import dk.netarkivet.harvester.harvesting.JobInfoTestImpl;
import dk.netarkivet.testutils.LogbackRecorder;
import dk.netarkivet.testutils.Serial;
import dk.netarkivet.testutils.TestResourceUtils;

public class LegacyHarvestReportTester {
    @Rule public TestName test = new TestName();
    private File WORKING_DIR;
    private LogbackRecorder logbackRecorder;
    private HeritrixFiles heritrixFiles;
    private static final String CRAWL_LOG = "crawl.log";

    @Before
    public void setUp() throws Exception {
        WORKING_DIR = new File(TestResourceUtils.OUTPUT_DIR, getClass().getSimpleName() + "/" + test.getMethodName());
        FileUtils.removeRecursively(WORKING_DIR);
        FileUtils.createDir(WORKING_DIR);
        FileUtils.createDir(new File(WORKING_DIR, "logs"));
        logbackRecorder = LogbackRecorder.startRecorder();
        heritrixFiles = HeritrixFiles.getH1HeritrixFilesWithDefaultJmxFiles(WORKING_DIR, new JobInfoTestImpl(1L, 1L));
        prepareCrawlLog(CRAWL_LOG);
    }

    @After
    public void tearDown() {
        logbackRecorder.stopRecorder();
    }

    @Test (expected = ArgumentNotValid.class)
    public void testConstructorWithNullArgument() {
        new LegacyHarvestReport(null);
    }

    @Test
    public void testConstructorInvalidReportFile() {
        prepareCrawlLog("invalid-crawl.log");
        new HarvestReportGenerator(heritrixFiles);
        logbackRecorder.assertLogContains("Should have log about invalid line", "Invalid line in");
    }

    @Test
    public void testConstructor() {
        // Test parse error
        prepareCrawlLog("invalid-crawl.log");
        HeritrixFiles hf = HeritrixFiles.getH1HeritrixFilesWithDefaultJmxFiles(WORKING_DIR, new JobInfoTestImpl(1L, 1L));
        
        HarvestReportGenerator hrg = new HarvestReportGenerator(hf);
        logbackRecorder.assertLogContains("Should have log about invalid line", "Invalid line in");
        
		DomainStatsReport dsr = new DomainStatsReport(hrg.getDomainStatsMap(), 
				hrg.getDefaultStopReason()); 
        
        AbstractHarvestReport hostReport = new LegacyHarvestReport(dsr);

        assertNotNull("A AbstractHarvestReport should have a non-null set of domain names", hostReport.getDomainNames());
        assertNotNull("A AbstractHarvestReport should have a non-null number of object counts",
                hostReport.getObjectCount("netarkivet.dk"));
        assertNotNull("A AbstractHarvestReport should have a non-null number of bytes retrieved",
                hostReport.getByteCount("netarkivet.dk"));
    }

    @Test
    public void testGetDomainNames() throws IOException {
    	prepareCrawlLog("logs/crawl.log");
        HeritrixFiles hf = HeritrixFiles.getH1HeritrixFilesWithDefaultJmxFiles(WORKING_DIR, new JobInfoTestImpl(1L, 1L));
        
		DomainStatsReport dsr = HarvestReportGenerator.getDomainStatsReport(hf);
        AbstractHarvestReport hostReport = new LegacyHarvestReport(dsr);

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File(WORKING_DIR, "logs/crawl.log"))));
        int lineCnt = 0;
        while (reader.readLine() != null) {
            ++lineCnt;
        }
        reader.close();

        if (lineCnt > 1) {
            assertTrue("Number of domain names in AbstractHarvestReport should be > 0, assuming "
                    + "that the number of lines in the host-reports.txt file is > 1", hostReport.getDomainNames()
                    .size() > 0);
        }

        // Number of domain names in AbstractHarvestReport should be less than or equal to
        // the number of lines in the host-reports.txt file (minus 1 , due to header):
        String TEST_DOMAIN = "netarkivet.dk";
        int NO_OF_TEST_DOMAINS = 2;
        Assert.assertEquals("Number of domain names in AbstractHarvestReport should equal testnumber "
                        + NO_OF_TEST_DOMAINS, NO_OF_TEST_DOMAINS,
                hostReport.getDomainNames().size());

        // Check if set of domain names contains normalized domain name TEST_DOMAIN:
        assertTrue("hostReport.getDomainNames() should contain domain name " + TEST_DOMAIN,
                hostReport.getDomainNames().contains(TEST_DOMAIN));
    }

    @Test
    public void testGetObjectCount() {
        String TEST_DOMAIN = "netarkivet.dk";
        int NO_OF_OBJECTS_TEST = 37;
        
		DomainStatsReport dsr = HarvestReportGenerator.getDomainStatsReport(heritrixFiles);
        AbstractHarvestReport hostReport = new LegacyHarvestReport(dsr);

        assertEquals("getObjectCount(TEST_DOMAIN)) should expected to return " + NO_OF_OBJECTS_TEST,
                NO_OF_OBJECTS_TEST,  (long) hostReport.getObjectCount(TEST_DOMAIN));
        assertNull("AbstractHarvestReport.getObjectCount('bibliotek.dk')) expected to return Null",
                hostReport.getObjectCount("bibliotek.dk"));

    }

    @Test
    public void testGetByteCount() {
        String TEST_DOMAIN = "netarkivet.dk";
        int NO_OF_BYTES_TEST = 1162154;
        
		DomainStatsReport dsr = HarvestReportGenerator.getDomainStatsReport(heritrixFiles); 
		
        AbstractHarvestReport hostReport = new LegacyHarvestReport(dsr);

        assertEquals("getByteCount(TEST_DOMAIN)) expected to return " + NO_OF_BYTES_TEST, NO_OF_BYTES_TEST,
                (long) hostReport.getByteCount(TEST_DOMAIN));
        assertNull("AbstractHarvestReport.getByteCount('bibliotek.dk')) expected to return Null",
                hostReport.getByteCount("bibliotek.dk"));
    }

    /** Test solution to bugs 391 - hosts report with long values. */
    @Test
    public void testLongValues() {
        String TEST_DOMAIN = "dom.dk";
        prepareCrawlLog("crawl-long.log");
        HeritrixFiles hf = HeritrixFiles.getH1HeritrixFilesWithDefaultJmxFiles(WORKING_DIR, new JobInfoTestImpl(1L, 1L));
        
		DomainStatsReport dsr = HarvestReportGenerator.getDomainStatsReport(hf);
        
        AbstractHarvestReport hr = new LegacyHarvestReport(dsr);
        Long expectedObjectCount = Long.valueOf(2L);
        assertEquals("Counts should equal input data", expectedObjectCount, hr.getObjectCount(TEST_DOMAIN));
        Long expectedByteCount = new Long(5500000001L);
        assertEquals("Counts should equal input data", expectedByteCount, hr.getByteCount(TEST_DOMAIN));

    }

    /**
     * Test solution to bugs 392 - hosts report with byte counts which add to a long value.
     */
    @Test
    public void testAddLongValues() {
    	prepareCrawlLog("crawl-addslong.log");
    	// NOt used ?
        //HeritrixFiles hf = HeritrixFiles.getH1HeritrixFilesWithDefaultJmxFiles(WORKING_DIR, new JobInfoTestImpl(1L, 1L));
		DomainStatsReport dsr = HarvestReportGenerator.getDomainStatsReport(heritrixFiles);
        
        AbstractHarvestReport hr = new LegacyHarvestReport(dsr);
      
        assertEquals("Counts should equal input data", new Long(2500000000l), hr.getByteCount("nosuchdomain.dk"));
    }

    @Test
    public void testStopReason() {
    	prepareCrawlLog("stop-reason-crawl.log");
    	// NOt used ?
        //HeritrixFiles hf = HeritrixFiles.getH1HeritrixFilesWithDefaultJmxFiles(WORKING_DIR, new JobInfoTestImpl(1L, 1L));
		DomainStatsReport dsr = HarvestReportGenerator.getDomainStatsReport(heritrixFiles);
        
        AbstractHarvestReport hr = new LegacyHarvestReport(dsr);
        
        assertEquals("kb.dk is unfinished", StopReason.DOWNLOAD_UNFINISHED, hr.getStopReason("kb.dk"));
        assertEquals("netarkivet.dk reached byte limit", StopReason.SIZE_LIMIT, hr.getStopReason("netarkivet.dk"));
        assertEquals("statsbiblioteket.dk reached object limit", StopReason.OBJECT_LIMIT,
                hr.getStopReason("statsbiblioteket.dk"));
        assertEquals("no information about bibliotek.dk", null, hr.getStopReason("bibliotek.dk"));
    }

    @Test
    public void testIDNA() {
        prepareCrawlLog("idna-crawl.log");
        HeritrixFiles hf = HeritrixFiles.getH1HeritrixFilesWithDefaultJmxFiles(WORKING_DIR, new JobInfoTestImpl(1L, 1L));
		DomainStatsReport dsr = HarvestReportGenerator.getDomainStatsReport(hf);
        
        AbstractHarvestReport hr = new LegacyHarvestReport(dsr);
       
        boolean disregardSeedUrl = Settings.getBoolean(HarvesterSettings.DISREGARD_SEEDURL_INFORMATION_IN_CRAWLLOG);
        if (disregardSeedUrl) {
            assertTrue(hr.getByteCount("oelejr.dk") != null);
            assertTrue(hr.getByteCount("østerbrogades-dyrlæger.dk") != null);
            assertTrue(hr.getDomainNames().size() > 2);
        } else {
            assertTrue(hr.getByteCount("ølejr.dk") != null);
            assertTrue(hr.getByteCount("østerbrogades-dyrlæger.dk") != null);
            assertTrue(hr.getDomainNames().size() == 2);
        }
    }

    /**
     * Tests object can be serialized and deserialized preserving state.
     */
    @Test
    public void testSerializability() throws IOException, ClassNotFoundException {
        prepareCrawlLog("logs/crawl.log");
        HeritrixFiles hf = HeritrixFiles.getH1HeritrixFilesWithDefaultJmxFiles(WORKING_DIR, new JobInfoTestImpl(1L, 1L));
        
		DomainStatsReport dsr = HarvestReportGenerator.getDomainStatsReport(hf);        
        AbstractHarvestReport hr = new LegacyHarvestReport(dsr);
 
        AbstractHarvestReport hr2 = Serial.serial(hr);
        assertEquals("Relevant state should be preserved", relevantState(hr), relevantState(hr2));
    }

    private String relevantState(AbstractHarvestReport hhr) {
        String s = "";
        List<String> list = new ArrayList<String>(hhr.getDomainNames());
        Collections.sort(list);
        for (String x : list) {
            s += x + "##" + hhr.getByteCount(x) + "##" + hhr.getObjectCount(x) + "\n";
        }
        return s;
    }

    /*
    private AbstractHarvestReport createValidHeritrixHostsReport() {
        File testFile = TestInfo.REPORT_FILE;
        FileUtils.copyFile(testFile, new File(TestInfo.WORKING_DIR, "logs/crawl.log"));
        HeritrixFiles hf = HeritrixFiles.getH1HeritrixFilesWithDefaultJmxFiles(TestInfo.WORKING_DIR, new JobInfoTestImpl(1L, 1L));
        return new LegacyHarvestReport(hf);
        */

    /**
     * Copies the indicated crawllog to the WORKING_DIR as logs/crawl.log
     * @param crawllog The name of the crawllog to lookup on the classpath.
     */
    private void prepareCrawlLog(String crawllog) {
        final File CRAWL_LOG_DIR = new File("src/test/resources/harvesting/report/LegacyHarvestReportTester");
        File crawlLog = new File(CRAWL_LOG_DIR, crawllog);
        FileUtils.copyFile(crawlLog, new File(WORKING_DIR, "logs/crawl.log"));
    }
}

