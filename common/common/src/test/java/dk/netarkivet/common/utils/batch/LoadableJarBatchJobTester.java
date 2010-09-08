/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.arc.TestInfo;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.batch.LoadableJarBatchJob;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

/**
 * Tests for the LoadableJarBatchJob class.
 *
 */
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
        		"dk.netarkivet.common.utils.batch.LoadableTestJob",
        		new ArrayList<String>(),
                new File(TestInfo.WORKING_DIR, "LoadableTestJob.jar")
                );
        OutputStream os = new ByteArrayOutputStream();
        job.initialize(os);
        assertEquals("Should have message from loaded class",
                     "initialize() called on me\n", os.toString());

        try {
            job = new LoadableJarBatchJob(
            		"dk.netarkivet.common.utils.batch.LoadableTestJob$InnerClass",
                        new ArrayList<String>(),
                new File(TestInfo.WORKING_DIR, "LoadableTestJob.jar")
                );
            job.initialize(os);
            fail("Should not be possible to load non-batchjob class");
        } catch (IOFailure e) {
            // Expected
        }

        job = new LoadableJarBatchJob(
        		"dk.netarkivet.common.utils.batch.LoadableTestJob$InnerBatchJob",
                        new ArrayList<String>(),
        		new File(TestInfo.WORKING_DIR, "LoadableTestJob.jar")
                );
        os = new ByteArrayOutputStream();
        job.initialize(os);
        assertEquals("Should have message from loaded class",
                     "initialize() called on inner\n", os.toString());
    }
    
    public void testLoadingJobWithoutPackage() {
        FileBatchJob job = new LoadableJarBatchJob(
        		"ExternalBatchSeveralClassesNoPackage",
                        new ArrayList<String>(),
                new File(TestInfo.WORKING_DIR, "ExternalBatchSeveralClassesNoPackage.jar")
                );
        OutputStream os = new ByteArrayOutputStream();
        job.initialize(os);
        
        File metadataFile = new File(TestInfo.WORKING_DIR, "2-metadata-1.arc");
        job.processFile(metadataFile, os);
    }
    public void testLoadingJobWithPackage() {
        FileBatchJob job = new LoadableJarBatchJob(
        		"batch.ExternalBatchSeveralClassesWithPackage",
                        new ArrayList<String>(),
                new File(TestInfo.WORKING_DIR, "ExternalBatchSeveralClassesWithPackage.jar")
                );
        OutputStream os = new ByteArrayOutputStream();
        job.initialize(os);
        
        File metadataFile = new File(TestInfo.WORKING_DIR, "2-metadata-1.arc");
        job.processFile(metadataFile, os);
    }

    /**
     * Tests the loadable batchjobs with arguments.
     */
    public void testJobWithArguments() {
        List<String> args = new ArrayList<String>();
        args.add(".*");
        args.add(".*[.]dk"); // url regex
        args.add("text/.*"); // mimetype regex
        FileBatchJob job = new LoadableJarBatchJob("dk.netarkivet.common.utils.batch.UrlSearch", args, 
                new File(TestInfo.WORKING_DIR, "MimeUrlSearch.jar"));
        File metadataFile = new File(TestInfo.WORKING_DIR, "input-1.arc");
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        job.initialize(os);
        job.processFile(metadataFile, os);
        job.finish(os);
        
        String result1 = os.toString();
        assertTrue("expected: Urls matched = 3, but got:\n" + result1, result1.contains("Urls matched = 3"));
        assertTrue("expected: Mimetypes mached = 4, but got:\n" + result1, result1.contains("Mimetypes matched = 4"));
        assertTrue("expected: Url and Mimetype maches = 3, but got:\n" + result1, result1.contains("Url and Mimetype matches = 3"));

        // try with different
        args.clear();
        args.add(".*");
        args.add(".*[.]dk"); // url regex
        args.add("image/.*"); // mimetype regex
        job = new LoadableJarBatchJob("dk.netarkivet.common.utils.batch.UrlSearch", args, 
                new File(TestInfo.WORKING_DIR, "MimeUrlSearch.jar"));

        os = new ByteArrayOutputStream();
        job.initialize(os);
        job.processFile(metadataFile, os);
        job.finish(os);
        
        String result2 = os.toString();
        assertFalse("Expected results to be different, but both was \n" 
                + result1, result1.equals(result2));
        
        // try again with original
        args.clear();
        args.add(".*");
        args.add(".*[.]dk"); // url regex
        args.add("text/.*"); // mimetype regex
        job = new LoadableJarBatchJob("dk.netarkivet.common.utils.batch.UrlSearch", args, 
                new File(TestInfo.WORKING_DIR, "MimeUrlSearch.jar"));

        os = new ByteArrayOutputStream();
        job.initialize(os);
        job.processFile(metadataFile, os);
        job.finish(os);
        
        String result3 = os.toString();
        assertTrue("Expected results to be identical: Results 1\n" + result1 
                + "\nResults3:\n" + result3, result1.equals(result3));
    }
}
