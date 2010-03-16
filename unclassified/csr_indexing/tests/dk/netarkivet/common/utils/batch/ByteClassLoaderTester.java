/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
import junit.framework.TestCase;

import dk.netarkivet.common.utils.arc.TestInfo;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

public class ByteClassLoaderTester extends TestCase {
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
    public ByteClassLoaderTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
        mtf.setUp();
    }

    public void tearDown() throws Exception {
        mtf.tearDown();
        super.tearDown();
    }

    public void testDefineClass() throws Exception {
        ByteClassLoader loader
                = new ByteClassLoader(new File(TestInfo.WORKING_DIR, "LoadableTestJob.class"));
        Class<LoadableTestJob> c = loader.defineClass();
        assertEquals("Class name should be correct",
                     "dk.netarkivet.common.utils.batch.LoadableTestJob",
                     c.getName());
        // Note that we can't cast it to a LoadableTestJob, as we've already
        // loaded that class through a different classloader, so they aren't
        // quite the same.
        FileBatchJob job = c.newInstance();

        try {
            loader = new ByteClassLoader(TestInfo.INPUT_1);
            c = loader.defineClass();
            fail("Should have died trying to load illegal class");
        } catch (ClassFormatError e) {
            // expected
        }
    }
}
