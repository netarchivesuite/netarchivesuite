/*$Id$
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
package dk.netarkivet.common.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;

import junit.framework.TestCase;

import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Unittests for the MD5 class. 
 */
public class MD5Tester extends TestCase {
    ReloadSettings rs = new ReloadSettings(new File(TestInfo.SETTINGSFILENAME));
    private static final String EMPTY_STRING_MD5 = "d41d8cd98f00b204e9800998ecf8427e";
    private static final String BIG_FILE_MD5 = "6179feb1f881b0aae442876d8a6086bc";

    public MD5Tester(String s) {
        super(s);
    }

    public void setUp() {
        rs.setUp();
        try {
            if (!TestInfo.TEMPDIR.exists()) {
                dk.netarkivet.common.utils.TestInfo.TEMPDIR.mkdir();
            }
            FileUtils.removeRecursively(TestInfo.TEMPDIR);
            TestFileUtils.copyDirectoryNonCVS(TestInfo.DATADIR, TestInfo.TEMPDIR);
        }
        catch (Exception e) {
            fail("Could not setup configuration");
        }
    }

    public void tearDown() {
        FileUtils.removeRecursively(dk.netarkivet.common.utils.TestInfo.TEMPDIR);
        rs.tearDown();
    }

    public void testGenerateMD5onFile() throws Exception {
        String md5 = MD5.generateMD5onFile(TestInfo.MD5_EMPTY_FILE);
        assertEquals("MD5 on empty file must match known value",
                EMPTY_STRING_MD5, md5);

        File zipFile = new File(TestInfo.DATADIR,
                TestInfo.FIVE_HUNDRED_MEGA_FILE_ZIPPED);
        assertTrue("File '" + TestInfo.FIVE_HUNDRED_MEGA_FILE_ZIPPED +
                " does not exist!", zipFile.exists());

        String bigMD5 = MD5.generateMD5onFile(zipFile);
        assertEquals("MD5 on larger than buffer file should be correct",
                BIG_FILE_MD5, bigMD5);
    }

    public void testGenerateMD5() throws Exception {
        assertEquals("MD5 on empty string must match known value",
                EMPTY_STRING_MD5, MD5.generateMD5("".getBytes()));

        assertEquals("MD5 on empty inputstream must match known value",
                EMPTY_STRING_MD5, MD5.generateMD5(new ByteArrayInputStream("".getBytes())));
    }

    public void testGenerateMD5InputStream() throws IOException {
        File zipFile = new File(TestInfo.DATADIR,
                TestInfo.FIVE_HUNDRED_MEGA_FILE_ZIPPED);
        FileInputStream bigFileInputStream = new FileInputStream(zipFile);
        assertEquals("MD5 on big fileinputstream must match known value",
                BIG_FILE_MD5, MD5.generateMD5(bigFileInputStream));
        assertEquals("File input stream must be empty after MD5",
                -1, bigFileInputStream.read());
        bigFileInputStream.close();
    }

    public void testGetMessageDigest() throws Exception {
        MessageDigest md5_1 = MD5.getMessageDigestInstance();
        MessageDigest md5_2 = MD5.getMessageDigestInstance();
        assertNotSame("Getter should return new objects.", md5_1, md5_2);
    }
}