/*
 * #%L
 * Netarchivesuite - wayback - test
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
package dk.netarkivet.wayback.batch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.archive.wayback.core.CaptureSearchResult;
import org.archive.wayback.resourceindex.cdx.CDXLineToSearchResultAdapter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.wayback.TestInfo;

/**
 * Unittest for the class DeduplicateToCDXAdapter.
 */
public class DeduplicateToCDXAdapterTester {

    public static final String DEDUP_CRAWL_LOG = "dedup_crawl_log.txt";
    public static final String DEDUP_CRAWL_STRING = "2009-05-25T13:00:00.992Z   200       "
            + "9717 https://wiki.statsbiblioteket.dk/wiki/summa/img/draft.png LLEE "
            + "https://wiki.statsbiblioteket.dk/wiki/summa/css/screen.css image/png #016 "
            + "20090525130000915+76 sha1:AXH2IFNXC4MUT26SRHRJZHGR3FDAJDNR - duplicate:\"1-1-20090513141823-00008-"
            + "sb-test-har-001.statsbiblioteket.dk.arc,22962264\",content-size:9969";
    public static final String DEDUP_CRAWL_STRING2 = "2011-05-11T16:41:49.968Z 200 50436 http://webtv.metropol.dk/swf/webtv_promohorizontal.swf LLX "
            + "http://www.sporten.dk/sport/fodbold application/x-shockwave-flash #008 20110511164149870+61 "
            + "sha1:KBHBHEUCX5CN7KB3P2ZVBHGCCIFJNIWH - le:IOException@ExtractorSWF,duplicate:"
            + "\"118657-119-20110428163750-00001-kb-prod-har-004.kb.dk.arc,69676377\",content-size:50842";


    public static final String DEDUP_CRAWL_STRING3 = "2016-11-21T13:10:51.640Z   200       5430 http://maps.google.com/favicon.ico"
            + " REPI http://maps.google.com/robots.txt image/x-icon #040 20161121131051607+18 sha1:JETDNFPWWDG5OL2FZ4NXOXTGB7ODNRQG"
            + " http://skraedderiet.dk duplicate:\"6646-248-20161114122816274-00000-kb-test-har-003.kb.dk.warc,93364,20161114122823282\",content-size:5776";

    public static final String MULTI_ANNOTATED_STRIMG = "2016-12-05T13:09:40.223Z   200       1100 http://douglasadams.s3-website-eu-west-1.amazonaws.com/images/title2.gif LLEER http://www.douglasadams.com/images/title2.gif image/gif #029 20161205130940079+141 sha1:GXNQ4E7NHP556JC7ADL2LLBAB7EH37EL www.trinekc.dk unsatisfiableContentEncoding:ISO-8859-1,duplicate:\"2-2-20161205125206020-00000-kb-test-har-004.kb.dk.warc,20135989,20161205125232263\",content-size:1484,3t\n";

    public static final String NAS_2598_STRING = "2016-12-05T10:11:38.808Z   200       2026 http://www.w3.org/Icons/valid-xhtml10-blue.png EI http://www.w3.org/Icons/valid-xhtml10-blue image/png #041 20161205101137572+1226 sha1:C3PH3IWTSURQ7XILQRHDIDDGAD2ORRPH www.kaarefc.dk duplicate:\"2-1-20161205100306320-00000-4320~kb-test-har-004.kb.dk~8173.arc,151298,20161205100315930\",content-size:2408";
    public static final String NAS_2598_STRING2 = "2016-12-05T10:35:50.553Z   200      45292 http://www.trineogkaare.dk/images/n003-a.jpg ELE http://www.trineogkaare.dk/bilkir.html image/jpeg #031 20161205103550286+264 sha1:UZ3MSPJL3PQR3C65IMEYZJ5NNJJPDTBA www.trineogkaare.dk duplicate:\"5-2-20161205101552457-00000-4551~kb-test-har-004.kb.dk~8171.arc,30098791,20161205101622637\",content-size:45603";


