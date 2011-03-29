/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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
package dk.netarkivet.archive.bitarchive;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.LogManager;

import junit.framework.TestCase;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;


/**
 * Unit test for Bitarchive API
 * The CTOR method is tested
 */
public class BitarchiveTesterCTOR extends TestCase {
    private static File PATH_TO_TEST =
            new File("tests/dk/netarkivet/archive/bitarchive/data/ctor");
    private static File NEW_ARCHIVE_DIR = new File(PATH_TO_TEST, "new");
    private static File EXISTING_ARCHIVE_NAME =
            new File(PATH_TO_TEST, "existing");
    private static File NOACCESS_ARCHIVE_DIR =
            new File(PATH_TO_TEST, "noaccess");
    private static File WORKING_ARCHIVE_DIR =
            new File(PATH_TO_TEST, "working");
    ReloadSettings rs = new ReloadSettings();

    /**
     * The properties-file containg properties for logging in unit-tests
     */
    private static final File TESTLOGPROP = new File("tests/dk/netarkivet/testlog.prop");
    private static File LOG_FILE = new File("tests/testlogs", "netarkivtest.log");
    private static final String CREDENTIALS = "42";

    public BitarchiveTesterCTOR(String sTestName) {
        super(sTestName);
    }

    public void setUp() {
        rs.setUp();
        try {
            // This forces an emptying of the log file.
            FileInputStream fis = new FileInputStream(TESTLOGPROP);
            LogManager.getLogManager().readConfiguration(fis);
            fis.close();
        } catch (IOException e) {
            fail("Could not load the testlog.prop file: " + e);
        }
        FileUtils.removeRecursively(WORKING_ARCHIVE_DIR);
        FileUtils.removeRecursively(NEW_ARCHIVE_DIR);
        try {
            // Copy over the "existing" bit archive.
            TestFileUtils.copyDirectoryNonCVS(EXISTING_ARCHIVE_NAME,
                    WORKING_ARCHIVE_DIR);
        } catch (IOFailure e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public void tearDown() {
        FileUtils.removeRecursively(WORKING_ARCHIVE_DIR);
        FileUtils.removeRecursively(NEW_ARCHIVE_DIR);
        rs.tearDown();
    }

    /**
     * Create bitarchive from scratch, no admin data and log files exists
     */
    public void testFromScratch() {
        assertFalse("No bitarchive should exist before creating it", NEW_ARCHIVE_DIR.exists());
        // Create new test archive and close it
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, NEW_ARCHIVE_DIR.getAbsolutePath());
        Bitarchive ba = Bitarchive.getInstance();
        ba.close();
        // verify that the directory, admin and log files are created
        assertTrue("The archive dir should exist after creation", NEW_ARCHIVE_DIR.exists());
        assertTrue("Log file should exist after creation", LOG_FILE.exists());
    }

    /**
     * Create bitarchive with access denied to location of admin data
     * verify that exceptions are thrown
     */
    public void testAccessDenied() {
        // Make sure archive exists
        assertTrue("Inaccessible archive dir must exist",
                NOACCESS_ARCHIVE_DIR.exists());

        if (NOACCESS_ARCHIVE_DIR.canWrite()) {
            NOACCESS_ARCHIVE_DIR.setReadOnly();
        }

        // and that admin file is inaccessible
        assertFalse("Must not be able to write to inaccessible admin file",
                NOACCESS_ARCHIVE_DIR.canWrite());

        try {
            Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR,
                         NOACCESS_ARCHIVE_DIR.getAbsolutePath());
            Bitarchive ba = Bitarchive.getInstance();
            ba.close();
            fail("Accessing read-only archive should throw exception"); // do not come here
        } catch (PermissionDenied e) {
            // Expected case
            StringAsserts.assertStringContains("Should mention noaccess dir",
                    "noaccess/filedir", e.getMessage());
        }
    }
}
