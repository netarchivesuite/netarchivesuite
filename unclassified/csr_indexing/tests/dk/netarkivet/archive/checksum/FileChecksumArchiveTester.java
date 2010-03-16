/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.archive.checksum;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;
import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/**
 * Tester class for the FileChecksumArchive.
 * 
 */
public class FileChecksumArchiveTester extends TestCase {
    FileChecksumArchive fca;
    ReloadSettings rs = new ReloadSettings();
    UseTestRemoteFile utrf = new UseTestRemoteFile();

    public void setUp() {
        rs.setUp();
        utrf.setUp();

        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        FileUtils.removeRecursively(TestInfo.TMP_DIR);

        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINAL_DIR,
                TestInfo.WORKING_DIR);

        Settings.set(ArchiveSettings.CHECKSUM_BASEDIR, TestInfo.CHECKSUM_DIR.getAbsolutePath());
        Settings.set(CommonSettings.USE_REPLICA_ID, "THREE");

        fca = FileChecksumArchive.getInstance();
    }

    public void tearDown() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        FileUtils.removeRecursively(TestInfo.TMP_DIR);
        rs.tearDown();
        utrf.tearDown();
        fca.cleanup();
    }

    /**
     * Ensure that there is enough space.
     */
    public void testChecksum() {
        assert(fca.hasEnoughSpace());
    }

    /**
     * Checks whether the filename for the checksum file is defined correct in
     * the settings.
     */ 
    public void testFilename() {
        String filename = Settings.get(ArchiveSettings.CHECKSUM_BASEDIR) + "/checksum_THREE.md5";
        assertEquals("The files should have the same name. ", fca.getFileName(), filename);
    }

    /**
     * Check the following:
     * 1. FileChecksumArchiev can perform Upload.
     * 2. The correct checksums are retrieved from the upload.
     * 3. The correct file can be retrieved.
     * 4. That the Correct function can be performed.
     * 5. That the data also is changed appropriately.
     * 6. The old and 'wrong' entries have been move to the backup file.
     * 
     * @throws Exception So it is unnecessary to catch IOExceptions, since the 
     * test should fail.
     */
    public void testContent() throws Exception {
        RemoteFile arcfile1 = RemoteFileFactory.getInstance(TestInfo.UPLOAD_FILE_1, false, false, false);
        assertFalse("The archive should not already contain TEST1.arc", 
                fca.hasEntry("TEST1.arc"));
        fca.upload(arcfile1, "TEST1.arc");
        RemoteFile arcfile2 = RemoteFileFactory.getInstance(TestInfo.UPLOAD_FILE_2, false, false, false);
        assertFalse("The archive should not already contain TEST2.arc", 
                fca.hasEntry("TEST2.arc"));
        fca.upload(arcfile2, "TEST2.arc");
        List<String> filenames = FileUtils.readListFromFile(fca.getAllFilenames());

        // ---------------------------------------------------------------
        // Make check for whether the correct files exist in the archive.
        // ---------------------------------------------------------------
        assertEquals("The expected number of filenames in the archive.", 2,
                filenames.size());
        assertTrue("TEST1.arc should be amongst the filenames", filenames.contains("TEST1.arc"));
        assertTrue("TEST2.arc should be amongst the filenames", filenames.contains("TEST2.arc"));

        // ---------------------------------------------------------------
        // Make check for whether the correct checksums are stored in the archive.
        // ---------------------------------------------------------------
        assertEquals("The stored value and the checksum calculated through the method.",
                fca.getChecksum("TEST1.arc"), fca.calculateChecksum(TestInfo.UPLOAD_FILE_1));
        assertEquals("The value stored in the checksum archive and a precalculated value of the file",
                fca.getChecksum("TEST1.arc"), TestInfo.TEST1_CHECKSUM);

        assertEquals("The stored value and the checksum calculated through the method.",
                fca.getChecksum("TEST2.arc"), fca.calculateChecksum(TestInfo.UPLOAD_FILE_2));
        assertEquals("The value stored in the checksum archive and a precalculated value of the file",
                fca.getChecksum("TEST2.arc"), TestInfo.TEST2_CHECKSUM);

        // ---------------------------------------------------------------
        // Check whether the archive file is identical to the retrieved archive
        // ---------------------------------------------------------------	
        List<String> archiveChecksums = FileUtils.readListFromFile(fca.getArchiveAsFile());
        List<String> fileChecksums = FileUtils.readListFromFile(new File(fca.getFileName()));

        assertEquals("The amount of checksums should be identical in the file and from the archive.", 
                fileChecksums.size(), archiveChecksums.size());
        assertEquals("The amount of checksum should be 2", 2, fileChecksums.size());
        for(int i = 0; i < fileChecksums.size(); i++) {
            assertEquals("The checksum entry in the file should be identical to the one in the archive",
                    archiveChecksums.get(i), fileChecksums.get(i));
        }

        // ---------------------------------------------------------------
        // Check the correct function, both good and bad examples.
        // ---------------------------------------------------------------
        try {
            fca.correct("ERROR!", TestInfo.UPLOAD_FILE_1);
            fail("It is not allowed for 'correct' to correct a wrong 'incorrectChecksum'.");
        } catch(IllegalState e) {
            assertTrue("The correct error message should be sent.",
                    e.getMessage().contains("No file entry for file 'ERROR!'"));
        }

        fca.correct("TEST1.arc", TestInfo.UPLOAD_FILE_2);
        assertEquals("The new checksum for 'TEST1.arc' should now be the checksum for 'TEST2.arc'.",
                fca.getChecksum("TEST1.arc"), TestInfo.TEST2_CHECKSUM);

        fca.correct("TEST2.arc", TestInfo.UPLOAD_FILE_1);
        assertEquals("The new checksum for 'TEST2.arc' should now be the checksum for 'TEST1.arc'.",
                fca.getChecksum("TEST2.arc"), TestInfo.TEST1_CHECKSUM);

        // ---------------------------------------------------------------
        // Check that the correct function has changed the archive file.
        // ---------------------------------------------------------------
        String correctChecksums = FileUtils.readFile(new File(fca.getFileName()));

        assertTrue("The new checksums should be reversed.",
                correctChecksums.contains("TEST1.arc" + "##" + TestInfo.TEST2_CHECKSUM));
        assertTrue("The new checksums should be reversed",
                correctChecksums.contains("TEST2.arc" + "##" + TestInfo.TEST1_CHECKSUM));

        // ---------------------------------------------------------------
        // Check that the correct function has put the old record into the 
        // wrong entry file.
        // ---------------------------------------------------------------
        String wrongEntryContent = FileUtils.readFile(new File(fca.getWrongEntryFilename()));

        assertTrue("The old checksums should be store here.",
                wrongEntryContent.contains("TEST1.arc" + "##" + TestInfo.TEST1_CHECKSUM));
        assertTrue("The old checksums should be store here.",
                wrongEntryContent.contains("TEST2.arc" + "##" + TestInfo.TEST2_CHECKSUM));
    }
    
    /**
     * Checks how the archive handles it, when there is an admin.data file.
     * @throws IOException 
     */
    public void testAdminData() throws IOException {
        FileChecksumArchive.getInstance().cleanup();
        // PRINT THE ADMIN FILE
        File adminFile = new File("admin.data");
        adminFile.delete();
        adminFile.createNewFile();
        FileWriter fw = new FileWriter(adminFile);
        // print version
        fw.append("0.4" + "\n");
        // print content.
        fw.append("TEST1.arc checksum1 UPLOAD_STARTED 0" + "\n");
        fw.append("TEST2.arc checksum2 UPLOAD_FAILED 1" + "\n");
        fw.append("TEST3.arc checksum3 UPLOAD_COMPLETED 2" + "\n");
        fw.append("TEST4.arc checksum4 UPLOAD_COMPLETED 3" + "\n");
        fw.append("TEST3.arc checksum3 UPLOAD_COMPLETED 4" + "\n");
        
        fw.flush();
        fw.close();
        
        try {
            FileChecksumArchive fca = FileChecksumArchive.getInstance();

            assertFalse("TEST1.arc is not complete and should not be in archive", 
                    fca.hasEntry("TEST1.arc"));
            assertFalse("TEST2.arc is not complete and should not be in archive", 
                    fca.hasEntry("TEST2.arc"));
            assertEquals("Unexpected checksum for TEST3.arc", 
                    "checksum3", fca.getChecksum("TEST3.arc"));
            assertEquals("Unexpected checksum for TEST4.arc", 
                    "checksum4", fca.getChecksum("TEST4.arc"));
            
            String archiveFile = FileUtils.readFile(fca.getArchiveAsFile());
            
            Matcher match = Pattern.compile("TEST3.arc").matcher(archiveFile);
            assertTrue("TEST3.arc should be found the first time", match.find());
            assertFalse("TEST3.arc should not be found the second time", 
                    match.find(match.end()));
        } finally {
            adminFile.delete();
        }
    }
}
