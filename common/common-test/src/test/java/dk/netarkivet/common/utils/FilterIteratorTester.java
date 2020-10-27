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
package dk.netarkivet.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.testutils.TestFileUtils;

/**
 * Unit tests for the abstract class FilterIterator. For this purpose it uses a private class TestIterator that extends
 * FilterIterator.
 */
public class FilterIteratorTester {

    @Before
    public void setUp() throws Exception {
        FileUtils.removeRecursively(TestInfo.TEMPDIR);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.DATADIR, TestInfo.TEMPDIR);
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.removeRecursively(TestInfo.TEMPDIR);
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
         * @return An object of the type iterated over by the list, or null if the file does not exist or cannot be used
         * to create an appropriate object.
         */
        protected File filter(File o) {
            if (o.exists()) {
                return o;
            } else {
                return null;
            }

        }

    }

    /**
     * Check that the empty list is handled correctly. Tests bug 193: Even if hasNext() returns true, there may not be a
     * next element.
     */
    @Test(expected = NoSuchElementException.class)
    public void testNextEmptyList() {
        Iterator<File> list = new TestIterator(new File[0]);
        assertFalse("List should not claim more elements " + "when the list is empty", list.hasNext());

        list.next();
        fail("Should get NoSuchElementException");
    }

    /**
     * Tests that an element can be taken from the list.
     */
    @Test(expected = NoSuchElementException.class)
    public void testNextOneElementList() {
        Iterator<File> list = new TestIterator(new File[] {TestInfo.XML_FILE_1});
        assertTrue("List should give the next file when it exists", list.hasNext());
        Object d = list.next();
        assertNotNull("We should get a file from the list", d);
        assertFalse("List should have no more file", list.hasNext());

        list.next();
        fail("Should get NoSuchElementException");
    }

    /**
     * Tests that a bad element cannot be taken from the list.
     */
    @Test(expected = NoSuchElementException.class)
    public void testNextBadElementList() {
        Iterator<File> list = new TestIterator(new File[] {TestInfo.NON_EXISTING_FILE});
        assertFalse("List should not claim more elements " + "when no existing objects are in the list", list.hasNext());
        list.next();
        fail("Should get NoSuchElementException");
    }

    /**
     * Tests that a disappearing element doesn't confuse the list.
     */
    @Test
    public void testNextDisappearingElementList() {
        Iterator<File> list = new TestIterator(new File[] {TestInfo.NON_EXISTING_FILE, TestInfo.DATADIR,
                TestInfo.XML_FILE_1});
        assertTrue("Should be able to skip past bad dir", list.hasNext());
        Object d = list.next();

        assertEquals("Should get the first existinf file first", TestInfo.DATADIR, d);
        assertTrue("Should see one more element", list.hasNext());
        FileUtils.removeRecursively(TestInfo.XML_FILE_1);
        assertFalse("File should be gone", TestInfo.XML_FILE_1.exists());
        d = list.next();
        assertNotNull("Should get file object even though it has disappeared", d);
        assertEquals("Should get correct domain", TestInfo.XML_FILE_1, d);
        assertFalse("Should not get more elements now", list.hasNext());
    }

    /** Test that remove throws UnsupportedOperationException. */
    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveOperation() {
        Iterator<File> list = new TestIterator(new File[] {TestInfo.NON_EXISTING_FILE, TestInfo.DATADIR,
                TestInfo.XML_FILE_1});
        list.remove();
        fail("Should have thrown UnsupportedOperationException");
    }
}
