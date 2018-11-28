/*
 * #%L
 * Netarchivesuite - harvester - test
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

package dk.netarkivet.harvester.datamodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.SlowTest;

/**
 * Unitests for the class GlobalCrawlerTrapListDBDAO.
 */
@SuppressWarnings({"unused"})
public class GlobalCrawlerTrapListDBDAOTester extends DataModelTestCase {

    GlobalCrawlerTrapList list1;
    GlobalCrawlerTrapList list2;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        list1 = new GlobalCrawlerTrapList(
                new FileInputStream(new File(TestInfo.TOPDATADIR, TestInfo.CRAWLER_TRAPS_01)), "list1",
                "A Description of list1", true);
        list2 = new GlobalCrawlerTrapList(
                new FileInputStream(new File(TestInfo.TOPDATADIR, TestInfo.CRAWLER_TRAPS_02)), "list2",
                "A Description of list2", true);
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        GlobalCrawlerTrapListDBDAO.reset();
    }

    /**
     * tests that we can get a singleton instance of the jobDAO class
     */
    @Category(SlowTest.class)
    @Test
    public void testGetInstance() throws SQLException {
        GlobalCrawlerTrapListDAO dao1 = GlobalCrawlerTrapListDAO.getInstance();
        GlobalCrawlerTrapListDAO dao2 = GlobalCrawlerTrapListDAO.getInstance();
        assertNotNull("Should get a non null jobDAO instance", dao1);
        assertEquals("Should get a unique instance (object identity) of jobDAO.", dao1, dao2);
        // Check that the tables exist
        Connection con = HarvestDBConnection.get();
        try {
            con.prepareStatement("SELECT * from global_crawler_trap_lists").execute();
            con.prepareStatement("SELECT * from global_crawler_trap_expressions").execute();
        } finally {
            HarvestDBConnection.release(con);
        }
    }

    @Category(SlowTest.class)
    @Test
    public void testCreate() {
        GlobalCrawlerTrapListDAO dao = GlobalCrawlerTrapListDAO.getInstance();
        int id = dao.create(list1);
        assertEquals("Should have set list id to returned id", id, list1.getId());
        try {
            dao.create(list1);
            fail("Should throw ArgumentNotValid exception");
        } catch (ArgumentNotValid e) {
            // expected
        }
    }

    /**
     * Tests that we can insert an object in the jobDAO and read it back.
     */
    @Category(SlowTest.class)
    @Test
    public void testCreateAndRead() {
        GlobalCrawlerTrapListDAO dao = GlobalCrawlerTrapListDAO.getInstance();
        int id = dao.create(list1);
        GlobalCrawlerTrapList list3 = dao.read(id);
        assertEquals("Should get back the object we inserted.", list1.getTraps(), list3.getTraps());
    }

    /**
     * Test that we can delete an object from the database.
     */
    @Category(SlowTest.class)
    @Test
    public void testDelete() {
        GlobalCrawlerTrapListDAO dao = GlobalCrawlerTrapListDAO.getInstance();
        int id = dao.create(list1);
        dao.delete(id);
        try {
            GlobalCrawlerTrapList list3 = dao.read(id);
            fail("Should have thrown an exception, not returned :'" + list3 + "'");
        } catch (UnknownID e) {
            // expected
        }
    }

    /**
     * Test that we can update a list and retrieve the updated list
     */
    @Category(SlowTest.class)
    @Test
    public void testUpdate() {
        GlobalCrawlerTrapListDAO dao = GlobalCrawlerTrapListDAO.getInstance();
        int id1 = dao.create(list1);
        int id2 = dao.create(list2);
        list2.setDescription("new description");
        Set<String> traps = list2.getTraps();
        int oldLength = traps.size();
        traps.remove(traps.iterator().next());
        dao.update(list2);
        GlobalCrawlerTrapList list3 = dao.read(id2);
        assertEquals("New list and old list should be equal", list2, list3);
    }

    /**
     * Tests that we can get a list of active traplists
     */
    @Category(SlowTest.class)
    @Test
    public void testGetAllActive() {
        GlobalCrawlerTrapListDAO dao = GlobalCrawlerTrapListDAO.getInstance();
        list1.setActive(true);
        list2.setActive(false);
        dao.create(list1);
        dao.create(list2);
        assertEquals("Should be one active list", 1, dao.getAllActive().size());
        assertEquals("Should be list1", list1, dao.getAllActive().get(0));
    }

    @Category(SlowTest.class)
    @Test
    public void testGetAllInactive() {
        GlobalCrawlerTrapListDAO dao = GlobalCrawlerTrapListDAO.getInstance();
        list1.setActive(false);
        list2.setActive(false);
        dao.create(list1);
        dao.create(list2);
        assertEquals("Should be two inactive lists", 2, dao.getAllInActive().size());
    }

    @Category(SlowTest.class)
    @Test
    public void testGetExpressions() {
        GlobalCrawlerTrapListDAO dao = GlobalCrawlerTrapListDAO.getInstance();
        list1.setActive(true);
        list2.setActive(true);
        dao.create(list1);
        dao.create(list2);
        assertEquals("Should combine the two lists to get 9 distinct traps", 9, dao.getAllActiveTrapExpressions()
                .size());
    }

    @Category(SlowTest.class)
    @Test
    public void testExists() {
        GlobalCrawlerTrapListDAO dao = GlobalCrawlerTrapListDAO.getInstance();
        String name = list1.getName();
        assertFalse("Crawlertrap with name '" + name + "' should not exist now", dao.exists(name));
        dao.create(list1);
        assertTrue("Crawlertrap with name '" + name + "' should exist now", dao.exists(name));
    }
}
