/*
 * #%L
 * Netarchivesuite - common - test
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
package dk.netarkivet.common.utils.cdx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;

import org.junit.Test;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.arc.ARCKey;

/**
 * Performs unit tests of the CDXReader class.
 */
public class CDXReaderTester {

    /**
     * Testing the constructor.
     *
     * @throws FileNotFoundException
     */
    @Test
    public void testConstructor() throws FileNotFoundException {
        CDXReader reader = new CDXReader(TestInfo.CDX_FILE1);
        reader.addCDXFile(TestInfo.CDX_FILE2);
        reader.addCDXFile(TestInfo.CDX_FILE3);
        reader.addCDXFile(TestInfo.CDX_FILE2);
    }

    /**
     * Testing adding a file to an existing CDXReader.
     */
    @Test
    public void testAddExistingFile() {
        CDXReader reader = new CDXReader(TestInfo.CDX_FILE1);
        reader.addCDXFile(TestInfo.CDX_FILE2);
    }

    /**
     * Testing adding a non-existing file !
     */
    @Test(expected = IOFailure.class)
    public void testAddMissingFile() {
        new CDXReader(TestInfo.MISSING_FILE);
        fail("Adding non-existing files should throw IOException");
    }

    /**
     * Testing different lookup-scenarios ! Finding objects i first-file - second-file - last-file - prefix-search.....
     */
    @Test
    public void testGetKey() {
        CDXReader reader = null;
        reader = new CDXReader(TestInfo.CDX_FILE1);
        reader.addCDXFile(TestInfo.CDX_FILE2);
        reader.addCDXFile(TestInfo.CDX_FILE3);
        reader.addCDXFile(TestInfo.CDX_FILE4);
        // Check a regular entry
        ARCKey key;
        key = reader.getKey("http://debat.computerworld.dk/images/menu_idg.gif");
        assertNotNull("Existing entry should be found", key);
        assertEquals("IAH-20040712071242-00000-pc770.sb.statsbiblioteket.dk.arc", key.getFile().getName());
        assertEquals(219921, key.getOffset());

        // Check that an entry that appears twice gets the first one
        key = reader.getKey("http://adserver.adtech.de/robots.txt");
        assertNotNull("Existing entry should be found", key);
        assertEquals("IAH-20040708140334-00000-pc770.sb.statsbiblioteket.dk.arc", key.getFile().getName());
        assertEquals(29865, key.getOffset());

        // Check that we can get a key from the second file
        key = reader.getKey("http://web2.jp.dk/quiz/quiz/frame.asp?q=149");
        assertNotNull("Entry in second file should be found", key);
        assertEquals("IAH-20040708140334-00000-pc770.sb.statsbiblioteket.dk.arc", key.getFile().getName());
        assertEquals(1283051, key.getOffset());

        // Check that a prefix key doesn't get the wrong entry
        key = reader.getKey("http://www.jp.dk/common/today");
        assertNotNull("Entry in second file should be found", key);
        assertEquals("IAH-20040708140334-00000-pc770.sb.statsbiblioteket.dk.arc", key.getFile().getName());
        assertEquals(123063, key.getOffset());

        // Check that a prefix key doesn't get the wrong entry
        key = reader.getKey("http://www.netarkivet.dk/testfile.html");
        assertNotNull("Entry in last file should be found", key);
        assertEquals("TESTFILE.arc", key.getFile().getName());
        assertEquals(0, key.getOffset());
    }

    /**
     * Testing looking for URI's that do not exist
     */
    @Test
    public void testGetKeyFailed() {
        CDXReader reader = null;
        reader = new CDXReader(TestInfo.CDX_FILE1);
        reader.addCDXFile(TestInfo.CDX_FILE2);
        reader.addCDXFile(TestInfo.CDX_FILE3);
        // Check that a non-existing entry isn't found
        assertNull("Missing entries should not be found.", reader.getKey("MissingFile"));
    }

    /**
     * Testing that prefix-search do not return objects that they were not supposed to do ! Prefix-search is not an
     * option at the moment
     */
    @Test
    public void testGetKeyPrefixFailed() {
        CDXReader reader = null;
        reader = new CDXReader(TestInfo.CDX_FILE1);
        reader.addCDXFile(TestInfo.CDX_FILE2);
        reader.addCDXFile(TestInfo.CDX_FILE3);
        // Check that a non-existing entry that is a prefix is not found.
        assertNull("Prefix entrys should not be found.", reader.getKey("http://"));
    }

