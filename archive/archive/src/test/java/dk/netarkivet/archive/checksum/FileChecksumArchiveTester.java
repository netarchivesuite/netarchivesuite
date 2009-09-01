/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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
package dk.netarkivet.archive.checksum;

import java.util.List;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.arcrepository.bitpreservation.ChecksumEntry;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import junit.framework.TestCase;

/**
 * 
test1.arc##1234567890
GNU.arc##1029384756
test2.arc##0987654321
 * @author jolf
 *
 */

public class FileChecksumArchiveTester extends TestCase {
    FileChecksumArchive fca;
    ReloadSettings rs = new ReloadSettings();
//    UseTestRemoteFile utrf = new UseTestRemoteFile();


    public void setUp() {
	rs.setUp();
	
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        FileUtils.removeRecursively(TestInfo.TMP_DIR);

        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINAL_DIR,
                TestInfo.WORKING_DIR);
        
        Settings.set(ArchiveSettings.CHECKSUM_FILENAME, TestInfo.CHECKSUM_FILE.getAbsolutePath());
	
	fca = FileChecksumArchive.getInstance();
    }
    
    public void tearDown() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        FileUtils.removeRecursively(TestInfo.TMP_DIR);
	rs.tearDown();
	fca.cleanup();
    }
    
    public void testChecksum() {
	assert(fca.hasEnoughSpace());
    }
    
    /**
     * Checks whether the filename for the checksum file is defined correct in
     * the settings.
     */ 

    public void testFilename() {
	String filename = Settings.get(ArchiveSettings.CHECKSUM_FILENAME);
	
	assert(fca.getFilename().equals(filename));
    }
    
    public void testContent() {
	RemoteFile arcfile1 = RemoteFileFactory.getInstance(TestInfo.UPLOAD_FILE_1, false, false, false);
	fca.upload(arcfile1, "TEST1.arc");
	RemoteFile arcfile2 = RemoteFileFactory.getInstance(TestInfo.UPLOAD_FILE_2, false, false, false);
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
	// Check whether the correct checksums exists.
	// ---------------------------------------------------------------	
	List<ChecksumEntry> checksums = ChecksumEntry.parseChecksumJob(fca.getArchiveAsFile());
	assertEquals("The expected number of checksum entries in the archive.", 
		2, checksums.size());
	assertTrue("Test1.arc should have the correct checksum entry", 
		checksums.contains(new ChecksumEntry("TEST1.arc", TestInfo.TEST1_CHECKSUM)));
	assertTrue("Test2.arc should have the correct checksum entry", 
		checksums.contains(new ChecksumEntry("TEST2.arc", TestInfo.TEST2_CHECKSUM)));
	System.out.println(checksums);

	// ---------------------------------------------------------------
	// Checks whether the correct checksums are found.
	assertEquals("The correct checksum for TEST1.arc should be retrieved.",
		TestInfo.TEST1_CHECKSUM, fca.getChecksum("TEST1.arc"));
	assertEquals("The correct checksum for TEST2.arc should be retrieved.", 
		TestInfo.TEST2_CHECKSUM, fca.getChecksum("TEST2.arc"));

	// ---------------------------------------------------------------
	// Check the removeRecord function, both good and bad examples.
	// ---------------------------------------------------------------

	// check that it is possible to remove a entry in this archive.
	boolean res = fca.removeRecord("TEST1.arc", "WRONG_CHECKSUM!!!!");
	assertTrue("It should be allowed to remove the TEST1.arc entry.", res);
	
	// check that a entry cannot be removed if the checksum is correct.
	res = fca.removeRecord("TEST2.arc", TestInfo.TEST2_CHECKSUM);
	assertFalse("It should not be allowed to remove the TEST2.arc entry, "
		+ "when the checksum is correct, '" + TestInfo.TEST2_CHECKSUM 
		+ "'.", res);
	
	// check that the same entry can be removed when the checksum is wrong.
	res = fca.removeRecord("TEST2.arc", "WRONG_CHECKSUM!!!!");
	assertTrue("It should be allowed to remove the TEST2.arc entry, when the checksum is wrong.",
		res);
	
	// check that a non-existing entry cannot be removed.
	res = fca.removeRecord("TEST3.arc", "NO_CHECKSUM!!");
	assertFalse("It should not be allowed to remove non-existing records "
		+ "in from the archive.", res);
    }
}
