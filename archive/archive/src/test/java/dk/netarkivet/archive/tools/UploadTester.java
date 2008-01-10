/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.archive.tools;

import java.io.File;

import junit.framework.TestCase;

import dk.netarkivet.archive.arcrepository.distribute.StoreMessage;
import dk.netarkivet.common.Settings;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.testutils.preconfigured.MockupArcRepositoryClient;
import dk.netarkivet.testutils.preconfigured.MockupJMS;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

public class UploadTester extends TestCase {
    private UseTestRemoteFile ulrf = new UseTestRemoteFile();
    private PreventSystemExit pse = new PreventSystemExit();
    private PreserveStdStreams pss = new PreserveStdStreams(true);
    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.DATA_DIR,TestInfo.WORKING_DIR);
    private MockupJMS mjms = new MockupJMS();
    private MockupArcRepositoryClient marc = new MockupArcRepositoryClient();

    public void setUp(){
        ulrf.setUp();
        mjms.setUp();
        mtf.setUp();
        pss.setUp();
        pse.setUp();
        marc.setUp();
        Settings.set(Settings.NOTIFICATIONS_CLASS,
                     RememberNotifications.class.getName());
    }
    public void tearDown(){
        Settings.reload();
        marc.tearDown();
        pse.tearDown();
        pss.tearDown();
        mtf.tearDown();
        mjms.tearDown();
        ulrf.tearDown();
        RememberNotifications.resetSingleton();
    }

    /**
     * Verify that uploading a single ARC file works as expected
     * and deletes the file locally.
     */
    public void testMainOneFile() {
        Upload.main(new String[]{TestInfo.ARC1.getAbsolutePath()});
        assertMsgCount(1,0);
        assertStoreStatus(0,TestInfo.ARC1,true);
    }

    /**
     * Verify that uploading more than one ARC file works as expected
     * and deletes the files locally.
     */
    public void testMainSeveralFiles() {
        Upload.main(new String[]{
                TestInfo.ARC1.getAbsolutePath(),
                TestInfo.ARC2.getAbsolutePath()});
        assertMsgCount(2,0);
        assertStoreStatus(0,TestInfo.ARC1,true);
        assertStoreStatus(1,TestInfo.ARC2,true);
    }
    /**
     * Verify that non-ARC files are rejected and execution fails.
     * Also verfies that nothing is stored in that case.
     */
    public void testMainNonArc() {
        try {
            Upload.main(new String[]{
                    TestInfo.ARC1.getAbsolutePath(),
                    TestInfo.INDEX_DIR.getAbsolutePath()});
            fail("Calling Upload with non-arc file should System.exit");
        } catch (SecurityException e) {
            //Expected
            assertMsgCount(0,0);
        }
    }

    /**
     * Verify that  the system fails as expected when the store operation
     * fails on the server side. (Local files must NOT be deleted).
     */
    public void testMainStoreFails1() {
        marc.failOnFile(TestInfo.ARC1.getName());
        int retries = Integer.parseInt(Settings.get(Settings.ARCREPOSITORY_STORE_RETRIES));
        Upload.main(new String[]{
                TestInfo.ARC1.getAbsolutePath(),
                TestInfo.ARC2.getAbsolutePath(),
                TestInfo.ARC3.getAbsolutePath()});
        assertMsgCount(2,1);
        int index = 0;
        for(int i = 0 ; i < retries ; i++) {
            assertStoreStatus(index,TestInfo.ARC1,false);
            index++;
        }
        assertStoreStatus(index,TestInfo.ARC2,true);
        index++;
        assertStoreStatus(index,TestInfo.ARC3,true);
    }

    /**
     * Verify that  the system fails as expected when the store operation
     * fails on the server side. (Local files must NOT be deleted).
     */
    public void testMainStoreFails2() {
        marc.failOnFile(TestInfo.ARC2.getName());
        int retries = Integer.parseInt(Settings.get(Settings.ARCREPOSITORY_STORE_RETRIES));
        Upload.main(new String[]{
                TestInfo.ARC1.getAbsolutePath(),
                TestInfo.ARC2.getAbsolutePath(),
                TestInfo.ARC3.getAbsolutePath()});
        assertMsgCount(2,1);
        int index = 0;
        assertStoreStatus(index,TestInfo.ARC1,true);
        index++;
        for(int i = 0 ; i < retries ; i++) {
            assertStoreStatus(index,TestInfo.ARC2,false);
            index++;
        }
        assertStoreStatus(index,TestInfo.ARC3,true);
    }

    /**
     * Verify that  the system fails as expected when the store operation
     * fails on the server side. (Local files must NOT be deleted).
     */
    public void testMainStoreFails3() {
        marc.failOnFile(TestInfo.ARC3.getName());
        int retries = Integer.parseInt(Settings.get(Settings.ARCREPOSITORY_STORE_RETRIES));
        Upload.main(new String[]{
                TestInfo.ARC1.getAbsolutePath(),
                TestInfo.ARC2.getAbsolutePath(),
                TestInfo.ARC3.getAbsolutePath()});
        assertMsgCount(2,1);
        int index = 0;
        assertStoreStatus(index,TestInfo.ARC1,true);
        index++;
        assertStoreStatus(index,TestInfo.ARC2,true);
        index++;
        for(int i = 0 ; i < retries ; i++) {
            assertStoreStatus(index,TestInfo.ARC3,false);
            index++;
        }
    }

    /**
     * Verifies that calling Upload without arguments fails.
     * Also verfies that nothing is stored in that case.
     */
    public void testNoArguments() {
        try {
            Upload.main(new String[]{});
            fail("Calling Upload without arguments should System.exit");
        } catch (SecurityException e) {
            //Expected
            assertMsgCount(0,0);
        }
    }

    /**
     * Asserts that we got the expected number of StoreMessages.     * @param succeeded
     * @param succeeded Number of files successfully stored
     * @param failed Number of files that never got stored
     */
    private void assertMsgCount(int succeeded, int failed) {
        int retries = Integer.parseInt(Settings.get(Settings.ARCREPOSITORY_STORE_RETRIES));
        int expected = succeeded + failed * retries;
        assertEquals("Upload should generate exactly 1 StoreMessage per succeeded arc file and "
                + retries + " per failed store",
                expected,marc.getMsgCount());
    }
    /**
     * Asserts that the nth StoreMessage is regarding the given arc file
     * and that the arc file is delete if and only if store suceeded.
     * @param n The relevant index to marc.getStoreMsgs()
     * @param arcFile The arc file that was stored
     * @param shouldSuceed Whether store was supposed to succeed
     */
    private void assertStoreStatus(int n, File arcFile, boolean shouldSuceed) {
        StoreMessage sm = marc.getStoreMsgs().get(n);
        assertEquals("Upload should attempt to upload the specified files",
                arcFile.getName(),sm.getArcfileName());
        if(shouldSuceed) {
            assertFalse("Upload should delete a properly uploaded file",
                arcFile.exists());
        } else {
            assertTrue("Upload should not delete a file that wasn't properly uploaded",
                    arcFile.exists());
        }
    }
}
