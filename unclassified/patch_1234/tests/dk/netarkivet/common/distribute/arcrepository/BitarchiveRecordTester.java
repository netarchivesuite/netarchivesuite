/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.TestUtils;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

public class BitarchiveRecordTester extends TestCase {
    private UseTestRemoteFile utrf = new UseTestRemoteFile();
    private File testFile = new File(TestInfo.ORIGINALS_DIR,
        "3-3-20070119143010-00000-sb-test-har-001.statsbiblioteket.dk.arc");

    protected void setUp() throws Exception {
        utrf.setUp();
        Settings.set(
                Settings.BITARCHIVE_LIMIT_FOR_RECORD_DATATRANSFER_IN_FILE,
                "10000");
        TestInfo.WORKING_DIR.mkdir();
        Settings.set(Settings.DIR_COMMONTEMPDIR, TestInfo.WORKING_DIR.getAbsolutePath());
        super.setUp();
    }
    protected void tearDown() throws Exception {
        super.tearDown();
        utrf.tearDown();
        Settings.reload();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
    }

    /** Test storing ArcRecord in byte array.
     * @throws IOException */
    public void testGetDataSmallRecord() throws IOException {
        File f = testFile;
        ARCReader ar = ARCReaderFactory.get(f);
        ARCRecord record = (ARCRecord) ar.get(2001); // record representing record of size 9471 bytes
        BitarchiveRecord br = new BitarchiveRecord(record);
        byte[] contents = TestUtils.inputStreamToBytes(
                br.getData(), (int) br.getLength());
        assertEquals("Should have same length", contents.length, br.getLength());
        // getData(outputStream)
        record = (ARCRecord) ar.get(2001); // record representing record of size 9471 bytes
        br = new BitarchiveRecord(record);
        // Store locally as tmp file
        f = new File(TestInfo.WORKING_DIR, "BitarchiveRecordGetData");
        OutputStream os = new FileOutputStream(f);
        br.getData(os);
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
        BitarchiveRecord br = new BitarchiveRecord(record);
        byte[] contents = TestUtils.inputStreamToBytes(
                br.getData(), (int) br.getLength());
        assertEquals("Should have same length: ",
                contents.length, br.getLength());

        // getData(outputStream)
        record = (ARCRecord) ar.get(11563); // record representing record of size 395390 bytes
        br = new BitarchiveRecord(record);
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
        BitarchiveRecord br = new BitarchiveRecord(record);

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
