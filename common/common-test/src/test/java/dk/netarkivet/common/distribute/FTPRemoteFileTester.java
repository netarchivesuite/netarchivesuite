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
package dk.netarkivet.common.distribute;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.TestInfo;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * Class testing the FTPRemoteFile class.
 */
public class FTPRemoteFileTester {

    /** testFile1-3 represents duplicates of TestInfo.TESTXML. */
    private File testFile1;
    private File testFile2;

    // A named logger for this class is retrieved
    protected final Logger logger = LoggerFactory.getLogger(FTPRemoteFileTester.class);

    ReloadSettings rs = new ReloadSettings();

    @Before
    public void setUp() {
        rs.setUp();
        try {
            if (!TestInfo.TEMPDIR.exists()) {
                dk.netarkivet.common.utils.TestInfo.TEMPDIR.mkdir();
            }

            FileUtils.removeRecursively(TestInfo.TEMPDIR);
            TestFileUtils.copyDirectoryNonCVS(TestInfo.DATADIR, TestInfo.TEMPDIR);

            /*
             * make 3 duplicates of TestInfo.TESTXML: test1.xml, test2.xml, test3.xml
             */
            testFile1 = new File(TestInfo.TEMPDIR, "test.xml");
            testFile2 = new File(TestInfo.TEMPDIR, "should_not_exist.xml");
            // testFile3 = new File(TestInfo.TEMPDIR, "test3.xml");
            assertTrue("The test xml file must exist", TestInfo.TESTXML.exists());
            // FileUtils.copyFile(TestInfo.TESTXML, testFile1);
            // FileUtils.copyFile(TestInfo.TESTXML, testFile3);
        } catch (Exception e) {
            fail("Could not setup configuration for");
        }
    }

    @After
    public void tearDown() throws IOException {
        FileUtils.removeRecursively(dk.netarkivet.common.utils.TestInfo.TEMPDIR);
        rs.tearDown();
    }

    /**
     * Test that we can set and reset test-behaviour to get a TestRemoteFile.
     */
    @Test
    public void testSetTest() {
        RemoteFile rf;
        try {
            Settings.set(CommonSettings.REMOTE_FILE_CLASS, TestRemoteFile.class.getName());
            rf = RemoteFileFactory.getInstance(testFile2, true, false, true);
            fail("Should have rejected a non-existing file");
        } catch (ArgumentNotValid e) {
            // Expected -- TestRemoteFile must have an existing file
            StringAsserts.assertStringContains("Should have gotten error message from TestRemoteFile",
                    "is not a readable file", e.getCause().getMessage());
        }

        // Must be sure the file does not exist, or it will try to upload.
        try {
            Settings.set(CommonSettings.REMOTE_FILE_CLASS, FTPRemoteFile.class.getName());
            rf = RemoteFileFactory.getInstance(testFile2, true, false, true);
            fail("Should have rejected a non-existing file");
        } catch (ArgumentNotValid e) {
            // Expected -- FTPRemoteFile must have an existing file
            StringAsserts.assertStringContains("Should have gotten error message from TestRemoteFile",
                    "is not a readable file", e.getCause().getMessage());
        }

        Settings.set(CommonSettings.REMOTE_FILE_CLASS, NullRemoteFile.class.getName());
        rf = RemoteFileFactory.getInstance(testFile1, true, false, true);
        assertTrue("Expected NullRemoteFile", rf instanceof NullRemoteFile);

        // Unknown behaviour?
        try {
            Settings.set(CommonSettings.REMOTE_FILE_CLASS, RemoteFile.class.getName());
            rf = RemoteFileFactory.getInstance(testFile1, true, false, true);
            fail("Should not instantiate interface RemoteFile");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            Settings.set(CommonSettings.REMOTE_FILE_CLASS, "dk.netarkivet.common.distribute.NoRemoteFile");
            rf = RemoteFileFactory.getInstance(testFile1, true, false, true);
            fail("No getInstance method should exist for NoRemoteFile");
        } catch (IOFailure e) {
            // Expected
        }
    }

}
