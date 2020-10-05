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
package dk.netarkivet.archive.bitarchive;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.LogbackRecorder;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Unit test for Bitarchive API The CTOR method is tested
 */
@SuppressWarnings({"unused"})
public class BitarchiveTesterCTOR {

    private static File PATH_TO_TEST = new File("tests/dk/netarkivet/archive/bitarchive/data/ctor");
    private static File NEW_ARCHIVE_DIR = new File(PATH_TO_TEST, "new");
    private static File EXISTING_ARCHIVE_NAME = new File(PATH_TO_TEST, "existing");
    private static File NOACCESS_ARCHIVE_DIR = new File(PATH_TO_TEST, "noaccess");
    private static File WORKING_ARCHIVE_DIR = new File(PATH_TO_TEST, "working");
    ReloadSettings rs = new ReloadSettings();

    /**
     * The properties-file containing properties for logging in unit-tests
     */
    private static final String CREDENTIALS = "42";

    @Before
    public void setUp() {
        rs.setUp();
        FileUtils.removeRecursively(WORKING_ARCHIVE_DIR);
        FileUtils.removeRecursively(NEW_ARCHIVE_DIR);
        try {
            // Copy over the "existing" bit archive.
            TestFileUtils.copyDirectoryNonCVS(EXISTING_ARCHIVE_NAME, WORKING_ARCHIVE_DIR);
        } catch (IOFailure e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @After
    public void tearDown() {
        FileUtils.removeRecursively(WORKING_ARCHIVE_DIR);
        FileUtils.removeRecursively(NEW_ARCHIVE_DIR);
        rs.tearDown();
    }

    /**
     * Create bitarchive from scratch, no admin data and log files exists
     */
    @Test
    public void testFromScratch() {
        LogbackRecorder lr = LogbackRecorder.startRecorder();
        assertFalse("No bitarchive should exist before creating it", NEW_ARCHIVE_DIR.exists());
        // Create new test archive and close it
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, NEW_ARCHIVE_DIR.getAbsolutePath());
        Bitarchive ba = Bitarchive.getInstance();
        ba.close();
        // verify that the directory, admin and log files are created
        assertTrue("The archive dir should exist after creation", NEW_ARCHIVE_DIR.exists());
        assertTrue("Log file should exist after creation", !lr.isEmpty());
        lr.stopRecorder();
    }

    /**
     * Create bitarchive with access denied to location of admin data verify that exceptions are thrown
     */
    @Test
    public void testAccessDenied() {
        // Make sure archive exists
        assertTrue("Inaccessible archive dir must exist", NOACCESS_ARCHIVE_DIR.exists());

        if (NOACCESS_ARCHIVE_DIR.canWrite()) {
            NOACCESS_ARCHIVE_DIR.setReadOnly();
        }

        // and that admin file is inaccessible
        assertFalse("Must not be able to write to inaccessible admin file", NOACCESS_ARCHIVE_DIR.canWrite());

        try {
            Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, NOACCESS_ARCHIVE_DIR.getAbsolutePath());
            Bitarchive ba = Bitarchive.getInstance();
            ba.close();
            fail("Accessing read-only archive should throw exception"); // do not come here
        } catch (PermissionDenied e) {
            // Expected case
            StringAsserts.assertStringContains("Should mention noaccess dir", "noaccess/filedir", e.getMessage());
        }
    }

}
