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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.NoSuchElementException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.testutils.TestFileUtils;

/**
 * Unit tests for the abstract class FileArrayIterator. For this purpose it uses a private class TestIterator that
 * extends FileArrayIterator.
 */
public class FileArrayIteratorTester {

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
    private class TestIterator extends FileArrayIterator<String> {
        public TestIterator(File[] files) {
            super(files);
        }

        /**
         * Gives an object created from the given file, or null.
         *
         * @param o The file to read
         * @return An object of the type iterated over by the list, or null if the file does not exist or cannot be used
         * to create an appropriate object.
         */
        protected String filter(File o) {
            if (o.exists()) {
                return o.getName();
            } else {
                return null;
            }

        }

        @Override
        protected String getNext(File file) {
            return file.getName();
        }

    }

    /**
     * Check that the empty list is handled correctly. Tests bug 193: Even if hasNext() returns true, there may not be a
     * next element.
     */
    @Test
    public void testNextEmptyList() {
        TestIterator list = new TestIterator(new File[0]);
        assertFalse("List should not claim more elements " + "when the list is empty", list.hasNext());

        try {
            list.next();
            fail("Should get NoSuchElementException");
        } catch (NoSuchElementException expected) {
            // Expected case
        }
    }

    /**
     * Tests that an element can be taken from the list.
     */
    @Test
    public void testNextOneElementList() {
        TestIterator list = new TestIterator(new File[] {TestInfo.XML_FILE_1});
        assertTrue("List should give the next file when it exists", list.hasNext());
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
    @Test
    public void testNextBadElementList() {
        TestIterator list = new TestIterator(new File[] {TestInfo.NON_EXISTING_FILE});
        assertFalse("List should not claim more elements " + "when no existing objects are in the list", list.hasNext());
        try {
            list.next();
            fail("Should get NoSuchElementException");
        } catch (NoSuchElementException expected) {
            // Expected case
        }
    }
}