    /**
     * Testing that a single URL could be found i 2 different arc-files - based on adding a filter before the second
     * search
     */
    @Test
    public void testGetKeyWithFilter() {
        CDXReader reader = null;
        reader = new CDXReader(TestInfo.CDX_FILE1);
        reader.addCDXFile(TestInfo.CDX_FILE2);
        reader.addCDXFile(TestInfo.CDX_FILE3);
        // Check first entry of this uri without filter
        ARCKey key;
        key = reader.getKey("http://server-dk.imrworldwide.com/a1.js");
        assertNotNull("Existing entry should be found", key);
        assertEquals("IAH-20040712071242-00000-pc770.sb.statsbiblioteket.dk.arc", key.getFile().getName());
        assertEquals(39001, key.getOffset());

        // Apply a filter to reader and check again - this time another ARC-file
        // should be found.
        reader.addCDXRecordFilter(new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", "filter1"));
        key = reader.getKey("http://server-dk.imrworldwide.com/a1.js");
        assertNotNull("Existing entry should be found", key);
        assertEquals("NETARKIVET_00001-20040708140334-00000-pc770.sb.statsbiblioteket.dk.arc", key.getFile().getName());
        assertEquals(1000, key.getOffset());
    }

    /**
     * Testing that nothing could be found when adding a filter with a non-matching arc-file-name
     */
    @Test
    public void testGetKeyWithBadFilter() {
        CDXReader reader = null;
        reader = new CDXReader(TestInfo.CDX_FILE1);
        reader.addCDXFile(TestInfo.CDX_FILE2);
        reader.addCDXFile(TestInfo.CDX_FILE3);

        // Apply a "bad" filter to reader and check for an existing URL (that
        // should not be found because of the filter)
        reader.addCDXRecordFilter(new ARCFilenameCDXRecordFilter("BAD_PATTERN.*", "filter1"));
        ARCKey key = reader.getKey("http://server-dk.imrworldwide.com/a1.js");
        assertNull("Entry should NOT be found", key);
    }

    /**
     * Testing what happens when adding a CDXRecordFilter with a null reference
     */
    @Test(expected = ArgumentNotValid.class)
    public void testNullFilter() {
        CDXReader reader = null;
        reader = new CDXReader(TestInfo.CDX_FILE1);

        ARCFilenameCDXRecordFilter nullfilter = null;
        reader.addCDXRecordFilter(nullfilter);
        fail("Should throw ArgumentNotValid here !!");
    }

    /**
     * Testing the method for removing all filters
     */
    @Test
    public void testRemoveAllFilters() {
        CDXReader reader = null;
        reader = new CDXReader(TestInfo.CDX_FILE1);
        reader.addCDXRecordFilter(new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", "filter2"));
        reader.removeAllCDXRecordFilters();
        assertEquals("There should be no filters connected to this CDXReader at this point",
                reader.getFilters().size(), 0);
    }

    /**
     * Testing the method to get all filters (testing only on the number of filters after adding one filter)
     */
    @Test
    public void testGetFilters() {
        CDXReader reader = null;
        reader = new CDXReader(TestInfo.CDX_FILE1);
        reader.addCDXRecordFilter(new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", "filter3"));
        assertEquals("There should be 1 filter connected to this CDXReader at this point", reader.getFilters().size(),
                1);
    }

    /**
     * Testing adding a filter with a filtername already used
     */
    @Test(expected = ArgumentNotValid.class)
    public void testNonUniqueFilterName() {
        CDXReader reader = null;
        reader = new CDXReader(TestInfo.CDX_FILE1);
        reader.addCDXRecordFilter(new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", "sameName"));
        reader.addCDXRecordFilter(new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", "sameName"));
        fail("ArgumentNotValid should have been thrown !");
    }

    /**
     * Testing getting filter by filtername
     */
    @Test
    public void testGetFilterByName() {
        CDXReader reader = null;
        reader = new CDXReader(TestInfo.CDX_FILE1);
        reader.addCDXRecordFilter(new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", "myName"));
        CDXRecordFilter cdxrf = reader.getCDXRecordFilter("myName");
        assertEquals("The CDXRecordFilter has the wrong name !", cdxrf.getFilterName(), "myName");
    }

    /**
     * Testing getting filter by illegal filtername (non existing filter name)
     */
    @Test
    public void testGetFilterByIllegalName() {
        CDXReader reader = null;
        reader = new CDXReader(TestInfo.CDX_FILE1);
        reader.addCDXRecordFilter(new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", "myName"));
        assertNull("The CDXRecordFilter should be null !", reader.getCDXRecordFilter("IllegalNAME"));
    }

    /**
     * Testing removing filter by filtername
     */
    @Test
    public void testRemoveFilterByName() {
        CDXReader reader = null;
        reader = new CDXReader(TestInfo.CDX_FILE1);
        reader.addCDXRecordFilter(new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", "myName"));
        assertEquals("There should be only on CDXRecordFilter attached !", reader.getFilters().size(), 1);
        reader.removeCDXRecordFilter("myName");
    }

    /**
     * Testing removing filter by illegal filtername (non existing filtername)
     */
    @Test(expected = UnknownID.class)
    public void testRemoveFilterByIllegalName() {
        CDXReader reader = null;
        reader = new CDXReader(TestInfo.CDX_FILE1);
        reader.addCDXRecordFilter(new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", "myName"));
        assertEquals("There should be only on CDXRecordFilter attached !", reader.getFilters().size(), 1);
        reader.removeCDXRecordFilter("nonexistingfiltername");
        fail("NoSuchElementException should have been thrown !");
    }
}
