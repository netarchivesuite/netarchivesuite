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
package dk.netarkivet.common.tools;

import dk.netarkivet.common.utils.cdx.CDXRecord;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;

import junit.framework.TestCase;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * We do not test behaviour on bad ARC files,
 * relying on the unit test of ExtractCDXJob.
 */
public class ExtractCDXTester extends TestCase {
    private PreventSystemExit pse = new PreventSystemExit();
    private PreserveStdStreams pss = new PreserveStdStreams(true);
    private MoveTestFiles mtf = new MoveTestFiles(
            TestInfo.DATA_DIR, TestInfo.WORKING_DIR);

    public void setUp(){
        mtf.setUp();
        pss.setUp();
        pse.setUp();
    }
    public void tearDown(){
        pse.tearDown();
        pss.tearDown();
        mtf.tearDown();
    }

    /**
     * Verify that indexing a single ARC file works as expected.
     */
    public void testMainOneFile() {
        File f = TestInfo.ARC1;
        ExtractCDX.main(new String[]{TestInfo.ARC1.getAbsolutePath()});
        List<CDXRecord> rList = getRecords();
        assertEquals(
                "Output CDX records should be 1-1 with input ARC file records",
                1, rList.size());
        assertMatches(rList, 0,
                TestInfo.ARC1_URI, TestInfo.ARC1_MIME, TestInfo.ARC1_CONTENT);
    }

    /**
     * Verify that indexing more than one ARC file works as expected.
     */
    public void testMainSeveralFiles() {
        ExtractCDX.main(new String[]{
                TestInfo.ARC1.getAbsolutePath(),
                TestInfo.ARC2.getAbsolutePath()});
        List<CDXRecord> rList = getRecords();
        assertEquals(
                "Output CDX records should be 1-1 with input ARC file records",
                2, rList.size());
        assertMatches(rList, 0,
                TestInfo.ARC1_URI, TestInfo.ARC1_MIME, TestInfo.ARC1_CONTENT);
        assertMatches(rList, 1,
                TestInfo.ARC2_URI, TestInfo.ARC2_MIME, TestInfo.ARC2_CONTENT);
    }

    /**
     * Verify that non-ARC files are rejected and execution fails.
     */
    public void testMainNonArc() {
        try {
            ExtractCDX.main(new String[]{
                    TestInfo.ARC1.getAbsolutePath(),
                    TestInfo.INDEX_FILE.getAbsolutePath()});
            fail("Calling ExtractCDX with non-arc file should System.exit");
        } catch (SecurityException e) {
            //Expected
            assertEquals(
                    "No output should be sent to stdout when ExtraqctCDX fails",
                    "", pss.getOut());
        }
    }

    /**
     * Verifies that calling ExtractCDX without arguments fails.
     */
    public void testNoArguments() {
        try {
            ExtractCDX.main(new String[]{});
            fail("Calling ExtractCDX without arguments should System.exit");
        } catch (SecurityException e) {
            //Expected
            assertEquals(
                    "No output should be sent to stdout when ExtraqctCDX fails",
                    "", pss.getOut());
        }
    }

    /**
     * Parses output from stdOut as a cdx file.
     * @return All records from the output cdx file, as a List.
     */
    private List<CDXRecord> getRecords() {
        List<CDXRecord> result = new ArrayList<CDXRecord>();
        for (String cdxLine : pss.getOut().split("\n")) {
            result.add(new CDXRecord(cdxLine.split("\\s+")));
        }
        return result;
    }

    /**
     * Asserts that the nth record in the given list
     * has the specified uri and mimetype, and the
     * the length field matches the length of the given
     * content.
     */
    private void assertMatches(
            List<CDXRecord> rList,
            int index,
            String uri,
            String mime,
            String content) {
        CDXRecord rec = rList.get(index);
        assertEquals(
                "Output CDX records should be 1-1 with input ARC file records",
                uri, rec.getURL());
        assertEquals(
                "Output CDX records should be 1-1 with input ARC file records",
                mime, rec.getMimetype());
        assertEquals(
                "Output CDX records should be 1-1 with input ARC file records",
                content.length(), rec.getLength());
    }
}
