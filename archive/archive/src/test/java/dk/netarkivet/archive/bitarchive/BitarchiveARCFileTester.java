/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.archive.bitarchive;

import java.io.File;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Unit tests for the class BitarchiveARCFile.
 *
 */
public class BitarchiveARCFileTester extends TestCase {
    private static final File EXISTING_FILE = 
        new File(TestInfo.ORIGINALS_DIR, "Upload2.ARC");
    private static final File NON__EXISTING__FILE = new File("Test2");

    public BitarchiveARCFileTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    public void testBitArchiveArcFile() throws Exception {
        try {
            new BitarchiveARCFile("Test", null);
            fail("Should fail on null argument");
        } catch (ArgumentNotValid e) {
            //expected
        }

        try {
            new BitarchiveARCFile(null, new File("Test"));
            fail("Should fail on null argument");
        } catch (ArgumentNotValid e) {
            //expected
        }

        try {
            new BitarchiveARCFile("", new File("Test"));
            fail("Should fail on empty argument");
        } catch (ArgumentNotValid e) {
            //expected
        }

        try {
            new BitarchiveARCFile("Test", new File(""));
            fail("Should fail on empty argument");
        } catch (ArgumentNotValid e) {
            //expected
        }

        BitarchiveARCFile f = new BitarchiveARCFile(
                "Test1", NON__EXISTING__FILE);
        assertEquals("File names should be the same",
                     f.getName(), "Test1");
        assertEquals("File paths should be the same",
                     f.getFilePath().getCanonicalPath(),
                     NON__EXISTING__FILE.getCanonicalPath());
        assertFalse("File should not exist", f.exists());

        BitarchiveARCFile f2 = new BitarchiveARCFile("Test1", EXISTING_FILE);
        assertFalse("File should exist", f.exists());
        assertEquals("Should get right file size", 136663, f2.getSize());

    }
}