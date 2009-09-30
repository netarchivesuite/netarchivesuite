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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.security.AccessControlException;

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
                new File(TestInfo.WORKING_DIR, "LoadableTestJob.class"));
        OutputStream os = new ByteArrayOutputStream();
        job.initialize(os);
        assertEquals("Should have message from loaded class",
                     "initialize() called on me\n", os.toString());
    }

    public void testToStringOnJob() {
        FileBatchJob job = new LoadableFileBatchJob(
                new File(TestInfo.WORKING_DIR, "LoadableTestJob.class"));
        OutputStream os = new ByteArrayOutputStream();
        job.initialize(os);
        assertTrue("No proper toString is defined in the job " + job , job.toString().indexOf("@") < 0); 
    }
    
    public void testProcessFile() {
        FileBatchJob job = new LoadableFileBatchJob(
                new File(TestInfo.WORKING_DIR, "LoadableTestJob.class"));
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
                new File(TestInfo.WORKING_DIR, "LoadableTestJob.class"));
        assertEquals("Should have name ",
                "dk.netarkivet.common.utils.batch.LoadableFileBatchJob processing LoadableTestJob.class", 
                job.toString());
    }    
    
    public void testFinish() {
        FileBatchJob job = new LoadableFileBatchJob(
                new File(TestInfo.WORKING_DIR, "LoadableTestJob.class"));
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
                new File(TestInfo.WORKING_DIR, "LoadableTestJob.class"));
        assertNull("Should not have loaded a job before transfer",
                      ((LoadableFileBatchJob) job).loadedJob);

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
}
