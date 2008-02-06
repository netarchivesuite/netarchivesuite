/* $Id$
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
package dk.netarkivet.common.utils.arc;

/* $Id$
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

import java.io.File;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

import junit.framework.*;

import dk.netarkivet.testutils.preconfigured.MoveTestFiles;


public class LoadableJarBatchJobTester extends TestCase {
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR,
                                          TestInfo.WORKING_DIR);
    public LoadableJarBatchJobTester(String s) {
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

    public void testInitialize() {
        FileBatchJob job = new LoadableJarBatchJob(
                new File(TestInfo.WORKING_DIR, "LoadableTestJob.jar"),
                "LoadableTestJob");
        OutputStream os = new ByteArrayOutputStream();
        job.initialize(os);
        assertEquals("Should have message from loaded class",
                     "initialize() called on inner\n", os.toString());
    }

    public void testLoadableJarBatchJob() throws Exception {
        FileBatchJob job = new LoadableJarBatchJob(
                new File(TestInfo.WORKING_DIR, "LoadableTestJob.jar"),
                "LoadableTestJob");
        assertNotNull("Should have loaded a job from JAR file",
                      ((LoadableJarBatchJob) job).loadedJob);
        fail("Test not implemented yet");
    }
}
