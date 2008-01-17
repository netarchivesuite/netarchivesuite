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

import dk.netarkivet.testutils.ARCTestUtils;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;

import junit.framework.TestCase;

import org.archive.io.ArchiveRecord;
import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;

public class ArcMergeTester extends TestCase {
    private PreventSystemExit pse = new PreventSystemExit();
    private PreserveStdStreams pss = new PreserveStdStreams();
    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.DATA_DIR,TestInfo.WORKING_DIR);
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
    public void testMain() throws IOException {
        File arcFile = new File(TestInfo.WORKING_DIR,"output.arc");
        PrintStream myOut = new PrintStream(new FileOutputStream(arcFile));
        System.setOut(myOut);
        ArcMerge.main(new String[]{TestInfo.ARC1.getAbsolutePath(),TestInfo.ARC2.getAbsolutePath(),TestInfo.ARC3.getAbsolutePath()});
        myOut.close();
        //Put an ARCReader on top of the file.
        ARCReader r = ARCReaderFactory.get(arcFile);
        Iterator<ArchiveRecord> it = r.iterator();
        it.next(); //Skip ARC file header
        //Read the three records, checking mime-type, uri and content.
        assertMatches(it.next(),TestInfo.ARC1_URI,TestInfo.ARC1_MIME,TestInfo.ARC1_CONTENT);
        assertMatches(it.next(),TestInfo.ARC2_URI,TestInfo.ARC2_MIME,TestInfo.ARC2_CONTENT);
        assertMatches(it.next(),TestInfo.ARC3_URI,TestInfo.ARC3_MIME,TestInfo.ARC3_CONTENT);
        //No more records, please.
        assertFalse("Should only have the file header + given records",it.hasNext());
    }

    /**
     * Asserts that the given ARCRecord has the specified uri, mimetype and content.
     */
    private static void assertMatches(ArchiveRecord record, String uri, String mime, String content) {
        ARCRecord arcRecord = (ARCRecord) record;
        ARCRecordMetaData meta = arcRecord.getMetaData();
        assertEquals("Should record the object under the original URI",
                uri, meta.getUrl());
        assertEquals("Should indicate the original MIME type",
                mime, meta.getMimetype());
        String foundContent = ARCTestUtils.readARCRecord(arcRecord);
        assertEquals("Should copy content unchanged",
                content,foundContent);
    }
}
