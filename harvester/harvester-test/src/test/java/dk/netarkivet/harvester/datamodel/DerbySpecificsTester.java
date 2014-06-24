package dk.netarkivet.harvester.datamodel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.utils.DBUtils;

/**
 *
 * Unit test testing the DerbySpecifics class.
 *
 */

public class DerbySpecificsTester extends DataModelTestCase {

    Logger log = LoggerFactory.getLogger(this.getClass());

    public DerbySpecificsTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        log.info("setup() init");
        super.setUp();
        log.info("setup() done");
    }

    public void tearDown() throws Exception {
        log.info("tearDown() init");
        super.tearDown();
        log.info("tearDown() done");
    }
    
    /**
     * Test added to fool JUnit.
     */
    public void testDummy() {
        
    }

    public void testGetTemporaryTable() throws SQLException {
        Connection c = HarvestDBConnection.get();

        try {
            String statement = "SELECT config_name, domain_name "
                + "FROM session.jobconfignames";
            PreparedStatement s = null;
            try {
                s = c.prepareStatement(statement);
                s.execute();
                fail("Should have failed query before table is made");
            } catch (SQLException e) {
                // expected
            }

            try {
                c.setAutoCommit(false);
                String tmpTable =
                    DBSpecifics.getInstance().getJobConfigsTmpTable(c);
                assertEquals("Should have given expected name for Derby temp table",
                        "session.jobconfignames", tmpTable);
                s = c.prepareStatement(statement);
                s.execute();
                s.close();
                s = c.prepareStatement("INSERT INTO " + tmpTable
                        + " VALUES ( ?, ? )");
                s.setString(1, "foo");
                s.setString(2, "bar");
                s.executeUpdate();
                s.close();
                String domain =
                    DBUtils.selectStringValue(
                            c,
                            "SELECT domain_name FROM " + tmpTable
                            + " WHERE config_name = ?", "bar");
                assertEquals("Should get expected domain name", "foo", domain);
                c.commit();
                c.setAutoCommit(true);
                DBSpecifics.getInstance().dropJobConfigsTmpTable(c, tmpTable);
            } catch (SQLException e) {
                fail("Should not have had SQL exception " + e);
            }

            try {
                s = c.prepareStatement(statement);
                s.execute();
                fail("Should have failed query on selection from table which has "
                     + "been dropped");
            } catch (SQLException e) {
                // expected
            }
                c.setAutoCommit(false);
                String tmpTable =
                    DBSpecifics.getInstance().getJobConfigsTmpTable(c);
                assertEquals("Should have given expected name for Derby temp table",
                        "session.jobconfignames", tmpTable);
                s = c.prepareStatement(statement);
                s.execute();
                s.close();
                s = c.prepareStatement("INSERT INTO " + tmpTable
                        + " VALUES ( ?, ? )");
                s.setString(1, "foo");
                s.setString(2, "bar");
                s.executeUpdate();
                s.close();
                String domain =
                    DBUtils.selectStringValue(c,
                            "SELECT domain_name FROM "
                            + tmpTable
                            + " WHERE config_name = ?", "bar");
                assertEquals("Should get expected domain name", "foo", domain);
                c.commit();
                c.setAutoCommit(true);
                DBSpecifics.getInstance().dropJobConfigsTmpTable(c, tmpTable);

        } finally {
            HarvestDBConnection.release(c);
        }
    }
}