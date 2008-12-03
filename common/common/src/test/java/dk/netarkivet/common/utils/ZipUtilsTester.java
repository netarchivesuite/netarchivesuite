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
package dk.netarkivet.common.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.TestFileUtils;

/**
 * Unit tests for the class ZipUtils.
 */
public class ZipUtilsTester extends TestCase {
    public ZipUtilsTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.FILEUTILS_DATADIR,
                TestInfo.TEMPDIR);
    }

    public void tearDown() throws Exception {
        FileUtils.removeRecursively(TestInfo.TEMPDIR);
        super.tearDown();
    }

    public void testZipDirectory() throws Exception {
        Set<String> files = getFileListNonDirectory();
        File zipFile = new File(TestInfo.TEMPDIR, "temp.zip");
        assertFalse("Zip file should not exist before", zipFile.exists());
        ZipUtils.zipDirectory(TestInfo.ZIPDIR, zipFile);
        assertTrue("Zip file must exist afterwards", zipFile.exists());
        ZipFile reader = new ZipFile(zipFile);
        Enumeration<? extends ZipEntry> entries = reader.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            assertTrue("Zipped file " + entry.getName() + " should be in dir",
                    files.contains(entry.getName()));
            File file = new File(TestInfo.ZIPDIR, entry.getName());
            assertEquals("Should have same original size as the file",
                    file.length(), entry.getSize());
            files.remove(entry.getName());
        }
        assertTrue("Should have zipped all files, but " + files + " remain",
                files.isEmpty());

        try {
            ZipUtils.zipDirectory(null, zipFile);
            fail("Should fail on null dir");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        try {
            ZipUtils.zipDirectory(zipFile, null);
            fail("Should fail on null file");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        try {
            ZipUtils.zipDirectory(zipFile, zipFile);
            fail("Should fail on source dir existing as file");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        try {
            ZipUtils.zipDirectory(TestInfo.ZIPDIR, TestInfo.TEMPDIR);
            fail("Should fail on target existing as dir");
        } catch (IOFailure e) {
            // Expected
        }

        // Should not fail on existing zipFile
        long beforeSize = zipFile.length();
        ZipUtils.zipDirectory(TestInfo.DATADIR, zipFile);
        assertFalse("Zipfile should have been overwritten",
                beforeSize == zipFile.length());
    }

    public void testUnzip() throws IOException {
        File unzipDir = new File(TestInfo.TEMPDIR, "unzip");
        assertFalse("Unzipdir should not exist beforehand", unzipDir.exists());
        ZipUtils.unzip(TestInfo.ZIPPED_DIR, unzipDir);
        assertTrue("Unzipdir should have been created", unzipDir.exists());
        Set<String> files = getFileListNonDirectory();
        for (String s : new ArrayList<String>(files)) {
            if (!s.equals(TestInfo.ZIPPED_DIR.getName())) {
                // Can't get zippedDir.zip to contain the right contents
                // (at least not easily), so that one is skipped.
                File unpackedFile = new File(unzipDir, s);
                assertTrue("File " + s + " should exist in unpacked dir",
                        unpackedFile.exists());
                File originalFile = new File(TestInfo.ZIPDIR, s);
                assertEquals("File " + s + " should have same size in unpacked dir",
                        originalFile.length(),
                        unpackedFile.length());
                assertEquals("MD5 should be the same on old and new file",
                        MD5.generateMD5onFile(originalFile),
                        MD5.generateMD5onFile(unpackedFile));
            }
            files.remove(s);
        }
        assertTrue("Should have no files left, but has " + files,
                files.isEmpty());

        FileUtils.removeRecursively(unzipDir);
        ZipUtils.unzip(TestInfo.ZIPPED_DIR_WITH_SUBDIRS, unzipDir);
        assertTrue("Should have dir a", new File(unzipDir, "a").isDirectory());
        assertTrue("Should have file b in a", new File(new File(unzipDir, "a"), "b").isFile());
        assertEquals("File b should have length 4", 4, new File(new File(unzipDir, "a"), "b").length());
        assertTrue("Should have dir c in b", new File(new File(unzipDir, "a"), "c").isDirectory());
        assertTrue("Should have file d in c", new File(new File(new File(unzipDir, "a"), "c"), "d").isFile());
        assertEquals("File b should have length 7", 7, new File(new File(new File(unzipDir, "a"), "c"), "d").length());

        try {
            ZipUtils.unzip(null, unzipDir);
            fail("Should fail on null zipfile");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            ZipUtils.unzip(TestInfo.ZIPPED_DIR, null);
            fail("Should fail on null target dir");
        } catch (ArgumentNotValid e) {
            // expected
        }

        // Output dir is file
        try {
            ZipUtils.unzip(TestInfo.ZIPPED_DIR, TestInfo.ZIPPED_DIR);
            fail("Should fail on file for target dir");
        } catch (PermissionDenied e) {
            // expected
        }

        // Input zip is not a zip file but a dir
        try {
            ZipUtils.unzip(unzipDir, unzipDir);
            fail("Should fail on dir for zipfile");
        } catch (IOFailure e) {
            // expected
        }

        // Input file is not a zip file
        try {
            ZipUtils.unzip(new File(TestInfo.ZIPDIR, "f1.arc"), unzipDir);
            fail("Should fail on non-zip zipfile");
        } catch (IOFailure e) {
            // expected
        }

    }

    private Set<String> getFileListNonDirectory() {
        File[] fileList = TestInfo.ZIPDIR.listFiles(
                new FileFilter() {
                    public boolean accept(File pathname) {
                        return !pathname.isDirectory();
                    }
                }
        );
        Set<String> files = new HashSet<String>(fileList.length);
        for (File f: fileList) {
            files.add(f.getName());
        }
        return files;
    }

    public void testGzipFiles() throws Exception {
        Method gzipFiles = ReflectUtils.getPrivateMethod(ZipUtils.class,
                "gzipFiles", File.class, File.class);
        File testInputDir = new File(new File(TestInfo.TEMPDIR, "cache"),
                "cdxindex");
        File testOutputDir = new File(TestInfo.TEMPDIR, "gzipped");
        gzipFiles.invoke(null, testInputDir, testOutputDir);
        assertTrue("Should have output file 1",
                new File(testOutputDir, "1-cache.gz").exists());
        assertTrue("Should have output file 2",
                new File(testOutputDir, "4-cache.gz").exists());
        assertEquals("Should have no tmpDir left",
                0, testOutputDir.getParentFile().list(new FilenameFilter() {
            public boolean accept(File f, String name) {
                return name.contains("tmpDir");
            }
        }).length);

        File internalDir = new File(testInputDir, "aDir");
        FileUtils.createDir(internalDir);
        FileUtils.removeRecursively(testOutputDir);
        gzipFiles.invoke(null, testInputDir, testOutputDir);
        assertTrue("Should have output file 1",
                new File(testOutputDir, "1-cache.gz").exists());
        assertTrue("Should have output file 2",
                new File(testOutputDir, "4-cache.gz").exists());
        assertFalse("SHould not have output zipped dir",
                new File(testOutputDir, "aDir.gz").exists());
        assertEquals("Should have no tmpDir left",
                0, testOutputDir.getParentFile().list(new FilenameFilter() {
            public boolean accept(File f, String name) {
                return name.contains("tmpDir");
            }
        }).length);

        try {
            gzipFiles.invoke(null, null, testOutputDir);
            fail("Should fail on null input dir");
        } catch (InvocationTargetException e) {
            StringAsserts.assertStringContains("Should mention fromDir",
                    "fromDir", e.getCause().getMessage());
        }

        try {
            gzipFiles.invoke(null, testInputDir, null);
            fail("Should fail on null output dir");
        } catch (InvocationTargetException e) {
            StringAsserts.assertStringContains("Should mention toDir",
                    "toDir", e.getCause().getMessage());
        }

        try {
            gzipFiles.invoke(null, new File(TestInfo.TEMPDIR, "FOO"),
                    testOutputDir);
            fail("Should fail on missing input dir");
        } catch (InvocationTargetException e) {
            StringAsserts.assertStringContains("Should mention fromDir",
                    "FOO", e.getCause().getMessage());
        }

        try {
            gzipFiles.invoke(null, testInputDir, TestInfo.TEMPDIR);
            fail("Should fail on existing output dir");
        } catch (InvocationTargetException e) {
            StringAsserts.assertStringContains("Should mention toDir",
                    TestInfo.TEMPDIR.getName(), e.getCause().getMessage());
        }

        assertEquals("Should have no tmpDir left",
                0, testOutputDir.getParentFile().list(new FilenameFilter() {
            public boolean accept(File f, String name) {
                return name.contains("tmpDir");
            }
        }).length);
    }

}