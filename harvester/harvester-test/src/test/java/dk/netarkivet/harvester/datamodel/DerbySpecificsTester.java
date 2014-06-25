/*
 * #%L
 * Netarchivesuite - harvester - test
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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
        log.trace("setup() init");
        super.setUp();
        log.trace("setup() done");
    }

    public void tearDown() throws Exception {
        log.trace("tearDown() init");
        super.tearDown();
        log.trace("tearDown() done");
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
