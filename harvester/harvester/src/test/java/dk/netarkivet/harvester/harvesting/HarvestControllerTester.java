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
package dk.netarkivet.harvester.harvesting;
/**
 * Tests for the HarvestController class (which was extracted from
 * HarvestControllerServer).
 */

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;
import org.archive.io.ArchiveRecord;
import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;
import org.dom4j.Document;

import dk.netarkivet.archive.arcrepository.distribute.JMSArcRepositoryClient;
import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.JMSConnectionTestMQ;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.harvester.datamodel.HeritrixTemplate;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.StopReason;
import dk.netarkivet.harvester.harvesting.distribute.DomainHarvestReport;
import dk.netarkivet.harvester.harvesting.distribute.MetadataEntry;
import dk.netarkivet.testutils.ARCTestUtils;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.MockupIndexServer;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;


public class HarvestControllerTester extends TestCase {
    HarvestController hc;

    public HarvestControllerTester(String s) {
        super(s);
    }

    MockupIndexServer mis = new MockupIndexServer(new File(TestInfo.ORIGINALS_DIR,
            "dedupcache"));
    UseTestRemoteFile rf = new UseTestRemoteFile();

    public void setUp()
            throws Exception, IllegalAccessException, IOException {
        super.setUp();
        JMSConnectionTestMQ.useJMSConnectionTestMQ();
        JMSConnectionTestMQ.clearTestQueues();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.CRAWLDIR_ORIGINALS_DIR,
                                          TestInfo.WORKING_DIR);
        rf.setUp();
        Settings.set(Settings.ARCREPOSITORY_STORE_RETRIES, "1");
        Settings.set(Settings.CACHE_DIR, new File(TestInfo.WORKING_DIR, "cacheDir").getAbsolutePath());
        Settings.set(Settings.DIR_COMMONTEMPDIR,
                new File(TestInfo.WORKING_DIR, "commontempdir").getAbsolutePath());
        mis.setUp();
   }

    public void tearDown() throws Exception {
        super.tearDown();
        mis.tearDown();
        JMSConnectionTestMQ.clearTestQueues();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        rf.tearDown();
        Settings.reload();
        if (hc != null) {
            hc.cleanup();
            hc = null;
        }
    }

    /** Test that if the arcrepository client cannot start we
     * get an exception.
     */
    public void testFailingArcRepositoryClient() {
        Settings.set(Settings.ARCREPOSITORY_STORE_RETRIES, "Not a number");
        try {
            HarvestController.getInstance();
            fail("Arc repository client should have thrown an exception");
        } catch (ArgumentNotValid e) {
            //expected
        }
    }



    /** Tests the writeHarvestFiles method.
     *
     * @throws Exception
     */
    public void testWriteHarvestFiles() throws Exception {

        // Check that harvest info file, seed.txt and order.xml are written,
        // and that the returned HeritrixFiles points to the given places.

        Job j = TestInfo.getJob();
        j.setJobID(1L);
        // Check whether job 1 is valid
        assertTrue("j.getSeedList should be non-empty",
                j.getSeedListAsString() != "");
        assertTrue("j.getOrderXMLdoc() must have a content",
                j.getOrderXMLdoc().hasContent());

        File crawlDir = new File(TestInfo.WORKING_DIR, "testcrawldir");
        FileUtils.createDir(crawlDir);
        List<MetadataEntry> metadata = Arrays.asList(new MetadataEntry[] {
                TestInfo.sampleEntry
        });

        File harvestInfo = new File(crawlDir, "harvestInfo.xml");
        File seedsTxt = new File(crawlDir, "seeds.txt");
        File orderXml = new File(crawlDir, "order.xml");
        File metadataFile = new File(crawlDir, j.getJobID() + "-preharvest-metadata-1.arc");

        assertFalse("metadata file should not exist",
                metadataFile.exists());
        assertFalse("Harvest info file should not exist",
                    harvestInfo.exists());
        assertFalse("seeds.txt file should not exist",
                    seedsTxt.exists());
        assertFalse("order.xml file should not exist",
                    orderXml.exists());
        HarvestController controller = HarvestController.getInstance();
        HeritrixFiles files = controller.writeHarvestFiles(
                crawlDir, j, metadata);

        assertTrue("Should have harvest info file after call",
                   harvestInfo.exists());
        assertTrue("Should have seed.txt file after call",
                   seedsTxt.exists());
        assertTrue("Should have order.xml file after call",
                   orderXml.exists());
        assertTrue("Should have preharvest metadata file after call",
                   metadataFile.exists());

        FileAsserts.assertFileContains("Should have jobID in harvestinfo file",
                                       "<jobId>" + j.getJobID() + "</jobId>", harvestInfo);
        FileAsserts.assertFileContains("Should have harvestID in harvestinfo file",
                                       "<origHarvestDefinitionID>" + j.getOrigHarvestDefinitionID() + "</origHarvestDefinitionID>",
                                       harvestInfo);
        FileAsserts.assertFileContains("Should have correct order.xml file",
                                       "OneLevel-order", orderXml);
        
        // Verify that order.xml is valid HeritrixTemplate
        Document order = XmlUtils.getXmlDoc(orderXml);
        HeritrixTemplate template = new HeritrixTemplate(order, true);
        
        FileAsserts.assertFileContains("Should have correct seeds.txt file",
                                       j.getSeedListAsString(), seedsTxt);
        FileAsserts.assertFileContains("Should have URL in file",
                                       "metadata://netarkivet.dk", metadataFile);
        FileAsserts.assertFileContains("Should have mimetype in file",
                                       "text/plain", metadataFile);
        FileAsserts.assertFileContains("Should have metadata in file",
                                       "DETTE ER NOGET METADATA", metadataFile);

        assertEquals("HarvestFiles should have correct crawlDir",
                     crawlDir, files.getCrawlDir());
        assertEquals("HarvestFiles should have correct order.xml file",
                     orderXml, files.getOrderXmlFile());
        assertEquals("HarvestFiles should have correct seeds.txt file",
                     seedsTxt, files.getSeedsTxtFile());

        assertTrue("Index directory should exist now",
                   files.getIndexDir().isDirectory());
        //There are three files in the zip file replied
        assertEquals("Index directory should contain unzipped files",
                     3, files.getIndexDir().listFiles().length);
    }

    /**
     * Test that writePreharvestMetadata() does what it's supposed to do.
     * @throws Exception
     */
    public void testWritePreharvestMetadata() throws Exception {
        Settings.set(Settings.HARVEST_CONTROLLER_SERVERDIR,
                     TestInfo.WORKING_DIR.getAbsolutePath());
        TestInfo.oneMetadata.add(TestInfo.sampleEntry);
        Job someJob = TestInfo.getJob();
        someJob.setJobID(1L);

        /** Test that empty metadata list does not produce any preharvest metadata file. */
        File arcFile = new File(TestInfo.CRAWLDIR_ORIGINALS_DIR, someJob.getJobID() + "-preharvest-metadata-1.arc");
        if (arcFile.exists()) {
            FileUtils.remove(arcFile);
        }
        assertFalse("preharvest-metadata file should not exist before calling this method", arcFile.exists());
        final HarvestController hc = HarvestController.getInstance();
        Method writePreharvestMetadata = ReflectUtils.getPrivateMethod(
                hc.getClass(), "writePreharvestMetadata",
                Job.class, List.class, File.class);

        writePreharvestMetadata.invoke(hc, someJob, TestInfo.emptyMetadata, TestInfo.CRAWLDIR_ORIGINALS_DIR);

        assertFalse("preharvest-metadata file should not be created with empty metadata list", arcFile.exists());

        /** Test that non-empty metadata list does produce a preharvest metadata file. */
        writePreharvestMetadata.invoke(hc, someJob, TestInfo.oneMetadata, TestInfo.CRAWLDIR_ORIGINALS_DIR);
        assertTrue("preharvest-metadata file should be created with non-empty metadata list", arcFile.exists());

        /** Test the contents of this preharvest-metadata file. */
        assertTrue("The preharvest-metadata-1.arc is not valid ARC", isArcValid(arcFile));
        ARCReader r = ARCReaderFactory.get(arcFile);
        Iterator<ArchiveRecord> iterator = r.iterator();
        iterator.next(); //Skip ARC file header
        // Read the record, checking mime-type, uri and content.
        ARCRecord record = (ARCRecord) iterator.next();
        ARCRecordMetaData meta = record.getMetaData();
        assertEquals("Should record the object under the given URI",
                     TestInfo.sampleEntry.getURL(), meta.getUrl());
        assertEquals("Should indicate the intended MIME type",
                     TestInfo.sampleEntry.getMimeType(), meta.getMimetype());
        String foundContent = ARCTestUtils.readARCRecord(record);
        assertEquals("Should store content unchanged",
                     new String(TestInfo.sampleEntry.getData()), foundContent);
        //Cleanup
        if (arcFile.exists()) {
            FileUtils.remove(arcFile);
        }
    }

    private boolean isArcValid(File thisArc) throws IOException {
        ARCReader ar = ARCReaderFactory.get(thisArc);
        return ar.isValid();
    }

    public void testRunHarvest() throws Exception {
        HeritrixFiles files = new HeritrixFiles(new File(TestInfo.WORKING_DIR, "bogus"), 42L, 23L);
        hc = HarvestController.getInstance();
        try {
            hc.runHarvest(files);
            fail("Should have died with bogus file structure");
        } catch (IOFailure e) {
            System.out.println("error: " + e.getMessage());
            StringAsserts.assertStringContains("Should have the right error message",
                                               "Unable to create index directory:", e.getMessage());
        } catch (ArgumentNotValid e) {
            StringAsserts.assertStringMatches("Should have the right error message",
                                              "File 'order.xml' must exist.*bogus/order.xml", e.getMessage());

        }
    }

    public void testGenerateHeritrixDomainHarvestReport() throws Exception {
        // Test that an existing crawl.log is used, or null is returned
        // if no hosts report is found.

        Method generateHeritrixDomainHarvestReport = ReflectUtils.getPrivateMethod(
                HarvestController.class, "generateHeritrixDomainHarvestReport",
                HeritrixFiles.class, StringBuilder.class);

        hc = HarvestController.getInstance();
        HeritrixFiles files = new HeritrixFiles(TestInfo.CRAWLDIR_ORIGINALS_DIR,
                                                1L, 1L);
        StringBuilder errs = new StringBuilder();
        DomainHarvestReport dhr = (DomainHarvestReport)
                generateHeritrixDomainHarvestReport.invoke(hc, files, errs);
        assertEquals("Error accumulator should be empty", 0, errs.length());

        assertEquals("Returned report should have right contents",
                     1162154L,
                     dhr.getByteCount("netarkivet.dk").longValue());

        File crawlDir2 = new File(TestInfo.CRAWLDIR_ORIGINALS_DIR, "bogus");
        HeritrixFiles files2 = new HeritrixFiles(crawlDir2, 1L, 1L);
        dhr = (DomainHarvestReport)generateHeritrixDomainHarvestReport.invoke(hc, files2, errs);
        assertNull("Generated domainHarvestReport should be null",
                   dhr);
        assertEquals("Should have expected error message in errs",
                     "No crawl.log found in '" + crawlDir2.getAbsolutePath() + "/logs/crawl.log'\n",
                     errs.toString());
    }

    public void testUploadFiles() throws Exception {
        hc = HarvestController.getInstance();
        Field arcrepField = ReflectUtils.getPrivateField(hc.getClass(), "arcRepController");
        final List<File> stored = new ArrayList<File>();
        arcrepField.set(hc, new JMSArcRepositoryClient() {
            public void store(File f) {
                if (f.exists()) {
                    stored.add(f);
                } else {
                    throw new ArgumentNotValid("Missing file " + f);
                }
            }
        });

        // Tests that all files in the list are uploaded, and that error
        // messages are correctly handled.
        Method uploadFiles = ReflectUtils.getPrivateMethod(hc.getClass(),
                                                           "uploadFiles", List.class, StringBuilder.class, List.class);
        StringBuilder errs = new StringBuilder();
        List<File> failed = new ArrayList<File>();

        uploadFiles.invoke(hc, list(TestInfo.CDX_FILE, TestInfo.ARC_FILE2),
                           errs, failed);

        assertEquals("Should have exactly two files uploaded",
                     2, stored.size());
        assertEquals("Should have CDX file first",
                     TestInfo.CDX_FILE, stored.get(0));
        assertEquals("Should have ARC file next",
                     TestInfo.ARC_FILE2, stored.get(1));
        assertEquals("Should have no error messages", 0, errs.length());
        assertEquals("Should have no failed files", 0, failed.size());

        stored.clear();

        uploadFiles.invoke(hc, list(TestInfo.CDX_FILE, new File(TestInfo.WORKING_DIR, "bogus")),
                           errs, failed);

        assertEquals("Should have exactly one file successfully uploaded",
                     1, stored.size());
        assertEquals("Should have CDX file first",
                     TestInfo.CDX_FILE, stored.get(0));
        StringAsserts.assertStringMatches("Should have no error messages",
                                          "Error uploading .*/bogus' Will be moved.*Missing file",
                                          errs.toString());
        assertEquals("Should have one failed file", 1, failed.size());
        assertEquals("Should have bogus file in failed list",
                     new File(TestInfo.WORKING_DIR, "bogus"), failed.get(0));

        stored.clear();
        errs = new StringBuilder();
        failed.clear();

        uploadFiles.invoke(hc, null, errs, failed);

        assertEquals("Should have no files uploaded", 0, stored.size());
        assertEquals("Should have no error messages", 0, errs.length());
        assertEquals("Should have no failed files", 0, failed.size());

        uploadFiles.invoke(hc, list(), errs, failed);

        assertEquals("Should have no files uploaded", 0, stored.size());
        assertEquals("Should have no error messages", 0, errs.length());
        assertEquals("Should have no failed files", 0, failed.size());
    }

    public static <T> List<T> list(T... objects) {
        return Arrays.asList(objects);
    }

    public void testFindDefaultStopReason() throws Exception {
        try {
            HarvestController.findDefaultStopReason(null);
            fail("Should throw argument not valid on null argument");
        } catch (ArgumentNotValid e) {
            assertTrue("Should contain varable name in exception",
                       e.getMessage().contains("logFile"));
        }
        assertEquals("Download should be completed",
                     StopReason.DOWNLOAD_COMPLETE,
                     HarvestController.findDefaultStopReason(
                             new File(TestInfo.CRAWLDIR_ORIGINALS_DIR,
                                      "logs/progress-statistics.log")));
        assertEquals("Download should be unfinished",
                     StopReason.DOWNLOAD_UNFINISHED,
                     HarvestController.findDefaultStopReason(
                             TestInfo.NON_EXISTING_FILE));
        assertEquals("Download should be unfinished",
                     StopReason.DOWNLOAD_UNFINISHED,
                     HarvestController.findDefaultStopReason(
                             new File(TestInfo.UNFINISHED_CRAWLDIR,
                                      "logs/progress-statistics.log")));
    }
}