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
package dk.netarkivet.archive.arcrepositoryadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

public class DBTester {

    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR, TestInfo.TEST_DIR);

    @Before
    public void setUp() {
        mtf.setUp();
    }

    @After
    public void tearDown() {
        mtf.tearDown();
    }

    @Test
    public void testDBConnect() {
        ReflectUtils.testUtilityConstructor(ArchiveDBConnection.class);
    }

    @Test
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

    @Test
    public void testDerbyEmbeddedSpecifics() {
        DerbySpecifics ds = new DerbyEmbeddedSpecifics();

        assertEquals("Wrong driver class name.", "org.apache.derby.jdbc.EmbeddedDriver", ds.getDriverClassName());

        ds.shutdownDatabase();

        try {
            ds.backupDatabase(null, TestInfo.TEST_DIR);
            fail("Should fail");
        } catch (Throwable e) {
            // expected.
        }

        // Cannot test the others!
        // try {
        // ds.backupDatabase(DriverManager.getConnection("jdbc:derby:;shutdown"), TestInfo.TEST_DIR);
        // fail("This should not happen!");
        // } catch (Throwable e) {
        // System.out.println(e);
        // e.printStackTrace();
        // }
    }
}
