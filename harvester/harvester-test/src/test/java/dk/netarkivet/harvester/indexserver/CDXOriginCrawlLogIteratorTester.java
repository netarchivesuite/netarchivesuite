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
package dk.netarkivet.harvester.indexserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.TestFileUtils;
import is.hi.bok.deduplicator.CrawlDataItem;

/**
 * Test-class for CDXOriginCrawlLogIterator.
 */
@SuppressWarnings({"unused"})
public class CDXOriginCrawlLogIteratorTester {

    @Before
    public void setUp() throws Exception {
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
    }

    @Test
    public void testOriginCrawlLogIterator() throws IOException {
        BufferedReader cdx = new BufferedReader(new FileReader(TestInfo.CDX_CACHE_4_SORTED));

        CDXOriginCrawlLogIterator it = new CDXOriginCrawlLogIterator(TestInfo.CRAWL_LOG_4_SORTED, cdx);

        assertTrue("Should have at least one item", it.hasNext());

        // Test idempotency of hasNext()
        for (int i = 0; i < 2; i++) {
            assertTrue("Should have next, still", it.hasNext());
        }

        // Check double next()s
        // First item found in both cdx and crawl.log
        CrawlDataItem item1 = it.next();
        StringAsserts.assertStringContains("Must have found expected url", "fag_www_front.intro", item1.getURL());
        assertEquals("Must have right origin from CDXReader for " + item1.getURL(),
                "54-8-20050620183552-00016-kb-prod-har-001.kb.dk.arc,95054220,20050506114817000", item1.getOrigin());
        // Second item found in both cdx and crawl.log
        CrawlDataItem item2 = it.next();
        StringAsserts.assertStringContains("Must have found expected url", "purl.org/robots.txt", item2.getURL());
        assertEquals("Must have right other origin from CDXReader for " + item2.getURL(), "check-arc,43",
                item2.getOrigin());

        assertTrue("Should have item 3 after next()'s", it.hasNext());

        // Four items are missing in the CDX file compared to the crawl.log.
        // They should be silently skipped, and no items should contain their
        // URLs (*om_faget*)
        CrawlDataItem last = null;
        for (int i = 2; i < 13; i++) {
            last = it.next();
            assertNotNull("Should have intervening items", last);
            StringAsserts.assertStringNotContains("Should not have om_faget", "om_faget", last.getURL());
            assertNotNull("Should have origin", last.getOrigin());
        }
        assertEquals("Last item must have origin from CDXReader",
                "54-8-20050620183552-00016-kb-prod-har-001.kb.dk.arc,95086720,20050506114822000", last.getOrigin());
        assertFalse("Should have no more items now", it.hasNext());
        try {
            it.next();
            fail("Should throw exception on getting next() after last");
        } catch (NoSuchElementException e) {
            assertEquals("Should have right message", "No more items", e.getMessage());
        }

        // Pathological cases
        try {
            new CDXOriginCrawlLogIterator(new File("dummy"), cdx);
            fail("Should die on missing log file");
        } catch (IOException e) {
            StringAsserts.assertStringContains("Should mention missing file", "dummy", e.getMessage());
        }

        try {
            new CDXOriginCrawlLogIterator(TestInfo.WORKING_DIR, cdx);
            fail("Should die on dir");
        } catch (IOException e) {
            StringAsserts.assertStringContains("Should mention dir", TestInfo.WORKING_DIR.getName(), e.getMessage());
        }

        try {
            new CDXOriginCrawlLogIterator(TestInfo.CRAWL_LOG_1, (BufferedReader) null);
            fail("Should die on null CDX reader");
        } catch (ArgumentNotValid e) {
            StringAsserts.assertStringContains("Should mention variable", "cdx", e.getMessage());
        }
        // Test crawl log 1 to make it easier to use in other tests
        cdx = new BufferedReader(new FileReader(TestInfo.CDX_CACHE_1_SORTED));
        it = new CDXOriginCrawlLogIterator(TestInfo.CRAWL_LOG_1_SORTED, cdx);
        for (int i = 0; i < TestInfo.VALID_ENTRIES_IN_CRAWL_LOG_1; i++) {
            CrawlDataItem item = it.next();
        }
        assertFalse("Should have read all entries", it.hasNext());
    }

