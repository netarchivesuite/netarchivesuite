/*
 * #%L
 * Netarchivesuite - archive - test
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.archive.arcrepository.bitpreservation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.arcrepositoryadmin.AdminData;
import dk.netarkivet.archive.arcrepositoryadmin.ArchiveDBConnection;
import dk.netarkivet.archive.arcrepositoryadmin.ReplicaCacheDatabase;
import dk.netarkivet.archive.arcrepositoryadmin.UpdateableAdminData;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.Channels;
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
import dk.netarkivet.common.utils.batch.ChecksumJob;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.harvester.datamodel.DatabaseTestUtils;
import dk.netarkivet.testutils.preconfigured.MockupJMS;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

@SuppressWarnings({"deprecation", "unused"})
// FIXME: @Ignore
@Ignore("test hangs")
public class DatabaseBasedActiveBitPreservationTester {

    private UseTestRemoteFile rf = new UseTestRemoteFile();
    private ReloadSettings rs = new ReloadSettings();
    private MockupJMS jmsConnection = new MockupJMS();
    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);

    DatabaseBasedActiveBitPreservation dbabp;

    Replica REPLICA_ONE = Replica.getReplicaFromId("ONE");
    Replica REPLICA_TWO = Replica.getReplicaFromId("TWO");
    Replica REPLICA_THREE = Replica.getReplicaFromId("THREE");

    @Before
    public void setUp() throws Exception {
        rs.setUp();
        Channels.reset();
        mtf.setUp();
        jmsConnection.setUp();
        rf.setUp();

        DatabaseTestUtils.createDatabase(TestInfo.DATABASE_FILE.getAbsolutePath(), TestInfo.DATABASE_DIR);

        Settings.set(ArchiveSettings.BASEURL_ARCREPOSITORY_ADMIN_DATABASE, TestInfo.DATABASE_URL);
        Settings.set(ArchiveSettings.MACHINE_ARCREPOSITORY_ADMIN_DATABASE, "");
        Settings.set(ArchiveSettings.PORT_ARCREPOSITORY_ADMIN_DATABASE, "");
        Settings.set(ArchiveSettings.DIR_ARCREPOSITORY_ADMIN_DATABASE, "");

        Settings.set(CommonSettings.ARC_REPOSITORY_CLIENT, MockupArcRepositoryClient.class.getName());
        Settings.set(ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN, TestInfo.WORKING_DIR.getAbsolutePath());
        Settings.set(ArchiveSettings.DIR_ARCREPOSITORY_BITPRESERVATION, TestInfo.WORKING_DIR.getAbsolutePath());
    }

    @After
    public void tearDown() throws Exception {
        // Make sure the ArcRepositoryClient is closed.
        ArcRepositoryClientFactory.getPreservationInstance().close();

        // Close the ActiveBitPreservation if it was instantiated.
        if (dbabp != null) {
            dbabp.close();
        }

        ArchiveDBConnection.cleanup();
        ReplicaCacheDatabase.getInstance().cleanup();

        rf.tearDown();
        mtf.tearDown();
        jmsConnection.tearDown();
        rs.tearDown();
    }

    /**
     * Test that it correctly identifies a missing file. This test assumes a clean database.
     *
     * @throws Exception If error!
     */
    @Test
    public void testMissingFiles() throws Exception {
        // initialise the database. Clean database and put new entries.

        Connection con = ArchiveDBConnection.get();
        try {
            clearDatabase(con);
        } finally {
            ArchiveDBConnection.release(con);
        }

        ReplicaCacheDatabase cache = ReplicaCacheDatabase.getInstance();
        File csFile1 = makeTemporaryChecksumFile1();
        cache.addChecksumInformation(csFile1, REPLICA_THREE);

        dbabp = DatabaseBasedActiveBitPreservation.getInstance();
        assertEquals("Replica '" + REPLICA_THREE + "' should not be missing any files.", 0,
                dbabp.getNumberOfMissingFiles(REPLICA_THREE));
        dbabp.findMissingFiles(REPLICA_TWO);

        assertEquals("Replica '" + REPLICA_TWO + "' should only be missing 1 file.", 1,
                dbabp.getNumberOfMissingFiles(REPLICA_TWO));
        assertEquals("Replica '" + REPLICA_TWO + "' should only be missing file '" + "integrity7.ARC" + '.',
                Arrays.asList("integrity7.ARC"), dbabp.getMissingFiles(REPLICA_TWO));

        assertEquals("Replica '" + REPLICA_THREE + "' should now be missing one file.", 1,
                dbabp.getNumberOfMissingFiles(REPLICA_THREE));
    }

    /**
     * Test that it correctly finds a changed file.
     *
     * @throws Exception if error.
     */
    @Test
    public void testChangedFiles() throws Exception {
        // This requires, that testMissingFiles has been run previously.
        // So therefore we run it here.
        testMissingFiles();
        // Comment (Mikis): This test appears to depend on MissingFiles test being run prior to this test.
        // So the comment stated at the top isn't strictly true. It would also be a serious side-effect of the
        // 'getInstance() methods that the database would be cleaned (or is the comment just obsolete??).
        // ReplicaCacheDatabase.getInstance();

        dbabp = DatabaseBasedActiveBitPreservation.getInstance();
        // dbabp.findMissingFiles(REPLICA_THREE);
        Date lastChangesFilesCheckDate = dbabp.getDateForMissingFiles(REPLICA_THREE);
        assertNotNull("The returned date should not be null", lastChangesFilesCheckDate);
        String stepMessage = "The date for last missing files check should be less than 30 min, but was: "
                + (Calendar.getInstance().getTimeInMillis() - lastChangesFilesCheckDate.getTime());
        assertTrue(stepMessage,
                Calendar.getInstance().getTimeInMillis() - lastChangesFilesCheckDate.getTime() < 1000 * 60 * 30);
        dbabp.findMissingFiles(REPLICA_THREE);

        // get checksum from all the first time.
        Date beforeCheckDate = new Date(Calendar.getInstance().getTimeInMillis());

        dbabp.findChangedFiles(REPLICA_ONE);
        Date afterCheckDate = new Date(Calendar.getInstance().getTimeInMillis());

        lastChangesFilesCheckDate = dbabp.getDateForChangedFiles(REPLICA_ONE);
        assertTrue(beforeCheckDate.before(lastChangesFilesCheckDate));
        assertTrue(afterCheckDate.after(lastChangesFilesCheckDate));

        assertEquals("Replica '" + REPLICA_THREE + "' should have 2 corrupted files", 2,
                dbabp.getNumberOfChangedFiles(REPLICA_THREE));
        assertEquals("Replica '" + REPLICA_THREE + "' should have corrupted the files "
                        + "'integrity11.ARC' and 'integrity12.ARC'.",
                Arrays.asList("integrity11.ARC", "integrity12.ARC"),
                dbabp.getChangedFiles(REPLICA_THREE));

        dbabp.replaceChangedFile(REPLICA_ONE, "integrity11.ARC", "XX", "399d2f9583da5516d7cdd4dfe3ed3b71");

        dbabp.replaceChangedFile(REPLICA_THREE, "integrity2.ARC", "XX", "b3bb49b72718b89950f8b861d2e0e2ca");

        Map<String, PreservationState> presMap = dbabp.getPreservationStateMap(new String[] {"integrity11.ARC"});

        assertEquals("The map should only contain a single element", 1, presMap.size());

        PreservationState pres = presMap.get("integrity11.ARC");
        assertNotNull("The preservation state should not be null.", pres);

        assertEquals("It should be upload completely, but not registret yet",
                ReplicaStoreState.UPLOAD_FAILED.toString(), pres.getAdminReplicaState(REPLICA_THREE));

        dbabp.findChangedFiles(REPLICA_THREE);

        pres = dbabp.getPreservationState("integrity11.ARC");
        assertEquals("It should be now be registreret as upload completely.",
                ReplicaStoreState.UPLOAD_COMPLETED.toString(), pres.getAdminReplicaState(REPLICA_THREE));

        try {
            dbabp.uploadMissingFiles(REPLICA_THREE, "integrity7.ARC");
            fail("It should not be allowed to upload a file, which is missing everywhere.");
        } catch (IOFailure e) {
            // expected.
        }

        // make replica THREE be missing 2 files (integrity11.ARC and integrity12.ARC)
        List<String> filelist = new ArrayList<String>();
        filelist.add("integrity1.ARC");
        filelist.add("integrity7.ARC");
        File tmpFile = File.createTempFile("file", "txt");
        FileUtils.writeCollectionToFile(tmpFile, filelist);

        ReplicaCacheDatabase.getInstance().addFileListInformation(tmpFile, REPLICA_THREE);
        FileUtils.remove(tmpFile);

        String misFiles = dbabp.getMissingFiles(REPLICA_THREE).toString();
        assertTrue("integrity2.ARC should be missing", misFiles.contains("integrity2.ARC"));
        assertTrue("integrity11.ARC should be missing", misFiles.contains("integrity11.ARC"));
        assertTrue("integrity12.ARC should be missing", misFiles.contains("integrity12.ARC"));

        // reupload the missing files.
        dbabp.uploadMissingFiles(REPLICA_THREE, "integrity2.ARC", "integrity12.ARC");

        // try to upload files, which does not exist anywhere!
        try {
            dbabp.uploadMissingFiles(REPLICA_THREE, "integrity11.ARC");
            fail("This should throw an IOFailure, since no bitarchive replica " + "should have the file.");
        } catch (IOFailure e) {
            // expected!
        }

        // send a filelist message which will tell that the file
        // 'integrity11.ARC' actually exists within the replica.
        dbabp.findMissingFiles(REPLICA_THREE);

        misFiles = dbabp.getMissingFiles(REPLICA_THREE).toString();
        assertFalse("integrity11.ARC should not be missing anymore", misFiles.contains("integrity11.ARC"));
        assertFalse("integrity12.ARC should not be missing anymore", misFiles.contains("integrity12.ARC"));
    }

    /**
     * Check whether it finds missing files from checksum jobs.
     */
    @Test
    public void testMissingDuringChecksum() throws Exception {
        Connection con = ArchiveDBConnection.get();
        try {
            clearDatabase(con);
        } finally {
            ArchiveDBConnection.release(con);
        }

        ReplicaCacheDatabase cache = ReplicaCacheDatabase.getInstance();
        dbabp = DatabaseBasedActiveBitPreservation.getInstance();

        // add a simple checksum list to replica TWO.
        List<String> checksumlist = new ArrayList<String>();
        checksumlist.add("1.arc##1234");
        checksumlist.add("2.arc##2345");

        File checksumFile = File.createTempFile("file", "txt");
        FileUtils.writeCollectionToFile(checksumFile, checksumlist);

        cache.addChecksumInformation(checksumFile, REPLICA_TWO);

        FileUtils.remove(checksumFile);

        // verify that all replicas has both files, and no 'wrong' entries.
        assertEquals("Unexpected number of files for " + REPLICA_TWO, 2, cache.getNumberOfFiles(REPLICA_TWO));
        assertEquals("Unexpected number of missing files for " + REPLICA_TWO, 0,
                cache.getNumberOfMissingFilesInLastUpdate(REPLICA_TWO));
        assertEquals("Unexpected number of wrong files for " + REPLICA_TWO, 0,
                cache.getNumberOfWrongFilesInLastUpdate(REPLICA_TWO));

        checksumlist.clear();
        checksumlist.add("1.arc##1234");
        checksumFile = File.createTempFile("file", "txt");
        FileUtils.writeCollectionToFile(checksumFile, checksumlist);
        cache.addChecksumInformation(checksumFile, REPLICA_TWO);
        FileUtils.remove(checksumFile);

        // verify that replica TWO is missing a file, but has no wrong files.
        assertEquals("Unexpected number of files for " + REPLICA_TWO, 1, cache.getNumberOfFiles(REPLICA_TWO));
        assertEquals("Unexpected number of missing files for " + REPLICA_TWO, 1,
                cache.getNumberOfMissingFilesInLastUpdate(REPLICA_TWO));
        assertEquals("Unexpected number of wrong files for " + REPLICA_TWO, 0,
                cache.getNumberOfWrongFilesInLastUpdate(REPLICA_TWO));
    }

    /**
     * Check that the bitpreservation factory works.
     */
    @Test
    public void testFactory() {
        ActiveBitPreservation abp = ActiveBitPreservationFactory.getInstance();

        assertTrue("ActiveBitPreservation default is currently " + FileBasedActiveBitPreservation.class.getName(),
                abp instanceof FileBasedActiveBitPreservation);

        Settings.set(ArchiveSettings.CLASS_ARCREPOSITORY_BITPRESERVATION,
                DatabaseBasedActiveBitPreservation.class.getName());

        ActiveBitPreservation abp2 = ActiveBitPreservationFactory.getInstance();
        assertTrue("ActiveBitPreservation should now be " + DatabaseBasedActiveBitPreservation.class.getName(),
                abp2 instanceof DatabaseBasedActiveBitPreservation);
    }

    @Test
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
        fileContent.append(ChecksumJob.makeLine("integrity1.ARC", "708afc1b7aebc12f7e65ecf1be054d23"));
        fileContent.append("\n");
        fileContent.append(ChecksumJob.makeLine("integrity7.ARC", "44ddf7a30f7fabb838e43a8505f927c2"));
        fileContent.append("\n");
        fileContent.append(ChecksumJob.makeLine("integrity11.ARC", "4236be8e67e0c10da2902764ff4b954a"));
        fileContent.append("\n");
        fileContent.append(ChecksumJob.makeLine("integrity12.ARC", "4236be8e67e0c10da2902764ff4b954a"));

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

        public BitarchiveRecord get(String arcfile, long index) throws ArgumentNotValid {
            if (overrideGet != null) {
                return overrideGet;
            }
            File file = new File(TestInfo.GOOD_ARCHIVE_FILE_DIR, arcfile);
            try {
                if (!file.exists()) {
                    return null;
                } else {
                    return new BitarchiveRecord((ARCRecord) ARCReaderFactory.get(file).get(index), arcfile);
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

        public BatchStatus batch(FileBatchJob job, String locationName, String... args) {
            if (overrideBatch != null) {
                return overrideBatch;
            }
            try {
                File output = File.createTempFile("Batch", ".dat", TestInfo.WORKING_DIR);
                File[] in_files = TestInfo.GOOD_ARCHIVE_FILE_DIR.listFiles();
                FileOutputStream os = new FileOutputStream(output);
                new BatchLocalFiles(in_files).run(job, os);
                os.close();
                // java 8
                //return new BatchStatus("BA1", Collections.<File>emptyList(), in_files.length,
                //        RemoteFileFactory.getMovefileInstance(output), new ArrayList<>(0));
                return new BatchStatus("BA1", Collections.<File>emptyList(),
                        in_files.length,
                        RemoteFileFactory.getMovefileInstance(output),
                        new ArrayList<FileBatchJob.ExceptionOccurrence>(0));
                
            } catch (IOException e) {
                fail("IO error during test");
                return null;
            }
        }

        public void updateAdminData(String fileName, String bitarchiveId, ReplicaStoreState newval) {
            UpdateableAdminData adminData = AdminData.getUpdateableInstance();
            if (!adminData.hasEntry(fileName)) {
                adminData.addEntry(fileName, null, "xx");
            }
            adminData.setState(fileName, bitarchiveId, newval);
        }

        public void updateAdminChecksum(String filename, String checksum) {
            UpdateableAdminData adminData = AdminData.getUpdateableInstance();
            adminData.setCheckSum(filename, checksum);
        }

        public File removeAndGetFile(String fileName, String bitarchiveName, String checksum, String credentials) {
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

                for (File file : files) {
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
        public File correct(String replicaId, String checksum, File file, String credentials) {
            return null; // this implementation is expected by the current tests
        }

        @Override
        public String getChecksum(String replicaId, String filename) {
            return null; // this implementation is expected by the current tests
        }
    }
}
