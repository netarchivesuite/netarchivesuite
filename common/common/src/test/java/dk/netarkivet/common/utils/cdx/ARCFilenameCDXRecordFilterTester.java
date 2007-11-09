/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Performs unit tests of the ARCFilenameCDXRecordFilter Implicits tests both
 * SimpleCDXRecordFilter and CDXRecordFilter
 */
public class ARCFilenameCDXRecordFilterTester extends TestCase {

    public void testConstructor() {
        try {
            new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", "filter1");
        } catch (Exception e) {
            fail("Constuctor should not throw exception !");
        }
    }

    public void testGetFiltername() {
        SimpleCDXRecordFilter cdxfil = new ARCFilenameCDXRecordFilter(
                "NETARKIVET_00001.*", "filter1");
        assertEquals("Filtername are not the same !", cdxfil.getFilterName(),
                     "filter1");
    }

    public void testNullFiltername() {
        try {
            new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", null);
            fail("ArgumentNotValid should have been thrown !");
        } catch (ArgumentNotValid e) {
            // expected case
        }

    }

    public void testEmptyFiltername() {
        try {
            new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", "");
            fail("ArgumentNotValid should have been thrown !");
        } catch (ArgumentNotValid e) {
            // expected case
        }

    }

    public void testEmptyFilenamePattern() {
        try {
            new ARCFilenameCDXRecordFilter("", "filter1");
            fail("ArgumentNotValid should have been thrown !");
        } catch (ArgumentNotValid e) {
            // expected case
        }

    }

    public void testNullFilenamePattern() {
        try {
            new ARCFilenameCDXRecordFilter(null, "filter1");
            fail("ArgumentNotValid should have been thrown !");
        } catch (ArgumentNotValid e) {
            // expected case
        }

    }

}
