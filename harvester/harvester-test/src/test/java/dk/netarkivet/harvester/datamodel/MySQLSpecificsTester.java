package dk.netarkivet.harvester.datamodel;

import junit.framework.TestCase;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

public class MySQLSpecificsTester extends TestCase {
    ReloadSettings rs = new ReloadSettings();

    public MySQLSpecificsTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        rs.setUp();
        Settings.set(CommonSettings.DB_SPECIFICS_CLASS,
                     "dk.netarkivet.harvester.datamodel.MySQLSpecifics");
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        rs.tearDown();
    }
    
    public void testLoadClass() {
        DBSpecifics instance = DBSpecifics.getInstance(
                CommonSettings.DB_SPECIFICS_CLASS);
        assertNotNull("instance should not be null", instance);
    }
    
   public void testGetDriverClassName() {
           DBSpecifics instance = DBSpecifics.getInstance(
                   CommonSettings.DB_SPECIFICS_CLASS);
           assertEquals("Wrong driver", instance.getDriverClassName(),
            "com.mysql.jdbc.Driver");
    }
}
