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
package dk.netarkivet.archive.arcrepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/**
 * Unit-test to test the BatchStatus class.
 */
public class BatchStatusTester {
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.DISTRIBUTE_ARCREPOSITORY_ORIGINALS_DIR,
    		TestInfo.DISTRIBUTE_ARCREPOSITORY_WORKING_DIR);

    private UseTestRemoteFile utrf = new UseTestRemoteFile();

    @Before
    public void setUp() {
        mtf.setUp();
        utrf.setUp();
    }

    @After
    public void tearDown() {
        utrf.tearDown();
        mtf.tearDown();
    }

    @Test
    public void testCopyResults() throws IOException {
        List<File> emptyList = Collections.emptyList();
        File tmpFile = new File(TestInfo.DISTRIBUTE_ARCREPOSITORY_WORKING_DIR, "newFile");
        String fileContents = FileUtils.readFile(TestInfo.DISTRIBUTE_ARCREPOSITORY_SAMPLE_FILE);
        TestRemoteFile lrf = new TestRemoteFile(TestInfo.DISTRIBUTE_ARCREPOSITORY_SAMPLE_FILE,
                false, false, false);
        BatchStatus bs = new BatchStatus("ONE", emptyList, 1, lrf, new ArrayList<FileBatchJob.ExceptionOccurrence>(0));
        bs.copyResults(tmpFile);
        FileAsserts.assertFileContains("Should have copied result contents", fileContents, tmpFile);
        assertTrue("Source (remote) file should be deleted", lrf.isDeleted());
        FileUtils.copyFile(tmpFile, TestInfo.DISTRIBUTE_ARCREPOSITORY_SAMPLE_FILE);

        File noSuchFile = new File(TestInfo.DISTRIBUTE_ARCREPOSITORY_WORKING_DIR, "noSuchFile");
        try {
            bs.copyResults(noSuchFile);
            fail("Should have thrown exception on already-read file");
        } catch (IllegalState e) {
            // expected
        }
        assertFalse("Should not have made a result file", noSuchFile.exists());

        lrf = new TestRemoteFile(TestInfo.DISTRIBUTE_ARCREPOSITORY_EMPTY_FILE, false, false,
                false);
        bs = new BatchStatus("KB", emptyList, 1, lrf, new ArrayList<FileBatchJob.ExceptionOccurrence>(0));
        bs.copyResults(tmpFile);
        assertEquals("Should have zero-length file", 0, tmpFile.length());
        assertTrue("Source (remote) file should be deleted", lrf.isDeleted());

        bs = new BatchStatus("KB", emptyList, 0, null, new ArrayList<FileBatchJob.ExceptionOccurrence>(0));
        try {
            bs.copyResults(noSuchFile);
            fail("Should have thrown exception on missing file");
        } catch (IllegalState e) {
            // expected
        }
        assertFalse("Should not have made a result file", noSuchFile.exists());

        try {
            bs.copyResults(null);
            fail("Should have throw ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            // expected
        }
        assertFalse("Should not have made a result file", noSuchFile.exists());
    }

    @Test
    public void testAppendResults() throws IOException {
        List<File> emptyList = Collections.emptyList();
        String fileContents = FileUtils.readFile(TestInfo.DISTRIBUTE_ARCREPOSITORY_SAMPLE_FILE);
        TestRemoteFile lrf = new TestRemoteFile(TestInfo.DISTRIBUTE_ARCREPOSITORY_SAMPLE_FILE,
                false, false, false);
        BatchStatus bs = new BatchStatus("KB", emptyList, 0, lrf, new ArrayList<FileBatchJob.ExceptionOccurrence>(0));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bs.appendResults(out);
        assertEquals("Should have same contents in outputstream", fileContents, out.toString());
        assertTrue("Should have deleted remotefile", lrf.isDeleted());

        try {
            bs.appendResults(new OutputStream() {
                public void write(int b) throws IOException {
                    fail("Should not write anything to outputstream");
                }
            });
            fail("Should have failed on already read file");
        } catch (IllegalState e) {
            // expected
        }

        lrf = new TestRemoteFile(TestInfo.DISTRIBUTE_ARCREPOSITORY_EMPTY_FILE, false, false,
                false);
        bs = new BatchStatus("KB", emptyList, 1, lrf, new ArrayList<FileBatchJob.ExceptionOccurrence>(0));
        bs.appendResults(new OutputStream() {
            public void write(int b) throws IOException {
                fail("Should not write anything to outputstream");
            }
        });

        bs = new BatchStatus("KB", emptyList, 0, null, new ArrayList<FileBatchJob.ExceptionOccurrence>(0));
        try {
            bs.appendResults(new OutputStream() {
                public void write(int b) throws IOException {
                    fail("Should not write anything to outputstream");
                }
            });
            fail("Should have failed on no result file");
        } catch (IllegalState e) {
            // expected
        }

        try {
            bs.copyResults(null);
            fail("Should have throw ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            // expected
        }

    }

    @Test
    public void testHasResultFile() throws IOException {
        List<File> emptyList = Collections.emptyList();
        TestRemoteFile lrf = new TestRemoteFile(TestInfo.DISTRIBUTE_ARCREPOSITORY_SAMPLE_FILE,
                false, false, false);
        BatchStatus bs = new BatchStatus("KB", emptyList, 0, lrf, new ArrayList<FileBatchJob.ExceptionOccurrence>(0));
        File tmpFile = new File(TestInfo.DISTRIBUTE_ARCREPOSITORY_WORKING_DIR, "newFile");
        assertTrue("Should have result file when given", bs.hasResultFile());
        bs.copyResults(tmpFile);
        assertFalse("Should not have result file after copying", bs.hasResultFile());
        FileUtils.copyFile(tmpFile, TestInfo.DISTRIBUTE_ARCREPOSITORY_SAMPLE_FILE);

        lrf = new TestRemoteFile(TestInfo.DISTRIBUTE_ARCREPOSITORY_SAMPLE_FILE, false, false,
                false);
        bs = new BatchStatus("KB", emptyList, 0, lrf, new ArrayList<FileBatchJob.ExceptionOccurrence>(0));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue("Should have result file when given", bs.hasResultFile());
        bs.appendResults(out);
        assertFalse("Should not have result file after appending", bs.hasResultFile());

        bs = new BatchStatus("KB", emptyList, 0, null, new ArrayList<FileBatchJob.ExceptionOccurrence>(0));
        assertFalse("Should not have result file with no-result BS", bs.hasResultFile());
    }
}
