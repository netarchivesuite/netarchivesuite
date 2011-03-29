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
package dk.netarkivet.archive.checksum.distribute;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;
import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.bitarchive.distribute.UploadMessage;
import dk.netarkivet.archive.checksum.ChecksumFileApplication;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.RememberNotifications;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SystemUtils;
import dk.netarkivet.testutils.ClassAsserts;
import dk.netarkivet.testutils.GenericMessageListener;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

public class FileChecksumServerTester extends TestCase {

    ReloadSettings rs = new ReloadSettings();
    private UseTestRemoteFile utrf = new UseTestRemoteFile();
    JMSConnectionMockupMQ conn;
    
    ChecksumFileServer cfs;
    
    protected void setUp() {
	rs.setUp();
	utrf.setUp();
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        // ??

	FileUtils.copyDirectory(TestInfo.ORIGINAL_DIR, TestInfo.WORK_DIR);
	
        // Set the test settings.
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, TestInfo.BASE_FILE_DIR.getAbsolutePath());
        Settings.set(ArchiveSettings.CHECKSUM_BASEDIR, TestInfo.CHECKSUM_FILE.getParentFile().getAbsolutePath());
        Settings.set(CommonSettings.USE_REPLICA_ID, "THREE");
        Settings.set(CommonSettings.NOTIFICATIONS_CLASS, RememberNotifications.class.getName());

        ChannelsTester.resetChannels();

        // Create/recreate the checksum.md5 file
        try {
	    FileWriter fw = new FileWriter(TestInfo.CHECKSUM_FILE);
	    
	    fw.write("test1.arc##1234567890" + "\n" + "test2.arc##0987654321" + "\n");
	    fw.flush();
	    fw.close();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	
        conn = (JMSConnectionMockupMQ) JMSConnectionFactory.getInstance();
    }
    
    protected void tearDown() {
	// ??
	JMSConnectionMockupMQ.clearTestQueues();
	utrf.tearDown();
	rs.tearDown();
	
	FileUtils.removeRecursively(TestInfo.WORK_DIR);
    }
    
    public void testSingletonicity() {
        ClassAsserts.assertSingleton(ChecksumFileServer.class);
    }

