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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Unittests for the GlobalCrawlerTrapList class.
 */
public class GlobalCrawlerTrapListTester extends DataModelTestCase {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Tests that we can construct a trap list.
     */
    @Test
    public void testConstructor() throws FileNotFoundException {
        GlobalCrawlerTrapList trapList = new GlobalCrawlerTrapList(new FileInputStream(new File(TestInfo.TOPDATADIR,
                TestInfo.CRAWLER_TRAPS_01)), "a_name", "A Description", true);
        assertEquals("Expected 5 traps in this list", 5, trapList.getTraps().size());
    }

    /**
     * Tests that the constructor throws expected exceptions on bad data
     */
    @Test
    public void testConstructorFail() throws FileNotFoundException {

        try {
            new GlobalCrawlerTrapList(new FileInputStream(new File(TestInfo.TOPDATADIR, TestInfo.CRAWLER_TRAPS_02)),
                    "", "A Description", true);
            fail("Should throw ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            new GlobalCrawlerTrapList(null, "a_name", "A Description", true);
            fail("Should throw ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            // expected
        }
    }

    /**
     * Unit test for https://sbforge.org/jira/browse/NAS-1793
     * 
     * @throws FileNotFoundException
     */
    @Test
    public void testBadRegexp() throws FileNotFoundException {
        try {
            new GlobalCrawlerTrapList(new FileInputStream(new File(TestInfo.TOPDATADIR, TestInfo.CRAWLER_TRAPS_03)),
                    "a_name", "A Description", true);
            fail("Should have thrown ArgumentNotValid on bad regexp");
        } catch (ArgumentNotValid e) {
            // expected
        }
    }
}