    public void testParseLine() throws Exception {
        List<String> originalLog = FileUtils.readListFromFile(TestInfo.CRAWL_LOG_4_SORTED);
        List<String> logLines = new ArrayList<String>();
        logLines.addAll(Arrays
                .asList("",
                        "looooooooooooooooooooooooooooooooooooooooooooooong bad line",
                        // Long empty line
                        "                                                           ",
                        "looooooooooooooooooooooong bad line with words      ",
                        // Long line missing final "
                        "2005-05-06T11:48:31.164Z   200         19 http://purl.org/robots.txt EP http://purl.org/metadata/dublin_core_elements text/plain #025 20050506114810579+20583 TKVAV2AQOUWH6ET7C53B3J7O2MU5IDXI - deduplicate:\"check-arc,43",
                        "bad line in crawl log"));
        logLines.addAll(originalLog);
        FileUtils.writeCollectionToFile(TestInfo.CRAWL_LOG_4_SORTED, logLines);

        BufferedReader cdx = new BufferedReader(new FileReader(TestInfo.CDX_CACHE_4_SORTED));

        CDXOriginCrawlLogIterator it = new CDXOriginCrawlLogIterator(TestInfo.CRAWL_LOG_4_SORTED, cdx);

        for (int i = 0; i < TestInfo.VALID_ENTRIES_IN_CRAWL_LOG_4; i++) {
            CrawlDataItem item = it.next();
            assertNotNull("Should have valid item", item);
        }
        assertFalse("Should have read all entries", it.hasNext());
        cdx.close();

        List<String> cdxLines = new ArrayList<String>();
        cdxLines.addAll(Arrays.asList("", "http://base.kb.dk/pls/fag_web/fag_www.om_faget?p_fg_id_nr=1400 foo bar",
                "http://base.kb.dk/pls/fag_web/fag_www_front.intro 2 3 foo 5 6 7 8", "1 2 3 4 5 bar 7 8"));
        cdxLines.addAll(FileUtils.readListFromFile(TestInfo.CDX_CACHE_4_SORTED));
        FileUtils.writeCollectionToFile(TestInfo.CDX_CACHE_4_SORTED, cdxLines);

        cdx = new BufferedReader(new FileReader(TestInfo.CDX_CACHE_4_SORTED));

        it = new CDXOriginCrawlLogIterator(TestInfo.CRAWL_LOG_4_SORTED, cdx);

        // Since the invalid CDX lines are dropped before comparision with the
        // crawl.log URL, having these unsorted entries doesn't matter for the
        // number of valid entries.
        for (int i = 0; i < TestInfo.VALID_ENTRIES_IN_CRAWL_LOG_4; i++) {
            CrawlDataItem item = it.next();
            assertNotNull("Should have valid item", item);
        }
        assertFalse("Should have read all entries", it.hasNext());

        // Test for bug #1004: Checksum now includes sha1. As a workaround, we
        // append it if needed.
        CrawlDataItem fromNonSha = it
                .parseLine("2005-05-06T11:48:24.182Z   200       1410 http://www.kb.dk/bevarbogen/script.js LE http://www.kb.dk/bevarbogen/ application/x-javascript #020 20050506114824169+3 LLPRTJSSTYX4TCKRKGWG44NTPHUR2ZCH - deduplicate:\"check-arc,42\"");
        assertNotNull("Must have correctly parsed item", fromNonSha);
        assertNotNull("Must have content digest", fromNonSha.getContentDigest());
        assertFalse("Checksum entry should not start with sha1: even if crawl log doesn't", fromNonSha
                .getContentDigest().toLowerCase().startsWith("sha1:"));
        CrawlDataItem fromSha = it
                .parseLine("2005-05-06T11:48:24.182Z   200       1410 http://www.kb.dk/bevarbogen/script.js LE http://www.kb.dk/bevarbogen/ application/x-javascript #020 20050506114824169+3 Sha1:LLPRTJSSTYX4TCKRKGWG44NTPHUR2ZCH - deduplicate:\"check-arc,42\"");
        assertNotNull("Must have correctly parsed item", fromSha);
        assertNotNull("Must have content digest", fromSha.getContentDigest());
        assertFalse("Checksum entry should not start with sha1:",
                fromSha.getContentDigest().toLowerCase().startsWith("sha1:"));

    }

