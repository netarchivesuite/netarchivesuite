/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.common.utils.batch;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import static dk.netarkivet.testutils.CollectionUtils.list;
import dk.netarkivet.testutils.Serial;

/**
 * A tester suite for an abstract class? Yes, it's true!
 *
 */

public class FileBatchJobTester extends TestCase {

    /**
     * Tests that the simplest concretization of this class is serializable
     */
    public void testSerializability() throws IOException, ClassNotFoundException {
        FileBatchJob f1 = new ConcreteFileBatchJob();
        f1.processOnlyFileNamed("helloWorld");
        FileBatchJob f2 = (FileBatchJob) Serial.serial(f1);
        assertEquals("Should have the same state before and after " +
                "serialization ", significantState(f1), significantState(f2));
    }

    private String significantState(FileBatchJob f) {
        return f.getFilenamePattern().pattern();
    }

    public void testProcessOnlyFilesNamed() throws Exception {
        FileBatchJob f1 = new ConcreteFileBatchJob();
        assertEquals("Should match everything from the start",
                ".*", f1.getFilenamePattern().pattern());
        List<String> filenames = new ArrayList<String>();
        filenames.add("foo");
        filenames.add("bar");
        f1.processOnlyFilesNamed(filenames);
        assertEquals("Should have set a regular expression matching the files",
                "(\\Qfoo\\E|\\Qbar\\E)", f1.getFilenamePattern().pattern());

        f1.processOnlyFilesNamed(new ArrayList<String>());
        assertEquals("Should give the empty regexp back",
                "()", f1.getFilenamePattern().pattern());

        f1.processOnlyFilesNamed(null);
        assertEquals("Should give an all-matching regexp back",
                ".*", f1.getFilenamePattern().pattern());
    }

    public void testProcessOnlyFileNamed() throws Exception {
        FileBatchJob f1 = new ConcreteFileBatchJob();
        assertEquals("Should match all files from start",
                ".*", f1.getFilenamePattern().pattern());

        f1.processOnlyFileNamed("qux");
        assertEquals("Should have set a regular expression matching the file",
                "\\Qqux\\E", f1.getFilenamePattern().pattern());

        // Setting this to check that it properly overrides.
        List<String> filenames = new ArrayList<String>();
        filenames.add("foo");
        filenames.add("bar");
        f1.processOnlyFilesNamed(filenames);

        f1.processOnlyFileNamed("baz");
        assertEquals("Should have set a regular expression matching the file",
                "\\Qbaz\\E", f1.getFilenamePattern().pattern());

        f1.processOnlyFileNamed("qux");
        assertEquals("Should have set a regular expression matching the file",
                "\\Qqux\\E", f1.getFilenamePattern().pattern());
    }

    public void testProcessOnlyFilesMatching_Pattern() throws Exception {
        FileBatchJob f1 = new ConcreteFileBatchJob();
        assertEquals("Should have .* pattern from beginning",
                ".*", f1.getFilenamePattern().pattern());

        f1.processOnlyFilesMatching("foo*bar");
        assertEquals("Should have stated regexp as pattern",
                "foo*bar", f1.getFilenamePattern().pattern());
    }

    public void testProcessOnlyFilesMatching_ListOfPattern() throws Exception {
        FileBatchJob f1 = new ConcreteFileBatchJob();
        assertEquals("Should have .* pattern from beginning",
                ".*", f1.getFilenamePattern().pattern());

        f1.processOnlyFilesMatching(list("foo", "bar"));
        assertEquals("Should get combined pattern after setting",
                "(foo|bar)", f1.getFilenamePattern().pattern());

        f1.processOnlyFilesMatching(list("foo*(bar|baz)", "qu*."));
        assertEquals("Should get combined pattern after setting, no escapes",
                "(foo*(bar|baz)|qu*.)", f1.getFilenamePattern().pattern());

        // Check that we can handle large patterns (thousands of cases)
        List<String> patterns = new ArrayList<String>(30000);
        for (int i = 0; i < 30000; i++) {
            patterns.add(i + "-[0-9]+-[0-9]+-[0-9]+-.*");
        }
        f1.processOnlyFilesMatching(patterns);
        assertTrue("Should have combined patterns into one regexp",
                f1.getFilenamePattern().pattern().startsWith("("
                        + patterns.get(0) + "|" + patterns.get(1)));
    }

    private static class ConcreteFileBatchJob extends FileBatchJob {

        public void initialize(OutputStream os) {
            throw new NotImplementedException("Not implemented");
        }

        public boolean processFile(File file, OutputStream os) {
            throw new NotImplementedException("Not implemented");
        }

        public void finish(OutputStream os) {
            throw new NotImplementedException("Not implemented");
        }
    }

}
