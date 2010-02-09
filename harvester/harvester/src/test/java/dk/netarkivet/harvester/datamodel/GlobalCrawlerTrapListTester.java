/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
import java.io.FileNotFoundException;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * csr forgot to comment this!
 *
 */

public class GlobalCrawlerTrapListTester extends DataModelTestCase {
    public GlobalCrawlerTrapListTester(String s) {
        super(s);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
      super.tearDown();
    }

    /**
     * Tests that we can construct a trap list.
     */
    public void testConstructor() throws FileNotFoundException {
        GlobalCrawlerTrapList trapList = new GlobalCrawlerTrapList(
                new FileInputStream(new File(TestInfo.TOPDATADIR, TestInfo.CRAWLER_TRAPS_01)), "a_name",
                "A Description", true);
        assertEquals("Expected 5 traps in this list", 5, trapList.getTraps().size());
    }

    /**
     * Tests that the constructor throws expected exceptions on bad data
     */
    public void testConstructorFail() throws FileNotFoundException {

        try {
            GlobalCrawlerTrapList trapList = new GlobalCrawlerTrapList(
                  new FileInputStream(new File(TestInfo.TOPDATADIR, TestInfo.CRAWLER_TRAPS_02)), "",
                  "A Description", true);
            fail("Should throw ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            //expected
        }

        try {
            GlobalCrawlerTrapList trapList = new GlobalCrawlerTrapList(
                  null, "a_name",
                  "A Description", true);
            fail("Should throw ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            //expected
        }
    }
}
