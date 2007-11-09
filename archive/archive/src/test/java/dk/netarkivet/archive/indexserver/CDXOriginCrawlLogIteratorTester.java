/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.archive.indexserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import is.hi.bok.deduplicator.CrawlDataItem;
import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.TestFileUtils;

/**
 * Test-class for CDXOriginCrawlLogIterator.
 *
 *
 */
public class CDXOriginCrawlLogIteratorTester extends TestCase {

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        // TODO Auto-generated method stub
        super.setUp();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                TestInfo.WORKING_DIR);

    }

    /* (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        // TODO Auto-generated method stub
        super.tearDown();
    }

    public void testOriginCrawlLogIterator() throws IOException {
        BufferedReader cdx =
                new BufferedReader(new FileReader(TestInfo.CDX_CACHE_4_SORTED));

        CDXOriginCrawlLogIterator it = new CDXOriginCrawlLogIterator
                (TestInfo.CRAWL_LOG_4_SORTED, cdx);

        assertTrue("Should have at least one item", it.hasNext());

        // Test idempotency of hasNext()
        for (int i = 0; i < 2; i++) {
            assertTrue("Should have next, still", it.hasNext());
        }

        // Check double next()s
        // First item found in both cdx and crawl.log
        CrawlDataItem item1 = it.next();
        StringAsserts.assertStringContains("Must have found expected url",
                "fag_www_front.intro", item1.getURL());
        assertEquals("Must have right origin from CDXReader for " + item1.getURL(),
                "54-8-20050620183552-00016-kb-prod-har-001.kb.dk.arc,95054220",
                item1.getOrigin());
        // Second item found in both cdx and crawl.log
        CrawlDataItem item2 = it.next();
        StringAsserts.assertStringContains("Must have found expected url",
                "purl.org/robots.txt", item2.getURL());
        assertEquals("Must have right other origin from CDXReader for " + item2.getURL(),
                "check-arc,43",
                item2.getOrigin());

        assertTrue("Should have item 3 after next()'s", it.hasNext());

        // Four items are missing in the CDX file compared to the crawl.log.
        // They should be silently skipped, and no items should contain their
        // URLs (*om_faget*)
        CrawlDataItem last = null;
        for (int i = 2; i < 13; i++) {
            last = it.next();
            assertNotNull("Should have intervening items", last);
            StringAsserts.assertStringNotContains("Should not have om_faget",
                    "om_faget", last.getURL());
            assertNotNull("Should have origin", last.getOrigin());
        }
        assertEquals("Last item must have origin from CDXReader",
                "54-8-20050620183552-00016-kb-prod-har-001.kb.dk.arc,95086720",
                last.getOrigin());
        assertFalse("Should have no more items now", it.hasNext());
        try {
            it.next();
            fail("Should throw exception on getting next() after last");
        } catch (NoSuchElementException e) {
            assertEquals("Should have right message",
                    "No more items", e.getMessage());
        }

        // Pathalogical cases
        try {
            new CDXOriginCrawlLogIterator(new File("dummy"), cdx);
            fail("Should die on missing log file");
        } catch (IOException e) {
            StringAsserts.assertStringContains("Should mention missing file",
                    "dummy", e.getMessage());
        }

        try {
            new CDXOriginCrawlLogIterator(TestInfo.WORKING_DIR, cdx);
            fail("Should die on dir");
        } catch (IOException e) {
            StringAsserts.assertStringContains("Should mention dir",
                    TestInfo.WORKING_DIR.getName(), e.getMessage());
        }

        try {
            new CDXOriginCrawlLogIterator(TestInfo.CRAWL_LOG_1, (BufferedReader)null);
            fail("Should die on null CDX reader");
        } catch (ArgumentNotValid e) {
            StringAsserts.assertStringContains("Should mention variable",
                    "cdx", e.getMessage());
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
        logLines.addAll(Arrays.asList("",
                "looooooooooooooooooooooooooooooooooooooooooooooong bad line",
                // Long empty line
                "                                                           ",
                "looooooooooooooooooooooong bad line with words      ",
                // Long line missing final "
                "2005-05-06T11:48:31.164Z   200         19 http://purl.org/robots.txt EP http://purl.org/metadata/dublin_core_elements text/plain #025 20050506114810579+20583 TKVAV2AQOUWH6ET7C53B3J7O2MU5IDXI - deduplicate:\"check-arc,43",
                "bad line in crawl log"));
        logLines.addAll(originalLog);
        FileUtils.writeCollectionToFile(TestInfo.CRAWL_LOG_4_SORTED, logLines);

        BufferedReader cdx =
                new BufferedReader(new FileReader(TestInfo.CDX_CACHE_4_SORTED));

        CDXOriginCrawlLogIterator it = new CDXOriginCrawlLogIterator
                (TestInfo.CRAWL_LOG_4_SORTED, cdx);

        for (int i = 0; i < TestInfo.VALID_ENTRIES_IN_CRAWL_LOG_4; i++) {
            CrawlDataItem item = it.next();
            assertNotNull("Should have valid item", item);
        }
        assertFalse("Should have read all entries", it.hasNext());
        cdx.close();

        List<String> cdxLines = new ArrayList<String>();
        cdxLines.addAll(Arrays.asList("",
                "http://base.kb.dk/pls/fag_web/fag_www.om_faget?p_fg_id_nr=1400 foo bar",
                "http://base.kb.dk/pls/fag_web/fag_www_front.intro 2 3 foo 5 6 7 8",
                "1 2 3 4 5 bar 7 8"));
        cdxLines.addAll(FileUtils.readListFromFile(TestInfo.CDX_CACHE_4_SORTED));
        FileUtils.writeCollectionToFile(TestInfo.CDX_CACHE_4_SORTED, cdxLines);

        cdx =  new BufferedReader(new FileReader(TestInfo.CDX_CACHE_4_SORTED));

        it = new CDXOriginCrawlLogIterator(TestInfo.CRAWL_LOG_4_SORTED, cdx);

        // Since the invalid CDX lines are dropped before comparision with the
        // crawl.log URL, having these unsorted entries doesn't matter for the
        // number of valid entries.
        for (int i = 0; i < TestInfo.VALID_ENTRIES_IN_CRAWL_LOG_4; i++) {
            CrawlDataItem item = it.next();
            assertNotNull("Should have valid item", item);
        }
        assertFalse("Should have read all entries", it.hasNext());

        // Test for bug #1004: Checksum now includes sha1.  As a workaround, we
        // append it if needed.
        CrawlDataItem fromNonSha =
                it.parseLine("2005-05-06T11:48:24.182Z   200       1410 http://www.kb.dk/bevarbogen/script.js LE http://www.kb.dk/bevarbogen/ application/x-javascript #020 20050506114824169+3 LLPRTJSSTYX4TCKRKGWG44NTPHUR2ZCH - deduplicate:\"check-arc,42\"");
        assertNotNull("Must have correctly parsed item", fromNonSha);
        assertNotNull("Must have content digest", fromNonSha.getContentDigest());
        assertFalse("Checksum entry should not start with sha1: even if crawl log doesn't",
                fromNonSha.getContentDigest().toLowerCase().startsWith("sha1:"));
        CrawlDataItem fromSha =
                it.parseLine("2005-05-06T11:48:24.182Z   200       1410 http://www.kb.dk/bevarbogen/script.js LE http://www.kb.dk/bevarbogen/ application/x-javascript #020 20050506114824169+3 Sha1:LLPRTJSSTYX4TCKRKGWG44NTPHUR2ZCH - deduplicate:\"check-arc,42\"");
        assertNotNull("Must have correctly parsed item", fromSha);
        assertNotNull("Must have content digest", fromSha.getContentDigest());
        assertFalse("Checksum entry should not start with sha1:",
                fromSha.getContentDigest().toLowerCase().startsWith("sha1:"));
    }
}
