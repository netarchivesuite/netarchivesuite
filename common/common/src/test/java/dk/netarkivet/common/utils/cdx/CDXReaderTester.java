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
package dk.netarkivet.common.utils.cdx;

import java.io.FileNotFoundException;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.arc.ARCKey;


/**
 * Performs unit tests of the CDXReader class.
 *
 */
public class CDXReaderTester extends TestCase {

    /**
     * Testing the constructor.
     * @throws FileNotFoundException
     */
    public void testConstructor() throws FileNotFoundException {
        CDXReader reader = new CDXReader(TestInfo.CDX_FILE1);
        reader.addCDXFile(TestInfo.CDX_FILE2);
        reader.addCDXFile(TestInfo.CDX_FILE3);
        reader.addCDXFile(TestInfo.CDX_FILE2);
    }

    /**
     * Testing adding a file to an existing CDXReader.
     */
    public void testAddExistingFile() {
        CDXReader reader = new CDXReader(TestInfo.CDX_FILE1);
        reader.addCDXFile(TestInfo.CDX_FILE2);
    }

    /**
     * Testing adding a non-existing file !
     */
    public void testAddMissingFile() {
        try {
            new CDXReader(TestInfo.MISSING_FILE);
            fail("Adding non-existing files should throw IOException");
        } catch (IOFailure e) {
            // Correct
        }
    }

    /**
     * Testing different lookup-scenarios !
     * Finding objects i first-file - second-file - last-file - prefix-search.....
     */
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
        assertEquals("IAH-20040712071242-00000-pc770.sb.statsbiblioteket.dk.arc",
                key.getFile().getName());
        assertEquals(219921, key.getOffset());

        // Check that an entry that appears twice gets the first one
        key = reader.getKey("http://adserver.adtech.de/robots.txt");
        assertNotNull("Existing entry should be found", key);
        assertEquals("IAH-20040708140334-00000-pc770.sb.statsbiblioteket.dk.arc",
                key.getFile().getName());
        assertEquals(29865, key.getOffset());

        // Check that we can get a key from the second file
        key = reader.getKey("http://web2.jp.dk/quiz/quiz/frame.asp?q=149");
        assertNotNull("Entry in second file should be found", key);
        assertEquals("IAH-20040708140334-00000-pc770.sb.statsbiblioteket.dk.arc",
                key.getFile().getName());
        assertEquals(1283051, key.getOffset());

        // Check that a prefix key doesn't get the wrong entry
        key = reader.getKey("http://www.jp.dk/common/today");
        assertNotNull("Entry in second file should be found", key);
        assertEquals("IAH-20040708140334-00000-pc770.sb.statsbiblioteket.dk.arc",
                key.getFile().getName());
        assertEquals(123063, key.getOffset());