    /**
     * Checks the following:
     * - The connection has only one listener when we start listening.
     * - We receive a reply on a message when we sent one.
     * - The checksum archive contains at least one entry.
     * - All the checksums are retrievable both as a map and individually, and
     * that these checksum are the same.
     * - It is possible to upload a file and then retrieve the checksum.
     * - The retrieved checksum has the correct precalculated checksum.
     * - It is possible to correct the file, and it new has a different value.
     * - That the new value equals a precalculated checksum for the new file.
     * @throws IOException If file handling error in test.
     */
    public void testCFS() throws IOException {
	File baseFileDir = TestInfo.BASE_FILE_DIR;
	if(!baseFileDir.isDirectory()) {
	    assertTrue(baseFileDir.mkdirs());	    
	}
	
        cfs = ChecksumFileServer.getInstance();
        ChannelID arcReposQ = Channels.getTheRepos();
        ChannelID theCs = Channels.getTheCR();
        conn = (JMSConnectionMockupMQ) JMSConnectionFactory.getInstance();

        GenericMessageListener listener = new GenericMessageListener();
        conn.setListener(arcReposQ, listener);

        // make sure that there is 1 listener 
        int expectedListeners = 1;
        assertEquals("Number of listeners on queue " + theCs + " should be "
                + expectedListeners + " before upload.",
                expectedListeners, conn.getListeners(theCs).size());

        File testFile = TestInfo.UPLOADMESSAGE_TESTFILE_1;
        assertTrue("The test file must already exist.", testFile.isFile());
        RemoteFile rf = RemoteFileFactory.getInstance(testFile, false, false, false);
        UploadMessage upMsg = new UploadMessage(theCs, arcReposQ, rf);
        JMSConnectionMockupMQ.updateMsgID(upMsg, "upload1");
        
        cfs.visit(upMsg);
        conn.waitForConcurrentTasksToFinish();
        expectedListeners = 0;

        // Check that UploadMessage has been replied to arcrepos queue.
        // It should have been received by GenericMessageListener:
        assertTrue("Should have received at least one message on the arcRepos queue",
                listener.messagesReceived.size() >= 1);

        // Retrieve the map of the checksums.
        GetAllChecksumsMessage gacMsg = new GetAllChecksumsMessage(theCs, 
        	arcReposQ, "THREE");
        JMSConnectionMockupMQ.updateMsgID(gacMsg, "getallchecksums1");
        assertEquals("The GetAllChecksumsMessage should have replica id THREE", 
                "THREE", gacMsg.getReplicaId());
        cfs.visit(gacMsg);
        conn.waitForConcurrentTasksToFinish();
        // retrieve the results
        File tmp = File.createTempFile("tmp2", "tmp", TestInfo.BASE_FILE_DIR);
        gacMsg.getData(tmp);
        
        String archive = FileUtils.readFile(tmp);
        
        // Retrieve all file names
        GetAllFilenamesMessage afnMsg = new GetAllFilenamesMessage(theCs, 
        	arcReposQ, "THREE");
        JMSConnectionMockupMQ.updateMsgID(afnMsg, "allfilenames1");
        assertEquals("The GetAllFilenamesMessage should have replica id THREE", 
                "THREE", afnMsg.getReplicaId());
        cfs.visit(afnMsg);
        conn.waitForConcurrentTasksToFinish();
  
        // initialise the GetChecksumMessage to retrieve the checksum of each 
        // file in the list of all the filenames.
        GetChecksumMessage csMsg;
        // Get the filenames
        File outfile = File.createTempFile("tmp", "tmp");
        afnMsg.getData(outfile);
        List<String> names = FileUtils.readListFromFile(outfile);
        
        // Assume more than one file has been uploaded (
        assertTrue("There should be files within the ChecksumArchive.", 
        	names.size() > 0);

        // Retrieve and verify checksum of all records in archive. 
        // (one file at the time)
        for(String name : names) {
            csMsg = new GetChecksumMessage(theCs, arcReposQ, name, "THREE");
            JMSConnectionMockupMQ.updateMsgID(csMsg, "cs" + name + "1");
            cfs.visit(csMsg);
            conn.waitForConcurrentTasksToFinish();
            
            // Check that the message is OK and message exists.
            assertTrue("Retrieving checksum for file '" + name + "'", 
        	    csMsg.isOk());
            assertNotNull("The checksum of file '" + name 
        	    + "' may not be null.", csMsg.getChecksum());

            // check that the entry exists.
            assertTrue("The archive should have a entry for the file with the correct checksum: "
                    + name + "##" + csMsg.getChecksum(), 
                    archive.contains(name + "##" + csMsg.getChecksum()));
        }

        
        // Retrieve the checksum for the uploaded file from the checksum 
        // archive.
        csMsg = new GetChecksumMessage(theCs, arcReposQ, testFile.getName(), "THREE");
        JMSConnectionMockupMQ.updateMsgID(csMsg, "cs1");
        assertEquals("The GetChecksumMessage should have replica id THREE", 
                "THREE", csMsg.getReplicaId());
        cfs.visit(csMsg);
        conn.waitForConcurrentTasksToFinish();
        
        // Check that the checksum is correct.
        String res = csMsg.getChecksum();
        assertEquals("The file '" + testFile.getName() + "' should have the checksum '" 
                + TestInfo.UPLOADFILE_1_CHECKSUM + "'", TestInfo.UPLOADFILE_1_CHECKSUM, res);
        
        // Check the CorrectMessage.
        CorrectMessage corMsg;
        // Check that it will not be possible to correct with wrong credentials.
        RemoteFile corFile = RemoteFileFactory.getCopyfileInstance(TestInfo.CORRECTMESSAGE_TESTFILE_1);
        corMsg = new CorrectMessage(theCs, arcReposQ, TestInfo.UPLOADFILE_1_CHECKSUM, corFile, "THREE", "ERROR-CREDENTIALS!");
        JMSConnectionMockupMQ.updateMsgID(corMsg, "correctError");
        cfs.visit(corMsg);
        conn.waitForConcurrentTasksToFinish();
        
        assertFalse("It should not be allowed to correct with a wrong credential.", corMsg.isOk());
        assertTrue("The error message of the correct message should contain the wrong credentials: " + corMsg.getErrMsg(), corMsg.getErrMsg().contains("ERROR-CREDENTIALS!"));
        
        // Check that a valid message will go through.
        corMsg = new CorrectMessage(theCs, arcReposQ, TestInfo.UPLOADFILE_1_CHECKSUM, corFile, "THREE", "examplecredentials");
        JMSConnectionMockupMQ.updateMsgID(corMsg, "correct1");
        assertEquals("The CorrectMessage should have replica id THREE", 
                "THREE", corMsg.getReplicaId());
        cfs.visit(corMsg);
        conn.waitForConcurrentTasksToFinish();
        
        assertTrue("The correct message should be OK.", corMsg.isOk());
        
        // make sure, that the checksum of the file has changed.
        csMsg = new GetChecksumMessage(theCs, arcReposQ, TestInfo.CORRECTMESSAGE_TESTFILE_1.getName(), "THREE");
        JMSConnectionMockupMQ.updateMsgID(csMsg, "cs2");
        cfs.visit(csMsg);
        conn.waitForConcurrentTasksToFinish();

        // Checks that the new file has been uploaded.
        res = csMsg.getChecksum();
        assertEquals("The checksum for the correct should be '" 
        	+ TestInfo.CORRECTFILE_1_CHECKSUM + "', but was '" + res + "'", 
        	TestInfo.CORRECTFILE_1_CHECKSUM, res);
        
        // Check that you cannot correct with a different checksum than within the archive.
        RemoteFile corFile2 = RemoteFileFactory.getCopyfileInstance(TestInfo.CORRECTMESSAGE_TESTFILE_2);
        corMsg = new CorrectMessage(theCs, arcReposQ, TestInfo.UPLOADFILE_1_CHECKSUM, corFile2, "THREE", "examplecredentials");
        JMSConnectionMockupMQ.updateMsgID(corMsg, "correct2");
        cfs.visit(corMsg);
        conn.waitForConcurrentTasksToFinish();
        
        assertFalse("The this correct message should not be OK.", 
                corMsg.isOk());
        
        // test missing functions.
        assertTrue("The application id should contain the local IP '" 
                + SystemUtils.getLocalIP() + "' but was '"+ cfs.getAppId() + "'", 
                cfs.getAppId().contains(SystemUtils.getLocalIP()));
        
        cfs.close();
    }

