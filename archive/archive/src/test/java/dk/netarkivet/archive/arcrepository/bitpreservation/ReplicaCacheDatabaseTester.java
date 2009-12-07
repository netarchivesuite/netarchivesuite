/* File:     $Id$
 * Revision: $Revision$
 * Author:   $Author$
 * Date:     $Date$
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
package dk.netarkivet.archive.arcrepository.bitpreservation;

import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import junit.framework.TestCase;
import org.apache.commons.collections.IteratorUtils;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

public class ReplicaCacheDatabaseTester extends TestCase {

    private ReloadSettings rs = new ReloadSettings();
    private Connection dbCon;
    private ReplicaCacheDatabase cache;
    static boolean clear = true;
    
    public void setUp() {
	try {
	    rs.setUp();
	    ReplicaCacheDatabase.getInstance().cleanup();
	    // retrieve the connection to the database
	    dbCon = DBConnect.getDBConnection(Settings.get(
	            ArchiveSettings.URL_ARCREPOSITORY_BITPRESERVATION_DATABASE));

	    // Only clear the database once during setUp.
	    if(clear) {
		clearDatabase(dbCon);
		clear = false;
	    }

	    cache = ReplicaCacheDatabase.getInstance();
	} catch (SQLException e) {
	    throw new IOFailure("Cannot clear the database, for testing.", e);
	}
    }
    
    public void tearDown() {
        rs.tearDown();
    }
    
    public void testAll() {
	try {
	    
	    Date beforeTest = new Date(Calendar.getInstance().getTimeInMillis());
	    
	    // try handling output from ChecksumJob.
	    File csFile = makeTemporaryChecksumFile1();
	    cache.addChecksumInformation(ChecksumEntry.parseChecksumJob(csFile), 
		    Replica.getReplicaFromId("ONE"));
	    
	    // try handling output from FilelistJob.
	    File flFile = makeTemporaryFilelistFile();
	    cache.addFileListInformation(FileUtils.readListFromFile(flFile), 
		    Replica.getReplicaFromId("TWO"));

	    Date dbDate = cache.getDateOfLastMissingFilesUpdate(
		    Replica.getReplicaFromId("TWO"));

	    Date afterInsert = new Date(Calendar.getInstance().getTimeInMillis());
	    
	    // Assert that the time of insert is between the start of this test
	    // and now.
	    assertTrue("The last missing file update for replica '" 
		    + Replica.getReplicaFromId("ONE") + "' should be after "
		    + "the test was begun. Thus '" + DateFormat
		    .getDateInstance().format(dbDate) + "' should be after '"
		    + DateFormat.getDateInstance().format(beforeTest) + "'.", 
		    dbDate.after(beforeTest));
	    assertTrue("The last missing file update for replica '" 
		    + Replica.getReplicaFromId("ONE") + "' should be before "
		    + "the current time. Thus '" + DateFormat
		    .getDateInstance().format(dbDate) + "' should be before '"
		    + DateFormat.getDateInstance().format(afterInsert) + "'.", 
		    dbDate.before(afterInsert));

	    // Check that getDateOfLastWrongFilesUpdate gives a date between
	    // the start of this test and now.
	    dbDate = cache.getDateOfLastWrongFilesUpdate(Replica.getReplicaFromId("ONE"));
	    assertTrue("The last missing file update for replica '" 
		    + Replica.getReplicaFromId("ONE") + "' should be after "
		    + "the test was begun. Thus '" + DateFormat
		    .getDateInstance().format(dbDate) + "' should be after '"
		    + DateFormat.getDateInstance().format(beforeTest) + "'.", 
		    dbDate.after(beforeTest));
	    assertTrue("The last missing file update for replica '" 
		    + Replica.getReplicaFromId("ONE") + "' should be before "
		    + "the current time. Thus '" + DateFormat
		    .getDateInstance().format(dbDate) + "' should be before '"
		    + DateFormat.getDateInstance().format(afterInsert) + "'.", 
		    dbDate.before(afterInsert));

	    // retrieve empty file and set all files in replica 'THREE' to missing
	    File fl2File = makeTemporaryEmptyFilelistFile();
	    cache.addFileListInformation(FileUtils.readListFromFile(fl2File), 
		    Replica.getReplicaFromId("THREE"));
	    
	    // check that all files are unknown for the uninitialised replica.
	    long files = FileUtils.countLines(csFile);
	    assertEquals("All the files for replica 'THREE' should be missing.",
		    files, cache.getNumberOfMissingFilesInLastUpdate(
			    Replica.getReplicaFromId("THREE")));

	    // check that the getMissingFilesInLastUpdate works appropriately. 
	    List<String> misFiles = IteratorUtils.toList(cache.getMissingFilesInLastUpdate(
		    Replica.getReplicaFromId("THREE")).iterator());

	    List<String> allFilenames = new ArrayList<String>();
	    for(String entry : FileUtils.readListFromFile(csFile)) {
		String[] e = entry.split("##");
		allFilenames.add(e[0]);
	    }
	    
	    assertEquals("All the files should be missing for replica 'THREE': "
		    + misFiles + " == " + allFilenames, misFiles, allFilenames);

	    // adding the checksum for the other replicas.
	    cache.addChecksumInformation(ChecksumEntry.parseChecksumJob(csFile), 
		    Replica.getReplicaFromId("TWO"));

	    // check that when a replica is given wrong checksums it will be 
	    // found by the update method.
	    assertEquals("Replica 'THREE' has not been assigned checksums yet."
		    + " Therefore not corrupt files yet!", 0, 
		    cache.getNumberOfWrongFilesInLastUpdate(Replica
			    .getReplicaFromId("THREE")));
	    assertEquals("No files has been assigned to replica 'THREE' yet.", 
		    0, cache.getNumberOfFiles(Replica.getReplicaFromId("THREE")));
	    
	    File csFile2 = makeTemporaryChecksumFile2();
	    cache.addChecksumInformation(ChecksumEntry.parseChecksumJob(csFile2), 
		    Replica.getReplicaFromId("THREE"));
	    assertEquals("All the files in Replica 'THREE' has been assigned "
		    + "checksums, but not checksum update has been run yet. "
		    + "Therefore no corrupt files yet!", 0, 
		    cache.getNumberOfWrongFilesInLastUpdate(Replica
			    .getReplicaFromId("THREE")));
	    assertEquals("Entries for replica 'THREE' has should be assigned.", 
		    FileUtils.countLines(csFile2), 
		    cache.getNumberOfFiles(Replica.getReplicaFromId("THREE")));

	    cache.updateChecksumStatus();
	    assertEquals("After update all the entries for replica 'THREE', "
	    		+ "they should all be set to 'CORRUPT'!", 
		    FileUtils.countLines(csFile2), 
		    cache.getNumberOfWrongFilesInLastUpdate(Replica
			    .getReplicaFromId("THREE")));
	    assertEquals("All the entries for replica 'THREE' is all corrupt, "
		    + "but they should still be counted.", 
		    FileUtils.countLines(csFile2), 
		    cache.getNumberOfFiles(Replica.getReplicaFromId("THREE")));

	    // Check that all files are wrong for replica 'THREE'
	    List<String> wrongFiles = IteratorUtils.toList(cache
		    .getWrongFilesInLastUpdate(Replica.getReplicaFromId("THREE"))
		    .iterator());
	    
	    assertEquals("All the files should be wrong for replica 'THREE': "
		    + wrongFiles + " == " + allFilenames, wrongFiles, allFilenames);
	    
	    
	    // check that a file can become missing, after it was ok, but still be ok!
	    cache.addFileListInformation(FileUtils.readListFromFile(flFile), 
		    Replica.getReplicaFromId("ONE"));
	    assertEquals("Replica 'ONE' had the files '" + allFilenames 
		    + "' before updating the filelist with '" 
		    + FileUtils.readListFromFile(flFile) + "'. Therefore one "
		    + "file should now be missing.", 1, 
		    cache.getNumberOfMissingFilesInLastUpdate(Replica.getReplicaFromId("ONE")));
	    assertEquals("Replica 'ONE' is missing 1 file, but since the "
		    + "checksum already is set to 'OK', then it is not CORRUPT", 
		    0, cache.getNumberOfWrongFilesInLastUpdate(Replica.getReplicaFromId("ONE")));

	    
	    // set replica THREE to having the same checksum as the other two,
	    // and update.
	    cache.addChecksumInformation(ChecksumEntry.parseChecksumJob(csFile),
		    Replica.getReplicaFromId("THREE"));
	    cache.updateChecksumStatus();
	    // reset the checksums of replica ONE, thus setting the 
	    // 'checksum_status' to UNKNOWN.
	    cache.addChecksumInformation(ChecksumEntry.parseChecksumJob(csFile), 
		    Replica.getReplicaFromId("ONE"));
	    
	    // Check that replica 'TWO' is found with good file.
	    assertEquals("Now only replica 'TWO' and 'THREE' both have "
		    + "checksum_status set to OK, and since replica 'TWO' is "
		    + "the only bitarchive, it should found when searching"
		    + "for replica with good file for the file 'TEST1'.", 
		    cache.getBitarchiveWithGoodFile("TEST1"), 
		    Replica.getReplicaFromId("TWO"));
	    
	    assertEquals("No bitarchive replica should be returned.",
		    null, cache.getBitarchiveWithGoodFile("TEST1", 
			    Replica.getReplicaFromId("TWO")));
	    
	    // print content
//	    cache.print();
	} catch (Exception e) {
	    throw new IOFailure("Unit test for ReplicaCacheDatabase failed!"
		    + " ERROR!!!!", e);
	}
    }
    
    private File makeTemporaryFilelistFile() throws Exception {
	File res = new File("filelist.out");
	FileWriter fw = new FileWriter(res);
	
	StringBuilder fileContent = new StringBuilder();
	fileContent.append("TEST1");
	fileContent.append("\n");
	fileContent.append("TEST2");
	fileContent.append("\n");
	fileContent.append("TEST3");
	fileContent.append("\n");
	
	fw.append(fileContent.toString());
	fw.flush();
	fw.close();
	
	return res;
    }
    
    private File makeTemporaryEmptyFilelistFile() throws Exception {
	File res = new File("filelist_empty.out");
	FileWriter fw = new FileWriter(res);
	
	StringBuilder fileContent = new StringBuilder("");
	
	fw.append(fileContent.toString());
	fw.flush();
	fw.close();
	
	return res;
    }

    
    private File makeTemporaryChecksumFile1() throws Exception {
	File res = new File("checksum_1.out");
	FileWriter fw = new FileWriter(res);
	
	StringBuilder fileContent = new StringBuilder();
	fileContent.append("TEST1##1234567890");
	fileContent.append("\n");
	fileContent.append("TEST2##0987654321");
	fileContent.append("\n");
	fileContent.append("TEST3##1029384756");
	fileContent.append("\n");
	fileContent.append("TEST4##0192837465");
	
	fw.append(fileContent.toString());
	fw.flush();
	fw.close();
	
	return res;
    }

    private File makeTemporaryChecksumFile2() throws Exception {
	File res = new File("checksum_2.out");
	FileWriter fw = new FileWriter(res);
	
	StringBuilder fileContent = new StringBuilder();
	fileContent.append("TEST1##ABCDEFGHIJ");
	fileContent.append("\n");
	fileContent.append("TEST2##JIHGFEDCBA");
	fileContent.append("\n");
	fileContent.append("TEST3##AJIBHCGDFE");
	fileContent.append("\n");
	fileContent.append("TEST4##JABICHDGEF");
	
	fw.append(fileContent.toString());
	fw.flush();
	fw.close();
	
	return res;
    }
   
    private void clearDatabase(Connection con) throws SQLException {
	    // clear the database.
        PreparedStatement statement = null;
        con.setAutoCommit(false);
        
        // Make the SQL statement for putting the replica into the database
        // and insert the variables for the entry to the replica table.
        // delete all entries in the 'replica' table.
        statement = con.prepareStatement("DELETE FROM replica ");
        statement.executeUpdate();
        // delete all entries in the 'file' table.
        statement = con.prepareStatement("DELETE FROM file ");
        statement.executeUpdate();
        // delete all entries in the 'replicafileinfo' table.
        statement = con.prepareStatement("DELETE FROM replicafileinfo ");
        statement.executeUpdate();
        // delete all entries in the 'segment' table.
        statement = con.prepareStatement("DELETE FROM segment ");
        statement.executeUpdate();
        con.commit();
    }
}
