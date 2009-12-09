/* File:        $Id: License.txt,v $
 * Revision:    $Revision: 1.4 $
 * Author:      $Author: csr $
 * Date:        $Date: 2005/04/11 16:29:16 $
 *
 * Copyright Det Kongelige Bibliotek og Statsbiblioteket, Danmark
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package dk.netarkivet.harvester.datamodel;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.SQLException;

import dk.netarkivet.common.exceptions.UnknownID;

/**
 * csr forgot to comment this!
 *
 * @author csr
 * @since Nov 26, 2009
 */

public class GlobalCrawlerTrapListDBDAOTester extends DataModelTestCase {

    GlobalCrawlerTrapList list1;
    GlobalCrawlerTrapList list2;

    public GlobalCrawlerTrapListDBDAOTester(String s) {
        super(s);
    }

    @Override
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
    public void tearDown() throws Exception {
       super.setUp();
    }

    /**
     * tests that we can get a singleton instance of the dao class
     */
    public void testGetInstance() throws SQLException {
        GlobalCrawlerTrapListDBDAO dao1 =
                GlobalCrawlerTrapListDBDAO.getInstance();
        GlobalCrawlerTrapListDBDAO dao2 =
                GlobalCrawlerTrapListDBDAO.getInstance();
        assertNotNull("Should get a non null dao instance", dao1);
        assertEquals("Should get a unique instance (object identity) of dao.",
                     dao1, dao2);
        // Check that the tables exist
        Connection con = DBConnect.getDBConnection();
        con.prepareStatement("SELECT * from global_crawler_trap_lists")
                .execute();
        con.prepareStatement("SELECT * from global_crawler_trap_expressions")
                .execute();        
    }

    /**
     * Tests that we can insert an object in the dao and read it back.
     */
   public void testCreateAndRead() {
       GlobalCrawlerTrapListDBDAO dao
               = GlobalCrawlerTrapListDBDAO.getInstance();
       int id = dao.create(list1);
       GlobalCrawlerTrapList list3 = dao.read(id);
        assertEquals("Should get back the object we inserted.", list1.getTraps(), list2.getTraps());
    }

    /**
     * Test that we can delete an object from the database.
     */
    public void testDelete() {
        GlobalCrawlerTrapListDBDAO dao
                = GlobalCrawlerTrapListDBDAO.getInstance();
        int id = dao.create(list1);
        dao.delete(id);
        try {
            GlobalCrawlerTrapList list3 = dao.read(id);
            fail("Should have thrown an exception, not returned :'" + list3 +
                 "'");
        } catch (UnknownID e) {
            //expected
        }
    }



}
