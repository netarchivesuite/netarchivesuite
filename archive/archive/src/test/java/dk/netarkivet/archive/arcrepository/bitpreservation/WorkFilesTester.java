/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.archive.arcrepository.bitpreservation;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.CollectionAsserts;
import dk.netarkivet.testutils.TestFileUtils;

/**
 * Unittest for the class WorkFiles.
 *
 */
public class WorkFilesTester extends TestCase {
    public void setUp() throws Exception {
        super.setUp();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                TestInfo.WORKING_DIR);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
    }

    public void testGetSortedFile() throws Exception {
        File f = new File(TestInfo.WORKING_DIR, "does-not-exist");
        assertFalse("Input file should not exist at start",
                f.exists());
        final List<String> list1 = Arrays.asList(new String[] {
                            "foo", "bar", "baz", "qux", "qux"
                        });
        FileUtils.writeCollectionToFile(f, list1);
        File sortedFileRaw =
                new File(TestInfo.WORKING_DIR, "sorted.txt").getAbsoluteFile();
        assertFalse("Sorted version should not exist before asking for it",
                sortedFileRaw.exists());
        File sortedFileGen = WorkFiles.getSortedFile(f).getAbsoluteFile();
        assertEquals("Sorted file should have the expected name",
                sortedFileRaw, sortedFileGen);
        assertTrue("Sorted version should exist after asking for it",
                sortedFileGen.exists());
        Collections.sort(list1);
        CollectionAsserts.assertIteratorEquals("Should have same order contents",
                list1.iterator(),
                FileUtils.readListFromFile(sortedFileGen).iterator());

        final List<String> list2 = Arrays.asList(new String[] {
            "foo2", "bar3", "baz4", "qux", "qux"
        });

        FileUtils.writeCollectionToFile(f, list2);
        File sortedFileGen2 = WorkFiles.getSortedFile(f);
        assertEquals("Sorted file should have the expected name",
                sortedFileRaw, sortedFileGen2);
        assertTrue("Sorted version should exist after asking for it",
                sortedFileGen2.exists());
        Collections.sort(list2);
        CollectionAsserts.assertIteratorEquals("Should have same order contents",
                list2.iterator(),
                FileUtils.readListFromFile(sortedFileGen2).iterator());

        // Test that we regenerate when needing to
        sortedFileGen2.delete();
        FileUtils.writeCollectionToFile(f, list1);
        assertFalse("Sorted version should not exist before asking for it",
                sortedFileRaw.exists());
        sortedFileGen = WorkFiles.getSortedFile(f);
        assertEquals("Sorted file should have the expected name",
                sortedFileRaw, sortedFileGen);
        assertTrue("Sorted version should exist after asking for it",
                sortedFileGen.exists());
        Collections.sort(list1);
        CollectionAsserts.assertIteratorEquals("Should have same order contents",
                list1.iterator(),
                FileUtils.readListFromFile(sortedFileGen).iterator());

        // Test that it fails without input file
        f.delete();
        try {
            sortedFileGen = WorkFiles.getSortedFile(f);
            fail("Should have thrown exception on missing input");
        } catch (IOFailure e) {
            // expected
        }

        final List<String> empty = Collections.emptyList();
        FileUtils.writeCollectionToFile(f, empty);
        sortedFileGen = WorkFiles.getSortedFile(f);
        assertEquals("Should have no contents",
                empty, FileUtils.readListFromFile(sortedFileGen));
    }
}
