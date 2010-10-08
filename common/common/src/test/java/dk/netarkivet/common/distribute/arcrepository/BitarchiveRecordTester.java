/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
package dk.netarkivet.common.distribute.arcrepository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import junit.framework.TestCase;
import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;
import org.archive.io.warc.WARCReader;
import org.archive.io.warc.WARCReaderFactory;
import org.archive.io.warc.WARCRecord;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StreamUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

public class BitarchiveRecordTester extends TestCase {
    private UseTestRemoteFile utrf = new UseTestRemoteFile();
    ReloadSettings rs = new ReloadSettings();
    private File testFile = new File(TestInfo.ORIGINALS_DIR,
        "3-3-20070119143010-00000-sb-test-har-001.statsbiblioteket.dk.arc");
    /**
     * The following warcfile consists of multiple record-types.
     * These unittests only handle the response-record which is similar to what we now extract
     * from our arc-files.
     * We use the following record with a normal HTTP response with mimetype text/html
     * (Record type, offset, ContentBegin, Length): response, 28917, 393, 1121
     * 
     * (Record type, offset, ContentBegin, Length): response, 955, 345, 621
     * 
     */
    private File warcTestFile = new File(TestInfo.ORIGINALS_DIR,
        "NAS-20100909163324-00000-mette.kb.dk.warc");
    /* private long warcOffset = 955;
    private int warcContentBegin = 345;
    private long warcRecordLength = 621;
    */
    /*
    (Record type, url, offset, ContentBegin, Length): response, http://netarkivet.dk/netarkivet_alm/billeder/netarkivet_guidelines_20.gif, 81527, 387, 887
        (Record type, url, offset, ContentBegin, Length): response, http://netarkivet.dk/netarkivet_alm/billeder/spacer.gif, 83458, 369, 660
        (Record type, url, offset, ContentBegin, Length): response, http://netarkivet.dk/organisation/index-da.php, 85108, 361, 9291
    */
    
    private long smallWarcRecordOffset =  85108;
 
    /** (Record type, url, offset, ContentBegin, Length): 
     *   response, http://netarkivet.dk/nyheder/index-da.php, 
     *   100262, 
     *   357, 
     *   14248 
    **/
    private long bigWarcRecordOffset =  100262;
    
    
    
