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
package dk.netarkivet.archive.bitarchive;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.TestFileUtils;

/**
 * BitArchiveAdminData test class
 * Unfortunately, in unit tests there is no way I can be sure to have
 * two directories with different amounts of space free, so first dir is always
 * used...
 */

public class BitarchiveAdminTester extends TestCase {
    public static final File DATA_DIR
            = new File("./tests/dk/netarkivet/archive/bitarchive/data/admindata");
    public static final File WORKING_DIR = new File(DATA_DIR, "working");
    public static final File BA_DIR_1 = new File(WORKING_DIR, "badir1");
    public static final File BA_DIR_2 = new File(WORKING_DIR, "badir2");
    public static final String[] BA_DIR_ALL = {BA_DIR_1.getPath(),
                                               BA_DIR_2.getPath()};
    private static final File NOT_BA_DIR = new File(WORKING_DIR, "nobadir");
    public static final File ORIGINALS_DIR = new File(DATA_DIR, "originals");
    private BitarchiveAdmin ad;
    private static final String ARC_FILE_NAME = "testfile.arc";
    private static final String TEMPDIR = "tempdir";
    private static final String FILEDIR = "filedir";

    public BitarchiveAdminTester(String s) {
        super(s);
    }

    public void setUp() {
        FileUtils.removeRecursively(WORKING_DIR);
        TestFileUtils.copyDirectoryNonCVS(ORIGINALS_DIR, WORKING_DIR);
        Settings.set(Settings.BITARCHIVE_SERVER_FILEDIR, BA_DIR_ALL);
        ad = BitarchiveAdmin.getInstance();
    }

    public void tearDown() {
        if (ad != null) {
            ad.close();
        }
        FileUtils.removeRecursively(WORKING_DIR);
        Settings.reload();
    }

    public void testHasEnoughSpace() throws Exception {
        //1) settings set to 0 bytes required
        Settings.set(Settings.BITARCHIVE_MIN_SPACE_LEFT, "1");
        ad.close();
        ad = BitarchiveAdmin.getInstance();
        assertTrue("Should return true", ad.hasEnoughSpace());
        //2) One directory and settings set to return false
        long df = FileUtils.getBytesFree(BA_DIR_1);
        Settings.set(Settings.BITARCHIVE_MIN_SPACE_LEFT, Long.toString(2L * df));
        ad.close();
        ad = BitarchiveAdmin.getInstance();
        assertFalse("Should return true", ad.hasEnoughSpace());
    }

    public void testGetTemporaryPath() throws Exception {
        File tempfile = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
        assertEquals("Filename should be as requested",
                     ARC_FILE_NAME, tempfile.getName());
        assertEquals("File should go to temporary directory",
                     TEMPDIR, tempfile.getParentFile().getName());
        assertEquals("File should go in bitarchive dir",
                     BA_DIR_1.getAbsolutePath(),
                     tempfile.getParentFile().getParentFile().getAbsolutePath());
    }

    public void testGetTemporaryPathThrowsException() throws Exception {
        try {
            ad.getTemporaryPath("", 1L);
            fail("should throw argument not valid on invalid filename");
        } catch (ArgumentNotValid e) {
            //expected
        }

        try {
            ad.getTemporaryPath(ARC_FILE_NAME, -1L);
            fail("should throw argument not valid on negative requested size");
        } catch (ArgumentNotValid e) {
            //expected
        }

        long df = FileUtils.getBytesFree(BA_DIR_1);
        try {
            ad.getTemporaryPath(ARC_FILE_NAME, 2L * df);
            fail("should throw iofailure on no space left for file");
        } catch (IOFailure e) {
            //expected
        }
    }


    public void testMoveToStorage() throws Exception {
        File tempfile = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
        FileUtils.writeBinaryFile(tempfile, "abc".getBytes());
        assertEquals("File should now contain 'abc'",
                     "abc", FileUtils.readFile(tempfile));
        File finalfile = ad.moveToStorage(tempfile);
        assertFalse("Temp file should no longer exist", tempfile.exists());
        assertTrue("Final file should now exist", finalfile.exists());
        assertEquals("Final file should now contain 'abc'",
                     "abc", FileUtils.readFile(finalfile));
        assertEquals("Filename should be as requested",
                     ARC_FILE_NAME, finalfile.getName());
        assertEquals("File should go to file directory",
                     "filedir", finalfile.getParentFile().getName());
        assertEquals("File should go in bitarchive dir",
                     BA_DIR_1.getAbsolutePath(),
                     finalfile.getParentFile().getParentFile().getAbsolutePath());
    }

