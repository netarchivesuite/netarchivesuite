/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.netarkivet.harvester.harvesting;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;
import org.archive.io.ArchiveRecord;
import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.cdx.CDXUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.testutils.ARCTestUtils;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

public class HarvestDocumentationTester extends TestCase {
    ReloadSettings rs = new ReloadSettings();

    public void setUp() {
        rs.setUp();
        FileUtils.createDir(TestInfo.WORKING_DIR);
    }

    public void tearDown() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        rs.tearDown();
    }

    /**
     * Unit test of method HarvestDocumentation.documentHarvest(). For
     * simplicity, this test only covers the normal case; error cases are tested
     * by the method below.
     *
     * Verifies that after calling the method, a new ARC file is created with
     * the appropriate name (see getMetadataARCFileName()). Verfies that this
     * ARC file contains one record of MIME type "application/cdx" per original
     * ARC file in the dir (and no other records of that type). Verifies that
     * the CDX records are named appropriately (see getCDXURI()).
     *
     * @throws IOException
     */
    public void testDocumentHarvestOrdinaryCase() throws IOException {
        /* Run the method on a working-dir that mirrors
         * TestInfo.METADATA_TEST_DIR.
         */
        TestInfo.WORKING_DIR.mkdirs();
        TestFileUtils.copyDirectoryNonCVS(
                TestInfo.METADATA_TEST_DIR,
                TestInfo.WORKING_DIR);
        HarvestDocumentation.documentHarvest(TestInfo.WORKING_DIR,
                                             TestInfo.JOB_ID,
                                             TestInfo.HARVEST_ID
        );
        //Verify that the new file exists.
        HarvestDocumentation.getMetadataARCFileName(
                TestInfo.ARC_JOB_ID);
        IngestableFiles inf = new IngestableFiles(TestInfo.WORKING_DIR,
                                                  Long.parseLong(
                                                          TestInfo.ARC_JOB_ID));
        List<File> fs = inf.getMetadataArcFiles();
        assertEquals("Should have created exactly one file ",
                     1, fs.size());
        File f = fs.get(0);
        assertTrue("The file should exist: " + f.toString(), f.exists());
        //Put an ARCReader on top of the file.
        ARCReader r = ARCReaderFactory.get(f);
        Iterator<ArchiveRecord> it = r.iterator();
        //Read each record, checking content-type and URI.
        Set<String> cdxURISet = new HashSet<String>();
        cdxURISet.add(HarvestDocumentation.getCDXURI(
                TestInfo.ARC_HARVEST_ID,
                TestInfo.ARC_JOB_ID,
                TestInfo.FST_ARC_TIME,
                TestInfo.FST_ARC_SERIAL).toASCIIString());
        cdxURISet.add(HarvestDocumentation.getCDXURI(
                TestInfo.ARC_HARVEST_ID,
                TestInfo.ARC_JOB_ID,
                TestInfo.SND_ARC_TIME,
                TestInfo.SND_ARC_SERIAL).toASCIIString());
        String aliasFound = null;
        while (it.hasNext()) {
            ARCRecord record = (ARCRecord) it.next();
            ARCRecordMetaData meta = record.getMetaData();
            if (meta.getMimetype().equals("application/x-cdx")) {
                assertTrue("Bad URI in metadata: " + meta.getUrl(),
                           cdxURISet.contains(meta.getUrl()));
                cdxURISet.remove(meta.getUrl());
            }
            if (meta.getUrl().startsWith(
                    "metadata://netarkivet.dk/crawl/setup/aliases")) {
                aliasFound = ARCTestUtils.readARCRecord(record);
            }
        }
        assertTrue("Metadata should contain CDX records: " + cdxURISet,
                   cdxURISet.isEmpty());
        assertNotNull("Should have found some alias information", aliasFound);
        assertEquals("Should have found the right alias metadata",
                     "tv2.dk is an alias of statsbiblioteket.dk\n", aliasFound);
    }

    /**
     * Unit test of method HarvestDocumentation.documentHarvest(). This unit
     * test covers the following error cases: - dir does not exist or is not dir
     * -> IOFailure - the ARC files in dir do not share harvestID and jobID ->
     * throw UnknownID
     */
    public void testDocumentHarvestExceptionalCases() {
        try {//Dir does not exist
            HarvestDocumentation.documentHarvest(new File("foodoesnotexist"),
                                                 TestInfo.JOB_ID,
                                                 TestInfo.HARVEST_ID);
            fail("Should have thrown IOFailure");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        try {//Dir is not a dir
            HarvestDocumentation.documentHarvest(TestInfo.ORDER_FILE,
                                                 TestInfo.JOB_ID,
                                                 TestInfo.HARVEST_ID);
            fail("Should have thrown IOFailure");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        //the ARC files in dir do not share harvestID and jobID
        TestInfo.WORKING_DIR.mkdirs();
        File arcsDir = new File(TestInfo.WORKING_DIR,
                                Constants.ARCDIRECTORY_NAME);
        TestFileUtils.copyDirectoryNonCVS(
                TestInfo.METADATA_TEST_DIR_INCONSISTENT,
                arcsDir);
        HarvestDocumentation.documentHarvest(TestInfo.WORKING_DIR,
                                             TestInfo.JOB_ID,
                                             TestInfo.HARVEST_ID);
        List<File> arcFiles = Arrays.asList(
                arcsDir.listFiles(FileUtils.ARCS_FILTER));
        assertEquals("Should have exactly 1 ARC file (no metadata here)",
                     1, arcFiles.size());
        assertEquals("Should only have consistently named arc file left",
                     TestInfo.ARC_FILE_0.getName(), arcFiles.get(0).getName());

        List<File> metadataFiles = new IngestableFiles(TestInfo.WORKING_DIR,
                                                       TestInfo.JOB_ID).getMetadataArcFiles();
        File metadataDir = new File(TestInfo.WORKING_DIR, "metadata");
        File target1 = new File(
                metadataDir, HarvestDocumentation.getMetadataARCFileName(
                        Long.toString(TestInfo.JOB_ID)));
        assertEquals("Should generate exactly one metadata file",
                     1, metadataFiles.size());
        assertTrue("Should generate file " + target1
                   + " but found only " + metadataFiles.toString(),
                   metadataFiles.contains(target1));
    }

    /**
     * Unit test for HarvestDocumentation.getMetadataARCFileName() Verifies that
     * the name of the new ARC file ends on .arc and that the parameter is part
     * of the file name. Also verifies that null parameters are not accepted.
     */
    public void testGetMetadataARCFileName() {
        String job = "7";
        try {
            HarvestDocumentation.getMetadataARCFileName(null);
            fail("Should have thrown ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        String fn = HarvestDocumentation.getMetadataARCFileName(job);
        assertTrue("File name should end on '-1.arc' - was " + fn,
                   fn.endsWith("-1.arc")
        );
        assertTrue("File name should contain jobID - was " + fn,
                   fn.contains(job)
        );
        assertTrue("File name should contain the string 'metadata' - was " + fn,
                   fn.contains("metadata")
        );
    }

    /**
     * Unit test for HarvestDocumentation.getPreharvestMetadataARCFileName()
     * Verifies that the name of the new ARC file ends on .arc and that the
     * parameter is part of the file name.
     */
    public void testGetPreharvestMetadataARCFileName() {
        long jobId = 7;
        String fn = HarvestDocumentation.getPreharvestMetadataARCFileName(
                jobId);
        assertTrue("File name should end on '-1.arc' - was " + fn,
                   fn.endsWith("-1.arc")
        );
        assertTrue("File name should contain jobID - was " + fn,
                   fn.contains(Long.toString(jobId))
        );
        assertTrue("File name should contain the string 'metadata' - was " + fn,
                   fn.contains("metadata")
        );
        assertTrue(
                "File name should contain the string 'preharvest' - was " + fn,
                fn.contains("preharvest")
        );
    }

    /**
     * Unit test for HarvestDocumentation.getCDXURI() Verfies that the URI
     * begins with "metadata://netarkivet.dk/crawl/index/cdx?" and that it
     * contains all four of the method's parameters. Also verifies that null
     * parameters are not accepted.
     */
    public void testGetCDXURI() {
        String harv = "42";
        String job = "7";
        String time = "99999999999999";
        String serial = "000000001";
        try {
            HarvestDocumentation.getCDXURI(null, job, time, serial);
            fail("Should have thrown ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        try {
            HarvestDocumentation.getCDXURI(harv, null, time, serial);
            fail("Should have thrown ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        try {
            HarvestDocumentation.getCDXURI(harv, job, null, serial);
            fail("Should have thrown ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        try {
            HarvestDocumentation.getCDXURI(harv, job, time, null);
            fail("Should have thrown ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        String uri = HarvestDocumentation
                .getCDXURI(harv, job, time, serial).toString();
        String prefix = "metadata://netarkivet.dk/crawl/index/cdx?";
        assertTrue(
                "Should name the CDX URI following the official pattern - was "
                + uri,
                uri.startsWith(prefix));
        assertTrue("CDX URI should contain harvestID - was "
                   + uri,
                   uri.contains(harv));
        assertTrue("CDX URI should contain jobID - was "
                   + uri,
                   uri.contains(job));
        assertTrue("CDX URI should contain timestamp - was "
                   + uri,
                   uri.contains(time));
        assertTrue("CDX URI should contain serial no - was "
                   + uri,
                   uri.contains(serial));
    }

    /**
     * Unit test method for generating a CDX index of an Arc file.
     *
     * @throws IOException
     */
    public void testCreateCDXFile() throws IOException {
        OutputStream cdxstream = new ByteArrayOutputStream();
        cdxstream.write("BEFORE\n".getBytes());
        CDXUtils.writeCDXInfo(TestInfo.ARC_FILE_1, cdxstream);
        assertEquals("Stream should have expected content",
                     "BEFORE\n" + FileUtils.readFile(TestInfo.CDX_FILE),
                     cdxstream.toString());


        //Testing on a non-arc file to see results
        cdxstream = new ByteArrayOutputStream();
        cdxstream.write("Start\n".getBytes());
        CDXUtils.writeCDXInfo(TestInfo.SEEDS_FILE, cdxstream);
        assertEquals("Stream should have no new content",
                     "Start\n",
                     cdxstream.toString());

        //Testing on a gzipped file to see results
        cdxstream = new ByteArrayOutputStream();
        cdxstream.write("Begin\n".getBytes());
        CDXUtils.writeCDXInfo(TestInfo.ARC_FILE2, cdxstream);
        assertEquals("Stream should have expected new content",
                     "Begin\n" + FileUtils.readFile(TestInfo.CDX_FILE2),
                     cdxstream.toString());
    }

    /**
     * Unit test for generating CDX indexes on all arc files.
     *
     * @throws IOException
     */
    public void testGenerateCDX() throws IOException {
        try {
            CDXUtils.generateCDX(null, TestInfo.CDX_WORKING_DIR);
            fail("Should throw exception on null argument");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        try {
            CDXUtils.generateCDX(TestInfo.ARC_REAL_DIR, null);
            fail("Should throw exception on null argument");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        try {
            CDXUtils.generateCDX(TestInfo.ARC_FILE_1, TestInfo.CDX_WORKING_DIR);
            fail("Should throw exception on non-directory");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        try {
            CDXUtils.generateCDX(TestInfo.ARC_REAL_DIR, TestInfo.ARC_FILE_1);
            fail("Should throw exception on non-directory");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        try {
            CDXUtils.generateCDX(new File("foo"), TestInfo.CDX_WORKING_DIR);
            fail("Should throw exception on non-existing");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        try {
            CDXUtils.generateCDX(TestInfo.ARC_REAL_DIR, new File("foo"));
            fail("Should throw exception on non-existing");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        try {
            CDXUtils.generateCDX(TestInfo.ARC_REAL_DIR, new File("/"));
            fail("Should throw exception on non-writable");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        TestInfo.CDX_WORKING_DIR.mkdirs();
        CDXUtils.generateCDX(TestInfo.ARC_REAL_DIR, TestInfo.CDX_WORKING_DIR);
        File[] originalFiles
                = TestInfo.ARC_REAL_DIR.listFiles(FileUtils.ARCS_FILTER);
        File[] generatedFiles = TestInfo.CDX_WORKING_DIR.listFiles();
        assertEquals("Should have generated the right number of files, but "
                     + "found " + Arrays.asList(generatedFiles),
                     originalFiles.length, generatedFiles.length);
        for (File original : originalFiles) {
            File cdxfile = new File(TestInfo.CDX_WORKING_DIR,
                                    original.getName().replaceFirst(
                                            "\\.arc(\\.gz)?$",
                                            ".cdx"));
            assertTrue("Should be a cdx file with correct name for '"
                       + original + "'", cdxfile.isFile());
            OutputStream content = new ByteArrayOutputStream();
            CDXUtils.writeCDXInfo(original, content);
            assertEquals("File '" + cdxfile.getAbsolutePath()
                         + "' should contain expected content",
                         content.toString(), FileUtils.readFile(cdxfile));
        }

    }

    public void testMoveAwayForeignFiles() throws Exception {
        Method m = ReflectUtils.getPrivateMethod(HarvestDocumentation.class,
                                                 "moveAwayForeignFiles",
                                                 File.class, Long.TYPE);
        // Set oldjobs place to a different name to check use of setting.
        Settings.set(HarvesterSettings.HARVEST_CONTROLLER_OLDJOBSDIR,
                     new File(TestInfo.WORKING_DIR,
                              "oddjobs").getAbsolutePath());
        TestInfo.WORKING_DIR.mkdirs();
        File arcsDir = new File(TestInfo.WORKING_DIR,
                                Constants.ARCDIRECTORY_NAME);
        TestFileUtils.copyDirectoryNonCVS(
                TestInfo.METADATA_TEST_DIR_INCONSISTENT,
                arcsDir);
        FileUtils.copyFile(new File(TestInfo.METADATA_TEST_DIR,
                                    "arcs/not-an-arc-file.txt"),
                           new File(arcsDir, "43-metadata-1.arc"));
        m.invoke(null, arcsDir, 42);
        // Check that one file got moved.
        LogUtils.flushLogs(HarvestDocumentation.class.getName());
        FileAsserts.assertFileContains("Should have found foreign files",
                                       "Found files not belonging",
                                       TestInfo.LOG_FILE);
        String badFile
                = "43-117-20051212141241-00000-sb-test-har-001.statsbiblioteket.dk.arc";
        String goodFile
                = "42-117-20051212141240-00000-sb-test-har-001.statsbiblioteket.dk.arc";
        File oldJobsDir43 = new File(TestInfo.WORKING_DIR, "oddjobs/"
                                                           + 43
                                                           + "-lost-files/arcs/");
        File movedFile = new File(oldJobsDir43, badFile);
        assertTrue("Moved file " + movedFile + " should exist",
                   movedFile.exists());
        File goneFile = new File(TestInfo.WORKING_DIR,
                                 Constants.ARCDIRECTORY_NAME +
                                 "/" + badFile);
        assertFalse(
                "Moved file " + goneFile + " should have gone from crawldir",
                goneFile.exists());
        File keptFile = new File(TestInfo.WORKING_DIR,
                                 Constants.ARCDIRECTORY_NAME +
                                 "/" + goodFile);
        assertTrue("Good file " + keptFile + " should not have moved",
                   keptFile.exists());
        File badMetadataFile = new File(TestInfo.WORKING_DIR,
                                        Constants.ARCDIRECTORY_NAME +
                                        "/43-metadata-1.arc");
        assertFalse("Metadata file " + badMetadataFile + " should have moved",
                    badMetadataFile.exists());
        File movedMetadataFile = new File(oldJobsDir43,
                                          badMetadataFile.getName());
        assertTrue(
                "Metadata file " + movedMetadataFile + " should be in oldjobs",
                movedMetadataFile.exists());
    }

    /**
     * This tests, that bug 722 is solved. It should document, that we 1)
     * generate a metadata-arc-file for the harvestjob, if one does not exist 2)
     * Dont generate metadata-arc file, it already exists but issue a warning
     * instead. 3) [not testable in this class] only upload the
     * metadata-arc-file after all other arc-files have been uploaded.
     *
     * @throws Exception
     */
    public void testDocumentHarvestBug722() throws Exception {
        TestInfo.WORKING_DIR.mkdirs();
        File arcsDir = new File(TestInfo.WORKING_DIR,
                                Constants.ARCDIRECTORY_NAME);
        TestFileUtils.copyDirectoryNonCVS(
                TestInfo.METADATA_TEST_DIR,
                TestInfo.WORKING_DIR);
        IngestableFiles ingestableFiles = new IngestableFiles(
                TestInfo.WORKING_DIR,
                TestInfo.JOB_ID);

        // Test 1:we generate a metadata-file, if it does not exist
        HarvestDocumentation.documentHarvest(TestInfo.WORKING_DIR,
                                             TestInfo.JOB_ID,
                                             TestInfo.HARVEST_ID);
        assertTrue("MetadataFile should exist now",
                   ingestableFiles.isMetadataReady());
        String fileContent = FileUtils.readFile(
                ingestableFiles.getMetadataArcFiles().get(0));

        // test 2:Dont generate metadata-arc file, it already exists
        // but issue a warning instead.

        HarvestDocumentation.documentHarvest(TestInfo.WORKING_DIR,
                                             TestInfo.JOB_ID,
                                             TestInfo.HARVEST_ID);
        FileUtils.remove(new File(arcsDir,
                                  "42-117-20051212141241-00001-sb-test-har-001.statsbiblioteket.dk.arc"));
        LogUtils.flushLogs(HarvestDocumentation.class.getName());
        FileAsserts.assertFileContains(
                "Should have issued warning about existing metadata-arcfile",
                "The metadata-arc already exists, so we don't make another one!",
                TestInfo.LOG_FILE
        );

        String newFileContent = FileUtils.readFile(
                ingestableFiles.getMetadataArcFiles().get(0));
        assertEquals("File contents should be unchanged", fileContent,
                     newFileContent);
    }

    /**
     * Test that all necessary harvest logs, reports and configurations are
     * stored in the proper way.
     *
     * @throws IOException
     */
    public void testWriteHarvestDetails() throws IOException {

        ReloadSettings rs = new ReloadSettings(
                new File(TestInfo.ORIGINALS_DIR, "metadata_settings.xml"));
        rs.setUp();

        TestInfo.WORKING_DIR.mkdirs();
        TestFileUtils.copyDirectoryNonCVS(
                TestInfo.CRAWLDIR_ORIGINALS_DIR,
                TestInfo.WORKING_DIR);

        HarvestDocumentation.documentHarvest(TestInfo.WORKING_DIR,
                                             TestInfo.JOB_ID,
                                             TestInfo.HARVEST_ID);

        // Ensure that now exists arc-file containing <jobid>-metadata-1.arc
        IngestableFiles iF = new IngestableFiles(TestInfo.WORKING_DIR,
                                                 TestInfo.JOB_ID);
        assertFalse("MetadataARC should have generated by now",
                    iF.getMetadataArcFiles().isEmpty());

        assertEquals("Only one metadata-1.arc expected",
                     1,
                     iF.getMetadataArcFiles().size());
        // Go through all the records, and check if all relevant data is being stored
        // Replace TestInfo.WORKING_DIR by TestInfo.CRAWLDIR_ORIGINALS_DIR, because the testing will not work after
        // we now remove the files.
        File ORIGINAL_CRAWLDIR = TestInfo.CRAWLDIR_ORIGINALS_DIR;
        File metadataArcFile = iF.getMetadataArcFiles().get(0);
        String URL =
                "metadata://netarkivet.dk/crawl/setup/order.xml?heritrixVersion="
                + Constants.getHeritrixVersionString() + "&harvestid="
                + TestInfo.HARVEST_ID
                + "&jobid=" + TestInfo.JOB_ID;
        findAndVerifyMetadata(metadataArcFile, URL);
        checkThatNoLongerExist(new File(TestInfo.WORKING_DIR, "order.xml"));

        URL = "metadata://netarkivet.dk/crawl/setup/seeds.txt?heritrixVersion="
              + Constants.getHeritrixVersionString() + "&harvestid="
              + TestInfo.HARVEST_ID
              + "&jobid=" + TestInfo.JOB_ID;
        findAndVerifyMetadata(metadataArcFile, URL);
        checkThatNoLongerExist(new File(TestInfo.WORKING_DIR, "seeds.txt"));

        URL
                =
                "metadata://netarkivet.dk/crawl/reports/hosts-report.txt?heritrixVersion="
                + Constants.getHeritrixVersionString() + "&harvestid="
                + TestInfo.HARVEST_ID
                + "&jobid=" + TestInfo.JOB_ID;
        findAndVerifyMetadata(metadataArcFile, URL);
        checkThatNoLongerExist(
                new File(TestInfo.WORKING_DIR, "hosts-report.txt"));

        // This section necessitated by bug 778
        URL
                =
                "metadata://netarkivet.dk/crawl/setup/harvestInfo.xml?heritrixVersion="
                + Constants.getHeritrixVersionString() + "&harvestid="
                + TestInfo.HARVEST_ID
                + "&jobid=" + TestInfo.JOB_ID;
        findAndVerifyMetadata(metadataArcFile, URL);
        // We need to force the filename to be non-interned, to check that
        // .equals() is called.
        checkThatStillExist(new File(TestInfo.WORKING_DIR,
                                     new String(
                                             "harvestInfo.xml".toCharArray())));


        URL
                =
                "metadata://netarkivet.dk/crawl/reports/seeds-report.txt?heritrixVersion="
                + Constants.getHeritrixVersionString() + "&harvestid="
                + TestInfo.HARVEST_ID
                + "&jobid=" + TestInfo.JOB_ID;
        findAndVerifyMetadata(metadataArcFile, URL);
        checkThatNoLongerExist(
                new File(TestInfo.WORKING_DIR, "seeds-report.txt"));

        URL = "metadata://netarkivet.dk/crawl/logs/crawl.log?heritrixVersion="
              + Constants.getHeritrixVersionString() + "&harvestid="
              + TestInfo.HARVEST_ID
              + "&jobid=" + TestInfo.JOB_ID;
        File logDir = new File(ORIGINAL_CRAWLDIR, "logs");
        findAndVerifyMetadata(metadataArcFile, URL);
        logDir = new File(TestInfo.WORKING_DIR, "logs");
        // We need to force the filename to be non-interned, to check that
        // .equals() is called.
        checkThatStillExist(new File(logDir,
                                     new String("crawl.log".toCharArray())));

        // Test that domain-specific settings (a.k.a overrides) are
        // stored in the proper way.
        URL = "metadata://netarkivet.dk/crawl/setup/settings.xml?"
              + "heritrixVersion="
              + Constants.getHeritrixVersionString() + "&harvestid="
              + TestInfo.HARVEST_ID
              + "&jobid=" + TestInfo.JOB_ID
              + "&domain=kb.dk";
        findAndVerifyMetadata(metadataArcFile, URL);

    }

    public void testMetadataFilters() {

        ReloadSettings rs = new ReloadSettings(
                new File(TestInfo.ORIGINALS_DIR, "metadata_settings.xml"));
        rs.setUp();

        String heritrixFilePattern =
                Settings.get(HarvesterSettings.METADATA_HERITRIX_FILE_PATTERN);

        String[] heritrixFiles = {
                "order.xml",
                "harvestInfo.xml",
                "seeds.txt",
                "crawl-report.txt",
                "frontier-report.txt",
                "hosts-report.txt",
                "mimetype-report.txt",
                "processors-report.txt",
                "responsecode-report.txt," +
                "seeds-report.txt",
                "crawl.log",
                "local-errors.log",
                "progress-statistics.log",
                "runtime-errors.log",
                "uri-errors.log",
                "heritrix.out"
        };

        for (String f : heritrixFiles) {
            assertTrue(
                    f + " does not match " + heritrixFilePattern,
                    f.matches(heritrixFilePattern));
        }

        String reportFilePattern =
                Settings.get(HarvesterSettings.METADATA_REPORT_FILE_PATTERN);

        String[] reportFiles = {
                "crawl-report.txt",
                "frontier-report.txt",
                "hosts-report.txt",
                "mimetype-report.txt",
                "processors-report.txt",
                "responsecode-report.txt," +
                "seeds-report.txt"
        };

        for (String f : reportFiles) {
            assertTrue(
                    f + " does not match " + heritrixFilePattern,
                    f.matches(heritrixFilePattern));
            assertTrue(
                    f + " does not match " + reportFilePattern,
                    f.matches(reportFilePattern));
        }

        String logFilePattern =
                Settings.get(HarvesterSettings.METADATA_LOG_FILE_PATTERN);

        String[] logFiles = {
                "crawl.log",
                "local-errors.log",
                "progress-statistics.log",
                "runtime-errors.log",
                "uri-errors.log",
                "heritrix.out"
        };

        for (String f : logFiles) {
            assertTrue(
                    f + " does not match " + heritrixFilePattern,
                    f.matches(heritrixFilePattern));
            assertTrue(
                    f + " does not match " + logFilePattern,
                    f.matches(logFilePattern));
        }

        rs.tearDown();
    }

    private void checkThatStillExist(File fileThatStillShouldExist) {
        if (!fileThatStillShouldExist.exists()) {
            fail("This file should still exist: "
                 + fileThatStillShouldExist.getAbsolutePath());
        }

    }

    private void checkThatNoLongerExist(File fileThatNoLongerShouldExist) {
        if (fileThatNoLongerShouldExist.exists()) {
            fail("This file should no longer exist: "
                 + fileThatNoLongerShouldExist.getAbsolutePath());
        }
    }


    private void findAndVerifyMetadata(File metadataArcFile, String url)
            throws IOException {
        ARCReader ar = null;
        try {
            ar = ARCReaderFactory.get(metadataArcFile);
            Iterator<ArchiveRecord> it = ar.iterator();
            while (it.hasNext()) {
                ARCRecord record = (ARCRecord) it.next();
                String thisUrl = record.getMetaData().getUrl();
                if (thisUrl.equals(url)) {
                    //System.out.println("Found needed URL: " + thisUrl);
                    //TODO: Compare the contents of the record with the contents
                    // of the argument file.
                    return;
                } else {
                    //System.out.println("Needs URL: " + url);
                    //System.out.println("Found URL: " + thisUrl);
                }

            }
            fail(url + " not found in metadatafile: "
                 + metadataArcFile.getAbsolutePath());
        } catch (IOException e) {
            fail("IOException not expected: " + e.toString());
        } finally {
            if (ar != null) {
                ar.close();
            }
        }
    }
}