    public void testNoInitialFile() throws IOException {
	// Delete the checksum archive file.
	File archiveFile = TestInfo.CHECKSUM_FILE;
	archiveFile.delete();

	// Restart the checksum file server.
	cfs = ChecksumFileServer.getInstance();
	cfs.cleanup();
        cfs = ChecksumFileServer.getInstance();
        ChannelID arcReposQ = Channels.getTheRepos();
        ChannelID theCs = Channels.getTheCR();
        conn = (JMSConnectionMockupMQ) JMSConnectionFactory.getInstance();

        GenericMessageListener listener = new GenericMessageListener();
        conn.setListener(arcReposQ, listener);

	GetAllChecksumsMessage gacMsg = new GetAllChecksumsMessage(theCs, 
		arcReposQ, Replica.getReplicaFromId("ONE").getId());
        JMSConnectionMockupMQ.updateMsgID(gacMsg, "gac1");
	cfs.visit(gacMsg);
        conn.waitForConcurrentTasksToFinish();

        File tmp = File.createTempFile("tmp1", "tmp", TestInfo.BASE_FILE_DIR);
        gacMsg.getData(tmp);
	
	assertNotNull("The file should exist.", tmp.isFile());
	assertEquals("The file should have 0 lines.", 0, FileUtils.countLines(tmp));
	
	GetAllFilenamesMessage gafMsg = new GetAllFilenamesMessage(theCs, 
		arcReposQ, Replica.getReplicaFromId("ONE").getId());
        JMSConnectionMockupMQ.updateMsgID(gafMsg, "gaf1");
	cfs.visit(gafMsg);
	conn.waitForConcurrentTasksToFinish();
	
        // Get the filenames
        File outfile = File.createTempFile("tmp", "tmp");
        gafMsg.getData(outfile);
        List<String> filenames = FileUtils.readListFromFile(outfile);
	
	assertNotNull("An empty list of filenames should be retrievable, "
		+ "not null.", filenames);
	assertEquals("The list of filenames should be empty.", 
		filenames.size(), 0);

        RemoteFile rf = RemoteFileFactory.getInstance(
        	TestInfo.UPLOADMESSAGE_TESTFILE_1, true, false, true);
	UploadMessage upMsg = new UploadMessage(theCs, arcReposQ, rf);
        JMSConnectionMockupMQ.updateMsgID(upMsg, "upload1");
	cfs.visit(upMsg);
	conn.waitForConcurrentTasksToFinish();
	
	gafMsg = new GetAllFilenamesMessage(theCs, arcReposQ, 
		Replica.getReplicaFromId("ONE").getId());
        JMSConnectionMockupMQ.updateMsgID(gafMsg, "gaf2");
	cfs.visit(gafMsg);
	conn.waitForConcurrentTasksToFinish();
	
        // Get the filenames
        outfile = File.createTempFile("tmp", "tmp");
        gafMsg.getData(outfile);
        filenames = FileUtils.readListFromFile(outfile);

	// check that only one entry was given, and that it had correct name.
	assertEquals("There should only be 1 file within the archive.", 
		filenames.size(), 1);
	assertEquals("Wrong file name retrieved.", 
		TestInfo.UPLOADMESSAGE_TESTFILE_1.getName(), filenames.get(0));
    }
    
    /**
     * Ensure, that the application dies if given the wrong input.
     */
    public void testApplication() {
        ReflectUtils.testUtilityConstructor(ChecksumFileApplication.class);

        PreventSystemExit pse = new PreventSystemExit();
        PreserveStdStreams pss = new PreserveStdStreams(true);
        pse.setUp();
        pss.setUp();
        
        try {
            ChecksumFileApplication.main(new String[]{"ERROR"});
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
