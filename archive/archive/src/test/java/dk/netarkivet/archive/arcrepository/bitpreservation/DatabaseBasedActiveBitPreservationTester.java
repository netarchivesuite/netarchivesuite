/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
package dk.netarkivet.archive.arcrepository.bitpreservation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.arcrepositoryadmin.AdminData;
import dk.netarkivet.archive.arcrepositoryadmin.DBConnect;
import dk.netarkivet.archive.arcrepositoryadmin.ReplicaCacheDatabase;
import dk.netarkivet.archive.arcrepositoryadmin.UpdateableAdminData;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.BatchLocalFiles;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.testutils.ClassAsserts;
import dk.netarkivet.testutils.DatabaseTestUtils;
import dk.netarkivet.testutils.preconfigured.MockupJMS;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

public class DatabaseBasedActiveBitPreservationTester extends TestCase {

    private UseTestRemoteFile rf = new UseTestRemoteFile();
    private ReloadSettings rs = new ReloadSettings();
    private MockupJMS jmsConnection = new MockupJMS();
    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR,
            TestInfo.WORKING_DIR);
    
    DatabaseBasedActiveBitPreservation dbabp;
    
    Replica ONE = Replica.getReplicaFromId("ONE");
    Replica TWO = Replica.getReplicaFromId("TWO");
    Replica THREE = Replica.getReplicaFromId("THREE");

    static boolean first = true;
    
    public void setUp() throws Exception {
	super.setUp();
        rs.setUp();
        ChannelsTester.resetChannels();
        mtf.setUp();
        jmsConnection.setUp();
        rf.setUp();
        
        DatabaseTestUtils.takeDatabase(TestInfo.DATABASE_FILE, 
                TestInfo.DATABASE_DIR);


        Settings.set(ArchiveSettings.BASEURL_ARCREPOSITORY_ADMIN_DATABASE,
                TestInfo.DATABASE_URL);
        Settings.set(ArchiveSettings.MACHINE_ARCREPOSITORY_ADMIN_DATABASE,
                "");
        Settings.set(ArchiveSettings.PORT_ARCREPOSITORY_ADMIN_DATABASE,
                "");
        Settings.set(ArchiveSettings.DIR_ARCREPOSITORY_ADMIN_DATABASE,
                "");

        Settings.set(CommonSettings.ARC_REPOSITORY_CLIENT,
                     MockupArcRepositoryClient.class.getName());
        Settings.set(ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN, 
        	TestInfo.WORKING_DIR.getAbsolutePath());
        Settings.set(ArchiveSettings.DIR_ARCREPOSITORY_BITPRESERVATION, 
        	TestInfo.WORKING_DIR.getAbsolutePath());
        
        if(first) {
            first = false;
            clearDatabase(DBConnect.getDBConnection(Settings.get(
                    ArchiveSettings.BASEURL_ARCREPOSITORY_ADMIN_DATABASE)));
        }
    }
    
    public void tearDown() throws Exception {
        // Make sure the ArcRepositoryClient is closed.
        ArcRepositoryClientFactory.getPreservationInstance().close();

        // Close the ActiveBitPreservation if it was instantiated.
        if (dbabp != null) {
            dbabp.close();
        }
        
        rf.tearDown();
        mtf.tearDown();
        jmsConnection.tearDown();
        rs.tearDown();
        super.tearDown();
    }

    /**
     * Check that this is a singleton.
     */
    public void testSingleton() {
	ClassAsserts.assertSingleton(DatabaseBasedActiveBitPreservation.class);
    }
    
    /**
     * Check that the bitpreservation factory works.
     */
    public void testFactory() {
	ActiveBitPreservation abp = ActiveBitPreservationFactory.getInstance();
	
	assertTrue("ActiveBitPreservation default is currently " 
		+ FileBasedActiveBitPreservation.class.getName(), 
		abp instanceof FileBasedActiveBitPreservation);
	
	Settings.set(ArchiveSettings.CLASS_ARCREPOSITORY_BITPRESERVATION, 
		DatabaseBasedActiveBitPreservation.class.getName());
	
	ActiveBitPreservation abp2 = ActiveBitPreservationFactory.getInstance();
	assertTrue("ActiveBitPreservation should now be " 
		+ DatabaseBasedActiveBitPreservation.class.getName(), 
		abp2 instanceof DatabaseBasedActiveBitPreservation);
    }
    
    /**
     * Test that it correctly identifies a missing file.
     * 
     * @throws Exception If error!
     */
    public void testMissingFiles() throws Exception {
	// initialise the database. Clean database and put new intries.
	ReplicaCacheDatabase cache = ReplicaCacheDatabase.getInstance();
	File csFile1 = makeTemporaryChecksumFile1();
	cache.addChecksumInformation(FileUtils.readListFromFile(csFile1), THREE);

	dbabp = DatabaseBasedActiveBitPreservation.getInstance();
	assertEquals("Replica '" + THREE + "' should initially not be missing any files.",
			0, dbabp.getNumberOfMissingFiles(THREE));
	dbabp.findMissingFiles(TWO);
	
	assertEquals("Replica '" + TWO + "' should only be missing 1 file.", 
		1, dbabp.getNumberOfMissingFiles(TWO));
	assertEquals("Replica '" + TWO + "' should only be missing file '"
		+ "integrity7.ARC" + "'.", Arrays.asList("integrity7.ARC"), 
		dbabp.getMissingFiles(TWO));
	
	assertEquals("Replica '" + THREE + "' should now have a missing file.",
			1, dbabp.getNumberOfMissingFiles(THREE));
    }
    
    /**
     * Test that it correctly finds a changed file.
     * 
     * @throws Exception if error.
     */
    public void testChangedFiles() throws Exception {
    // initialise the database. Clean database and put new entries.
    ReplicaCacheDatabase.getInstance();

    dbabp = DatabaseBasedActiveBitPreservation.getInstance();
    Date date = dbabp.getDateForMissingFiles(THREE);
    assertTrue("The date for last missing files check should be less than 30 min, but was: " 
            + (Calendar.getInstance().getTimeInMillis() - date.getTime()), 
            Calendar.getInstance().getTimeInMillis() - date.getTime() < 1000*60*30);
    dbabp.findMissingFiles(THREE);
    
        
    // get checksum from all the first time.
    Date beforeUpdate = new Date(Calendar.getInstance().getTimeInMillis());
    
    dbabp.findChangedFiles(ONE);

        date = dbabp.getDateForChangedFiles(ONE);
        assertTrue("The date for last changed files check for replica THREE after "
                + beforeUpdate.getTime() + " but was " + date.getTime(), 
                date.getTime() > beforeUpdate.getTime());
        assertTrue("The date for last changed files check for replica THREE before now "
                + Calendar.getInstance().getTimeInMillis() + " but was " + date.getTime(), 
                date.getTime() < Calendar.getInstance().getTimeInMillis());
        
       
    assertEquals("Replica '" + THREE + "' should have 2 corrupted files", 
        2, dbabp.getNumberOfChangedFiles(THREE));
    assertEquals("Replica '" + THREE + "' should have corrupted the files "
        + "'integrity11.ARC' and 'integrity12.ARC'.", 
        Arrays.asList("integrity11.ARC", "integrity12.ARC"), 
        dbabp.getChangedFiles(THREE));

    
    dbabp.replaceChangedFile(ONE, "integrity11.ARC", "XX", 
        "399d2f9583da5516d7cdd4dfe3ed3b71");
    
    dbabp.replaceChangedFile(THREE, "integrity2.ARC", "XX", 
        "b3bb49b72718b89950f8b861d2e0e2ca");
    
    Map<String, PreservationState> presMap = dbabp.getPreservationStateMap(
            new String[]{"integrity11.ARC"});
    
    assertEquals("The map should only contain a single element",
            1, presMap.size());
    
    PreservationState pres = presMap.get("integrity11.ARC");
    assertNotNull("The preservation state should not be null.", pres);

    assertEquals("It should be upload completely, but not registret yet", 
            ReplicaStoreState.UPLOAD_FAILED.toString(), pres.getAdminReplicaState(THREE));
    
    dbabp.findChangedFiles(THREE);
    
    pres = dbabp.getPreservationState("integrity11.ARC");
        assertEquals("It should be now be registreret as upload completely.", 
                ReplicaStoreState.UPLOAD_COMPLETED.toString(), pres.getAdminReplicaState(THREE));
        

        try {
            dbabp.uploadMissingFiles(THREE, "integrity7.ARC");
            fail("It should not be allowed to upload a file, which is missing everywhere.");
        } catch (IOFailure e) {
            // expected.
        }

        // make replica THREE be missing 2 files (integrity11.ARC and integrity12.ARC)
        List<String> filelist = new ArrayList<String>();
        filelist.add("integrity1.ARC");
        filelist.add("integrity7.ARC");
        ReplicaCacheDatabase.getInstance().addFileListInformation(filelist, THREE);
        
        String misFiles = dbabp.getMissingFiles(THREE).toString();
        assertTrue("integrity2.ARC should be missing", 
                misFiles.contains("integrity2.ARC"));
        assertTrue("integrity11.ARC should be missing", 
                misFiles.contains("integrity11.ARC"));
        assertTrue("integrity12.ARC should be missing", 
                misFiles.contains("integrity12.ARC"));
        
        // reupload the missing files.
        dbabp.uploadMissingFiles(THREE, "integrity2.ARC", "integrity12.ARC");
        
        // try to upload files, which does not exist anywhere!
        try {
            dbabp.uploadMissingFiles(THREE, "integrity11.ARC");
            fail("This should throw an IOFailure, since no bitarchive replica "
                    + "should have the file.");
        } catch (IOFailure e) {
            // expected!
        }
        
        // send a filelist message which will tell that the file 
        // 'integrity11.ARC' actually exists within the replica.
        dbabp.findMissingFiles(THREE);
        
        misFiles = dbabp.getMissingFiles(THREE).toString();
        assertFalse("integrity11.ARC should not be missing anymore", 
                misFiles.contains("integrity11.ARC"));
        assertFalse("integrity12.ARC should not be missing anymore", 
                misFiles.contains("integrity12.ARC"));
    }
        
    /**
     * Check whether it finds missing files from checksum jobs.
     */
    public void testMissingDuringChecksum() throws Exception {
        ReplicaCacheDatabase.getInstance().cleanup();
        clearDatabase(DBConnect.getDBConnection(Settings.get(
                ArchiveSettings.BASEURL_ARCREPOSITORY_ADMIN_DATABASE)));

        ReplicaCacheDatabase cache = ReplicaCacheDatabase.getInstance();
        dbabp = DatabaseBasedActiveBitPreservation.getInstance();
        
        // add a simple checksum list to replica TWO.
        List<String> checksumlist = new ArrayList<String>();
        checksumlist.add("1.arc##1234");
        checksumlist.add("2.arc##2345");
        
        cache.addChecksumInformation(checksumlist, TWO);

        // verify that all replicas has both files, and no 'wrong' entries.
        assertEquals("Unexpected number of files for " + TWO, 2, cache.getNumberOfFiles(TWO));
        assertEquals("Unexpected number of missing files for " + TWO, 0, cache.getNumberOfMissingFilesInLastUpdate(TWO));
        assertEquals("Unexpected number of wrong files for " + TWO, 0, cache.getNumberOfWrongFilesInLastUpdate(TWO));
        
        checksumlist.clear();
        checksumlist.add("1.arc##1234");

        cache.addChecksumInformation(checksumlist, TWO);        

        // verify that replica TWO is missing a file, but has no wrong files.
        assertEquals("Unexpected number of files for " + TWO, 1, cache.getNumberOfFiles(TWO));
        assertEquals("Unexpected number of missing files for " + TWO, 1, cache.getNumberOfMissingFilesInLastUpdate(TWO));
        assertEquals("Unexpected number of wrong files for " + TWO, 0, cache.getNumberOfWrongFilesInLastUpdate(TWO));
    }

    
    public void testFails() {
        dbabp = DatabaseBasedActiveBitPreservation.getInstance();
        
        try {
            dbabp.changeStateForAdminData("ONE");
            fail("This should not be allowed");
        } catch (Throwable e) {
            // expected
        }
        
        try {
            dbabp.getMissingFilesForAdminData();
            fail("This should not be allowed");
        } catch (Throwable e) {
            // expected
        }

        try {
            dbabp.getChangedFilesForAdminData();
            fail("This should not be allowed");
        } catch (Throwable e) {
            // expected
        }

        try {
            dbabp.addMissingFilesToAdminData("ONE");
            fail("This should not be allowed");
        } catch (Throwable e) {
            // expected
        }

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
    
    
    private File makeTemporaryChecksumFile1() throws Exception {
	File res = new File("checksum_1.out");
	FileWriter fw = new FileWriter(res);
	
	StringBuilder fileContent = new StringBuilder();
	fileContent.append(ChecksumJob.makeLine("integrity1.ARC", 
		"708afc1b7aebc12f7e65ecf1be054d23"));
	fileContent.append("\n");
	fileContent.append(ChecksumJob.makeLine("integrity7.ARC", 
	"44ddf7a30f7fabb838e43a8505f927c2"));
	fileContent.append("\n");
	fileContent.append(ChecksumJob.makeLine("integrity11.ARC", 
	"4236be8e67e0c10da2902764ff4b954a"));
	fileContent.append("\n");
	fileContent.append(ChecksumJob.makeLine("integrity12.ARC", 
	"4236be8e67e0c10da2902764ff4b954a"));
	
	fw.append(fileContent.toString());
	fw.flush();
	fw.close();
	
	return res;
    }
    
    public static class MockupArcRepositoryClient implements ArcRepositoryClient {
        private static MockupArcRepositoryClient instance;
        private BitarchiveRecord overrideGet;
        private File overrideGetFile;
        private File overrideStore;
        private BatchStatus overrideBatch;
        private File overrideRemoveAndGetFile;

        public static MockupArcRepositoryClient getInstance() {
            if (instance == null) {
                instance = new MockupArcRepositoryClient();
            }
            return instance;
        }

        public void close() {
            instance = null;
        }

        public BitarchiveRecord get(String arcfile, long index)
        throws ArgumentNotValid {
            if (overrideGet != null) {
                return overrideGet;
            }
            File file = new File(TestInfo.GOOD_ARCHIVE_FILE_DIR, arcfile);
            try {
                if (!file.exists()) {
                    return null;
                } else {
                    return new BitarchiveRecord(
                            (ARCRecord) ARCReaderFactory.get(file).get(index));
                }
            } catch (IOException e) {
                fail("Test failure while reading file '" + file + "'");
                return null;
            }
        }

        public void getFile(String arcfilename, Replica replica, File toFile) {
            if (overrideGetFile != null) {
                FileUtils.copyFile(overrideGetFile, toFile);
                return;
            }
            File file = new File(TestInfo.GOOD_ARCHIVE_FILE_DIR, arcfilename);
            FileUtils.copyFile(file, toFile);
        }

        public void store(File file) throws IOFailure, ArgumentNotValid {
            File f = new File(TestInfo.GOOD_ARCHIVE_FILE_DIR, file.getName());
            if (overrideStore != null) {
                FileUtils.copyFile(overrideStore, f);
                return;
            }
            FileUtils.copyFile(file, f);
        }

        public BatchStatus batch(FileBatchJob job, String locationName) {
            if (overrideBatch != null) {
                return overrideBatch;
            }
            try {
                File output = File.createTempFile("Batch", ".dat", TestInfo.WORKING_DIR);
                File[] in_files = TestInfo.GOOD_ARCHIVE_FILE_DIR.listFiles();
                FileOutputStream os = new FileOutputStream(output);
                new BatchLocalFiles(in_files).run(job, os);
                os.close();
                return new BatchStatus("BA1", Collections.<File>emptyList(),
                        in_files.length,
                        RemoteFileFactory.getMovefileInstance(output),
                        new ArrayList<FileBatchJob.ExceptionOccurrence>(0));
            } catch (IOException e) {
                fail("IO error during test");
                return null;
            }
        }

        public void updateAdminData(String fileName, String bitarchiveId,
                                    ReplicaStoreState newval) {
            UpdateableAdminData adminData
                    = AdminData.getUpdateableInstance();
            if (!adminData.hasEntry(fileName)) {
                adminData.addEntry(fileName, null, "xx");
            }
            adminData.setState(fileName, bitarchiveId, newval);
        }

        public void updateAdminChecksum(String filename, String checksum) {
            UpdateableAdminData adminData
                    = AdminData.getUpdateableInstance();
            adminData.setCheckSum(filename, checksum);
        }

        public File removeAndGetFile(String fileName, String bitarchiveName,
                                     String checksum, String credentials) {
            if (overrideRemoveAndGetFile != null) {
                return overrideRemoveAndGetFile;
            }
            File output = null;
            try {
                output = File.createTempFile(fileName, ".removed", TestInfo.WORKING_DIR);
            } catch (IOException e) {
                fail("IO error during test.");
            }
            if (!credentials.equals("XX")) {
                throw new PermissionDenied("Credentials are not XX");
            }
            FileUtils.copyFile(new File(TestInfo.GOOD_ARCHIVE_FILE_DIR, fileName), output);
            return output;
        }

	@Override
	public File getAllChecksums(String replicaId) {
	    try {
	        ChecksumJob job = new ChecksumJob();
	        File output = File.createTempFile("checksum", ".all", TestInfo.WORKING_DIR);
	        File[] in_files = TestInfo.GOOD_ARCHIVE_FILE_DIR.listFiles();
	        FileOutputStream os = new FileOutputStream(output);
	        new BatchLocalFiles(in_files).run(job, os);
	        os.close();
	        return output;
	    } catch (IOException e) {
	        throw new IOFailure("", e);
	    }
	}

	@Override
	public File getAllFilenames(String replicaId) {
	    try {
		File result = File.createTempFile("temp", "temp");
		FileWriter fw = new FileWriter(result);
		File[] files = TestInfo.GOOD_ARCHIVE_FILE_DIR.listFiles();

		for(File file : files) {
		    fw.append(file.getName());
		    fw.append("\n");
		}

		fw.flush();
		fw.close();

		return result;
	    } catch (IOException e) {
		throw new IOFailure("Cannot get all filenames!", e);
	    }
	}

	@Override
	public File correct(String replicaId, String checksum, File file, 
		String credentials) {
	    // TODO: something!
	    return null;
	}

    @Override
    public String getChecksum(String replicaId, String filename) {
        // TODO Auto-generated method stub
        return null;
    }
    }
}
