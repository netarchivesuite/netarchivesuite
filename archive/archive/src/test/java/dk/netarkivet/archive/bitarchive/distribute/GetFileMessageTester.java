/* File:                 $Id$
 * Revision:             $Revision$
 * Author:               $Author$
 * Date:                 $Date $
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
package dk.netarkivet.archive.bitarchive.distribute;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import junit.framework.TestCase;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/**
 * Tests for GetFileMessage
 *
 */
public class GetFileMessageTester extends TestCase {
    UseTestRemoteFile utrf = new UseTestRemoteFile();

    private static final File WORKING = TestInfo.UPLOADMESSAGE_TEMP_DIR;
    ReloadSettings rs = new ReloadSettings();

    protected void setUp() throws IOException {
        rs.setUp();
        utrf.setUp();
        FileUtils.removeRecursively(WORKING);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.UPLOADMESSAGE_ORIGINALS_DIR,
                WORKING);
    }

    protected void tearDown() {
        FileUtils.removeRecursively(WORKING);
        utrf.tearDown();
        rs.tearDown();
    }


    public void testGetData() throws IOException, NoSuchFieldException,
            IllegalAccessException {
        File origFile = new File(WORKING, "NetarchiveSuite-store1.arc");
        GetFileMessage message = new GetFileMessage(Channels.getAllBa(),
                Channels.getThisReposClient(), origFile.getName(), "ONE");
        message.setFile(origFile);
        File destDir = new File(WORKING, "dest");
        FileUtils.createDir(destDir);
        File destFile = new File(destDir, "NetarchiveSuite-store1.arc");
        message.getData(destFile);
        assertEquals("File from GetFile should have same content as original",
                FileUtils.readFile(origFile), FileUtils.readFile(destFile));
        Field remoteFileField = ReflectUtils.getPrivateField(GetFileMessage.class,
                "remoteFile");
        assertNull("Remote file field should have been nulled",
                remoteFileField.get(message));
        FileUtils.remove(destFile);
        // Error cases:
        try {
            message.getData(destFile);
            fail("Should not be able to read file a second time");
        } catch (IOFailure e) {
            StringAsserts.assertStringContains("Should mention arcfilename in"
                    + " error message", origFile.getName(), e.getMessage());
        }
        message = new GetFileMessage(Channels.getAllBa(),
                Channels.getThisReposClient(), origFile.getName(), "KB");
        try {
            message.getData(destFile);
            fail("Should die if no file is set");
        } catch (IOFailure e) {
            StringAsserts.assertStringContains("Should mention arcfilename in"
                    + " error message", origFile.getName(), e.getMessage());
        }
        message.setFile(origFile);
        try {
            message.getData(null);
            fail("Should die on null file");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            message.getData(new File("/fnord"));
            fail("Should die on impossible file");
        } catch (ArgumentNotValid e) {
            StringAsserts.assertStringContains("Should mention file in error",
                    "/fnord", e.getMessage());
        }
        assertFalse("New file should not exist", destFile.exists());
        assertTrue("Should not have deleted remote file on error",
                origFile.exists());


    }
}
