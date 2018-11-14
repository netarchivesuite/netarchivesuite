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
package dk.netarkivet.archive.arcrepositoryadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.arcrepository.distribute.StoreMessage;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.PrintNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.datamodel.DatabaseTestUtils;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

public class DatabaseAdminTester {

    private ReloadSettings rs = new ReloadSettings();
    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR, TestInfo.TEST_DIR);

    private UseTestRemoteFile utrf = new UseTestRemoteFile();

    Replica ONE = Replica.getReplicaFromId("ONE");
    Replica TWO = Replica.getReplicaFromId("TWO");
    Replica THREE = Replica.getReplicaFromId("THREE");

    @Before
    public void setUp() throws Exception {
        Channels.reset();
        rs.setUp();
        mtf.setUp();
        utrf.setUp();

        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();

        DatabaseTestUtils.createDatabase(TestInfo.DATABASE_FILE.getAbsolutePath(), TestInfo.DATABASE_DIR);

        // define the settings for accessing the database
        Settings.set(ArchiveSettings.BASEURL_ARCREPOSITORY_ADMIN_DATABASE, TestInfo.DATABASE_URL);
        Settings.set(ArchiveSettings.MACHINE_ARCREPOSITORY_ADMIN_DATABASE, "");
        Settings.set(ArchiveSettings.PORT_ARCREPOSITORY_ADMIN_DATABASE, "");
        Settings.set(ArchiveSettings.DIR_ARCREPOSITORY_ADMIN_DATABASE, "");

        Settings.set(CommonSettings.NOTIFICATIONS_CLASS, PrintNotifications.class.getName());
    }

    @After
    public void tearDown() {
        JMSConnectionMockupMQ.clearTestQueues();
        utrf.setUp();
        mtf.tearDown();
        rs.tearDown();
    }

    /** Disabled, fails on Jenkins */
    @Test
    @Ignore("FIXME")
    // FIXME: test temporarily disabled
    public void failingTestArcRepositoryCalls() {
        DatabaseAdmin da = DatabaseAdmin.getInstance();

        assertFalse("Should not contain NON-EXISTING-FILE", da.hasEntry("NON-EXISTING-FILE"));
        assertFalse("Should not contain StoreMessage for NON-EXISTING-FILE", da.hasReplyInfo("NON-EXISTING-FILE"));

        StoreMessage storeMsg1 = new StoreMessage(Channels.getError(), TestInfo.TEST_FILE_1);
        JMSConnectionMockupMQ.updateMsgID(storeMsg1, "store1");

        da.addEntry(TestInfo.TEST_FILE_1.getName(), storeMsg1, "1234567890");

        // make sure that the test instance can now be found.
        assertTrue("Should contain " + TestInfo.TEST_FILE_1.getName(), da.hasEntry(TestInfo.TEST_FILE_1.getName()));
        assertTrue("Should have replyInfo for " + TestInfo.TEST_FILE_1.getName(),
                da.hasReplyInfo(TestInfo.TEST_FILE_1.getName()));
        assertEquals("Should have the given checksum", "1234567890", da.getCheckSum(TestInfo.TEST_FILE_1.getName()));

        // Test the hasState.
        assertFalse("Should not yet have a acceptable state",
                da.hasState(TestInfo.TEST_FILE_1.getName(), ONE.getIdentificationChannel().getName()));
        da.setState(TestInfo.TEST_FILE_1.getName(), ONE.getIdentificationChannel().getName(),
                ReplicaStoreState.UPLOAD_STARTED);
        assertTrue("Should now have a acceptable state",
                da.hasState(TestInfo.TEST_FILE_1.getName(), ONE.getIdentificationChannel().getName()));
        assertEquals("Should have the same state",
                da.getState(TestInfo.TEST_FILE_1.getName(), ONE.getIdentificationChannel().getName()),
                ReplicaStoreState.UPLOAD_STARTED);

        StoreMessage storeMsg2 = new StoreMessage(Channels.getError(), TestInfo.TEST_FILE_1);
        JMSConnectionMockupMQ.updateMsgID(storeMsg2, "store2");

        da.setReplyInfo(storeMsg2.getArcfileName(), storeMsg2);

        da.setState(TestInfo.TEST_FILE_1.getName(), ONE.getIdentificationChannel().getName(),
                ReplicaStoreState.UPLOAD_COMPLETED);
        da.setState(TestInfo.TEST_FILE_1.getName(), TWO.getIdentificationChannel().getName(),
                ReplicaStoreState.UPLOAD_COMPLETED);
        da.setState(TestInfo.TEST_FILE_1.getName(), THREE.getIdentificationChannel().getName(),
                ReplicaStoreState.UPLOAD_COMPLETED);

        StoreMessage retrievedMsg = da.removeReplyInfo(TestInfo.TEST_FILE_1.getName());

        assertEquals("The message should have the new ID.", "store2", retrievedMsg.getID());

        try {
            da.setCheckSum(TestInfo.TEST_FILE_1.getName(), "0987654321");
            fail("An illegal state exception should be thrown here.");
        } catch (IllegalState e) {
            // expected
        }

        Set<String> filenames = da.getAllFileNames();
        assertTrue("Should contain the file '" + TestInfo.TEST_FILE_1.getName() + "' but was '" + filenames,
                filenames.contains(TestInfo.TEST_FILE_1.getName()));

        filenames = da.getAllFileNames(THREE, ReplicaStoreState.UPLOAD_FAILED);
        assertTrue("The list of files with state UPLOAD_FAILED for replica " + "THREE should be empty, but it was: "
                + filenames, filenames.isEmpty());

        filenames = da.getAllFileNames(THREE, ReplicaStoreState.UPLOAD_COMPLETED);
        String assertMessage = "The list of files with state UPLOAD_COMPLETED for replica THREE should contain the " +
                "file: '" + TestInfo.TEST_FILE_1.getName() + "', but it contained: " + filenames;
        assertTrue(assertMessage, filenames.contains(TestInfo.TEST_FILE_1.getName()));
    }

}
