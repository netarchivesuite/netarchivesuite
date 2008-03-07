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
package dk.netarkivet.common.distribute.arcrepository;
/**
 * lc forgot to comment this!
 */

import java.io.ByteArrayOutputStream;
import java.io.File;

import junit.framework.TestCase;

import dk.netarkivet.archive.arcrepository.bitpreservation.FileListJob;
import dk.netarkivet.common.Settings;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.MD5;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

public class TrivialArcRepositoryClientTester extends TestCase {
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR,
            TestInfo.WORKING_DIR);
    ReloadSettings rs = new ReloadSettings();
    UseTestRemoteFile utrf = new UseTestRemoteFile();

    public TrivialArcRepositoryClientTester(String s) {
        super(s);
    }

    public void setUp() {
        rs.setUp();
        utrf.setUp();

        Settings.set(Settings.DIR_COMMONTEMPDIR,
                TestInfo.WORKING_DIR.getAbsolutePath());
        mtf.setUp();
    }

    public void tearDown() {
        mtf.tearDown();
        utrf.tearDown();
        rs.tearDown();
    }

    public void testStore() throws Exception {
        ArcRepositoryClient arcrep = new TrivialArcRepositoryClient();

        BatchStatus status = arcrep.batch(new FileListJob(), "BA");
        assertEquals("Should have no files processed at outset",
                0, status.getNoOfFilesProcessed());

        FileUtils.copyFile(TestInfo.SAMPLE_FILE, TestInfo.SAMPLE_FILE_COPY);
        arcrep.store(TestInfo.SAMPLE_FILE);
        assertFalse("Should ahve deleted file after upload",
                TestInfo.SAMPLE_FILE.exists());
        status = arcrep.batch(new FileListJob(), "BA");
        assertEquals("Should have 1 files processed after store",
                1, status.getNoOfFilesProcessed());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        status.getResultFile().appendTo(out);
        assertEquals("Should list the one file",
                TestInfo.SAMPLE_FILE.getName() + "\n",
                out.toString());
        File f = File.createTempFile("foo", "bar", FileUtils.getTempDir());
        arcrep.getFile(TestInfo.SAMPLE_FILE.getName(), Location.get("KB"), f);
        assertEquals("Should have expected contents back",
                MD5.generateMD5onFile(TestInfo.SAMPLE_FILE_COPY),
                MD5.generateMD5onFile(f));
    }
}