        // Check that a prefix key doesn't get the wrong entry
        key = reader.getKey("http://www.netarkivet.dk/testfile.html");
        assertNotNull("Entry in last file should be found", key);
        assertEquals("TESTFILE.arc",
                key.getFile().getName());
        assertEquals(0, key.getOffset());
    }

    /**
     * Testing looking for URI's that do not exist
     */
    public void testGetKeyFailed() {
        CDXReader reader = null;
        reader = new CDXReader(TestInfo.CDX_FILE1);
        reader.addCDXFile(TestInfo.CDX_FILE2);
        reader.addCDXFile(TestInfo.CDX_FILE3);
        // Check that a non-existing entry isn't found
        assertNull("Missing entries should not be found.",
                reader.getKey("MissingFile"));
    }

    /**
     * Testing that prefix-search do not return objects that they were not supposed to do !
     * Prefix-search is not an option at the moment
     */
    public void testGetKeyPrefixFailed() {
        CDXReader reader = null;
        reader = new CDXReader(TestInfo.CDX_FILE1);
        reader.addCDXFile(TestInfo.CDX_FILE2);
        reader.addCDXFile(TestInfo.CDX_FILE3);
        // Check that a non-existing entry that is a prefix is not found.
        assertNull("Prefix entrys should not be found.",
                reader.getKey("http://"));
    }

    /**
     * Testing that a single URL could be found i 2 different arc-files - based on adding a filter before the second search
     */
    public void testGetKeyWithFilter() {
        CDXReader reader = null;
        reader = new CDXReader(TestInfo.CDX_FILE1);
        reader.addCDXFile(TestInfo.CDX_FILE2);
        reader.addCDXFile(TestInfo.CDX_FILE3);
        // Check first entry of this uri without filter
        ARCKey key;
        key = reader.getKey("http://server-dk.imrworldwide.com/a1.js");
        assertNotNull("Existing entry should be found", key);
        assertEquals("IAH-20040712071242-00000-pc770.sb.statsbiblioteket.dk.arc",
                key.getFile().getName());
        assertEquals(39001, key.getOffset());

        // Apply a filter to reader and check again - this time another ARC-file should be found.
        reader.addCDXRecordFilter(new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", "filter1"));
        key = reader.getKey("http://server-dk.imrworldwide.com/a1.js");
        assertNotNull("Existing entry should be found", key);
        assertEquals("NETARKIVET_00001-20040708140334-00000-pc770.sb.statsbiblioteket.dk.arc",
                key.getFile().getName());
        assertEquals(1000, key.getOffset());
    }

    /**
     * Testing that nothing could be found when adding a filter with a non-matching arc-file-name
     */
    public void testGetKeyWithBadFilter() {
        CDXReader reader = null;
        reader = new CDXReader(TestInfo.CDX_FILE1);
        reader.addCDXFile(TestInfo.CDX_FILE2);
        reader.addCDXFile(TestInfo.CDX_FILE3);

        // Apply a "bad" filter to reader and check for an existing URL (that should not be found because of the filter)
        reader.addCDXRecordFilter(new ARCFilenameCDXRecordFilter("BAD_PATTERN.*", "filter1"));
        ARCKey key = reader.getKey("http://server-dk.imrworldwide.com/a1.js");
        assertNull("Entry should NOT be found", key);
    }

    /**
     * Testing what happens when adding a CDXRecordFilter with a null reference
     */
    public void testNullFilter() {
        CDXReader reader = null;
        reader = new CDXReader(TestInfo.CDX_FILE1);

        ARCFilenameCDXRecordFilter nullfilter = null;
        try {
            reader.addCDXRecordFilter(nullfilter);
            fail("Should throw ArgumentNotValid here !!");
        } catch (ArgumentNotValid e) {
            // this is the expected case
        }
    }

    /**
     * Testing the method for removing all filters
     */
    public void testRemoveAllFilters() {
        CDXReader reader = null;
        reader = new CDXReader(TestInfo.CDX_FILE1);
        reader.addCDXRecordFilter(new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", "filter2"));
        reader.removeAllCDXRecordFilters();
        assertEquals("There should be no filters connected to this CDXReader at this point", reader.getFilters().size(), 0);
    }

    /**
     * Testing the method to get all filters (testing only on the number of filters after adding one filter)
     */
    public void testGetFilters() {
        CDXReader reader = null;
        reader = new CDXReader(TestInfo.CDX_FILE1);
        reader.addCDXRecordFilter(new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", "filter3"));
        assertEquals("There should be 1 filter connected to this CDXReader at this point", reader.getFilters().size(), 1);
    }

    /**
     * Testing adding a filter with a filtername already used
     */
    public void testNonUniqueFilterName() {
        CDXReader reader = null;
        reader = new CDXReader(TestInfo.CDX_FILE1);
        try {
            reader.addCDXRecordFilter(new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", "sameName"));
            reader.addCDXRecordFilter(new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", "sameName"));
            fail("ArgumentNotValid should have been thrown !");
        } catch (ArgumentNotValid e) {
            // expected case
        }
    }

    /**
     * Testing getting filter by filtername
     */
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
    public void testGetFilterByIllegalName() {
        CDXReader reader = null;
        reader = new CDXReader(TestInfo.CDX_FILE1);
        reader.addCDXRecordFilter(new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", "myName"));
        assertNull("The CDXRecordFilter should be null !", reader.getCDXRecordFilter("IllegalNAME"));
    }

    /**
     * Testing removing filter by filtername
     */
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
    public void testRemoveFilterByIllegalName() {
        CDXReader reader = null;
        reader = new CDXReader(TestInfo.CDX_FILE1);
        try {
            reader.addCDXRecordFilter(new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", "myName"));
            assertEquals("There should be only on CDXRecordFilter attached !", reader.getFilters().size(), 1);
            reader.removeCDXRecordFilter("nonexistingfiltername");
            fail("NoSuchElementException should have been thrown !");
        } catch (UnknownID e) {
            // expected case
        }

    }
}
