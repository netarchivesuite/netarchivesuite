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

package dk.netarkivet.common.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.testutils.CollectionAsserts;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;


/**
 * Unit tests for the FileUtils class.
 */
public class FileUtilsTester extends TestCase{
    private UseTestRemoteFile rf = new UseTestRemoteFile();
    ReloadSettings rs = new ReloadSettings();

    private static final File BASE_DIR = new File("tests/dk/netarkivet/common/utils");
    private static final File ORIGINALS = new File(BASE_DIR, "fileutils_data");
    private static final File WORKING = new File(BASE_DIR, "working");
    private static final File NO_SUCH_FILE = new File(WORKING, "no_file");
    private static final File SUBDIR = new File(WORKING, "subdir");
    private static final File EMPTY = new File(WORKING, "emptyfile.txt");

    public void setUp() {
        rs.setUp();
        FileUtils.removeRecursively(WORKING);
        TestFileUtils.copyDirectoryNonCVS(ORIGINALS, WORKING);
        rf.setUp();
    }

    public void tearDown() {
        FileUtils.removeRecursively(WORKING);
        rf.tearDown();
        rs.tearDown();
    }

    /**
     * Tests removeRecusively method
     */
    public void testRemoveRecursively(){
        assertFalse("Should return false when removing non-existent file: ",
                FileUtils.removeRecursively(NO_SUCH_FILE));
        assertTrue("Should return true when removing actually existing " +
                "directory: ",
                FileUtils.removeRecursively(SUBDIR)) ;
        assertFalse("Have not removed subdirectory", SUBDIR.isDirectory());
    }

    /**
     * Check that recursive file filter works correctly
     */
    public void testGetFilesRecursively() {
        List<File> l = FileUtils.getFilesRecursively(WORKING.getAbsolutePath(),
                new ArrayList<File>(), ".arc");
        assertEquals("Wrong number of files returned", 4, l.size());
    }

