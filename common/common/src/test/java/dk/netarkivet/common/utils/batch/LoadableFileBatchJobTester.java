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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.arc.TestInfo;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.batch.LoadableFileBatchJob;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;


public class LoadableFileBatchJobTester extends TestCase {
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR,
                                          TestInfo.WORKING_DIR);
    private static final File FNORD_FILE = new File("fnord");

    public LoadableFileBatchJobTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
        mtf.setUp();
        //clean up fnord file if it exists from earlier failed unitTests
        if (FNORD_FILE.exists()) {
        	FileUtils.remove(FNORD_FILE);
        }
    }

    public void tearDown() throws Exception {
        mtf.tearDown();
        super.tearDown();
    }

    public void testInitialize() {
        FileBatchJob job = new LoadableFileBatchJob(
                new File(TestInfo.WORKING_DIR, "LoadableTestJob.class"), 
                new ArrayList<String>());
        OutputStream os = new ByteArrayOutputStream();
        job.initialize(os);
        assertEquals("Should have message from loaded class",
                     "initialize() called on me\n", os.toString());
    }

    public void testToStringOnJob() {
        FileBatchJob job = new LoadableFileBatchJob(
                new File(TestInfo.WORKING_DIR, "LoadableTestJob.class"),
                new ArrayList<String>());
        OutputStream os = new ByteArrayOutputStream();
        job.initialize(os);
        assertTrue("No proper toString is defined in the job " + job , job.toString().indexOf("@") < 0); 
    }
    
    public void testProcessFile() {
        FileBatchJob job = new LoadableFileBatchJob(
                new File(TestInfo.WORKING_DIR, "LoadableTestJob.class"),
                new ArrayList<String>());
        OutputStream os = new ByteArrayOutputStream();
        job.initialize(os);
        assertEquals("Should have message from loaded class",
                     "initialize() called on me\n", os.toString());
        boolean retval = job.processFile(TestInfo.INPUT_1, os);
        assertEquals("Should have added message from process",
                     "initialize() called on me\n" +
                     "processFile() called on me with input-1.arc\n",
                     os.toString());
        assertTrue("File should have odd length", retval);
        retval = job.processFile(TestInfo.INPUT_2, os);
        assertEquals("Should have added message from process",
                     "initialize() called on me\n" +
                     "processFile() called on me with input-1.arc\n" +
                     "processFile() called on me with input-2.arc\n",
                     os.toString());
        assertFalse("File should have even length", retval);
        
        //try to make hack file via loaded batch file
        try {
            retval = job.processFile(new File("makeahack"), os);
            fail("Should have been denied a hack via file");
        } catch (AccessControlException e) {
            // expected
        }
        assertFalse("Hack should not be successfull", retval);
        assertFalse("Hacked file should not exist", FNORD_FILE.exists());
    }

    public void testToString() {
        FileBatchJob job = new LoadableFileBatchJob(
                new File(TestInfo.WORKING_DIR, "LoadableTestJob.class"),
                new ArrayList<String>());
        assertEquals("Should have name ",
                "dk.netarkivet.common.utils.batch.LoadableFileBatchJob processing LoadableTestJob.class", 
                job.toString());
    }    
    
    public void testFinish() {
        FileBatchJob job = new LoadableFileBatchJob(
                new File(TestInfo.WORKING_DIR, "LoadableTestJob.class"),
                new ArrayList<String>());
        OutputStream os = new ByteArrayOutputStream();
        job.initialize(os);
        assertEquals("Should have message from loaded class",
                     "initialize() called on me\n", os.toString());
        boolean retval = job.processFile(TestInfo.INPUT_1, os);
        assertEquals("Should have added message from process",
                     "initialize() called on me\n" +
                     "processFile() called on me with input-1.arc\n",
                     os.toString());
        assertTrue("File should have odd length", retval);
        job.finish(os);
        assertEquals("Should have added message from process",
                     "initialize() called on me\n" +
                     "processFile() called on me with input-1.arc\n" +
                     "finish() called on me\n",
                     os.toString());
    }

    public void testLoadableFileBatchJob() throws Exception {
        FileBatchJob job = new LoadableFileBatchJob(
                new File(TestInfo.WORKING_DIR, "LoadableTestJob.class"),
                new ArrayList<String>());

        FileBatchJob job1;
        // Test that we can serialize then deserialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream ous = new ObjectOutputStream(baos);
        ous.writeObject(job);
        ous.close();
        baos.close();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(
                baos.toByteArray()));
        job1 = (FileBatchJob) ois.readObject();

        assertNull("Should not have loaded a job after transfer before init",
                   ((LoadableFileBatchJob) job1).loadedJob);
        assertNotNull("Should have a log after transfer before init",
                   ((LoadableFileBatchJob) job1).log);

        baos = new ByteArrayOutputStream();
        job1.initialize(baos);
        assertNotNull("Should have loaded a job after init",
                   ((LoadableFileBatchJob) job1).loadedJob);
        assertEquals("Should have message from loaded class",
                     "initialize() called on me\n", baos.toString());

        String input1 = FileUtils.readFile(TestInfo.INPUT_1);

        job1.processFile(TestInfo.INPUT_1, baos);
        assertEquals("Should have added message from process",
                     "initialize() called on me\n" +
                     "processFile() called on me with "
                     + TestInfo.INPUT_1.getName() + "\n",
                     baos.toString());
        assertEquals("Should not have disturbed the input file",
                     input1, FileUtils.readFile(TestInfo.INPUT_1));

        baos = new ByteArrayOutputStream();
        try {
            job1.processFile(new File("makeahack"), baos);
            fail("Should not have been permitted to escape");
        } catch (AccessControlException e) {
            // expected
        }
        assertEquals("Should have added message from process",
                     "processFile() called on me with makeahack\n" +
                     "Trying direct breakout.\n",
                     baos.toString());

        baos = new ByteArrayOutputStream();
        try {
            job1.processFile(new File("makeajailbreak"), baos);
            fail("Should not have been permitted to escape");
        } catch (IOFailure e) {
            // expected
        }
        assertEquals("Should have added message from process",
                     "processFile() called on me with makeajailbreak\n" +
                     "Trying indirect breakout.\n",
                     baos.toString());

        baos = new ByteArrayOutputStream();
        try {
            job1.processFile(new File("useclassloadertoclimb"), baos);
            fail("Should not have been permitted to escape");
        } catch (AccessControlException e) {
            // expected
        }
        assertEquals("Should have added message from process",
                     "processFile() called on me with useclassloadertoclimb\n" +
                     "Trying backwards breakout.\n",
                     baos.toString());

        baos = new ByteArrayOutputStream();
        final File smashable = new File(TestInfo.WORKING_DIR, "trytosmash");
        try {
            FileUtils.writeBinaryFile(smashable, "good content".getBytes());
            job1.processFile(smashable, baos);
            fail("Should not have been permitted to smash file");
        } catch (IOFailure e) {
            // expected
        }
        assertEquals("Should have added message from process",
                     "processFile() called on me with trytosmash\n" +
                     "Trying vandalism.\n",
                     baos.toString());
        assertEquals("Should not have disturbed file",
                     "good content", FileUtils.readFile(smashable));
        baos = new ByteArrayOutputStream();
        try {
            job1.processFile(new File(TestInfo.WORKING_DIR, "performexec"), baos);
            fail("Should not have been permitted to exec process");
        } catch (AccessControlException e) {
            // expected
        }
        assertEquals("Should have added message from process",
                     "processFile() called on me with performexec\n" +
                     "Trying external process.\n",
                     baos.toString());
    }
    
    /**
     * Tests the loadable batchjobs with arguments.
     */
    public void testJobWithArguments() {
        List<String> args = new ArrayList<String>();
        args.add(".*");
        args.add(".*[.]dk"); // url regex
        args.add("text/.*"); // mimetype regex
        FileBatchJob job = new LoadableFileBatchJob(new File(TestInfo.WORKING_DIR, 
                "UrlSearch.class"), args);
        File metadataFile = new File(TestInfo.WORKING_DIR, "input-1.arc");
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        job.initialize(os);
        job.processFile(metadataFile, os);
        job.finish(os);
        
        String result1 = os.toString();
        assertTrue("expected: Urls matched = 3, but got:\n" + result1, 
                result1.contains("Urls matched = 3"));
        assertTrue("expected: Mimetypes mached = 4, but got:\n" + result1, 
                result1.contains("Mimetypes matched = 4"));
        assertTrue("expected: Url and Mimetype maches = 3, but got:\n" + result1, 
                result1.contains("Url and Mimetype matches = 3"));

        // try with different
        args.clear();
        args.add(".*");
        args.add(".*[.]dk"); // url regex
        args.add("image/.*"); // mimetype regex
        job = new LoadableFileBatchJob(new File(TestInfo.WORKING_DIR, 
                "UrlSearch.class"), args);

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
        job = new LoadableFileBatchJob(new File(TestInfo.WORKING_DIR, 
                "UrlSearch.class"), args);

        os = new ByteArrayOutputStream();
        job.initialize(os);
        job.processFile(metadataFile, os);
        job.finish(os);
        
        String result3 = os.toString();
        assertTrue("Expected results to be identical: Results 1\n" + result1 
                + "\nResults3:\n" + result3, result1.equals(result3));
    }

    public void testBatchjobWithArgumentsFail() {
        // Verify that the batchjob cannot be loaded, when it requires arguments
        // but none are given. 
        try {
            new LoadableFileBatchJob(new File(TestInfo.WORKING_DIR, "UrlSearch.class"), 
                    new ArrayList<String>());
            fail("The should not be allowed. Batchjob should require arguments.");
        } catch (Exception e) {
            assertTrue("A InstantiationException should be thrown through a IOFailure: " + e, 
                    e instanceof IOFailure);
            assertTrue("A InstantiationException should be thrown through a IOFailure:" + e, 
                    e.getCause() instanceof InstantiationException);
        }
        // Verify that the batchjob does not work when bad arguments.
        try {
            List<String> arg = new ArrayList<String>();
            arg.add(".*");
            new LoadableFileBatchJob(new File(TestInfo.WORKING_DIR, "UrlSearch.class"), 
                    arg);
            fail("The should not be allowed. Batchjob should require more arguments.");
        } catch (Exception e) {
            assertTrue("A NoSuchMethod should be thrown through a IOFailure", 
                    e instanceof IOFailure);
            assertTrue("A NoSuchMethod should be thrown through a IOFailure", 
                    e.getCause() instanceof NoSuchMethodException);
        }
    }
}
