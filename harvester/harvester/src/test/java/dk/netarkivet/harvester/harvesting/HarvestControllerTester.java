/*$Id$
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

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.HeritrixTemplate;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobUtils;
import dk.netarkivet.harvester.datamodel.StopReason;
import dk.netarkivet.harvester.harvesting.metadata.MetadataEntry;
import dk.netarkivet.harvester.harvesting.metadata.PersistentJobData.HarvestDefinitionInfo;
import dk.netarkivet.harvester.harvesting.report.AbstractHarvestReport;
import dk.netarkivet.harvester.harvesting.report.HarvestReport;
import dk.netarkivet.harvester.harvesting.report.HarvestReportFactory;
import dk.netarkivet.testutils.*;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for the HarvestController class (which was extracted from
 * HarvestControllerServer).
 */
public class HarvestControllerTester extends TestCase {
    HarvestController hc;

    public HarvestControllerTester(String s) {
        super(s);
    }

    // Out commented to avoid reference to archive module from harvester module.
    //MockupIndexServer mis = new MockupIndexServer(new File(TestInfo.ORIGINALS_DIR, "dedupcache"));
    UseTestRemoteFile rf = new UseTestRemoteFile();
    ReloadSettings rs = new ReloadSettings();

    public void setUp()
            throws Exception, IllegalAccessException, IOException {
        super.setUp();
        rs.setUp();
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        JMSConnectionMockupMQ.clearTestQueues();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.CRAWLDIR_ORIGINALS_DIR,
                                          TestInfo.WORKING_DIR);
        rf.setUp();
        // Out commented to avoid reference to archive module from harvester module.
        // Settings.set(JMSArcRepositoryClient.ARCREPOSITORY_STORE_RETRIES, "1");
        Settings.set(CommonSettings.CACHE_DIR, new File(
                TestInfo.WORKING_DIR, "cacheDir").getAbsolutePath());
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, new File(TestInfo.WORKING_DIR, "commontempdir").getAbsolutePath());

        // Out commented to avoid reference to archive module from harvester module.
        // mis.setUp();
   }

    public void tearDown() throws Exception {
        super.tearDown();
        // Uncommented to avoid reference to archive module from harvester module.
        // mis.tearDown();
        JMSConnectionMockupMQ.clearTestQueues();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        rf.tearDown();
        if (hc != null) {
            hc.cleanup();
            hc = null;
        }
        rs.tearDown();
    }

    /** Test that if the arcrepository client cannot start we
     * get an exception.
     */