    /**
     * test that FileUtils.append can append between two remote files.
     * @throws IOException
     */
    public void testAppendRemoteFiles() throws IOException {
        Settings.set(CommonSettings.REMOTE_FILE_CLASS, TestRemoteFile.class.getName());
        File in_1 = new File(WORKING, "append_data/file1");
        File in_2 = new File(WORKING, "append_data/file2");
        File out_file = new File(WORKING, "append_data/output");
        RemoteFile rf1 = RemoteFileFactory.getInstance(in_1, true, false, true);
        RemoteFile rf2 = RemoteFileFactory.getInstance(in_2, true, false, true);
        OutputStream out = new FileOutputStream(out_file);
        // Test null arguments
        try {
            rf1.appendTo(null);
            fail("Should throw ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            //expected
        }
        // Now do the actual test
        rf1.appendTo(out);
        rf2.appendTo(out);
        out.close();
        FileAsserts.assertFileNumberOfLines("File has wrong number of lines",
                out_file, 2);
        FileAsserts.assertFileContains("Missing content", "1", out_file);
        FileAsserts.assertFileContains("Missing content", "2", out_file);
    }

    /** Check that we can at least get some information out of getBytesFree
     *  @throws Exception
     */
    public void testGetBytesFree() throws Exception {
        long free1 = FileUtils.getBytesFree(NO_SUCH_FILE);
        assertEquals("Should report 0 on non-existing file", 0,
                free1);
        long free2 = FileUtils.getBytesFree(EMPTY);
        assertTrue("Should get a value on a file", free2 > 0);
        long free3 = FileUtils.getBytesFree(WORKING);
        assertTrue("Should get a value on a directory", free3 > 0);
        long free4 = FileUtils.getBytesFree(new File("/does/not/exist"));
        assertEquals("Should report 0 on non-existing dir", 0,
                free4);
    }

    public void testCreateDir() throws InterruptedException {
        try {
            FileUtils.createDir(new File("/foo"));
            fail("Should fail when given a nonwritable path");
        } catch (PermissionDenied e) {
            // Expected
        }
        try {
            FileUtils.createDir(new File(""));
            fail("Should fail when given no dirs");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        File newdir = new File(WORKING, "newdir");
        FileUtils.createDir(newdir);
        assertTrue("Directory should be created and writable",
                   newdir.isDirectory() && newdir.canWrite());

        File newdir2 = new File(new File(WORKING, "newdir2"), "subdir");
        FileUtils.createDir(newdir2);
        assertTrue("Directory with parent should be created and writable",
                   newdir2.isDirectory() && newdir2.canWrite());

    }
    public void testCreateDirInParallel() throws InterruptedException {
        //Test that multiple threads making a directory at once don't fail
        List<Thread> threads = new ArrayList<Thread>();
        final boolean[] failed = new boolean[] { false };
        final File threaddir = new File(WORKING, "threaddir/dir1/dir2");
        for (int i=0; i<10; i++) {
            threads.clear();
            threaddir.delete();
            for (int j =0; j <10; j++) {
                threads.add(new Thread() {
                    public void run() {
                        try {
                            ApplicationUtils.dirMustExist(threaddir);
                        } catch (Exception e) {
                            System.out.println("Exception " + e);
                            e.printStackTrace();
                            failed[0] = true;
                        }
                    }
                });
            }
            for (Thread t : threads) {
                t.start();
            }
            WAITLOOP: do {
                Thread.sleep(10);
                for (Thread t: threads) {
                    if (t.isAlive()) {
                        continue WAITLOOP;
                    }
                }
                // If we get here, no thread was still alive, we can go on.
                break;
            } while (true);
            if (failed[0]) {
                fail("Failed to create dir in parallel");
                break;
            }
        }
    }

    public void testCDXFilter(){
        File testDir = TestInfo.CDX_FILTER_TEST_DATA_DIR;
        File[] foundFiles = testDir.listFiles(FileUtils.CDX_FILE_FILTER);
        Set<String> fileNames = new HashSet<String>();
        for ( File f : foundFiles){
            fileNames.add(f.getName());
        }
        assertEquals("CDX_FILE_FILTER should accept exactly the occurring .cdx-files",
                TestInfo.CDX_FILTER_TEST_CDX_FILES,
                fileNames);
    }

    public void testConstantPatterns() {
        assertTrue("Should match", "file.arc.gz".matches(".*" + FileUtils.ARC_PATTERN));
        assertTrue("Should match", "file.ARC".matches(".*" + FileUtils.ARC_PATTERN));
        assertTrue("Should match", "file.aRc.GZ".matches(".*" + FileUtils.ARC_PATTERN));
        assertFalse("Should NOT match", "file.ARC.open".matches(".*" + FileUtils.ARC_PATTERN));
        assertTrue("Should match", "file.arc.gz.open".matches(".*" + FileUtils.OPEN_ARC_PATTERN));
        assertTrue("Should match", "file.ARC.OPEN".matches(".*" + FileUtils.OPEN_ARC_PATTERN));
        assertTrue("Should match", "file.arc.GZ.OpEn".matches(".*" + FileUtils.OPEN_ARC_PATTERN));
        assertFalse("Should NOT match", "file.ARC.open.txt".matches(".*" + FileUtils.OPEN_ARC_PATTERN));
    }


    public void testGetEphemeralInputStream() throws Exception {
        // Check that closing removes the file
        File f = File.createTempFile("foo", "bar", WORKING);
        InputStream in = FileUtils.getEphemeralInputStream(f);
        assertTrue("Temp file should exist before close", f.exists());
        in.close();
        assertFalse("Temp file should disappear at close", f.exists());
    }

    /** Test that the FilenameParser gives the right things.
     * Checks bug #709 */
    public void testFilenameParser() {
        // See HeritrixLauncer.getCrawlID for what we define of the filename
        File arcFile1 =
                new File("26-8-20050619145044-00018-kb-prod-har-001.kb.dk.arc");
        FileUtils.FilenameParser parser1 = new FileUtils.FilenameParser(arcFile1);
        assertEquals("Should have right jobId", "26", parser1.getJobID());
        assertEquals("Should have right harvestId", "8", parser1.getHarvestID());
        assertEquals("Should have right timestamp",
                "20050619145044", parser1.getTimeStamp());
        assertEquals("Should have right serial#", "00018", parser1.getSerialNo());

        File arcFile2 =
                new File("26--20050619145044-00018-kb-prod-har-001.kb.dk.arc");
        try {
            new FileUtils.FilenameParser(arcFile2);
            fail("Should throw UnknownID");
        } catch (UnknownID e) {
            // expected
        }
    }

    /**
     * Tests that the new makeValidFile method behaves as designed. It must either
     * return a valid file, or throw an IOException.
     *
     */
    public void testMakeValidFileFromExistingMakeAValidFileFromExisting() {
        // Make a valid file by a valid file name
        File existingFile = new File(WORKING, "emptyfile.txt");
        assertNotNull(existingFile);
        assertTrue(existingFile.isFile());
        File f = FileUtils.makeValidFileFromExisting(existingFile.getAbsolutePath());
        // Test it
        assertNotNull(f);
        assertTrue(f.isFile());
    }
    public void testMakeValidFileFromExistingMakeAnInvalidFileFromNonExisting() {
        try {
            FileUtils.makeValidFileFromExisting(NO_SUCH_FILE.getAbsolutePath());
            TestCase.fail("Expected an IOFailure exception from attaching to non-existing file.");
        } catch (IOFailure e) {
            // This is intended behaviour
        }
    }


    public void testCreateUniqueTempDir() throws Exception {
        // This test cannot ensure the concurrency properties, but it can
        // at least check single behaviour
        File checkDir = new File(TestInfo.TEMPDIR, "checkUniques");
        FileUtils.createDir(checkDir);
        int prevCount = checkDir.list().length;
        String prefix = "myPrefix";
        File newDir = FileUtils.createUniqueTempDir(checkDir, prefix);
        assertNotNull("Should return non-null", newDir);
        assertTrue("Returned dir should exist", newDir.isDirectory());
        assertEquals("Returned dir should be under checkDir",
                newDir.getParentFile().getCanonicalPath(),
                checkDir.getCanonicalPath());
        assertEquals("Exactly one more dir should have been made",
                prevCount + 1, checkDir.list().length);

        // Error cases
        try {
            FileUtils.createUniqueTempDir(null, prefix);
            fail("Should fail on null dir");
        } catch (ArgumentNotValid e) {
            //expected
        }

        try {
            FileUtils.createUniqueTempDir(checkDir, null);
            fail("Should fail on null dir");
        } catch (ArgumentNotValid e) {
            //expected
        }

        try {
            FileUtils.createUniqueTempDir(TestInfo.INVALIDXML, prefix);
            fail("Should not be able to make dir under file");
        } catch (ArgumentNotValid e) {
            StringAsserts.assertStringContains("Should mention file",
                    TestInfo.INVALIDXML.getName(), e.getMessage());
        }

        try {
            FileUtils.createUniqueTempDir(new File(TestInfo.TEMPDIR, "foo"), prefix);
            fail("Should not be able to make dir under non-existing dir");
        } catch (ArgumentNotValid e) {
            StringAsserts.assertStringContains("Should mention file",
                    "foo", e.getMessage());
        }

        String[] prevFiles = checkDir.list();
        try {
            FileUtils.createUniqueTempDir(checkDir, "foo/bar");
            fail("Should not be able to make dir with illegal name");
        } catch (IOFailure e) {
            StringAsserts.assertStringContains("Should mention file",
                    "foo/bar", e.getMessage());
            CollectionAsserts.assertIteratorEquals("Should not have changed dir contents",
                    Arrays.asList(prevFiles).iterator(),
                    Arrays.asList(checkDir.list()).iterator());
        }
    }

    public void testReadLastLine() throws Exception {
        try {
            FileUtils.readLastLine(null);
            fail("Expected ArgumentNotValid on null value");
        } catch(ArgumentNotValid e) {
            //expected
        }

        try {
            FileUtils.readLastLine(new File("NonExistingFile"));
            fail("Expected ArgumentNotValid on not readable file");
        } catch(ArgumentNotValid e) {
            //expected
        }

        assertEquals("Must get correct last line",
                     TestInfo.LAST_LINE_TEXT,
                     FileUtils.readLastLine(TestInfo.FILE_WITH_NEWLINE_AT_END));

        assertEquals("Must get correct last line",
                     TestInfo.LAST_LINE_TEXT,
                     FileUtils.readLastLine(TestInfo.FILE_WITH_NEWLINE_AT_END));

        assertEquals("Must get correct last line",
                     TestInfo.LAST_LINE_TEXT,
                     FileUtils.readLastLine(TestInfo.FILE_WITH_ONE_LINE));

        assertEquals("Must get correct last line",
                     "",
                     FileUtils.readLastLine(TestInfo.EMPTY_FILE));
     }

    public void testAppendToFile() {
        File testFile = new File(WORKING, "test");
        FileUtils.appendToFile(testFile);
        assertEquals("Should have no lines in file, but existing file",
                     0, FileUtils.countLines(testFile));
        FileUtils.appendToFile(testFile, "A single line");
        CollectionAsserts.assertListEquals("Should have one line in file",
                                           FileUtils.readListFromFile(testFile),
                                           "A single line");
        FileUtils.appendToFile(testFile, "Another line", "and then one");
        CollectionAsserts.assertListEquals("Should have three lines in file",
                                           FileUtils.readListFromFile(testFile),
                                           "A single line", "Another line",
                                           "and then one");
        FileUtils.appendToFile(testFile, "A broken\nline");
        CollectionAsserts.assertListEquals("Should have five lines in file",
                                           FileUtils.readListFromFile(testFile),
                                           "A single line", "Another line",
                                           "and then one", "A broken", "line");
    }
}
