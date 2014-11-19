/*
 * #%L
 * Netarchivesuite - harvester - test
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.harvester.harvesting;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.harvesting.metadata.MetadataFileWriter;
import dk.netarkivet.testutils.TestResourceUtils;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

public class IngestableFilesTester {
    private static final String MSG = "This a test message from IngestableFilesTester";

    /* variables used by all tests */
    private Long testJobId = 1L;
    private Long testHarvestId = 2L;
    private Long badJobId = -33L;
    private JobInfo acceptableJobInfoForJobTwo = new JobInfoTestImpl(2L, testHarvestId);
    private JobInfo acceptableJobInfoForJob42 = new JobInfoTestImpl(Heritrix1ControllerTestInfo.JOB_ID, testHarvestId);

    @Rule public TestName test = new TestName();
    private File WORKING_DIR;

    @Before
    public void initialize() {
        WORKING_DIR = new File(TestResourceUtils.OUTPUT_DIR, getClass().getSimpleName() + "/" + test.getMethodName());
        FileUtils.removeRecursively(WORKING_DIR);
        FileUtils.createDir(WORKING_DIR);
        Settings.set(HarvesterSettings.METADATA_FORMAT, "arc");
        MoveTestFiles mtf = new MoveTestFiles(Heritrix1ControllerTestInfo.CRAWLDIR_ORIGINALS_DIR, WORKING_DIR);
        mtf.setUp();
    }

    /**
     * Verify that ordinary construction does not throw Exception.
     */
    @Test
    public void testConstructor() {
        JobInfo acceptableJobInfoForJobOne = new JobInfoTestImpl(testJobId, testHarvestId);
        HeritrixFiles OkFiles = new HeritrixFiles(WORKING_DIR, acceptableJobInfoForJobOne);
        new IngestableFiles(OkFiles);
    }

    @Test (expected = ArgumentNotValid.class)
    public void testConstructorWithNonExistingCrawlDir() {
        JobInfo acceptableJobInfoForJobOne = new JobInfoTestImpl(testJobId, testHarvestId);
        HeritrixFiles heritrixFilesNonExistingCrawlDir =
                new HeritrixFiles(new File(WORKING_DIR, "doesnotexist"), acceptableJobInfoForJobOne);
        new IngestableFiles(heritrixFilesNonExistingCrawlDir);
    }

    @Test (expected = ArgumentNotValid.class)
    public void testConstructorWithBadJobInfo() {
        JobInfo unacceptableJobInfo = new JobInfoTestImpl(badJobId, testHarvestId);
        HeritrixFiles heritrixFilesWithBadJobInfo = new HeritrixFiles(WORKING_DIR, unacceptableJobInfo);
        new IngestableFiles(heritrixFilesWithBadJobInfo);
    }

    /**
     * Verify that method returns false before metadata has been generated. Verify that method returns false before
     * metadata generation has finished (indicated by setMetadataReady()). Verify that method returns true after
     * metadata generation has finished.
     * <p>
     * Note that disallowed actions concerning metdataReady are tested in another method. Note that rediscovery of
     * metadata is tested in another method.
     */
    @Test
    public void testGetSetMetadataReady() {
        JobInfo acceptableJobInfoForJobOne = new JobInfoTestImpl(testJobId, testHarvestId);
        HeritrixFiles OkFiles = new HeritrixFiles(WORKING_DIR, acceptableJobInfoForJobOne);
        IngestableFiles inf = new IngestableFiles(OkFiles);
        assertFalse("isMetadataReady() should return false before metadata has been generated", inf.isMetadataReady());
        assertFalse("isMetadataFailed() should return false before metadata has been generated", inf.isMetadataFailed());
        MetadataFileWriter mfw = inf.getMetadataWriter();

        writeOneRecord(mfw);
        assertFalse("isMetadataReady() should return false before all metadata has been generated",
                inf.isMetadataReady());
        assertFalse("isMetadataFailed() should return false before all metadata has been generated",
                inf.isMetadataFailed());
        inf.setMetadataGenerationSucceeded(true);
        assertTrue("isMetadataReady() should return true after metadata has been generated", inf.isMetadataReady());
        assertFalse("isMetadataFailed() should return false after metadata has been generated", inf.isMetadataFailed());
        HeritrixFiles OkFilesTwo = new HeritrixFiles(WORKING_DIR, acceptableJobInfoForJobTwo);
        inf = new IngestableFiles(OkFilesTwo);
        assertFalse("isMetadataReady() should return false before metadata has been generated", inf.isMetadataReady());
        assertFalse("isMetadataFailed() should return false before metadata has been generated", inf.isMetadataFailed());
        mfw = inf.getMetadataWriter();
        writeOneRecord(mfw);
        assertFalse("isMetadataReady() should return false before all metadata has been generated",
                inf.isMetadataReady());
        assertFalse("isMetadataFailed() should return false before all metadata has been generated",
                inf.isMetadataFailed());
        inf.setMetadataGenerationSucceeded(false);
        assertFalse("isMetadataReady() should return false after metadata has been generated", inf.isMetadataReady());
        assertTrue("isMetadataFailed() should return true before all metadata has been generated",
                inf.isMetadataFailed());
    }

    /**
     * Verify that a PermissionDenied is thrown if - metadata is NOT ready and getMetadataFiles() is called - metadata
     * IS ready and getMetadataArcWriter is called
     */
    @Test
    public void testDisallowedActions() {
        JobInfo acceptableJobInfoForJobOne = new JobInfoTestImpl(testJobId, testHarvestId);
        HeritrixFiles OkFiles = new HeritrixFiles(WORKING_DIR, acceptableJobInfoForJobOne);
        IngestableFiles inf = new IngestableFiles(OkFiles);

        assertCannotGetMetadata(inf);

        MetadataFileWriter aw = inf.getMetadataWriter();
        assertCannotGetMetadata(inf);
        writeOneRecord(aw);
        assertCannotGetMetadata(inf);
        inf.setMetadataGenerationSucceeded(true);
        try {
            inf.getMetadataWriter();
            fail("Should reject getMetadataArcWriter() when metadata is ready");
        } catch (PermissionDenied e) {
            // Expected
        }
        try {
            writeOneRecord(aw);
            fail("Should fail to write when metadata is ready");
        } catch (Throwable e) {
            // Expected
        }
        HeritrixFiles OkFilesTwo = new HeritrixFiles(WORKING_DIR, acceptableJobInfoForJobTwo);
        inf = new IngestableFiles(OkFilesTwo);

        assertCannotGetMetadata(inf);
        aw = inf.getMetadataWriter();
        assertCannotGetMetadata(inf);
        writeOneRecord(aw);
        assertCannotGetMetadata(inf);
        inf.setMetadataGenerationSucceeded(false);
        try {
            inf.getMetadataWriter();
            fail("Should reject getMetadataArcWriter() when metadata is failed");
        } catch (PermissionDenied e) {
            // Expected
        }
        try {
            writeOneRecord(aw);
            fail("Should fail to write when metadata is failed");
        } catch (Throwable e) {
            // Expected -- cannot tell what aw.write throws when closed.
        }

    }

    /**
     * Fail if inf.getMetadataArcFiles() does not throw PermissionDenied.
     */
    private void assertCannotGetMetadata(IngestableFiles inf) {
        try {
            inf.getMetadataArcFiles();
            fail("Should reject getMetadataArcFiles() when metadata is not ready");
        } catch (PermissionDenied e) {
            // Expected
        }
    }

    /**
     * Verify that IngestableFiles discovers old (final) metadata in the crawldir.
     */
    @Test
    public void testMetadataRediscovery() throws FileNotFoundException, IOException {
        // Original crawl: write some metadata
        JobInfo acceptableJobInfoForJobOne = new JobInfoTestImpl(testJobId, testHarvestId);
        HeritrixFiles OkFiles = new HeritrixFiles(WORKING_DIR, acceptableJobInfoForJobOne);
        IngestableFiles inf = new IngestableFiles(OkFiles);

        MetadataFileWriter aw = inf.getMetadataWriter();
        writeOneRecord(aw);
        inf.setMetadataGenerationSucceeded(true);
        // Now forget about old state:
        inf = new IngestableFiles(OkFiles);
        // Everything should be well:
        assertTrue("Should rediscover old metadata", inf.isMetadataReady());
        boolean found = false;
        for (File f : inf.getMetadataArcFiles()) {
            if (FileUtils.readFile(f).contains(MSG)) {
                found = true;
            }
        }
        assertTrue(
                "Test metadata should be contained in one of the metadata ARC files " + "but wasn't found in "
                        + inf.getMetadataArcFiles(), found);
    }

    /**
     * Verify that a non-null ArcWriter is returned.
     */
    @Test
    public void testGetMetadataArcWriter() {
        JobInfo acceptableJobInfoForJobOne = new JobInfoTestImpl(testJobId, testHarvestId);
        HeritrixFiles OkFiles = new HeritrixFiles(WORKING_DIR, acceptableJobInfoForJobOne);
        IngestableFiles inf = new IngestableFiles(OkFiles);
        MetadataFileWriter aw = inf.getMetadataWriter();
        writeOneRecord(aw);
    }

    /**
     * Verify that a file containing data written to the metadata ARCWriter is contained in one the returned files.
     */
    @Test
    public void testGetMetadataFiles() throws IOException {
        JobInfo acceptableJobInfoForJobOne = new JobInfoTestImpl(testJobId, testHarvestId);
        HeritrixFiles OkFiles = new HeritrixFiles(WORKING_DIR, acceptableJobInfoForJobOne);
        IngestableFiles inf = new IngestableFiles(OkFiles);
        MetadataFileWriter aw = inf.getMetadataWriter();
        writeOneRecord(aw);
        inf.setMetadataGenerationSucceeded(true);
        boolean found = false;
        for (File f : inf.getMetadataArcFiles()) {
            if (FileUtils.readFile(f).contains(MSG)) {
                found = true;
            }
        }
        assertTrue(
                "Test metadata should be contained in one of the metadata ARC files " + "but wasn't found in "
                        + inf.getMetadataArcFiles(), found);
    }

    @Test
    public void testMetadataFailure() {
        JobInfo acceptableJobInfoForJobOne = new JobInfoTestImpl(testJobId, testHarvestId);
        HeritrixFiles OkFiles = new HeritrixFiles(WORKING_DIR, acceptableJobInfoForJobOne);
        IngestableFiles inf = new IngestableFiles(OkFiles);
        inf.setMetadataGenerationSucceeded(false);
        try {
            inf.getMetadataArcFiles();
            fail("Should not have been allowed to get failed metadata");
        } catch (PermissionDenied e) {
            // expected
        }
        assertTrue("Metadata should be failed", inf.isMetadataFailed());
    }

    /**
     * Test that closeOpenFiles closes the right files.
     *
     * @throws Exception
     */
    @Test
    public void testCloseOpenFiles() throws Exception {
        // These files should end up closed
        File arcsDir = new File(WORKING_DIR, "arcs");
        File[] openFiles = new File[] {new File(arcsDir, "test1.arc.open"), new File(arcsDir, "test2.arc.gz.open")};
        // These files should be untouched
        File[] nonOpenFiles = new File[] {new File(arcsDir, "test3.arcygz.open"), new File(arcsDir, "test4.arc"),
                new File(arcsDir, "test5.arcagz"), new File(arcsDir, "test6.arcaopen")};
        for (File openFile : openFiles) {
            openFile.createNewFile();
            assertTrue("Open file '" + openFile + "' should exist before calling closeOpenFiles()", openFile.exists());
        }
        for (File nonOpenFile : nonOpenFiles) {
            nonOpenFile.createNewFile();
            assertTrue("Open file '" + nonOpenFile + "' should exist before calling closeOpenFiles()",
                    nonOpenFile.exists());
        }

        HeritrixFiles OkFiles42 = new HeritrixFiles(WORKING_DIR, acceptableJobInfoForJob42);
        IngestableFiles inf = new IngestableFiles(OkFiles42);

        inf.closeOpenFiles(0);
        for (File openFile1 : openFiles) {
            assertFalse("Open file '" + openFile1 + "' should not exist after calling closeOpenFiles()",
                    openFile1.exists());
            final String path = openFile1.getAbsolutePath();
            assertTrue("Open file '" + openFile1 + "' should have been closed after calling closeOpenFiles()",
                    new File(path.substring(0, path.length() - 5)).exists());
        }
        for (File nonOpenFile1 : nonOpenFiles) {
            assertTrue("Non-open file '" + nonOpenFile1 + "' should exist after calling closeOpenFiles()",
                    nonOpenFile1.exists());
            final String path = nonOpenFile1.getAbsolutePath();
            final String changedPath = path.substring(0, path.length() - 5);
            assertFalse("Changed non-open file '" + changedPath + "' should not exist after calling closeOpenFiles()",
                    new File(changedPath).exists());
        }
    }

    /**
     * Writes a single ARC record (containing MSG) and closes the ARCWriter.
     */
    private static void writeOneRecord(MetadataFileWriter aw) {
        try {
            aw.write("test://test.test/test", "text/plain", "0.0.0.0", new Date().getTime(), MSG.getBytes());
        } catch (IOException e) {
            fail("Should have written a test record and closed ARCWriter");
        }
    }

}
