/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;

import junit.framework.TestCase;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;


/**
 * csr forgot to comment this!
 *
 */

public class ApplicationUtilsTester extends TestCase {
    private PreventSystemExit pse = new PreventSystemExit();

    public void setUp() throws Exception {
        super.setUp();
        FileInputStream fis = new FileInputStream("tests/dk/netarkivet/testlog.prop");
        LogManager.getLogManager().reset();
        LogManager.getLogManager().readConfiguration(fis);
        fis.close();
        FileUtils.removeRecursively(TestInfo.TEMPDIR);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.DATADIR, TestInfo.TEMPDIR);
        Settings.set(Settings.NOTIFICATIONS_CLASS,
                     RememberNotifications.class.getName());
        pse.setUp();
    }

    public void tearDown() throws Exception {
        FileUtils.removeRecursively(TestInfo.TEMPDIR);
        pse.tearDown();
        Settings.reload();
        RememberNotifications.resetSingleton();
        super.tearDown();
    }

    /** Check that we create the dir if needed and get a warning if created. */
    public void testEnsureDirExists() {
        // Illegal file
        try {
            final File dir = new File("/SHOULD_NOT_EXIST:AND_IS_ILLEGAL");
            ApplicationUtils.dirMustExist(dir);
            FileUtils.removeRecursively(dir);
            fail("Should not be able to create " + dir);
        } catch (PermissionDenied e) {
            // Expected
        }

        // Existing dir - no log message
        ApplicationUtils.dirMustExist(TestInfo.TEMPDIR);
        LogUtils.flushLogs(ApplicationUtils.class.getName());
        FileAsserts.assertFileNotContains("Should not give log on existing dir",
                                          TestInfo.LOG_FILE, TestInfo.TEMPDIR.getName());

        // New dir - get log message
        ApplicationUtils.dirMustExist(new File(TestInfo.TEMPDIR, "newdir"));
        LogUtils.flushLogs(ApplicationUtils.class.getName());
        FileAsserts.assertFileContains("Should give log on non-existing dir",
                                       TestInfo.TEMPDIR.getName(), TestInfo.LOG_FILE);
        FileAsserts.assertFileContains("Should give warning on non-existing dir",
                                       "WARNING", TestInfo.LOG_FILE);
    }

    /** Test that startApp checks the required things */
    public void testStartApp() throws InterruptedException {
        // Check that no args are allowed.
        try {
            ApplicationUtils.startApp(this.getClass(), new String[] { "arg1" });
            fail("Should have died starting an app with args");
        } catch (SecurityException e) {
            // Expected System.exit to happen
            e.printStackTrace();
            assertEquals("Should have exit value WRONG_ARGUMENTS:\n"
                         + ExceptionUtils.getStackTrace(e),
                         ApplicationUtils.WRONG_ARGUMENTS, pse.getExitValue());
        }

        // Check that missing factory method is checked
        try {
            ApplicationUtils.startApp(this.getClass(), new String[0]);
            fail("Should have died starting an app with no factory method");
        } catch (SecurityException e) {
            // Expected System.exit to happen
            assertEquals("Should have exit value NO_FACTORY_METHOD", 2, pse.getExitValue());
        }

        // Check that throwing factory method is checked
        try {
            ApplicationUtils.startApp(App1.class, new String[0]);
            fail("Should have died starting an app that throws exception");
        } catch (SecurityException e) {
            // Expected System.exit to happen
            assertEquals("Should have exit value EXCEPTION_WHILE_INSTANTIATING",
                         3, pse.getExitValue());
        }

        pse.reset();

        // Check that non-CleanupIF factory method is checked
        try {
            ApplicationUtils.startApp(App2.class, new String[0]);
            fail("Should have died starting an app that is not a CleanupIF");
        } catch (SecurityException e) {
            // Expected System.exit to happen
            assertEquals("Should have exit value EXCEPTION_WHILE_INSTANTIATING",
                         3, pse.getExitValue());
        }

        // Check that successfull start logs and makes tmpdir
        File tempdir = new File(TestInfo.TEMPDIR, "new_temp_dir");
        Settings.set(Settings.DIR_COMMONTEMPDIR, tempdir.getAbsolutePath());
        assertFalse("Should not have tempdir before start", tempdir.exists());
        FileAsserts.assertFileNotContains("Should not have shutdown hook mentioned in log",
                                          TestInfo.LOG_FILE, "Added shutdown hook");
        ApplicationUtils.startApp(App3.class, new String[0]);
        assertEquals("Should have correct appName",
                     "dk.netarkivet.common.utils.App3", Settings.get(Settings.APPLICATIONNAME));
        LogUtils.flushLogs(ApplicationUtils.class.getName());
        FileAsserts.assertFileContains("Should have shutdown hook mentioned in log",
                                       "Added shutdown hook for dk.netarkivet.common.utils.App3", TestInfo.LOG_FILE);
        assertTrue("Should have tempdir after start", tempdir.exists());

        // Check, that aliveness is logged
        // Cf. http://kb-prod-udv-001.kb.dk/twiki/bin/view/Netarkiv/AssignmentGroupE1#E1.1)

        Thread.sleep(10);
        LogUtils.flushLogs(LivenessLogger.class.getName());
        FileAsserts.assertFileContains("Should have liveness entries in log",
                "dk.netarkivet.common.utils.App3 is alive at", TestInfo.LOG_FILE);

    }
}

class App1 implements CleanupIF {
    public static Object getInstance() { throw new NullPointerException(); }
    public void cleanup() { }
}

class App2 {
    public static Object getInstance() { return new App2(); }
}

class App3 implements CleanupIF {
    public static Object getInstance() { return new App3(); }
    public void cleanup() { }
}

