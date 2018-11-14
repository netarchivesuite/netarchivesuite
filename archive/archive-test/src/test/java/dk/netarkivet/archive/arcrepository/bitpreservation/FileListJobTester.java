/*
 * #%L
 * Netarchivesuite - archive - test
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

package dk.netarkivet.archive.arcrepository.bitpreservation;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.batch.FileListJob;
import dk.netarkivet.testutils.Serial;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

/**
 * Unit tests for FileListJob. TODO Move unittest to common.utils.batch
 */
public class FileListJobTester {
    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);

    @Before
    public void setUp() throws Exception {
        mtf.setUp();
    }

    @After
    public void tearDown() throws Exception {
        mtf.tearDown();
    }

    /**
     * Test that FileBatchJob outputs the right data.
     */
    @Test
    public void testProcessFile() {
        File bitarchive = new File(TestInfo.WORKING_DIR, "bitarchive1");
        File arcfile = new File(bitarchive, "integrity1.ARC");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileBatchJob job = new FileListJob();
        job.processFile(arcfile, baos);
        assertEquals("Job should return filename + newline", arcfile.getName() + "\n", baos.toString());
    }

    /**
     * Tests serializability of this class, under the assumption that its toString() method is dependent on its entire
     * relevant state.
     *
     * @throws Exception On any error
     */
    @Test
    public void testSerializable() throws Exception {
        FileBatchJob job = new FileListJob();
        FileBatchJob job2 = (FileBatchJob) Serial.serial(job);
        assertEquals("Should have same toString()", job.toString(), job2.toString());
    }

}
