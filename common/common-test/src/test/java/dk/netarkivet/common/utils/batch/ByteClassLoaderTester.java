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
package dk.netarkivet.common.utils.batch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import dk.netarkivet.common.utils.arc.TestInfo;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

@SuppressWarnings({"unused", "unchecked"})
public class ByteClassLoaderTester {
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);

    @Before
    public void setUp() throws Exception {
        mtf.setUp();
    }

    @After
    public void tearDown() throws Exception {
        mtf.tearDown();
    }

    @Test(expected = ClassFormatError.class)
    @Ignore("Surefire considered LoadableTestJob.class a test in the wrong location, and failed the build")
    public void testDefineClass() throws Exception {
        ByteClassLoader loader = new ByteClassLoader(new File(TestInfo.WORKING_DIR, "LoadableTestJob.class"));
        Class<LoadableTestJob> c = loader.defineClass();
        assertEquals("Class name should be correct", "dk.netarkivet.common.utils.batch.LoadableTestJob", c.getName());
        // Note that we can't cast it to a LoadableTestJob, as we've already
        // loaded that class through a different classloader, so they aren't
        // quite the same.
        FileBatchJob job = c.newInstance();

        loader = new ByteClassLoader(TestInfo.INPUT_1);
        c = loader.defineClass();
        fail("Should have died trying to load illegal class");
    }
}
