/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import junit.framework.TestCase;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.arcrepositoryadmin.AdminData;
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
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.BatchLocalFiles;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.testutils.ClassAsserts;
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

        if(first) {
            first = false;
            clearDatabase(DBConnect.getDBConnection());
//            initChecksumReplica();
        }
        
        
        Settings.set(CommonSettings.ARC_REPOSITORY_CLIENT,
                     MockupArcRepositoryClient.class.getName());
        Settings.set(ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN, 
        	TestInfo.WORKING_DIR.getAbsolutePath());
        Settings.set(ArchiveSettings.DIR_ARCREPOSITORY_BITPRESERVATION, 
        	TestInfo.WORKING_DIR.getAbsolutePath());
    }
    
    public void tearDown() throws Exception {
        // Make sure the ArcRepositoryClient is closed.
        ArcRepositoryClientFactory.getPreservationInstance().close();

        // Close the ActiveBitPreservation if it was instantiated.
        if (dbabp != null) {
            dbabp.close();
        }
        
//        MockupArcRepositoryClient.instance = null;

        rf.tearDown();
        mtf.tearDown();
        jmsConnection.tearDown();
        rs.tearDown();
        super.tearDown();
    }

    public void testSingleton() {
	ClassAsserts.assertSingleton(DatabaseBasedActiveBitPreservation.class);
    }
    
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
    
    public void testMissingFiles() throws Exception {
	// initialise the database. Clean database and put new intries.
	ReplicaCacheDatabase cache = ReplicaCacheDatabase.getInstance();
	File csFile1 = makeTemporaryChecksumFile1();
	cache.addChecksumInformation(ChecksumEntry.parseChecksumJob(
		csFile1), THREE);

	dbabp = DatabaseBasedActiveBitPreservation.getInstance();
	dbabp.findMissingFiles(TWO);
	
	assertEquals("Replica '" + TWO + "' should only be missing 1 file.", 
		1, dbabp.getNumberOfMissingFiles(TWO));
	assertEquals("Replica '" + TWO + "' should only be missing file '"
		+ "integrity7.ARC" + '.', Arrays.asList("integrity7.ARC"), 
		dbabp.getMissingFiles(TWO));
	
	assertEquals("Replica '" + THREE + "' should not be missing any files.",
		0, dbabp.getNumberOfMissingFiles(THREE));
    }
    
    public void testChangedFiles() throws Exception {
	// initialise the database. Clean database and put new intries.
	ReplicaCacheDatabase cache = ReplicaCacheDatabase.getInstance();

	dbabp = DatabaseBasedActiveBitPreservation.getInstance();
	dbabp.findMissingFiles(THREE);
	
	// get checksum from all the first time.
	dbabp.findChangedFiles(ONE);

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
	    
    }

    /**
     * Test normal behaviour of runFileListJob():
     *
     * It should pick normal or reference dir. It should generate the correct
     * files. It should restrict itself to specified files. It should check the
     * number of lines. It should remove the temporary file.
     *
     * Note that we don't need to test if the expected files are found, as the
     * file scanning is done in submethods, but it comes automatically when we
     * check for restriction.
     *
     * @throws IOException
     */
/*    public void testRunFileListJob() throws IOException, NoSuchMethodException,
                                            InvocationTargetException,
                                            IllegalAccessException {
        Method runFilelistJob = ReflectUtils.getPrivateMethod(
                DatabaseBasedActiveBitPreservation.class, "getChecksumList",
                Replica.class);

        DatabaseBasedActiveBitPreservation dbabp = DatabaseBasedActiveBitPreservation.getInstance();

        // Check normal run
        final String replicaId = TestInfo.REPLICA_ID_TWO;
        Replica replica = Replica.getReplicaFromId(replicaId);
        //final String otherLocationName = TestInfo.OTHER_LOCATION_NAME;
        // Location otherLocation = Location.get(otherLocationName);
        runFilelistJob.invoke(dbabp, TWO);
        File normalOutputFile =
                WorkFiles.getFile(replica, WorkFiles.FILES_ON_BA);
        File referenceOutputFile =
                WorkFiles.getFile(replica, WorkFiles.FILES_ON_REFERENCE_BA);
        System.out.println(normalOutputFile.getAbsolutePath());
        assertTrue("Output should exist", normalOutputFile.exists());
        assertFalse("Reference output should not exist",
                    referenceOutputFile.exists());
        normalOutputFile.delete();
/*
        // Check that wrong counts are caught
        MockupArcRepositoryClient.getInstance().overrideBatch
                = new BatchStatus("AP1", Collections.<File>emptyList(), 17,
                        RemoteFileFactory.getMovefileInstance(
                                new File(TestInfo.WORKING_DIR, 
                                        "test_file_list_output/"
                                        + "filelistOutput/unsorted.txt")),
                                        new ArrayList<FileBatchJob
                                            .ExceptionOccurrence>(0));
        runFilelistJob.invoke(dbabp, replica);
        LogUtils.flushLogs(FileBasedActiveBitPreservation.class.getName());
        FileAsserts.assertFileContains("Should have warning about wrong count",
                                       "Number of files found (" + 6
                                       + ") does not"
                                       + " match with number reported by job (17)",
                                       TestInfo.LOG_FILE);

        dbabp.close();
// */
//    }
    
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
    
/*
    private void initChecksumReplica() {
	try {
	    FileWriter fw = new FileWriter(TestInfo.CHECKSUM_FILE);

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
	    fileContent.append("\n");

	    fw.append(fileContent.toString());
	    fw.flush();
	    fw.close();
	} catch (IOException e) {
	    throw new IOFailure("Cannot initialise checksum file", e);
	}
    }
*/
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
	    // TODO Auto-generated method stub
	    throw new NotImplementedException("TODO: Implement me!");
	}

	@Override
	public File getAllFilenames(String replicaId) {
	    
	    // TODO Auto-generated method stub
//	    throw new NotImplementedException("TODO: Implement me!");
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
	public void correct(String replicaId, String checksum, File file, 
		String credentials) {
	    // TODO: something!
	    
	}
    }
}