    public void testMoveToStorageThrowsException() throws Exception {
        try {
            ad.moveToStorage(null);
            fail("Should throw exception on null argument");
        } catch(ArgumentNotValid e) {
            //expected
        }

        //Invalid file to rename
        File invalidtopdir = new File(BA_DIR_1, "notintempdir.arc");
        invalidtopdir.createNewFile();
        try {
            ad.moveToStorage(invalidtopdir);
            fail("Should throw exception when giving file not in tempdir");
        } catch(IOFailure e) {
            //expected
            assertTrue("File should still exist", invalidtopdir.exists());
        }

        //Invalid file to rename
        File invalidbadir = new File(new File(NOT_BA_DIR,
                                              TEMPDIR),
                                     "notinbadir.arc");
        invalidbadir.createNewFile();
        try {
            ad.moveToStorage(invalidbadir);
            fail("Should throw exception when giving file not in tempdir");
        } catch(IOFailure e) {
            //expected
            assertTrue("File should still exist", invalidbadir.exists());
        }


        //Failures on renaming
        File knownfile = ad.getTemporaryPath("file1", 1L);
        try {
            ad.moveToStorage(knownfile);
            fail("Should throw exception when moving fails (file exists)");
        } catch(IOFailure e) {
            //expected
        }

        File tempfile = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
        try {
            ad.moveToStorage(tempfile);
            fail("Should throw exception when moving fails (no such file)");
        } catch(IOFailure e) {
            //expected
        }

        FileUtils.writeBinaryFile(tempfile, "abc".getBytes());
        FileUtils.removeRecursively(new File(BA_DIR_1, FILEDIR));
        try {
            ad.moveToStorage(tempfile);
            fail("Should throw exception when moving fails (to does not exist)");
        } catch(IOFailure e) {
            //expected
            assertTrue("Temp file should still exist", tempfile.exists());
        }
    }

    public void testGetFiles() throws Exception {
        File[] files = ad.getFiles();
        assertEquals("Should find the four files", 4, files.length);
        new File(new File(BA_DIR_1, FILEDIR), "dir").mkdir();
        files = ad.getFiles();
        assertEquals("Adding a dir should change nothing", 4, files.length);
        new File(new File(BA_DIR_1, FILEDIR), "file5").createNewFile();
        files = ad.getFiles();
        assertEquals("Should find new file", 5, files.length);
        FileUtils.removeRecursively(BA_DIR_2);
        files = ad.getFiles();
        assertEquals("Should survive dead dir and still find files",
                     3, files.length);
    }

    public void testLookup() throws Exception {
        try {
            ad.lookup(null);
            fail("Should throw exception on null argument");
        } catch(ArgumentNotValid e) {
            //Expected
        }

        BitarchiveARCFile file = ad.lookup("file1");
        assertNotNull("Should find existing file", file);
        assertEquals("Should be right file", "file1", file.getName());
        assertEquals("Should be right file",
                     new File(new File(BA_DIR_1, FILEDIR), "file1").getAbsolutePath(),
                     file.getFilePath().getAbsolutePath());
        file = ad.lookup("file3");
        assertNotNull("Should find existing file", file);
        assertEquals("Should be right file", "file3", file.getName());
        assertEquals("Should be right file",
                     new File(new File(BA_DIR_2, FILEDIR), "file3").getAbsolutePath(),
                     file.getFilePath().getAbsolutePath());
        file = ad.lookup("none");
        assertNull("Should return null on non-existing file", file);
    }

    public void testGetInstance() throws Exception {
        ad.close();
        File baddir = new File(WORKING_DIR, "baddir");
        baddir.createNewFile();
        Settings.set(Settings.BITARCHIVE_SERVER_FILEDIR, baddir.getPath());
        try {
            ad = BitarchiveAdmin.getInstance();
            fail("Should throw permission denied on bad dir");
        } catch (PermissionDenied e) {
            //expected
        }

        FileUtils.removeRecursively(baddir);
        baddir.mkdir();
        File tempdir = new File(baddir, TEMPDIR);
        tempdir.createNewFile();
        try {
            ad = BitarchiveAdmin.getInstance();
            fail("Should throw permission denied on bad dir");
        } catch (PermissionDenied e) {
            //expected
        }

        FileUtils.removeRecursively(baddir);
        baddir.mkdir();
        File filedir = new File(baddir, FILEDIR);
        filedir.createNewFile();
        try {
            ad = BitarchiveAdmin.getInstance();
            fail("Should throw permission denied on bad dir");
        } catch (PermissionDenied e) {
            //expected
        }
    }

    public void testGetFilesMatching_All() throws Exception {
        // First test that the getFiles() tests work on getFilesMatching w/.*
        File[] files = ad.getFilesMatching(Pattern.compile(".*"));
        assertEquals("Should find the four files", 4, files.length);
        new File(new File(BA_DIR_1, FILEDIR), "dir").mkdir();
        files = ad.getFilesMatching(Pattern.compile(".*"));
        assertEquals("Adding a dir should change nothing", 4,
                files.length);
        new File(new File(BA_DIR_1, FILEDIR), "file5").createNewFile();
        files = ad.getFilesMatching(Pattern.compile(".*"));
        assertEquals("Should find new file", 5, files.length);
        FileUtils.removeRecursively(BA_DIR_2);
        files = ad.getFilesMatching(Pattern.compile(".*"));
        assertEquals("Should survive dead dir and still find files",
                3, files.length);
    }

    public void testGetFilesMatching_Some() throws Exception {
        // Then test that the regexps are obeyed
        File[] files = ad.getFilesMatching(Pattern.compile("ile."));
        assertEquals("Should find no files since regexp doesn't match start", 0,
                files.length);
        files  = ad.getFilesMatching(Pattern.compile("file[24]"));
        assertEquals("Should find two files buf found " + files,
                2, files.length);
        List<String> filePaths = new ArrayList<String>();
        for (File f : files) {
            filePaths.add(f.getAbsolutePath());
        }

        String file2path = new File(new File(BA_DIR_1, "filedir"), "file2").getAbsolutePath();
        assertTrue("Should have " + file2path + " but found " + filePaths,
                filePaths.contains(file2path));
        String file4path = new File(new File(BA_DIR_2, "filedir"), "file4").getAbsolutePath();
        assertTrue("Should have " + file4path + " but found " + filePaths,
                filePaths.contains(file4path));
    }
}