    protected void setUp() throws Exception {
        rs.setUp();
        utrf.setUp();
        Settings.set(CommonSettings.BITARCHIVE_LIMIT_FOR_RECORD_DATATRANSFER_IN_FILE,
                     "10000");
        TestInfo.WORKING_DIR.mkdir();
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, TestInfo.WORKING_DIR.getAbsolutePath());
        super.setUp();
    }
    protected void tearDown() throws Exception {
        super.tearDown();
        utrf.tearDown();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        rs.tearDown();
    }

    /** Test storing ArcRecord in byte array.
     * @throws IOException */
    public void testGetDataSmallRecord() throws IOException {
        File f = testFile;
        ARCReader ar = ARCReaderFactory.get(f);
        ARCRecord record = (ARCRecord) ar.get(2001); // record representing record of size 9471 bytes
        BitarchiveRecord br = new BitarchiveRecord(record, f.getName());
        byte[] contents = StreamUtils.inputStreamToBytes(
                br.getData(), (int) br.getLength());
        assertEquals("Should have same length", contents.length, br.getLength());
        // getData(outputStream)
        record = (ARCRecord) ar.get(2001); // record representing record of size 9471 bytes
        br = new BitarchiveRecord(record, f.getName());
        // Store locally as tmp file
        f = new File(TestInfo.WORKING_DIR, "BitarchiveRecordGetData");
        OutputStream os = new FileOutputStream(f);
        br.getData(os);
        assertEquals("Output file should have same length as record length", f.length(), br.getLength());
        f.delete();        
    }

    /** Test storing WArcRecord in byte array.
     * Tests on WarcRecord less than 10000 bytes.
     * @throws IOException */
    public void testGetDataSmallRecordWithWarc() throws IOException {
        File f = warcTestFile;
        WARCReader ar = WARCReaderFactory.get(f);
        
        WARCRecord record = (WARCRecord) ar.get(smallWarcRecordOffset);
        BitarchiveRecord br = new BitarchiveRecord(record, f.getName());
        
        byte[] contents = StreamUtils.inputStreamToBytes(
                br.getData(), (int) br.getLength());
        assertEquals("Should have same length", contents.length, br.getLength());
        // getData(outputStream)
         
        record = (WARCRecord) ar.get(smallWarcRecordOffset); 
        br = new BitarchiveRecord(record, f.getName());
        // Store locally as tmp file
        f = new File(TestInfo.WORKING_DIR, "BitarchiveRecordGetData");
        OutputStream os = new FileOutputStream(f);
        br.getData(os);
        //assertFalse("Failed: " + FileUtils.readFile(f), true);
        assertEquals("Output file should have same length as record length", f.length(), br.getLength());
        
        f.delete();        
    }    
    
    /**
     * Test storing ArcRecord in RemoteFile.
     * @throws IOException
     */
    public void testGetDataLargeRecord() throws IOException {
        File f = testFile;
        ARCReader ar = ARCReaderFactory.get(f);
        ARCRecord record = (ARCRecord) ar.get(11563); // record representing record of size 395390 bytes
        BitarchiveRecord br = new BitarchiveRecord(record, f.getName());
        byte[] contents = StreamUtils.inputStreamToBytes(
                br.getData(), (int) br.getLength());
        assertEquals("Should have same length: ",
                contents.length, br.getLength());

        // getData(outputStream)
        record = (ARCRecord) ar.get(11563); // record representing record of size 395390 bytes
        br = new BitarchiveRecord(record, f.getName());
        // Store locally as tmp file
        f = new File(TestInfo.WORKING_DIR, "BitarchiveRecordGetData");
        OutputStream os = new FileOutputStream(f);
        br.getData(os);
        assertEquals("Output file should have same length as record length",
                f.length(), br.getLength());
        f.delete();
    }

    /**
     * Test storing WarcRecord in RemoteFile.
     * Tests on WarcRecord greater than 10000 bytes.
     * @throws IOException
     */
    public void testGetDataLargeRecordWithWarc() throws IOException {
        File f = warcTestFile;
        WARCReader ar = WARCReaderFactory.get(f);
        WARCRecord record = (WARCRecord) ar.get(bigWarcRecordOffset); 
        BitarchiveRecord br = new BitarchiveRecord(record, f.getName());
        byte[] contents = StreamUtils.inputStreamToBytes(
                br.getData(), (int) br.getLength());
        assertEquals("Should have same length: ",
                contents.length, br.getLength());

        // getData(outputStream)
        record = (WARCRecord) ar.get(bigWarcRecordOffset); 
        br = new BitarchiveRecord(record, f.getName());
        // Store locally as tmp file
        f = new File(TestInfo.WORKING_DIR, "BitarchiveRecordGetData");
        OutputStream os = new FileOutputStream(f);
        br.getData(os);
        assertEquals("Output file should have same length as record length",
                f.length(), br.getLength());
        f.delete();
    }
    
    
    /**
     * Test serializability of this class.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void testSerializability() throws IOException, ClassNotFoundException {
        File f = testFile;
        ARCReader ar = ARCReaderFactory.get(f);
        ARCRecord record = (ARCRecord) ar.get(11563); // record representing record of size 395390 bytes
        BitarchiveRecord br = new BitarchiveRecord(record, f.getName());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream ous = new ObjectOutputStream(baos);
        ous.writeObject(br);
        ous.close();
        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(baos.toByteArray()));
        BitarchiveRecord brCopy = (BitarchiveRecord) ois.readObject();

        f = new File(TestInfo.WORKING_DIR, "BitarchiveRecordGetData");
        OutputStream os = new FileOutputStream(f);
        brCopy.getData(os);
        assertEquals("Output file should have same length as record length",
                f.length(), br.getLength());

        // finally, compare their states
        assertEquals("After serialization, their state is different",
                relevantState(br), relevantState(brCopy));
    }

    private String relevantState(BitarchiveRecord br) {
        return br.getFile();
    }


}
