package dk.netarkivet.archive.arcrepository;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.bitarchive.distribute.BitarchiveMonitorServer;
import dk.netarkivet.archive.bitarchive.distribute.BitarchiveServer;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: csr
 * Date: Mar 14, 2005
 * Time: 11:35:26 AM
 * To change this template use File | Settings | File Templates.
 *
 *  setup and teardown methods for connecting to two bitarchives
 */
public class ServerSetUp {
    /*
     * The head test directory
     */
    public static final File TEST_DIR =
            new File("tests/dk/netarkivet/archive/arcrepository/data/store");

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

        ChannelsTester.resetChannels();

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
