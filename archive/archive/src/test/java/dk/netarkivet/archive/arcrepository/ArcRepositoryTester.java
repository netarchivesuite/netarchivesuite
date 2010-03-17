/*$Id$
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
package dk.netarkivet.archive.arcrepository;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.arcrepository.bitpreservation.AdminDataMessage;
import dk.netarkivet.archive.arcrepositoryadmin.Admin;
import dk.netarkivet.archive.arcrepositoryadmin.AdminData;
import dk.netarkivet.archive.arcrepositoryadmin.ArcRepositoryEntry;
import dk.netarkivet.archive.arcrepositoryadmin.UpdateableAdminData;
import dk.netarkivet.archive.bitarchive.distribute.BatchReplyMessage;
import dk.netarkivet.archive.distribute.ReplicaClient;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.NullRemoteFile;
import dk.netarkivet.common.distribute.StringRemoteFile;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaStoreState;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.ClassAsserts;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

public class ArcRepositoryTester extends TestCase {
    /** A repeatedly used reflected method, used across method calls. */
    Method readChecksum;
    ReloadSettings rs = new ReloadSettings();
    static boolean first = true;

    public void setUp() throws Exception {
        // reset the channels.
        if (first) {
            ChannelsTester.resetChannels();
        }

        super.setUp();
        rs.setUp();
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        TestFileUtils.copyDirectoryNonCVS(
                TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
        Settings.set(ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN,
                     TestInfo.WORKING_DIR.getAbsolutePath());
        Settings.set(CommonSettings.NOTIFICATIONS_CLASS, 
                RememberNotifications.class.getName());
    }

    public void tearDown() throws Exception {
        ArcRepository.getInstance().close();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        // Empty the log file.
        new FileOutputStream(TestInfo.LOG_FILE).close();
        rs.tearDown();
        super.tearDown();
    }

    /** Test that BitarchiveMonitorServer is a singleton. */
    public void testIsSingleton() {
        ClassAsserts.assertSingleton(ArcRepository.class);
        ArcRepository.getInstance();
    }


    /** Verify that calling the protected no-arg constructor does not fail. */
    public void testConstructor() {
        ArcRepository.getInstance().close();
    }

    /** Test parameters. */
    public void testGetReplicaClientFromReplicaNameParameters() {
        ArcRepository a = ArcRepository.getInstance();
        /**
         * test with null parameter.
         */
        try {
            a.getReplicaClientFromReplicaId(null);
            fail("ArgumentNotValid should have been thrown");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        /**
         * Test with invalid parameter.
         */
        try {
            a.getReplicaClientFromReplicaId("-1");
            fail("ArgumentNotValid should have been thrown");
        } catch (UnknownID e) {
            // Expected
        }
    }

    /** Test a valid BitarchiveClient is returned. */
    public void testGetReplicaClientFromReplicaName() {
        ArcRepository a = ArcRepository.getInstance();
        String[] locations = Settings.getAll(
                CommonSettings.REPLICA_IDS);
        for (String location : locations) {
            ReplicaClient bc = a.getReplicaClientFromReplicaId(
                    location);
            assertNotNull("Should return a valid BitarchiveClient", bc);
        }
    }

    /**
     * Test that the readChecksum() method works as required.
     *
     * @throws Throwable if something are thrown
     */
    public void testReadChecksum() throws Throwable {
        readChecksum = ArcRepository.class.getDeclaredMethod("readChecksum",
                new Class[]{File.class, String.class});
        readChecksum.setAccessible(true);

        try {
            // Missing file
            String result = (String) readChecksum.invoke(
                    ArcRepository.getInstance(),
                    TestInfo.TMP_FILE, "foobar");
            fail("Should get failure on missing file, not " + result);
        } catch (InvocationTargetException e) {
            assertEquals("Should throw IOFailure",
                         IOFailure.class, e.getCause().getClass());
        }
//
//        assertEquals("Should get empty output from empty file",
//                     "", callReadChecksum("", "foobar"));
//
//        assertEquals("Should get empty output from other-file file",
//                     "", callReadChecksum("bazzoon##klaf", "foobar"));
//
//        assertEquals("Should not match checksum with filename",
//                     "", callReadChecksum("bar##foo", "foo"));
//
//        assertEquals("Should get right checksum when matching",
//                     "foo", callReadChecksum("bar##foo", "bar"));
//
//        assertEquals("Should get right checksum if not on first line",
//                     "bonk", callReadChecksum("bar##baz\nfoo##bonk", "foo"));
//        LogUtils.flushLogs(ArcRepository.class.getName());
//        FileAsserts.assertFileContains(
//                "Should have warning about unwanted line",
//                "There were an unexpected arc-file name in checksum result for arc-file 'foo'(line: 'bar##baz')",
//                TestInfo.LOG_FILE);
//        FileAsserts.assertFileNotContains(
//                "Should have no warning about wanted line",
//                TestInfo.LOG_FILE, "Read unexpected line 'foo##bonk");
//
//        assertEquals("Should get right checksum if not on last line",
//                     "baz", callReadChecksum("bar##baz\nfoo##bonk", "bar"));
//
//        assertEquals("Should get right checksum if empty lines",
//                     "bar", callReadChecksum("foo##bar\n\n", "foo"));
//
//        // Check that the lines are validated correctly.
//        try {
//            callReadChecksum("barf", "foobar");
//            fail("A checksum output file only containing 'barf' should through IllegalState");
//        } catch (IllegalState e) {
//            assertEquals("Not expected error message!",
//                         "Read checksum line had unexpected format 'barf'",
//                         e.getMessage());
//            // This is expected!
//        }
//        // Check that a entry may not have two different checksums.
//        try {
//            callReadChecksum("foo##bar\nfoo##notBar", "foo");
//            fail("A checksum output file containing two entries with for same "
//                 + "name with different checksums should through IllegalState");
//        } catch (IllegalState e) {
//            assertEquals("Not expected error message!",
//                         "The arc-file 'foo' was found with two different checksums: "
//                         + "bar and notBar. Last line: 'foo##notBar'.",
//                         e.getMessage());
//            // This is expected!
//        }
//
//        LogUtils.flushLogs(ArcRepository.class.getName());
//        FileAsserts.assertFileContains(
//                "Should have warning about two different checksums",
//                "The arc-file 'foo' was found with two different checksums: bar and notBar.",
//                TestInfo.LOG_FILE);

    }


    /**
     * Call the readChecksum method with some input and a file to look for.
     *
     * @param input       Will be written to a file that readChecksum reads.
     *                    Valid input is of the form <arcfilename>##<checksum>,
     *                    but invalid input is part of the test.
     * @param arcfilename The name of the arcfile that readChecksum should look
     *                    for.
     *
     * @return The string found for the given filename.
     *
     * @throws IOFailure when readChecksum does.
     */
    public String callReadChecksum(String input, String arcfilename)
            throws Throwable {
        FileUtils.writeBinaryFile(TestInfo.TMP_FILE, input.getBytes());
        try {
            return (String) readChecksum.invoke(ArcRepository.getInstance(),
                                                TestInfo.TMP_FILE, arcfilename);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /**
     * Test that the OnBatchReply method updates state and responds correctly.
     * This is a rather complex test, but should not attempt to test
     * processCheckSum().  It has to set up the following:
     * outstandingChecksumFiles should contain an entry replyOfId->arcfilename
     * msg should contain id, errmsg, resultfile, filesprocessed, filesfailed,
     * but the channels are not used. ad should contain some checksum for the
     * arcfilename but no replyinfo -- we can check the effect by seeing
     * warnings and state.
     *
     * @throws Exception if exception is thrown
     */
    @SuppressWarnings("unchecked")
    public void testOnBatchReply() throws Exception {
        ArcRepository a = ArcRepository.getInstance();
        UpdateableAdminData ad = UpdateableAdminData.getUpdateableInstance();
        Field ocf = a.getClass().getDeclaredField("outstandingChecksumFiles");
        ocf.setAccessible(true);
        Map<String, String> outstanding = (Map<String, String>) ocf.get(a);
        //Field adm = ad.getClass().getDeclaredField("storeEntries");
        Field adm = ad.getClass().
                getSuperclass().getDeclaredField("storeEntries");

        adm.setAccessible(true);
        Map<String, ArcRepositoryEntry> admindataentries =
                (Map<String, ArcRepositoryEntry>) adm.get(ad);

        String id1 = "id1";
        String arcname1 = "arc1";

        // First try the regular cases:
        // Matching checksum
        outstanding.put(id1, arcname1);
        ad.addEntry(arcname1, null, "f00");
        BatchReplyMessage bamsg0 = new BatchReplyMessage(Channels.getTheRepos(),
                                                         Channels.getTheBamon(),
                                                         id1, 0,
                                                         new ArrayList<File>(0),
                                                         new StringRemoteFile(
                                                                 arcname1
                                                                 + dk.netarkivet.archive.arcrepository.bitpreservation.Constants.STRING_FILENAME_SEPARATOR
                                                                 + "f00\n"));
        JMSConnectionMockupMQ.updateMsgID(bamsg0, id1);
        a.onBatchReply(bamsg0);
        LogUtils.flushLogs(ArcRepository.class.getName());
        //System.out.println(FileUtils.readFile(TestInfo.LOG_FILE));
        FileAsserts.assertFileNotContains("Should have no warnings",
                                          TestInfo.LOG_FILE,
                                          "WARNING: Read unex");
        assertEquals("Should have updated the store state",
                     ReplicaStoreState.UPLOAD_COMPLETED,
                     ad.getState(arcname1, Channels.getTheBamon().getName()));

        // Test what happens when a known arcfile gets an error message.
        outstanding.put(id1, arcname1);
        ad.addEntry(arcname1, null, "f00");
        BatchReplyMessage bamsg2 = new BatchReplyMessage(Channels.getTheRepos(),
                                                         Channels.getTheBamon(),
                                                         id1, 0,
                                                         new ArrayList<File>(0),
                                                         new NullRemoteFile());
        JMSConnectionMockupMQ.updateMsgID(bamsg2, id1);
        bamsg2.setNotOk("Test an error");
        a.onBatchReply(bamsg2);
        LogUtils.flushLogs(ArcRepository.class.getName());
        FileAsserts.assertFileContains(
                "Should have warning about error message",
                "Reported error: 'Test an error'", TestInfo.LOG_FILE
        );
        assertEquals("Bad message should set entry to failed",
                     ReplicaStoreState.UPLOAD_FAILED,
                     ad.getState(arcname1, Channels.getTheBamon().getName()));

        // Check what happens if not in AdminData
        // Related bug: 574 -- processing of errors is strange
        admindataentries.remove(arcname1);
        outstanding.put(id1, arcname1);
        BatchReplyMessage bamsg3 = new BatchReplyMessage(Channels.getTheRepos(),
                                                         Channels.getTheBamon(),
                                                         id1, 0,
                                                         new ArrayList<File>(0),
                                                         new NullRemoteFile());
        JMSConnectionMockupMQ.updateMsgID(bamsg3, id1);
        bamsg3.setNotOk("Test another error");
        try {
            a.onBatchReply(bamsg3);
            fail("Should have thrown UnknownID when presented with an unknown"
                 + " arc file " + arcname1);
        } catch (UnknownID e) {
            StringAsserts.assertStringContains("Should have mention of file "
                                               + "in error message", arcname1,
                                               e.getMessage());
        }
        LogUtils.flushLogs(ArcRepository.class.getName());
        FileAsserts.assertFileContains(
                "Should have warning about error message",
                "Reported error: 'Test another error'", TestInfo.LOG_FILE);
        assertFalse("Should not have info about non-yet-processed arcfile",
                    ad.hasEntry(arcname1));
        // Try one without matching arcfilename -- should give warning.
        BatchReplyMessage bamsg1 = new BatchReplyMessage(Channels.getTheRepos(),
                                                         Channels.getTheBamon(),
                                                         id1, 0,
                                                         new ArrayList<File>(0),
                                                         new NullRemoteFile());
        a.onBatchReply(bamsg1);
        LogUtils.flushLogs(ArcRepository.class.getName());
        FileAsserts.assertFileContains("Should have warning about unknown id",
                                       "unknown originating ID " + id1,
                                       TestInfo.LOG_FILE);
        assertFalse("Should not have info about non-yet-processed arcfile",
                    ad.hasEntry(arcname1));

    }

    public void testChecksumCalls() throws Exception {
        ArcRepository.getInstance().cleanup();
        Settings.set(CommonSettings.USE_REPLICA_ID, "THREE");

        testOnBatchReply();
    }
    
    public void testAdminMessages() {
        ArcRepository arc = ArcRepository.getInstance();
        Admin admin = AdminData.getUpdateableInstance();
        
        assertEquals("The admin data should be empty.", 
                0, admin.getAllFileNames().size());
        admin.addEntry("filename", null, "checksum");
        assertEquals("The admin data should have one file.",
                1, admin.getAllFileNames().size());
        assertEquals("The admin data should not have any UPLOAD_COMPLETED files for replica ONE",
                0, admin.getAllFileNames(Replica.getReplicaFromId("ONE"), 
                        ReplicaStoreState.UPLOAD_COMPLETED).size());
        
        AdminDataMessage msg = new AdminDataMessage("filename", "ONE", ReplicaStoreState.UPLOAD_COMPLETED);
        arc.updateAdminData(msg);
        
        assertEquals("The admin data should now have one UPLOAD_COMPLETED files for replica ONE",
                1, admin.getAllFileNames(Replica.getReplicaFromId("ONE"), 
                        ReplicaStoreState.UPLOAD_COMPLETED).size());
        List<String> res = new ArrayList<String>();
        res.add("filename");
        assertEquals("The admin data should have the files 'filename' as UPLOAD_COMPLETE for replica ONE",
                res.toString(), admin.getAllFileNames(Replica.getReplicaFromId("ONE"), 
                        ReplicaStoreState.UPLOAD_COMPLETED).toString());
        
        msg = new AdminDataMessage("filename", "TWO", ReplicaStoreState.UPLOAD_FAILED);
        arc.updateAdminData(msg);
        
        assertEquals("The admin data should have no UPLOAD_COMPLETED files for replica TWO",
                0, admin.getAllFileNames(Replica.getReplicaFromId("TWO"), 
                        ReplicaStoreState.UPLOAD_COMPLETED).size());
        assertEquals("The admin data should have one UPLOAD_FAILED files for replica TWO",
                1, admin.getAllFileNames(Replica.getReplicaFromId("TWO"), 
                        ReplicaStoreState.UPLOAD_FAILED).size());
        
        // check for changing the checksum.
        assertEquals("Should give the first assigned checksum.", 
                "checksum", admin.getCheckSum("filename"));
        
        msg = new AdminDataMessage("filename", "muskcehc");
        arc.updateAdminData(msg);
        
        assertEquals("Should give the first assigned checksum.", 
                "muskcehc", admin.getCheckSum("filename"));
    }
    
    /**
     * Ensure, that the application dies if given the wrong input.
     */
    public void testApplication() {
        ReflectUtils.testUtilityConstructor(ArcRepositoryApplication.class);

        PreventSystemExit pse = new PreventSystemExit();
        PreserveStdStreams pss = new PreserveStdStreams(true);
        pse.setUp();
        pss.setUp();
        
        try {
            ArcRepositoryApplication.main(new String[]{"ERROR"});
            fail("It should throw an exception ");
        } catch (SecurityException e) {
            // expected !
        }

        pss.tearDown();
        pse.tearDown();
        
        assertEquals("Should give exit code 1", 1, pse.getExitValue());
        assertTrue("Should tell that no arguments are expected.", 
                pss.getOut().contains("This application takes no arguments"));
    }

}
