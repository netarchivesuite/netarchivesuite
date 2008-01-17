package dk.netarkivet.harvester.datamodel;
/**
 * lc forgot to comment this!
 * @author lc
 * @since Mar 5, 2007
 */

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;


public class DerbySpecificsTester extends DataModelTestCase {
    public DerbySpecificsTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }
    public void testGetTemporaryTable() {
        Connection c = DBConnect.getDBConnection();
        String statement = "SELECT config_name, domain_name "
                + "FROM session.jobconfignames";
        PreparedStatement s = null;
        try {
            s = c.prepareStatement(statement);
            s.execute();
            fail("Should have failed query before table is made");
        } catch (SQLException e) {
            // expected
        } finally {
            DBConnect.closeStatementIfOpen(s);
        }

        try {
            c.setAutoCommit(false);
            String tmpTable = DBSpecifics.getInstance().getJobConfigsTmpTable(c);
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
                    DBConnect.selectStringValue("SELECT domain_name FROM " + tmpTable
                            + " WHERE config_name = ?", "bar");
            assertEquals("Should get expected domain name", "foo", domain);
            c.commit();
            c.setAutoCommit(true);
            DBSpecifics.getInstance().dropJobConfigsTmpTable(c, tmpTable);
        } catch (SQLException e) {
            fail("Should not have had SQL exception " + e);
        } finally {
            DBConnect.closeStatementIfOpen(s);
        }

        try {
            s = c.prepareStatement(statement);
            s.execute();
            String domain =
                    DBConnect.selectStringValue("SELECT domain_name "
                            + "FROM session.jobconfignames "
                            + "WHERE config_name = 'foo'");
            fail("Should have failed query after table is dead");
        } catch (SQLException e) {
            // expected
        } finally {
            DBConnect.closeStatementIfOpen(s);
        }

        // Should be possible to get another temp table.
        try {
            c.setAutoCommit(false);
            String tmpTable = DBSpecifics.getInstance().getJobConfigsTmpTable(c);
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
                    DBConnect.selectStringValue("SELECT domain_name FROM " + tmpTable
                            + " WHERE config_name = ?", "bar");
            assertEquals("Should get expected domain name", "foo", domain);
            c.commit();
            c.setAutoCommit(true);
            DBSpecifics.getInstance().dropJobConfigsTmpTable(c, tmpTable);
        } catch (SQLException e) {
            fail("Should not have had SQL exception " + e);
        } finally {
            DBConnect.closeStatementIfOpen(s);
        }
    }

    /**
     * Test backup-functionality of our Derby database.
     * @throws Exception
     */
    public void testBackupDatabase() throws Exception {
        File tempdir = new File(TestInfo.TEMPDIR,
                                "db-backup-" + System.currentTimeMillis() );
        DBSpecifics.getInstance().backupDatabase(tempdir);
        assertTrue("Backup dir should exist after calling backup.",
                tempdir.exists());
        File backupdir = new File(tempdir, "fullhddb");
        assertTrue("Backup dir should contain fullhddb dir",
                backupdir.exists());
        assertTrue("Backup history file should exist",
                new File(backupdir, "BACKUP.HISTORY").exists());

        LogUtils.flushLogs(DBConnect.class.getName());
        FileAsserts.assertFileNotContains("Should not have warned about non-existing dir being created",
                TestInfo.LOG_FILE, "WARNING: Non-existing directory created");
        FileAsserts.assertFileContains("Should have backed-up info",
                "Backed up database to " + tempdir.getCanonicalPath(),
                TestInfo.LOG_FILE);

        FileUtils.removeRecursively(tempdir);
        // Check that it complains if it can't backup
        try {
            DBSpecifics.getInstance().backupDatabase(new File("/foo/bar"));
            fail("Should have complained on illegal backup dir");
        } catch (PermissionDenied e) {
            //expected
        }
    }

}