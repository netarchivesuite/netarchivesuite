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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.arc.TestInfo;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

/**
 * Tests for the LoadableJarBatchJob class.
 */
public class LoadableJarBatchJobTester {
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);

    @Before
    public void setUp() throws Exception {
        mtf.setUp();
    }

    @After
    public void tearDown() throws Exception {
        mtf.tearDown();
    }

    @Test
    public void testInitialize() {
        FileBatchJob job = new LoadableJarBatchJob("dk.netarkivet.common.utils.batch.LoadableTestJob",
                new ArrayList<String>(), new File(TestInfo.WORKING_DIR, "LoadableTestJob.jar"));
        OutputStream os = new ByteArrayOutputStream();
        job.initialize(os);
        assertEquals("Should have message from loaded class", "initialize() called on me\n", os.toString());

        try {
            job = new LoadableJarBatchJob("dk.netarkivet.common.utils.batch.LoadableTestJob$InnerClass",
                    new ArrayList<String>(), new File(TestInfo.WORKING_DIR, "LoadableTestJob.jar"));
            job.initialize(os);
            fail("Should not be possible to load non-batchjob class");
        } catch (IOFailure e) {
            // Expected
        }

        job = new LoadableJarBatchJob("dk.netarkivet.common.utils.batch.LoadableTestJob$InnerBatchJob",
                new ArrayList<String>(), new File(TestInfo.WORKING_DIR, "LoadableTestJob.jar"));
        os = new ByteArrayOutputStream();
        job.initialize(os);
        assertEquals("Should have message from loaded class", "initialize() called on inner\n", os.toString());
    }

    @Test
    public void testLoadingJobWithoutPackage() {
        FileBatchJob job = new LoadableJarBatchJob("ExternalBatchSeveralClassesNoPackage", new ArrayList<String>(),
                new File(TestInfo.WORKING_DIR, "ExternalBatchSeveralClassesNoPackage.jar"));
        OutputStream os = new ByteArrayOutputStream();
        job.initialize(os);

        File metadataFile = new File(TestInfo.WORKING_DIR, "2-metadata-1.arc");
        job.processFile(metadataFile, os);
    }

    @Test
    public void testLoadingJobWithPackage() {
        FileBatchJob job = new LoadableJarBatchJob("batch.ExternalBatchSeveralClassesWithPackage",
                new ArrayList<String>(), new File(TestInfo.WORKING_DIR, "ExternalBatchSeveralClassesWithPackage.jar"));
        OutputStream os = new ByteArrayOutputStream();
        job.initialize(os);

        File metadataFile = new File(TestInfo.WORKING_DIR, "2-metadata-1.arc");
        job.processFile(metadataFile, os);
    }

    /**
     * Tests the loadable batchjobs with arguments.
     */
    @Test
    public void testJobWithArguments() {
        List<String> args = new ArrayList<String>();
        args.add(".*");
        args.add(".*[.]dk"); // url regex
        args.add("text/.*"); // mimetype regex
        FileBatchJob job = new LoadableJarBatchJob("dk.netarkivet.common.utils.batch.UrlSearch", args, new File(
                TestInfo.WORKING_DIR, "MimeUrlSearch.jar"));
        File metadataFile = new File(TestInfo.WORKING_DIR, "input-1.arc");
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        job.initialize(os);
        job.processFile(metadataFile, os);
        job.finish(os);

        String result1 = os.toString();
        assertTrue("expected: Urls matched = 3, but got:\n" + result1, result1.contains("Urls matched = 3"));
        assertTrue("expected: Mimetypes mached = 4, but got:\n" + result1, result1.contains("Mimetypes matched = 4"));
        assertTrue("expected: Url and Mimetype maches = 3, but got:\n" + result1,
                result1.contains("Url and Mimetype matches = 3"));

        // try with different
        args.clear();
        args.add(".*");
        args.add(".*[.]dk"); // url regex
        args.add("image/.*"); // mimetype regex
        job = new LoadableJarBatchJob("dk.netarkivet.common.utils.batch.UrlSearch", args, new File(
                TestInfo.WORKING_DIR, "MimeUrlSearch.jar"));

        os = new ByteArrayOutputStream();
        job.initialize(os);
        job.processFile(metadataFile, os);
        job.finish(os);

        String result2 = os.toString();
        assertFalse("Expected results to be different, but both was \n" + result1, result1.equals(result2));

        // try again with original
        args.clear();
        args.add(".*");
        args.add(".*[.]dk"); // url regex
        args.add("text/.*"); // mimetype regex
        job = new LoadableJarBatchJob("dk.netarkivet.common.utils.batch.UrlSearch", args, new File(
                TestInfo.WORKING_DIR, "MimeUrlSearch.jar"));

        os = new ByteArrayOutputStream();
        job.initialize(os);
        job.processFile(metadataFile, os);
        job.finish(os);

        String result3 = os.toString();
        assertTrue("Expected results to be identical: Results 1\n" + result1 + "\nResults3:\n" + result3,
                result1.equals(result3));
    }
}