    @Before
    public void setUp() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
        // System.out.println(DEDUP_CRAWL_STRING);
    }

    @After
    public void tearDown() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
    }

    @Test
    public void testCtor() {
        new DeduplicateToCDXAdapter();
    }

    @Test
    public void testMultiAdaptLine() {
        DeduplicateToCDXAdapterInterface adapter = new DeduplicateToCDXAdapter();
        String cdx_line = adapter.adaptLine(MULTI_ANNOTATED_STRIMG);
        CDXLineToSearchResultAdapter adapter2 = new CDXLineToSearchResultAdapter();
        CaptureSearchResult result = adapter2.adapt(cdx_line);
        assertEquals("Should get the arcfilename back out of the cdx line",
                "2-2-20161205125206020-00000-kb-test-har-004.kb.dk.warc", result.getFile());
        assertEquals("Should get the right http code out of the cdx line", "200", result.getHttpCode());
    }

    @Test
    public void testAdaptLineNAS2598() {
        DeduplicateToCDXAdapterInterface adapter = new DeduplicateToCDXAdapter();
        String cdx_line = adapter.adaptLine(NAS_2598_STRING);
        CDXLineToSearchResultAdapter adapter2 = new CDXLineToSearchResultAdapter();
        CaptureSearchResult result = adapter2.adapt(cdx_line);
        assertEquals("Should get the arcfilename back out of the cdx line",
                "2-1-20161205100306320-00000-4320~kb-test-har-004.kb.dk~8173.arc", result.getFile());
    }


    @Test
    public void testAdaptLine() {
        DeduplicateToCDXAdapterInterface adapter = new DeduplicateToCDXAdapter();
        String cdx_line = adapter.adaptLine(DEDUP_CRAWL_STRING3);
        CDXLineToSearchResultAdapter adapter2 = new CDXLineToSearchResultAdapter();
        CaptureSearchResult result = adapter2.adapt(cdx_line);
        assertEquals("Should get the arcfilename back out of the cdx line",
                "6646-248-20161114122816274-00000-kb-test-har-003.kb.dk.warc", result.getFile());
        assertEquals("Should get the right http code out of the cdx line", "200", result.getHttpCode());
    }

    @Test
    public void testAdaptLineExtended() {
        DeduplicateToCDXAdapterInterface adapter = new DeduplicateToCDXAdapter();
        String cdx_line = adapter.adaptLine(DEDUP_CRAWL_STRING);
        CDXLineToSearchResultAdapter adapter2 = new CDXLineToSearchResultAdapter();
        CaptureSearchResult result = adapter2.adapt(cdx_line);
        assertEquals("Should get the arcfilename back out of the cdx line",
                "1-1-20090513141823-00008-sb-test-har-001.statsbiblioteket.dk.arc", result.getFile());
        assertEquals("Should get the right http code out of the cdx line", "200", result.getHttpCode());

        String cdx_line2 = adapter.adaptLine(DEDUP_CRAWL_STRING2);
        CaptureSearchResult result2 = adapter2.adapt(cdx_line2);
        assertEquals("Should get the arcfilename back out of the cdx line",
                "118657-119-20110428163750-00001-kb-prod-har-004.kb.dk.arc", result2.getFile());
        assertEquals("Should get the right http code out of the cdx line", "200", result2.getHttpCode());

    }

    @Test
    public void testAdaptStream() throws IOException {
        InputStream is = new FileInputStream(new File(TestInfo.WORKING_DIR, DEDUP_CRAWL_LOG));
        OutputStream os = new ByteArrayOutputStream();
        DeduplicateToCDXAdapterInterface adapter = new DeduplicateToCDXAdapter();
        adapter.adaptStream(is, os);
        os.close();
        String output = os.toString();
        String[] lines = output.split("\n");
        CDXLineToSearchResultAdapter adapter2 = new CDXLineToSearchResultAdapter();
        for (String line : lines) {
            CaptureSearchResult csr = adapter2.adapt(line);
            assertNotNull("Should have a valid mime type for every line, inclding '" + line + "'", csr.getMimeType());
        }
        assertTrue("expect at least 3 lines of output, got " + lines.length, lines.length > 2);
    }

}
