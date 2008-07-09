/*
* File:     $Id$
* Revision: $Revision$
* Author:   $Author$
* Date:     $Date$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.TrivialArcRepositoryClient;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.cdx.CDXRecord;
import dk.netarkivet.testutils.CollectionAsserts;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/**
 * Unit tests for Reporting class
 */
public class ReportingTester extends TestCase {
    private UseTestRemoteFile utrf = new UseTestRemoteFile();
    private ReloadSettings rs = new ReloadSettings();
    private TrivialArcRepositoryClient tarc;
    private File dir;

    public void setUp() throws Exception {
        super.setUp();
        rs.setUp();
        utrf.setUp();
        String[] values = new String[]{TrivialArcRepositoryClient.class.getName()};
        Settings.set(Settings.ARC_REPOSITORY_CLIENT, values);
        ArcRepositoryClientFactory.getViewerInstance().close();
        tarc = (TrivialArcRepositoryClient) ArcRepositoryClientFactory
                .getViewerInstance();
        dir = (File) ReflectUtils.getPrivateField(
                TrivialArcRepositoryClient.class, "dir").get(tarc);
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
        utrf.tearDown();
        rs.tearDown();
    }

    public void testGetFilesForJob() throws Exception {
        try {
            Reporting.getFilesForJob(-1);
            fail("Should fail on negative values");
        } catch (ArgumentNotValid e) {
            //Expectied
        }
        try {
            Reporting.getFilesForJob(0);
            fail("Should fail on zero");
        } catch (ArgumentNotValid e) {
            //Expectied
        }
        CollectionAsserts.assertListEquals("Job 2 chould contain two files", Reporting.getFilesForJob(2), "2-1-20080601120000-00000-dev.arc", "2-metadata-1.arc");
        CollectionAsserts.assertListEquals("Job 4 not harvested, list should be empty", Reporting.getFilesForJob(4));
    }

    public void testGetMetdataCDXRecordsForJob() throws Exception {
        try {
            Reporting.getMetdataCDXRecordsForJob(-1);
            fail("Should fail on negative values");
        } catch (ArgumentNotValid e) {
            //Expectied
        }
        try {
            Reporting.getMetdataCDXRecordsForJob(0);
            fail("Should fail on zero");
        } catch (ArgumentNotValid e) {
            //Expectied
        }
        List<CDXRecord> recordsForJob = Reporting.getMetdataCDXRecordsForJob(2);
        assertEquals("Should return the expected number of records", 18, recordsForJob.size());
        StringAsserts.assertStringMatches("First record should be preharvester metadata dedup", "^metadata://netarkivet.dk/crawl/setup/duplicatereductionjobs.*", recordsForJob.get(0).getURL());
        StringAsserts.assertStringMatches("Last record should be cdx", "^metadata://netarkivet.dk/crawl/index/cdx.*", recordsForJob.get(recordsForJob.size() - 1).getURL());
        CollectionAsserts.assertListEquals("Job 4 not harvested, list should be empty", Reporting.getMetdataCDXRecordsForJob(4));
    }

    public void testGetCrawlLogForDomainInJob() throws Exception {
        try {
            Reporting.getCrawlLogForDomainInJob("test.dk", -1);
            fail("Should fail on negative values");
        } catch (ArgumentNotValid e) {
            //Expectied
        }
        try {
            Reporting.getCrawlLogForDomainInJob("test.dk", 0);
            fail("Should fail on zero");
        } catch (ArgumentNotValid e) {
            //Expectied
        }
        try {
            Reporting.getCrawlLogForDomainInJob("", 1);
            fail("Should fail on empty domain");
        } catch (ArgumentNotValid e) {
            //Expectied
        }
        try {
            Reporting.getCrawlLogForDomainInJob(null, 1);
            fail("Should fail on null domain");
        } catch (ArgumentNotValid e) {
            //Expectied
        }
        File file = Reporting.getCrawlLogForDomainInJob("netarkivet.dk", 2);
        List<String> lines = FileUtils.readListFromFile(file);
        StringAsserts.assertStringContains("First line should be dns", "dns:", lines.get(0));
        StringAsserts.assertStringContains("Last line should be www.netarkivet.dk", "www.netarkivet.dk", lines.get(lines.size() - 1));
        assertEquals("Should have 126 lines (2 dns, 1 netarchive.dk, 121 netarkivet.dk, and 2 www.netarkivet.dk)", 126, lines.size());
        file = Reporting.getCrawlLogForDomainInJob("kaarefc.dk", 2);
        lines = FileUtils.readListFromFile(file);
        StringAsserts.assertStringContains("First line should be dns", "dns:", lines.get(0));
        StringAsserts.assertStringContains("Last line should be w3.org", "w3.org", lines.get(lines.size() - 1));
        assertEquals("Should have 44 lines (1 dns, 40 kaarefc.dk, 3 others)", 44, lines.size());
        System.out.println(FileUtils.readFile(file));
        file = Reporting.getCrawlLogForDomainInJob("doesnotexit.dk",
                                                         2);
        lines = FileUtils.readListFromFile(file);
        assertEquals("Should be empty, no such domain", 0, lines.size());
        file = Reporting.getCrawlLogForDomainInJob("netarkivet.dk",
                                                         4);
        lines = FileUtils.readListFromFile(file);
        assertEquals("Should be empty, no job 4", 0, lines.size());
    }

}