    /**
     * Checks, if the CDXOriginCrawlLogIterator works with password protected contents, where one crawl.log corresponds
     * with two CDXlines, and the correct one, (the last one) needs to be selected. bug 680
     *
     * @throws Exception
     */
    @Test
    public void testbug680() throws Exception {
        File unsortedCrawlLogFile = new File(TestInfo.CRAWLLOGS_DIR, "crawl-680.log");
        File sortedCrawlLogFile = new File(TestInfo.CRAWLLOGS_DIR, "crawl-680-sorted.log");

        File unsortedCDXFile = new File(TestInfo.CDXDATACACHE_DIR, "cdxdata-680");
        File sortedCDXFile = new File(TestInfo.CDXDATACACHE_DIR, "cdxdata-sorted-680");

        FileUtils.sortCrawlLog(unsortedCrawlLogFile, sortedCrawlLogFile);
        FileUtils.sortCDX(unsortedCDXFile, sortedCDXFile);

        BufferedReader cdx = new BufferedReader(new FileReader(sortedCDXFile));
        CDXOriginCrawlLogIterator it = new CDXOriginCrawlLogIterator(sortedCrawlLogFile, cdx);

        String privateUrl = "http://www.kaarefc.dk/private/";

        assertTrue("Must contain at least one item", it.hasNext());
        boolean foundPrivateUrl = false;
        CrawlDataItem item = null;
        while (!foundPrivateUrl && it.hasNext()) {
            item = it.next();
            if (item.getURL().equals(privateUrl)) {
                foundPrivateUrl = true;
            }
        }
        assertTrue("Should have found the private url", foundPrivateUrl);
        String correctOrigin = "1-1-20071206233504-00000-dhcppc1.arc,10204,20071206233508000";
        assertTrue("Wrong origin. Excepted: " + correctOrigin + ", Found: " + item.getOrigin(), item.getOrigin().equals(correctOrigin));

        // Introducing new set of testfiles based on job 9 run 27-12-2007

        unsortedCrawlLogFile = new File(TestInfo.CRAWLLOGS_DIR, "crawl-9.log");
        sortedCrawlLogFile = new File(TestInfo.CRAWLLOGS_DIR, "crawl-9-sorted.log");

        unsortedCDXFile = new File(TestInfo.CDXDATACACHE_DIR, "cdxdata-9-cache");
        sortedCDXFile = new File(TestInfo.CDXDATACACHE_DIR, "cdxdata-9-sorted-cache");

        FileUtils.sortCrawlLog(unsortedCrawlLogFile, sortedCrawlLogFile);
        FileUtils.sortCDX(unsortedCDXFile, sortedCDXFile);

        cdx = new BufferedReader(new FileReader(sortedCDXFile));
        it = new CDXOriginCrawlLogIterator(sortedCrawlLogFile, cdx);

        assertTrue("Must contain at least one item", it.hasNext());
        foundPrivateUrl = false;
        item = null;
        while (!foundPrivateUrl && it.hasNext()) {
            item = it.next();
            if (item.getURL().equals(privateUrl)) {
                foundPrivateUrl = true;
            }
        }
        assertTrue("Should have found the private url", foundPrivateUrl);
        correctOrigin = "9-2-20071227125128-00000-kb-test-har-002.kb.dk.arc,9167,20071227125134000";
        assertTrue("Wrong Origin. Should have been '" + correctOrigin + "', but was '" + item.getOrigin() + "'.", item
                .getOrigin().equals(correctOrigin));

    }
}
