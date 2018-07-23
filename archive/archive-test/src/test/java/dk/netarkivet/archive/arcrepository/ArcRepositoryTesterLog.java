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
package dk.netarkivet.archive.arcrepository;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import dk.netarkivet.archive.arcrepository.distribute.StoreMessage;
import dk.netarkivet.archive.arcrepositoryadmin.AdminData;
import dk.netarkivet.archive.arcrepositoryadmin.UpdateableAdminData;
import dk.netarkivet.archive.bitarchive.BitarchiveAdmin;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.utils.ChecksumCalculator;
import dk.netarkivet.testutils.LogbackRecorder;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

@SuppressWarnings({"deprecation"})
public class ArcRepositoryTesterLog {
    protected final Logger log = LoggerFactory.getLogger(ArcRepositoryTesterLog.class);
    private UseTestRemoteFile rf = new UseTestRemoteFile();
    private static final File TEST_DIR = new File("tests/dk/netarkivet/archive/arcrepository/data");

    /** The directory storing the arcfiles in the already existing bitarchive - including credentials and admin-files. */
    private static final File ORIGINALS_DIR = new File(new File(TEST_DIR, "logging"), "originals");

    private LogbackRecorder logbackRecorder;

    @Before
    public void setUp() throws IOException {
        ServerSetUp.setUp();
        rf.setUp();
        logbackRecorder = LogbackRecorder.startRecorder();
    }

    @After
    public void tearDown() {
        rf.tearDown();
        ServerSetUp.tearDown();
        logbackRecorder.stopRecorder();
    }

    /**
     * Test logging of store command.
     */
    @Test
    public void testLogStore() throws Exception {
        final String FILE_NAME =  "logging1.ARC";
        File f = new File(ORIGINALS_DIR, FILE_NAME);
        BitarchiveAdmin admin = BitarchiveAdmin.getInstance();
        assertNotNull("Must have admin object.", admin);
        if (!admin.hasEnoughSpace()) {
        	System.err.println("Skipping test. Not enough space on disk to perform test");
        	return;
        }
        UpdateableAdminData adminData = AdminData.getUpdateableInstance();
        adminData.addEntry(f.getName(), new StoreMessage(Channels.getThisReposClient(), f),
                ChecksumCalculator.calculateMd5(f));
        adminData.setState(f.getName(), Channels.retrieveReplicaChannelFromReplicaId("TWO").getName(),
                ReplicaStoreState.UPLOAD_COMPLETED);
        adminData.setState(f.getName(), Channels.retrieveReplicaChannelFromReplicaId("THREE").getName(),
                ReplicaStoreState.UPLOAD_COMPLETED);
        
        StoreMessage msg = new StoreMessage(Channels.getError(), f);
        ServerSetUp.getArcRepository().store(msg.getRemoteFile(), msg);
        UploadWaiting.waitForUpload(f, this);
        logbackRecorder.assertLogContains(Level.INFO, FILE_NAME);
        logbackRecorder.assertLogContains(Level.INFO, "Store started");
    }
}
