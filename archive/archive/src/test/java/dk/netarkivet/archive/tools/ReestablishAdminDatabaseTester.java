package dk.netarkivet.archive.tools;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.arcrepositoryadmin.DBConnect;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import junit.framework.TestCase;

public class ReestablishAdminDatabaseTester extends TestCase {
    private PreventSystemExit pse = new PreventSystemExit();
    private PreserveStdStreams pss = new PreserveStdStreams(true);
    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.DATA_DIR,
            TestInfo.WORKING_DIR);
    ReloadSettings rs = new ReloadSettings();

    public void setUp() {
        DBConnect.cleanup();
        rs.setUp();
        mtf.setUp();
        pss.setUp();
        pse.setUp();
        
        Settings.set(ArchiveSettings.BASEURL_ARCREPOSITORY_ADMIN_DATABASE, 
                TestInfo.DATABASE_URL);
        Settings.set(ArchiveSettings.MACHINE_ARCREPOSITORY_ADMIN_DATABASE,
                "");
        Settings.set(ArchiveSettings.PORT_ARCREPOSITORY_ADMIN_DATABASE,
                "");
        Settings.set(ArchiveSettings.DIR_ARCREPOSITORY_ADMIN_DATABASE,
                "");

    }
    
    public void tearDown() {
        mtf.tearDown();
        pse.tearDown();
        pss.tearDown();
        rs.tearDown();
    }
    
    public void testNonFile() {
        String[] args = new String[]{TestInfo.DATABASE_ADMIN_DATA_FALSE.getPath()};
        try {
            ReestablishAdminDatabase.main(args);
            fail("Should try to System.exit.");
        } catch (SecurityException e) {
            // expected 
        }
        
        int exitCode = pse.getExitValue();
        assertEquals("Did not give expected exit code.", 1, exitCode);
        String errMsg = pss.getErr();
        assertTrue("Did not receive correct error message: " + errMsg, errMsg.contains(
                "The file '" + TestInfo.DATABASE_ADMIN_DATA_FALSE.getAbsolutePath() 
                + "' is not a valid file."));
    }
    
    public void testNoReadFile() {
        String[] args = new String[]{TestInfo.DATABASE_ADMIN_DATA_1.getPath()};
        TestInfo.DATABASE_ADMIN_DATA_1.setReadable(false);
        
        try {
            ReestablishAdminDatabase.main(args);
            fail("Should try to System.exit.");
        } catch (SecurityException e) {
            // expected 
        }
        
        int exitCode = pse.getExitValue();
        assertEquals("Did not give expected exit code.", 1, exitCode);
        String errMsg = pss.getErr();
        assertTrue("Did not receive correct error message: " + errMsg, errMsg.contains(
                "Cannot read the file '" + TestInfo.DATABASE_ADMIN_DATA_1.getAbsolutePath() 
                + "'"));
    }
    
    public void testSuccess() {
        String[] args = new String[]{TestInfo.DATABASE_ADMIN_DATA_2.getPath()};
        try {
            ReestablishAdminDatabase.main(args);
            fail("The tool should attempt to System.exit");
        } catch(SecurityException e) {
            // expected
        }
        
        int exitCode = pse.getExitValue();
        assertEquals("Should have the exitcode 0", 0, exitCode);
        pss.tearDown();
        System.out.println(pss.getOut());
        System.err.println(pss.getErr());
    }
    
    public void testNotEmptyDatabase() {
        String[] args = new String[]{TestInfo.DATABASE_ADMIN_DATA_2.getPath()};
        try {
            ReestablishAdminDatabase.main(args);
            fail("The tool should attempt to System.exit");
        } catch(SecurityException e) {
            // expected
        }
        
        int exitCode = pse.getExitValue();
        assertEquals("Should have the exitcode 1", 1, exitCode);
        String errMsg = pss.getErr();
        assertTrue("The err output '" + errMsg + "' was wrong",
                errMsg.contains("The database is not empty."));
    }
}
