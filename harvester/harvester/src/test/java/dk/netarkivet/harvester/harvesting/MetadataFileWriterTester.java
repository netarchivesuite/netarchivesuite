/* $Id: HarvestDocumentationTester.java 2566 2012-12-05 15:08:14Z svc $
 * $Revision: 2566 $
 * $Date: 2012-12-05 16:08:14 +0100 (Wed, 05 Dec 2012) $
 * $Author: svc $
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

import org.archive.util.anvl.ANVLRecord;

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
    
    public void testMetadataArcwriter() {
        File metadataArcFile = new File(TestInfo.WORKING_DIR, "42-metadata-1.arc");
        MetadataFileWriter mfwa = MetadataFileWriterArc.createWriter(metadataArcFile);
        for (File f : logsDir.listFiles()) {
            mfwa.writeFileTo(f, "metadata://netarkivet.dk/crawl/logs/" + f.getName(), "text/plain");
        }
    }
    
    public void testMetadataWarcwriter() {
        File metadataArcFile = new File(TestInfo.WORKING_DIR, "42-metadata-1.warc");
        MetadataFileWriter mfwa = MetadataFileWriterWarc.createWriter(metadataArcFile);
        ((MetadataFileWriterWarc) mfwa).insertInfoRecord(new ANVLRecord());
        for (File f : logsDir.listFiles()) {
            mfwa.writeFileTo(f, "metadata://netarkivet.dk/crawl/logs/" + f.getName(), "text/plain");
        }
    }
    
    
    
    
}