// Out commented to avoid reference to archive module from harvester module.
//    public void testFailingArcRepositoryClient() {
//        // If the harvestController is already instantiated,
//        // make sure that isn't any longer.
//        HarvestController hc = HarvestController.getInstance();
//        hc.cleanup();
//        // Out commented to avoid reference to archive module from harvester module.
//        Settings.set(JMSArcRepositoryClient.ARCREPOSITORY_STORE_RETRIES,
//                     "Not a number");
//        try {
//            HarvestController.getInstance();
//            fail("The ArcRepositoryClient should have thrown an exception");
//        } catch (ArgumentNotValid e) {
//            //expected
//        }
//    }

    /** Tests the writeHarvestFiles method.
     * FIXME fails when run as part of the UnitTesterSuite.java. See bug 1912.
     *
     * @throws Exception
     */
    public void failingTestWriteHarvestFiles() throws Exception {

        // Check that harvest info file, seed.txt and order.xml are written,
        // and that the returned HeritrixFiles points to the given places.

        Job j = JobUtils.getHighPriorityJob(TestInfo.ONE_LEVEL_ORDER_FILE);
        j.setJobID(1L);
        // Check whether job 1 is valid
        assertFalse("j.getSeedList should be non-empty",
                j.getSeedListAsString().isEmpty());
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
                crawlDir, j,
                new HarvestDefinitionInfo("test", "test", "test"),
                metadata);

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
                                       "<origHarvestDefinitionID>"
                + j.getOrigHarvestDefinitionID() + "</origHarvestDefinitionID>",
                                       harvestInfo);
        FileAsserts.assertFileContains("Should have correct order.xml file",
                                       "OneLevel-order", orderXml);

        // Verify that order.xml is a valid HeritrixTemplate
        new HeritrixTemplate(XmlUtils.getXmlDoc(orderXml), true);

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

        /** Check, that arcsdir is created in the this method.
         * Part of fixing bug #924. */
        assertTrue("ArcsDir should exist prior to crawl-start",
                files.getArcsDir().isDirectory());
    }

    /**
     * Test that writePreharvestMetadata() does what it's supposed to do.
     * @throws Exception
     */
    public void testWritePreharvestMetadata() throws Exception {
        Settings.set(HarvesterSettings.HARVEST_CONTROLLER_SERVERDIR, TestInfo.WORKING_DIR.getAbsolutePath());
        TestInfo.oneMetadata.add(TestInfo.sampleEntry);
        Job someJob = JobUtils.getSnapshotJobWithSbforgeAsSeed();
        someJob.setJobID(1L);

        /** Test that empty metadata list does not produce any preharvest metadata objects in crawldir/metadata. */
        File metadataDir = new File(TestInfo.WORKING_DIR, IngestableFiles.METADATA_SUB_DIR);
        if (metadataDir.exists()) {
            FileUtils.removeRecursively(metadataDir);
        }
        
        
        assertFalse("metadata dir should not exist before calling this method", metadataDir.isDirectory());
        final HarvestController hc = HarvestController.getInstance();
        Method writePreharvestMetadata = ReflectUtils.getPrivateMethod(
                hc.getClass(), "writePreharvestMetadata",
                Job.class, List.class, File.class);

        writePreharvestMetadata.invoke(hc, someJob, TestInfo.emptyMetadata, TestInfo.WORKING_DIR);

        assertFalse("metadata files should not be created with empty metadata list", metadataDir.isDirectory());

        /** Test that non-empty metadata list produces serialized metadata. */
        writePreharvestMetadata.invoke(hc, someJob, TestInfo.oneMetadata, TestInfo.WORKING_DIR);
        
        List<MetadataEntry> metadata = MetadataEntry.getMetadataFromDisk(metadataDir);
        
        assertTrue("preharvest-metadata files should be created with non-empty metadata list", 
                metadata.size() > 0);
        
        /** Test the contents of this preharvest metadata */
        
        MetadataEntry meta = metadata.get(0);

        assertEquals("Should record the object under the given URI",
                     TestInfo.sampleEntry.getURL(), meta.getURL());
        assertEquals("Should indicate the intended MIME type",
                     TestInfo.sampleEntry.getMimeType(), meta.getMimeType());;
        String foundContent = new String(meta.getData());
        assertEquals("Should store content unchanged",
                     new String(TestInfo.sampleEntry.getData()), foundContent);
    }

    /**
     * test constructor behaviour given bad arguments.
     * The introduction of a factory for the HeritrixLauncher hides the actual
     * cause behind the message "Error creating singleton of class
     * 'dk.netarkivet.harvester.harvesting.controller.BnfHeritrixLauncher'.
     */
    public void testRunHarvest() throws Exception {
        HeritrixFiles files = new HeritrixFiles(
                new File(TestInfo.WORKING_DIR, "bogus"), new JobInfoTestImpl(42L, 23L));
        hc = HarvestController.getInstance();
        String cause =  "Error creating singleton of class '"
            + "dk.netarkivet.harvester.harvesting.controller.BnfHeritrixLauncher':";
        //String cause = "File 'order.xml' must exist.*bogus/order.xml:"
        try {
            hc.runHarvest(files);
            fail("Should have died with bogus file structure");
        } catch (IOFailure e) {
            System.out.println("error: " + e.getMessage());
            StringAsserts.assertStringContains(
                    "Should have the right error message",
                    "Unable to create index directory:", e.getMessage());
        } catch (ArgumentNotValid e) {
            StringAsserts.assertStringMatches(
                    "Should have the right error message",
                     cause, e.getMessage());

        }
    }

    public void testGenerateHeritrixDomainHarvestReport() throws Exception {
        // Test that an existing crawl.log is used, or null is returned
        // if no hosts report is found.

        hc = HarvestController.getInstance();
        HeritrixFiles files = new HeritrixFiles(TestInfo.CRAWLDIR_ORIGINALS_DIR,
                new JobInfoTestImpl(1L, 1L));
        StringBuilder errs = new StringBuilder();
        HarvestReport dhr = HarvestReportFactory.generateHarvestReport(files);
        assertEquals("Error accumulator should be empty", 0, errs.length());

        assertEquals("Returned report should have right contents",
                     1162154L,
                     dhr.getByteCount("netarkivet.dk").longValue());

        File crawlDir2 = new File(TestInfo.CRAWLDIR_ORIGINALS_DIR, "bogus");
        HeritrixFiles files2 = new HeritrixFiles(crawlDir2, new JobInfoTestImpl(1L, 1L));

        dhr = null;
        try {
            dhr = HarvestReportFactory.generateHarvestReport(files2);
            fail("Should have expected error message in errs" +
                    "No crawl.log found in '"
                    + crawlDir2.getAbsolutePath() + "/logs/crawl.log'\n"
                    + errs.toString());
        } catch (ArgumentNotValid anv) {
            assertTrue(anv.getCause()instanceof IOFailure);
            assertEquals(
                    "Not a file or not readable: " + crawlDir2.getAbsolutePath()
                    + "/logs/crawl.log",
                    ((IOFailure) anv.getCause()).getMessage());
        }
        assertNull("Generated harvestReport should be null", dhr);
    }

