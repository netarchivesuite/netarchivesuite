/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.harvester.datamodel;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.DBUtils;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;

/**
 * 
 * Unit test testing the DerbySpecifics class.
 *
 */
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
        Connection connection = DBConnect.getDBConnection();
        Connection c = connection;
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
            DBUtils.closeStatementIfOpen(s);
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
                            connection,
                            "SELECT domain_name FROM " + tmpTable
                            + " WHERE config_name = ?", "bar");
            assertEquals("Should get expected domain name", "foo", domain);
            c.commit();
            c.setAutoCommit(true);
            DBSpecifics.getInstance().dropJobConfigsTmpTable(c, tmpTable);
        } catch (SQLException e) {
            fail("Should not have had SQL exception " + e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }

        try {
            s = c.prepareStatement(statement);
            s.execute();
            String domain =
                    DBUtils.selectStringValue(connection,
                                              "SELECT domain_name "
                            + "FROM session.jobconfignames "
                            + "WHERE config_name = 'foo'");
            fail("Should have failed query after table is dead, "
                    + "but return domain= " + domain);
        } catch (SQLException e) {
            // expected
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }

        // Should be possible to get another temporary table.
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
                    DBUtils.selectStringValue(connection,
                                              "SELECT domain_name FROM "
                            + tmpTable
                            + " WHERE config_name = ?", "bar");
            assertEquals("Should get expected domain name", "foo", domain);
            c.commit();
            c.setAutoCommit(true);
            DBSpecifics.getInstance().dropJobConfigsTmpTable(c, tmpTable);
        } catch (SQLException e) {
            fail("Should not have had SQL exception " + e);
        } finally {
            DBUtils.closeStatementIfOpen(s);
        }
    }

    /**
     * Test backup-functionality of our Derby database.
     * @throws SQLException If unable to backup derby database.
     * @throws IOException If unable to get the canonical path of
     * the backup directory.
     */
    public void testBackupDatabase() throws SQLException, IOException {
        File tempdir = new File(TestInfo.TEMPDIR,
                                "db-backup-" + System.currentTimeMillis());
        DBSpecifics.getInstance().backupDatabase(tempdir);
        assertTrue("Backup dir should exist after calling backup.",
                tempdir.exists());
        File backupdir = new File(tempdir, "fullhddb");
        assertTrue("Backup dir should contain fullhddb dir",
                backupdir.exists());
        assertTrue("Backup history file should exist",
                new File(backupdir, "BACKUP.HISTORY").exists());

        LogUtils.flushLogs(DBConnect.class.getName());
        FileAsserts.assertFileNotContains(
                "Should not have warned about non-existing dir being created",
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