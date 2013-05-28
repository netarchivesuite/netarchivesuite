/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
package dk.netarkivet.harvester.harvesting;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.archive.util.anvl.ANVLRecord;

import junit.framework.Assert;
import junit.framework.TestCase;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.harvester.harvesting.metadata.MetadataFileWriter;
import dk.netarkivet.harvester.harvesting.metadata.MetadataFileWriterArc;
import dk.netarkivet.harvester.harvesting.metadata.MetadataFileWriterWarc;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

public class MetadataFileWriterTester extends TestCase {
    ReloadSettings rs = new ReloadSettings();
    File logsDir;
    
    public void setUp() {
        rs.setUp();
        TestInfo.WORKING_DIR.mkdirs();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.CRAWLDIR_ORIGINALS_DIR, TestInfo.WORKING_DIR);
        logsDir = new File(TestInfo.WORKING_DIR, "logs");
    }

    public void tearDown() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        rs.tearDown();
    }
    
    public void testMetadataFileWriterArc() {
        File metafile = new File("metadata.arc");
        MetadataFileWriter mdfw = MetadataFileWriterArc.createWriter(metafile);

        String uri = "http://www.netarkivet.dk/";
        long ctm = System.currentTimeMillis();

        SecureRandom random = new SecureRandom();
        byte[] payload = new byte[8192];
        random.nextBytes(payload);

        try {
            mdfw.write(uri, "application/binary", "127.0.0.1", ctm, payload);
            mdfw.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        }

        metafile.deleteOnExit();

        File metadataArcFile = new File(TestInfo.WORKING_DIR, "42-metadata-1.arc");
        MetadataFileWriter mfwa = MetadataFileWriterArc.createWriter(metadataArcFile);
        for (File f : logsDir.listFiles()) {
            mfwa.writeFileTo(f, "metadata://netarkivet.dk/crawl/logs/" + f.getName(), "text/plain");
        }
    }

    public void testMetadataFileWriterWarc() {
        File metafile = new File("metadata.warc");
        MetadataFileWriter mdfw = MetadataFileWriterWarc.createWriter(metafile);

        String uri = "http://www.netarkivet.dk/";
        long ctm = System.currentTimeMillis();

        SecureRandom random = new SecureRandom();
        byte[] payload = new byte[8192];
        random.nextBytes(payload);

        try {
            mdfw.write(uri, "application/binary", "127.0.0.1", ctm, payload);
            mdfw.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            Assert.fail("Unexpected exception!");
        }

        metafile.deleteOnExit();

        File metadataArcFile = new File(TestInfo.WORKING_DIR, "42-metadata-1.warc");
        MetadataFileWriter mfwa = MetadataFileWriterWarc.createWriter(metadataArcFile);
        ((MetadataFileWriterWarc) mfwa).insertInfoRecord(new ANVLRecord());
        for (File f : logsDir.listFiles()) {
            mfwa.writeFileTo(f, "metadata://netarkivet.dk/crawl/logs/" + f.getName(), "text/plain");
        }
    }
    
    /** 
     * This is not run automatically, as this takes a long time to complete (15 seconds).
     * 
     * @throws IOException
     */
    public void notestMetadataFileWriterWarcMassiveLoadTest() throws IOException {
        //TODO verify content of produced warc-file to ensure that all is OK
        File metafile = new File("metadata.warc");
        MetadataFileWriter mdfw = MetadataFileWriterWarc.createWriter(metafile);
        ((MetadataFileWriterWarc) mdfw).insertInfoRecord(new ANVLRecord());
        // Create 5000 small files
        String contentPart = "blablabla";
        String someText = StringUtils.repeat(contentPart, 5000);
        List textArray = new ArrayList<String>();
        textArray.add(someText);
        Set<File> files = new HashSet<File>();
        for (int i=0; i < 10000; i++) {
            File f = File.createTempFile("metadata", "cdx");
            FileUtils.writeCollectionToFile(f, textArray);
            files.add(f);
        }
        System.out.println("Finished writing files");
        int count = 0;
        for (File f: files) {
            mdfw.writeFileTo(f, "http://netarkivet/ressource-" + count, "text/plain");
            f.delete();
            count++;
        }
        metafile.delete();
        System.out.println("Finished adding files to warc");
        
    }
}
