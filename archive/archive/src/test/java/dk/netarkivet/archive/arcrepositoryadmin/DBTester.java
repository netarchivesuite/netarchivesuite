package dk.netarkivet.archive.arcrepositoryadmin;

import junit.framework.TestCase;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

public class DBTester extends TestCase {

    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR, TestInfo.TEST_DIR);
    
    public void setUp() {
        mtf.setUp();
    }
    
    public void tearDown() {
        mtf.tearDown();
    }
    
    public void testDBConnect() {
        ReflectUtils.testUtilityConstructor(DBConnect.class);
    }
    
    public void testDerbyServerSpecifics() {
        DerbySpecifics ds = new DerbyServerSpecifics();
        
        ds.shutdownDatabase();
        
        try {
            ds.backupDatabase(null, TestInfo.TEST_DIR);
            fail("Should fail");
        } catch (Throwable e) {
            // expected.
        }
    }
    
    public void testDerbyEmbeddedSpecifics() {
        DerbySpecifics ds = new DerbyEmbeddedSpecifics();
        
        assertEquals("Wrong driver class name.", "org.apache.derby.jdbc.EmbeddedDriver", 
                ds.getDriverClassName());

        ds.shutdownDatabase();

        try {
            ds.backupDatabase(null, TestInfo.TEST_DIR);
            fail("Should fail");
        } catch (Throwable e) {
            // expected.
        }
        
        // Cannot test the others!
//        try {
//            ds.backupDatabase(DriverManager.getConnection("jdbc:derby:;shutdown"), TestInfo.TEST_DIR);
//            fail("This should not happen!");
//        } catch (Throwable e) {
//            System.out.println(e);
//            e.printStackTrace();
//        }
    }
}
