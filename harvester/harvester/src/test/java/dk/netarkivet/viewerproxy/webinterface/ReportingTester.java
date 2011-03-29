/*
* File:     $Id$
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
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package dk.netarkivet.viewerproxy.webinterface;

import java.io.File;
import java.util.List;

import junit.framework.TestCase;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.TrivialArcRepositoryClient;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.cdx.CDXRecord;
import dk.netarkivet.testutils.CollectionAsserts;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/**
 * Unit tests for Reporting class.
 */
public class ReportingTester extends TestCase {
    private UseTestRemoteFile utrf = new UseTestRemoteFile();
    private ReloadSettings rs = new ReloadSettings();
    private TrivialArcRepositoryClient tarc;
    private File working = new File("tests/dk/netarkivet/viewerproxy/data/working");
    private File tempdir = new File(working, "commontempdir");
    private File dir;

    public void setUp() throws Exception {
        super.setUp();
        rs.setUp();
        utrf.setUp();
        working.mkdirs();
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR,
                tempdir.getAbsolutePath());
              Settings.set(CommonSettings.ARC_REPOSITORY_CLIENT,
                     TrivialArcRepositoryClient.class.getName());
        ArcRepositoryClientFactory.getViewerInstance().close();
        tarc = (TrivialArcRepositoryClient) ArcRepositoryClientFactory
                .getViewerInstance();
        dir = (File) ReflectUtils.getPrivateField(
                TrivialArcRepositoryClient.class, "dir").get(tarc);
        
        // Copy the two files "2-1-20080601120000-00000-dev.arc", "2-metadata-1.arc" to our local
        // archive accessed using a TrivalArcRepositoryClient
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, dir);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        if (tarc != null) {
            tarc.close();
        }
        if (dir != null && dir.isDirectory()) {
            FileUtils.removeRecursively(dir);
        }
        FileUtils.removeRecursively(working);
        utrf.tearDown();
        rs.tearDown();
    }

    public void testGetFilesForJob() throws Exception {
        try {
            Reporting.getFilesForJob(-1);
            fail("Should fail on negative values");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        try {
            Reporting.getFilesForJob(0);
            fail("Should fail on zero");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        CollectionAsserts.assertListEquals("Job 2 chould contain two files", Reporting.getFilesForJob(2), 
                "2-1-20080601120000-00000-dev.arc", "2-metadata-1.arc");
        CollectionAsserts.assertListEquals("Job 4 not harvested, list should be empty", Reporting.getFilesForJob(4));
    }

    public void testGetMetdataCDXRecordsForJob() throws Exception {
        try {
            Reporting.getMetdataCDXRecordsForJob(-1);
            fail("Should fail on negative values");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        try {
            Reporting.getMetdataCDXRecordsForJob(0);
            fail("Should fail on zero");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        List<CDXRecord> recordsForJob = Reporting.getMetdataCDXRecordsForJob(2);
        assertEquals("Should return the expected number of records", 18, recordsForJob.size());
        StringAsserts.assertStringMatches("First record should be preharvester metadata dedup", 
                "^metadata://netarkivet.dk/crawl/setup/duplicatereductionjobs.*", recordsForJob.get(0).getURL());
        StringAsserts.assertStringMatches("Last record should be cdx", 
                "^metadata://netarkivet.dk/crawl/index/cdx.*", recordsForJob.get(recordsForJob.size() - 1).getURL());
        CollectionAsserts.assertListEquals("Job 4 not harvested, list should be empty", 
                Reporting.getMetdataCDXRecordsForJob(4));
    }

    /**
     * Tests the method getCrawlLogForDomainInJob.
     * This unit-test also implicitly tests the class HarvestedUrlsForDomainBatchJob
     * @throws Exception
     */
    public void testGetCrawlLogForDomainInJob() throws Exception {
        int jobId = -1;
        try {
            Reporting.getCrawlLogForDomainInJob("test.dk", jobId);
            fail("Should fail on negative job id");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        jobId = 0;
        try {
            Reporting.getCrawlLogForDomainInJob("test.dk", jobId);
            fail("Should fail on zero jobid");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        jobId = 1;        
        try {
            Reporting.getCrawlLogForDomainInJob("", jobId);
            fail("Should fail on empty domain");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        try {
            Reporting.getCrawlLogForDomainInJob(null, jobId);
            fail("Should fail on null domain");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        
        jobId = 2;
        // Find the crawl-log lines for domain netarkivet.dk in metadata file for job 2
        File file = Reporting.getCrawlLogForDomainInJob("netarkivet.dk", jobId);
        List<String> lines = FileUtils.readListFromFile(file);
        StringAsserts.assertStringContains("First line should be dns", "dns:", lines.get(0));
        StringAsserts.assertStringContains("Last line should be www.netarkivet.dk", 
                "www.netarkivet.dk", lines.get(lines.size() - 1));
        assertEquals("Should have 126 lines (2 dns, 1 netarchive.dk, 121 netarkivet.dk, and 2 www.netarkivet.dk)", 126, lines.size());
 
        // Find the crawl-log lines for domain kaarefc.dk in metadata file for job 2
        file = Reporting.getCrawlLogForDomainInJob("kaarefc.dk", jobId);
        lines = FileUtils.readListFromFile(file);
        StringAsserts.assertStringContains("First line should be dns", "dns:", lines.get(0));
        StringAsserts.assertStringContains("Last line should be w3.org", "w3.org", lines.get(lines.size() - 1));
        assertEquals("Should have 44 lines (1 dns, 40 kaarefc.dk, 3 others)", 44, lines.size());
        System.out.println(FileUtils.readFile(file));

        // Try to find the crawl-log lines for domain doesnotexist.dk in metadata file for job 2
        file = Reporting.getCrawlLogForDomainInJob("doesnotexist.dk", jobId);
        lines = FileUtils.readListFromFile(file);
        assertEquals("Should be empty, no such domain", 0, lines.size());
        
        int nonExistingJobId = 4;
        file = Reporting.getCrawlLogForDomainInJob("netarkivet.dk", nonExistingJobId);
        lines = FileUtils.readListFromFile(file);
        assertEquals("Should be empty, no job 4", 0, lines.size());
    }

}
