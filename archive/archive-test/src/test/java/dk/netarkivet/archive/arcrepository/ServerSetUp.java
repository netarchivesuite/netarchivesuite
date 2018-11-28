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

import java.io.File;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.bitarchive.distribute.BitarchiveMonitorServer;
import dk.netarkivet.archive.bitarchive.distribute.BitarchiveServer;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Created by IntelliJ IDEA. User: csr Date: Mar 14, 2005 Time: 11:35:26 AM To change this template use File | Settings
 * | File Templates.
 * <p>
 * setup and teardown methods for connecting to two bitarchives
 */
public class ServerSetUp {
    /*
     * The head test directory
     */
    public static final File TEST_DIR = new File("tests/dk/netarkivet/archive/arcrepository/data/store");

    /** The directory used for controller admindata. */
    private static final File ADMINDATA_DIR = new File(TEST_DIR, "admindata");

    /** The bitarchive directory to work on. */
    static final File ARCHIVE_DIR = new File(TEST_DIR, "bitarchive");

    /** The directory used for storing temporary files */
    private static final File TEMP_DIR = new File(TEST_DIR, "tempdir");

    /** The bitarchive servers we need to communicate with. */
    static BitarchiveServer bitarchive;
    /** The bitarchive monitor we need to communicate with. */
    static BitarchiveMonitorServer bitarchiveMonitor;

    /** The arc repository. */
    private static ArcRepository arcRepos;

    static ReloadSettings rs = new ReloadSettings();

    protected static void setUp() {
        rs.setUp();
        Settings.set(ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN, ADMINDATA_DIR.getAbsolutePath());
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, ARCHIVE_DIR.getAbsolutePath());
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, TEMP_DIR.getAbsolutePath());

        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        JMSConnectionMockupMQ.clearTestQueues();

        Channels.reset();

        FileUtils.removeRecursively(ARCHIVE_DIR);
        FileUtils.removeRecursively(TEMP_DIR);
        FileUtils.removeRecursively(ADMINDATA_DIR);
        FileUtils.createDir(ADMINDATA_DIR);

        // Create a bit archive server that listens to archive events
        bitarchive = BitarchiveServer.getInstance();
        bitarchiveMonitor = BitarchiveMonitorServer.getInstance();
        arcRepos = ArcRepository.getInstance();
    }

    protected static void tearDown() {
        arcRepos.close();// close down ArcRepository Controller
        bitarchive.close();
        bitarchiveMonitor.close();

        FileUtils.removeRecursively(ADMINDATA_DIR);
        FileUtils.removeRecursively(ARCHIVE_DIR);
        FileUtils.removeRecursively(TEMP_DIR);

        JMSConnectionMockupMQ.clearTestQueues();
        rs.tearDown();
    }

    public static ArcRepository getArcRepository() {
        return arcRepos;
    }
}
