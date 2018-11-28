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
package dk.netarkivet.archive.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import javax.jms.Message;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.archive.bitarchive.distribute.BatchReplyMessage;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.testutils.TestMessageListener;
import dk.netarkivet.testutils.preconfigured.MockupJMS;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

public class RunBatchTester {
    private PreventSystemExit pse = new PreventSystemExit();
    private PreserveStdStreams pss = new PreserveStdStreams(true);
    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.DATA_DIR, TestInfo.WORKING_DIR);
    private MockupJMS mjms = new MockupJMS();
    TestMessageListener listener;
    UseTestRemoteFile rf = new UseTestRemoteFile();

    @Before
    public void setUp() {
        pss.setUp();
        pse.setUp();
        mjms.setUp();
        listener = new GetListener();
        JMSConnectionFactory.getInstance().setListener(Channels.getTheRepos(), listener);
        mtf.setUp();
        rf.setUp();
    }

    @After
    public void tearDown() {
        mtf.tearDown();
        JMSConnectionFactory.getInstance().removeListener(Channels.getTheRepos(), listener);
        mjms.tearDown();
        pss.tearDown();
        pse.tearDown();
        rf.tearDown();
    }

    /**
     * Tests whether the correct error message is given if no arguments are given.
     */
    @Test
    public void testNoArguments() {
        String expectedErrorMessage = "Missing required argument: jar or class file";
        String[] args = new String[] {};
        try {
            RunBatch.main(args);
            fail("An security Exception should be thrown, since we exit here!");
        } catch (SecurityException e) {
            // Expected
        }

        String errMsg = pss.getErr();
        int pseVal = pse.getExitValue();

        assertEquals("The exception should be 1 but was: " + pseVal, 1, pseVal);
        assertTrue("The error message should start with '" + expectedErrorMessage + "' but it is: " + errMsg,
                errMsg.startsWith(expectedErrorMessage));
    }

    /**
     * Tests whether the correct error message is given if too many arguments are given.
     */
    @Test
    public void testTooManyArguments() {
        String expectedErrorMessage = "Too many arguments";
        String[] args = new String[] {"-Jsd", "-Nasdf", "-Basdf", "-Jasdf", "-Oasdf", "-Easdf", "-Easdf", "-Jasdf",
                "-Jasdf", "-Jasdf", "-Jasdf", "-Jasdf", "-Jasdf", "-Jasdf", "-Jasdf", "-Jasdf", "-Jasdf", "-Jasdf",
                "-Jasdf", "-Jasdf", "-Jasdf", "-Jasdf", "-Jasdf", "-Jasdf", "-Jasdf", "-Jasdf", "-Jasdf", "-Jasdf",
                "-Jasdf", "-Jasdf", "-Jasdf", "-Jasdf", "-Jasdf", "-Jasdf", "-Jasdf", "-Jasdf"};
        try {
            RunBatch.main(args);
            fail("An security Exception should be thrown, since we exit here!");
        } catch (SecurityException e) {
            // Expected
        }

        String errMsg = pss.getErr();
        int pseVal = pse.getExitValue();

        assertEquals("The exception should be 1 but was: " + pseVal, 1, pseVal);
        assertTrue("The error message should start with '" + expectedErrorMessage + "' but it is: " + errMsg,
                errMsg.startsWith(expectedErrorMessage));
    }

    /**
     * Test whether the correct error message when given neither class nor jar file.
     */
    @Test
    public void testArgumentsMissingFile() {
        String expectedErrorMessage = "Missing required class file argument (-C) " + "or Jarfile argument (-J)";
        String[] args = new String[] {"-BNONEEXISTINGREPLICA"};
        try {
            RunBatch.main(args);
            fail("RunBatch always tries to 'System.exit'");
        } catch (SecurityException e) {
            // expected
        }

        String errMsg = pss.getErr();
        int pseVal = pse.getExitValue();

        assertEquals("The exception should be 1 but was: " + pseVal, 1, pseVal);
        assertTrue("The error message should start with '" + expectedErrorMessage + "' but it is: " + errMsg,
                errMsg.startsWith(expectedErrorMessage));
    }

    /**
     * Test whether the correct error message when given both class file and jar file arguments.
     */
    @Test
    public void testArgumentsBothClassAndJar() {
        String expectedErrorMessage = "Cannot use option -J and -C at the same time";
        String[] args = new String[] {"-CClassFile.class", "-JJarFile.jar"};
        try {
            RunBatch.main(args);
            fail("RunBatch always tries to 'System.exit'");
        } catch (SecurityException e) {
            // expected
        }

        String errMsg = pss.getErr();
        int pseVal = pse.getExitValue();

        assertEquals("The exception should be 1 but was: " + pseVal, 1, pseVal);
        assertTrue("The error message should start with '" + expectedErrorMessage + "' but it is: " + errMsg,
                errMsg.startsWith(expectedErrorMessage));
    }

    /**
     * Test whether the correct error message when given wrong extension to a class file.
     */
    @Test
    public void testArgumentsNotClassFile() {
        String expectedErrorMessage = "Argument '" + TestInfo.BATCH_ARG_ERROR_FILE_EXT
                + "' is not denoting a class file";
        String[] args = new String[] {"-C" + TestInfo.BATCH_ARG_ERROR_FILE_EXT};
        try {
            RunBatch.main(args);
            fail("RunBatch always tries to 'System.exit'");
        } catch (SecurityException e) {
            // expected
        }

        String errMsg = pss.getErr();
        int pseVal = pse.getExitValue();

        assertEquals("The exception should be 1 but was: " + pseVal, 1, pseVal);
        assertTrue("The error message should start with '" + expectedErrorMessage + "' but it is: " + errMsg,
                errMsg.startsWith(expectedErrorMessage));
    }

    /**
     * Test whether the correct error message when given an unreadable class file.
     */
    @Test
    public void testArgumentsCannotReadClassfile() {
        String expectedErrorMessage = "Cannot read class file: '" + TestInfo.BATCH_C_ARG_NOREAD_FILE + "'";
        String[] args = new String[] {"-C" + TestInfo.BATCH_C_ARG_NOREAD_FILE};
        try {
            RunBatch.main(args);
            fail("RunBatch always tries to 'System.exit'");
        } catch (SecurityException e) {
            // expected
        }

        String errMsg = pss.getErr();
        int pseVal = pse.getExitValue();

        assertEquals("The exception should be 1 but was: " + pseVal, 1, pseVal);
        assertTrue("The error message should start with '" + expectedErrorMessage + "' but it is: " + errMsg,
                errMsg.startsWith(expectedErrorMessage));
    }

    /**
     * Test whether the correct error message when given no -N argument.
     */
    @Test
    public void testArgumentsJarWithoutMethod() {
        String expectedErrorMessage = "Using option -J also requires" + "option -N (the name of the class).";
        String[] args = new String[] {"-J" + TestInfo.BATCH_ARG_ERROR_FILE_EXT};
        try {
            RunBatch.main(args);
            fail("RunBatch always tries to 'System.exit'");
        } catch (SecurityException e) {
            // expected
        }

        String errMsg = pss.getErr();
        int pseVal = pse.getExitValue();

        assertEquals("The exception should be 1 but was: " + pseVal, 1, pseVal);
        assertTrue("The error message should start with '" + expectedErrorMessage + "' but it is: " + errMsg,
                errMsg.startsWith(expectedErrorMessage));
    }

    /**
     * Test whether the correct error message when given wrong extension on jar file.
     */
    @Test
    public void testArgumentsJarWrongExtension() {
        String expectedErrorMessage = "Argument '" + TestInfo.BATCH_ARG_ERROR_FILE_EXT + "' is not denoting a jar file";
        String[] args = new String[] {"-J" + TestInfo.BATCH_ARG_ERROR_FILE_EXT, "-Nerror"};
        try {
            RunBatch.main(args);
            fail("RunBatch always tries to 'System.exit'");
        } catch (SecurityException e) {
            // expected
        }

        String errMsg = pss.getErr();
        int pseVal = pse.getExitValue();

        assertEquals("The exception should be 1 but was: " + pseVal, 1, pseVal);
        assertTrue("The error message should start with '" + expectedErrorMessage + "' but it is: " + errMsg,
                errMsg.startsWith(expectedErrorMessage));
    }

    /**
     * Test whether the correct error message when given unreadable jar file.
     */
    @Test
    public void testArgumentsUnreadableJar() {
        String expectedErrorMessage = "Cannot read jar file: '" + TestInfo.BATCH_J_ARG_NOREAD_FILE + "'";
        String[] args = new String[] {"-J" + TestInfo.BATCH_J_ARG_NOREAD_FILE, "-Nerror"};
        try {
            RunBatch.main(args);
            fail("RunBatch always tries to 'System.exit'");
        } catch (SecurityException e) {
            // expected
        }

        String errMsg = pss.getErr();
        int pseVal = pse.getExitValue();

        assertEquals("The exception should be 1 but was: " + pseVal, 1, pseVal);
        assertTrue("The error message should start with '" + expectedErrorMessage + "' but it is: " + errMsg,
                errMsg.startsWith(expectedErrorMessage));
    }

    /**
     * Test whether the correct error message when wrong method in jar file.
     */
    @Test
    public void testArgumentsWrongMethodInJarFile() {
        String expectedErrorMessage = "Cannot create batchjob '" + TestInfo.BATCH_TEST_JAR_ERROR_CLASS
                + "' from the jarfiles '" + TestInfo.BATCH_TEST_JAR_FILE.getAbsolutePath() + "'";
        String[] args = new String[] {"-J" + TestInfo.BATCH_TEST_JAR_FILE.getAbsolutePath(),
                "-N" + TestInfo.BATCH_TEST_JAR_ERROR_CLASS};
        try {
            RunBatch.main(args);
            fail("RunBatch always tries to 'System.exit'");
        } catch (SecurityException e) {
            // expected
        }

        String errMsg = pss.getErr();
        int pseVal = pse.getExitValue();

        assertEquals("The exception should be 1 but was: " + pseVal, 1, pseVal);
        assertTrue("The error message should start with '" + expectedErrorMessage + "' but it is: " + errMsg,
                errMsg.startsWith(expectedErrorMessage));
    }

    /**
     * Test whether the correct error message when wrong replica.
     */
    @Test
    public void testArgumentsUnknownReplica() {
        String expectedErrorMessage = "Unknown replica name '" + TestInfo.BATCH_REPLICA_ERROR
                + "', known replicas are ";
        String[] args = new String[] {"-J" + TestInfo.BATCH_TEST_JAR_FILE.getAbsolutePath(),
                "-N" + TestInfo.BATCH_TEST_JAR_GOOD_CLASS, "-B" + TestInfo.BATCH_REPLICA_ERROR};
        try {
            RunBatch.main(args);
            fail("RunBatch always tries to 'System.exit'");
        } catch (SecurityException e) {
            // expected
        }

        String errMsg = pss.getErr();
        int pseVal = pse.getExitValue();

        assertEquals("The exception should be 1 but was: " + pseVal, 1, pseVal);
        assertTrue("The error message should start with '" + expectedErrorMessage + "' but it is: " + errMsg,
                errMsg.startsWith(expectedErrorMessage));
    }

    /**
     * Test whether the correct error message when sending to a checksum replica.
     */
    @Test
    public void testArgumentsChecksumReplica() {
        String expectedErrorMessage = "Can only send a batchjob to a " + "bitarchive replica, and '"
                + Replica.getReplicaFromName(TestInfo.BATCH_CS_REPLICA_NAME) + "' is of the type '"
                + Replica.getReplicaFromName(TestInfo.BATCH_CS_REPLICA_NAME).getType() + "'";
        String[] args = new String[] {"-J" + TestInfo.BATCH_TEST_JAR_FILE.getAbsolutePath(),
                "-N" + TestInfo.BATCH_TEST_JAR_GOOD_CLASS, "-B" + TestInfo.BATCH_CS_REPLICA_NAME};
        try {
            RunBatch.main(args);
            fail("RunBatch always tries to 'System.exit'");
        } catch (SecurityException e) {
            // expected
        }

        String errMsg = pss.getErr();
        int pseVal = pse.getExitValue();

        assertEquals("The exception should be 1 but was: " + pseVal, 1, pseVal);
        assertTrue("The error message should start with '" + expectedErrorMessage + "' but it is: " + errMsg,
                errMsg.startsWith(expectedErrorMessage));
    }

    /**
     * Test whether the correct error message when wrong output file.
     */
    @Test
    public void testArgumentsWrongOutputFile() {
        String expectedErrorMessage = "Output file '" + TestInfo.BATCH_TEST_JAR_FILE.getAbsolutePath()
                + "' does already exist";
        String[] args = new String[] {"-J" + TestInfo.BATCH_TEST_JAR_FILE.getAbsolutePath(),
                "-N" + TestInfo.BATCH_TEST_JAR_GOOD_CLASS, "-O" + TestInfo.BATCH_TEST_JAR_FILE.getAbsolutePath()};
        try {
            RunBatch.main(args);
            fail("RunBatch always tries to 'System.exit'");
        } catch (SecurityException e) {
            // expected
        }

        String errMsg = pss.getErr();
        int pseVal = pse.getExitValue();

        assertEquals("The exception should be 1 but was: " + pseVal, 1, pseVal);
        assertTrue("The error message should start with '" + expectedErrorMessage + "' but it is: " + errMsg,
                errMsg.startsWith(expectedErrorMessage));
    }

    /**
     * Test whether the correct error message when wrong output file.
     */
    @Test
    public void testArgumentsWrongErrorFile() {
        String expectedErrorMessage = "Error file '" + TestInfo.BATCH_TEST_JAR_FILE.getAbsolutePath()
                + "' does already exist";
        String[] args = new String[] {"-J" + TestInfo.BATCH_TEST_JAR_FILE.getAbsolutePath(),
                "-N" + TestInfo.BATCH_TEST_JAR_GOOD_CLASS, "-E" + TestInfo.BATCH_TEST_JAR_FILE.getAbsolutePath()};
        try {
            RunBatch.main(args);
            fail("RunBatch always tries to 'System.exit'");
        } catch (SecurityException e) {
            // expected
        }

        String errMsg = pss.getErr();
        int pseVal = pse.getExitValue();

        assertEquals("The exception should be 1 but was: " + pseVal, 1, pseVal);
        assertTrue("The error message should start with '" + expectedErrorMessage + "' but it is: " + errMsg,
                errMsg.startsWith(expectedErrorMessage));
    }

    /**
     * Test success fully arguments.
       @Test
     */
    public void testSuccess() {
        String[] args = new String[] {"-J" + TestInfo.BATCH_TEST_JAR_FILE.getAbsolutePath(),
                "-N" + TestInfo.BATCH_TEST_JAR_GOOD_CLASS};
        try {
            RunBatch.main(args);
            fail("RunBatch always tries to 'System.exit'");
        } catch (SecurityException e) {
            // Expected
        }

        int exitCode = pse.getExitValue();
        assertEquals("The exit code should be 0, but was: " + exitCode, 0, exitCode);
    }

    /**
     * This class is a MessageListener that responds to GetMessage, simulating an ArcRepository. It sends a constant
     * response if the GetMessage matches the values given to GetListener's constructor, otherwise it sends a null
     * record as response.
     */
    private static class GetListener extends TestMessageListener {
        public GetListener() {
        }

        public void onMessage(Message o) {
            super.onMessage(o);
            NetarkivetMessage nmsg = received.get(received.size() - 1);
            if (nmsg instanceof BatchMessage) {
                BatchMessage m = (BatchMessage) nmsg;

                BatchReplyMessage brm = new BatchReplyMessage(m.getReplyTo(), Channels.getTheBamon(), m.getID(), 0,
                        (Collection<File>) new ArrayList<File>(),
                        RemoteFileFactory.getCopyfileInstance(TestInfo.BATCH_TEST_JAR_FILE));

                JMSConnectionFactory.getInstance().send(brm);
            }
        }
    }

    ;
}