// Uncommented to avoid reference to archive module from harvester module.
//    public void testUploadFiles() throws Exception {
//        hc = HarvestController.getInstance();
//        Field arcrepField = ReflectUtils.getPrivateField(hc.getClass(), "arcRepController");
//        final List<File> stored = new ArrayList<File>();
//        arcrepField.set(hc, new JMSArcRepositoryClient() {
//            public void store(File f) {
//                if (f.exists()) {
//                    stored.add(f);
//                } else {
//                    throw new ArgumentNotValid("Missing file " + f);
//                }
//            }
//        });
//
//        // Tests that all files in the list are uploaded, and that error
//        // messages are correctly handled.
//        Method uploadFiles = ReflectUtils.getPrivateMethod(hc.getClass(),
//                                                           "uploadFiles", List.class, StringBuilder.class, List.class);
//        StringBuilder errs = new StringBuilder();
//        List<File> failed = new ArrayList<File>();
//
//        uploadFiles.invoke(hc, list(TestInfo.CDX_FILE, TestInfo.ARC_FILE2),
//                           errs, failed);
//
//        assertEquals("Should have exactly two files uploaded",
//                     2, stored.size());
//        assertEquals("Should have CDX file first",
//                     TestInfo.CDX_FILE, stored.get(0));
//        assertEquals("Should have ARC file next",
//                     TestInfo.ARC_FILE2, stored.get(1));
//        assertEquals("Should have no error messages", 0, errs.length());
//        assertEquals("Should have no failed files", 0, failed.size());
//
//        stored.clear();
//
//        uploadFiles.invoke(hc, list(TestInfo.CDX_FILE, new File(TestInfo.WORKING_DIR, "bogus")),
//                           errs, failed);
//
//        assertEquals("Should have exactly one file successfully uploaded",
//                     1, stored.size());
//        assertEquals("Should have CDX file first",
//                     TestInfo.CDX_FILE, stored.get(0));
//        StringAsserts.assertStringMatches("Should have no error messages",
//                                          "Error uploading .*/bogus' Will be moved.*Missing file",
//                                          errs.toString());
//        assertEquals("Should have one failed file", 1, failed.size());
//        assertEquals("Should have bogus file in failed list",
//                     new File(TestInfo.WORKING_DIR, "bogus"), failed.get(0));
//
//        stored.clear();
//        errs = new StringBuilder();
//        failed.clear();
//
//        uploadFiles.invoke(hc, null, errs, failed);
//
//        assertEquals("Should have no files uploaded", 0, stored.size());
//        assertEquals("Should have no error messages", 0, errs.length());
//        assertEquals("Should have no failed files", 0, failed.size());
//
//        uploadFiles.invoke(hc, list(), errs, failed);
//
//        assertEquals("Should have no files uploaded", 0, stored.size());
//        assertEquals("Should have no error messages", 0, errs.length());
//        assertEquals("Should have no failed files", 0, failed.size());
//    }

    public static <T> List<T> list(T... objects) {
        return Arrays.asList(objects);
    }

    public void testFindDefaultStopReason() throws Exception {
        try {
            AbstractHarvestReport.findDefaultStopReason(null);
            fail("Should throw argument not valid on null argument");
        } catch (ArgumentNotValid e) {
            assertTrue("Should contain varable name in exception",
                       e.getMessage().contains("logFile"));
        }
        assertEquals("Download should be completed",
                     StopReason.DOWNLOAD_COMPLETE,
                     AbstractHarvestReport.findDefaultStopReason(
                             new File(TestInfo.CRAWLDIR_ORIGINALS_DIR,
                                      "logs/progress-statistics.log")));
        assertEquals("Download should be unfinished",
                     StopReason.DOWNLOAD_UNFINISHED,
                     AbstractHarvestReport.findDefaultStopReason(
                             TestInfo.NON_EXISTING_FILE));
        assertEquals("Download should be unfinished",
                     StopReason.DOWNLOAD_UNFINISHED,
                     AbstractHarvestReport.findDefaultStopReason(
                             new File(TestInfo.UNFINISHED_CRAWLDIR,
                                      "logs/progress-statistics.log")));
    }
}