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
package dk.netarkivet.common.utils;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import junit.framework.TestCase;

import dk.netarkivet.testutils.TestFileUtils;

/**
 * Unit tests for the abstract class FilterIterator.
 * For this purpose it uses a private class TestIterator
 * that extends FilterIterator.
 */
public class FilterIteratorTester extends TestCase {
    public FilterIteratorTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
        FileUtils.removeRecursively(TestInfo.TEMPDIR);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.DATADIR, TestInfo.TEMPDIR);
    }

    public void tearDown() throws Exception {
        FileUtils.removeRecursively(TestInfo.TEMPDIR);
        super.tearDown();
    }

    /** This test iterator filters out non-existing files. */
    private class TestIterator extends FilterIterator<File, File> {
        public TestIterator(File[] files) {
            super(Arrays.asList(files).iterator());
        }

        /**
         * Gives an object created from the given file, or null.
         *
         * @param o The file to read
         * @return An object of the type iterated over by the list, or null
         *         if the file does not exist or cannot be used to create
         *         an appropriate object.
         */
        protected File filter(File o) {
            if (o.exists()) {
                return o;
            } else {
                return null;
            }

        }

    }

    /** Check that the empty list is handled correctly.
     * Tests bug 193: Even if hasNext() returns true, there may not be a
     * next element.
     */
    public void testNextEmptyList() {
        Iterator<File> list = new TestIterator(new File[0]);
        assertFalse("List should not claim more elements "
                    + "when the list is empty",
                list.hasNext());

        try {
            list.next();
            fail("Should get NoSuchElementException");
        } catch (NoSuchElementException expected) {
            // Expected case
        }
    }

    /** Tests that an element can be taken from the list.
     */
    public void testNextOneElementList() {
        Iterator<File> list = new TestIterator(
                new File[] {TestInfo.XML_FILE_1});
        assertTrue("List should give the next file when it exists",
                list.hasNext());
        Object d = list.next();
        assertNotNull("We should get a file from the list", d);
        assertFalse("List should have no more file", list.hasNext());

        try {
            list.next();
            fail("Should get NoSuchElementException");
        } catch (NoSuchElementException expected) {
            // Expected case
        }
    }
    
    /** 
     * Tests that a bad element cannot be taken from the list.
     */
    public void testNextBadElementList() {
        Iterator<File> list = new TestIterator(new File[] {
                TestInfo.NON_EXISTING_FILE });
        assertFalse("List should not claim more elements "
                    + "when no existing objects are in the list",
                list.hasNext());
        try {
            list.next();
            fail("Should get NoSuchElementException");
        } catch (NoSuchElementException expected) {
            // Expected case
        }
    }

    /** 
     * Tests that a disappearing element doesn't confuse the list.
     */
    public void testNextDisappearingElementList() {
        Iterator<File> list = new TestIterator(new File[] {
                TestInfo.NON_EXISTING_FILE,
                TestInfo.DATADIR,
                TestInfo.XML_FILE_1});
        assertTrue("Should be able to skip past bad dir",
                list.hasNext());
        Object d = list.next();

        assertEquals("Should get the first existinf file first",
                TestInfo.DATADIR, d);
        assertTrue("Should see one more element", list.hasNext());
        FileUtils.removeRecursively(TestInfo.XML_FILE_1);
        assertFalse("File should be gone", TestInfo.XML_FILE_1.exists());
        d = list.next();
        assertNotNull("Should get file object even though it has disappeared",
                d);
        assertEquals("Should get correct domain", TestInfo.XML_FILE_1, d);
        assertFalse("Should not get more elements now", list.hasNext());
    }
}