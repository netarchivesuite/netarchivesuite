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
package dk.netarkivet.common.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;

/**
 * Tests the ToolRunner using a mocked up tool, which can be induced to throw exceptions through parameters.
 */
@SuppressWarnings({"serial"})
public class ToolRunnerTester {

    private static SimpleCmdlineToolForTest sctfTest = new SimpleCmdlineToolForTest();

    private PreventSystemExit pse = new PreventSystemExit();
    private PreserveStdStreams pss = new PreserveStdStreams(true);

    @Before
    public void setUp() {
        pss.setUp();
        pse.setUp();
    }

    @After
    public void tearDown() {
        pse.tearDown();
        pss.tearDown();
    }

    /**
     * Test a normal configuration, with the right number of parameters (2).
     */
    @Test
    public void testNormalRun() {
        try {
            sctfTest.runTheTool("n", "n", "n"); // Run with no exceptions
        } catch (SecurityException e) {
            assertEquals("Should have exited normally", 0, pse.getExitValue());
        }
        assertTrue(FakeTool.setupCompleted);
        assertTrue(FakeTool.runCompleted);
        assertTrue(FakeTool.teardownCompleted);
    }

    /**
     * Test using wrong number oo parameters. Must fail.
     */
    @Test
    public void testWrongNumberOfParameters() {
        try {
            sctfTest.runTheTool("n"); // Run with too few parameters
            fail("Should have failed before getting here");
        } catch (java.lang.SecurityException e) {
            assertFalse(FakeTool.setupCompleted);
            assertFalse(FakeTool.runCompleted);
            assertFalse(FakeTool.teardownCompleted);
        }
    }

    /**
     * Test where fail is induced during setup.
     */
    @Test
    public void testFailSetup() {
        try {
            sctfTest.runTheTool("n", "y", "n"); // Force fail during setup
            fail("Should have failed before getting here");
        } catch (java.lang.SecurityException e) {
            assertFalse(FakeTool.setupCompleted);
            assertFalse(FakeTool.runCompleted);
            assertTrue(FakeTool.teardownCompleted);
        }
    }

    /**
     * Test where fail is induced during main loop / processing ("run" method).
     */
    @Test
    public void testFailRun() {
        try {
            sctfTest.runTheTool("n", "n", "y"); // Force fail during run
            fail("Should have failed before getting here");
        } catch (java.lang.SecurityException e) {
            assertTrue(FakeTool.setupCompleted);
            assertFalse(FakeTool.runCompleted);
            assertTrue(FakeTool.teardownCompleted);
        }
    }

    /**
     * Test where fail is induced during main loop / processing ("run" method).
     */
    @Test
    public void testFailCheckargs() {
        try {
            sctfTest.runTheTool("y", "n", "n"); // Force fail during run
            fail("Should have failed before getting here");
        } catch (java.lang.SecurityException e) {
            assertFalse(FakeTool.setupCompleted);
            assertFalse(FakeTool.runCompleted);
            assertFalse(FakeTool.teardownCompleted);
        }
    }

    static private class FakeToolException extends RuntimeException {
        FakeToolException(String msg) {
            super(msg);
        }
    }

    static private class FakeTool implements SimpleCmdlineTool {
        private boolean failSetup;
        private boolean failRun;
        public static boolean setupCompleted;
        public static boolean runCompleted;
        static boolean teardownCompleted;

        public boolean checkArgs(String... args) {
            setupCompleted = false;
            runCompleted = false;
            teardownCompleted = false;
            if (args[0].equals("y")) {
                throw new FakeToolException("checkArgs failed");
            }
            return (args.length == 3);
        }

        public void setUp(String... args) {
            failSetup = (args[1].equals("y"));
            failRun = (args[2].equals("y"));
            if (failSetup) {
                throw new FakeToolException("Setup failed");
            }
            setupCompleted = true;
        }

        public void tearDown() {
            teardownCompleted = true;
        }

        public void run(String... args) {
            if (failRun) {
                throw new FakeToolException("Run failed");
            }
            runCompleted = true;
        }

        public String listParameters() {
            return "fail_checkargs fail_setup fail_run";
        }

    }

    static private class SimpleCmdlineToolForTest extends ToolRunnerBase {

        protected SimpleCmdlineTool makeMyTool() {
            return new FakeTool();
        }

    }
}
