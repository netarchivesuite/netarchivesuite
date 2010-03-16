/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.externalsoftware;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import org.archive.io.ArchiveRecord;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCWriter;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCReader;


import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.arc.ARCUtils;

import junit.framework.TestCase;

public class Bug901Tester extends TestCase {

    private static final int BLOCKSIZE = 32768;
    private static final long LARGE = ((long) Integer.MAX_VALUE) + 1L;
    public static final String LARGE_FILE = "largeFile";
    static final File TEST_DIR = new File("tests/dk/netarkivet/externalsoftware/data/launcher");
    static final File WORKING_DIR = new File(TEST_DIR, "working");
    
    public Bug901Tester(String arg0) {
        super(arg0);
    }

    protected void setUp() throws Exception {
        WORKING_DIR.mkdirs();
        super.setUp();
    }

    protected void tearDown() throws Exception {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        super.tearDown();
    }
    
    /**
     * Test, if bug 901 is fixed.
     * Try to insert 3.0 gb file into an ARC file
     * file 4000-metadata-2.arc is taken from kb-prod-udv-001.kb.dk:/home/test
     * @throws IOException
     */
    public void testbug901Fixed() throws IOException {
        byte[] block = new byte[BLOCKSIZE];
        File largeFile = new File(WORKING_DIR, LARGE_FILE);
        OutputStream os = new FileOutputStream(largeFile);
        System.out.println("Creating file - this will take a long time");
        for (long l = 0; l < 4 * LARGE / ((long) BLOCKSIZE) + 1L; l++) {
            os.write(block);
        }
        os.close();
     
        File destArc = new File(WORKING_DIR, "veryBig.arc");
        ARCWriter aw = ARCUtils.createARCWriter(destArc);
        ARCUtils.writeFileToARC(aw, largeFile, "http://dummy", "application/null");
        aw.close();
        System.out.println("Big file has size: " + destArc.length());
        ARCReader ar = ARCReaderFactory.get(destArc);
        Iterator<ArchiveRecord> iterator = ar.iterator();
        int recordCount=0;
        while (iterator.hasNext()) {
            ARCRecord r = (ARCRecord) iterator.next();
            recordCount++;            
        }
        assertTrue("recordCount must be 2 and not: " + recordCount, recordCount==2);
    }

}
