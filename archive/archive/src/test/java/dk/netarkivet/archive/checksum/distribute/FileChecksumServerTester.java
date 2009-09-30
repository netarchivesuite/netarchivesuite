/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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
package dk.netarkivet.archive.checksum.distribute;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.arcrepository.bitpreservation.ChecksumEntry;
import dk.netarkivet.archive.bitarchive.distribute.UploadMessage;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.ClassAsserts;
import dk.netarkivet.testutils.GenericMessageListener;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;
import junit.framework.TestCase;

public class FileChecksumServerTester extends TestCase {

    ReloadSettings rs = new ReloadSettings();
    UseTestRemoteFile utrf = new UseTestRemoteFile();
    ChannelID arcReposQ;
    ChannelID theCs;
    JMSConnectionMockupMQ conn;
    GenericMessageListener listener;
    
    ChecksumFileServer cfs;
    
    protected void setUp() {
	rs.setUp();
	JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        // ??

        // Set the test settings.
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, TestInfo.BASE_FILE_DIR.getAbsolutePath());
        Settings.set(ArchiveSettings.CHECKSUM_FILENAME, TestInfo.CHECKSUM_FILE.getAbsolutePath());

        // Create/recreate the checksum.md5 file
        try {
	    FileWriter fw = new FileWriter(TestInfo.CHECKSUM_FILE);
	    
	    fw.write("test1.arc##1234567890" + "\n" + "test2.arc##0987654321");
	    fw.flush();
	    fw.close();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	
	Settings.set(CommonSettings.USE_REPLICA_ID, "THREE");
	Replica THREE = Replica.getReplicaFromId("THREE");
	
	arcReposQ = Channels.getTheRepos();
	theCs = Channels.getTheCR();
        conn = (JMSConnectionMockupMQ) JMSConnectionFactory.getInstance();
        listener = new GenericMessageListener();
    }
    
    protected void tearDown() {
	// ??
	JMSConnectionMockupMQ.clearTestQueues();
	rs.tearDown();
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
	
        conn.cleanup();
        conn = (JMSConnectionMockupMQ) JMSConnectionFactory.getInstance();

	cfs = ChecksumFileServer.getInstance();
	
        conn.setListener(arcReposQ, listener);

        int expectedListeners = 1;
        assertEquals("Number of listeners on queue " + theCs + " should be "
                + expectedListeners + " before upload.",
                expectedListeners, conn.getListeners(theCs).size());

        File testFile = TestInfo.UPLOADMESSAGE_TESTFILE_1;
        RemoteFile rf = RemoteFileFactory.getInstance(
                testFile, true, false, true);
        UploadMessage upMsg = new UploadMessage(theCs, arcReposQ, rf);

        cfs.visit(upMsg);
        conn.waitForConcurrentTasksToFinish();

        // Check that UploadMessage has been replied to arcrepos queue.
        // It should have been received by GenericMessageListener:
        assertTrue("Should have received at least one message on the arcRepos queue",
                listener.messagesReceived.size() >= 1);

        // Retrieve the map of the checksums.
        GetAllChecksumsMessage gacMsg = new GetAllChecksumsMessage(theCs, 
        	arcReposQ, Replica.getReplicaFromId("ONE").getId());
        cfs.visit(gacMsg);
        conn.waitForConcurrentTasksToFinish();
        // retrieve the results
        File tmp = File.createTempFile("tmp2", "tmp", TestInfo.BASE_FILE_DIR);
        gacMsg.getData(tmp);

        // Retrieve all file names
        GetAllFilenamesMessage afnMsg = new GetAllFilenamesMessage(theCs, 
        	arcReposQ, Replica.getReplicaFromId("ONE").getId());
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
            cfs.visit(csMsg);
            conn.waitForConcurrentTasksToFinish();
            
            // Check that the message is OK and message exists.
            assertTrue("Retrieving checksum for file '" + name + "'", 
        	    csMsg.isOk());
            assertNotNull("The checksum of file '" + name 
        	    + "' may not be null.", csMsg.getChecksum());
  
            // TODO: fix this.
/*            // Check that the message contains the same checksum as the map
            assertEquals("The checksum message in the Map is '" 
        	    + csMap.get(name) + "' whereas the checksum from the "
        	    + "checksum message is '" + csMsg.getChecksum() + "'.", 
        	    csMsg.getChecksum(), csMap.get(name));
*/
        }

        
        // Retrieve the checksum for the uploaded file from the checksum 
        // archive.
        csMsg = new GetChecksumMessage(theCs, arcReposQ, testFile.getName(), "THREE");
        cfs.visit(csMsg);
        conn.waitForConcurrentTasksToFinish();
        
        // Check that the checksum is correct.
        String res = csMsg.getChecksum();
        assertEquals("The arc file should have the checksum '" + TestInfo.TESTFILE_1_CHECKSUM
        	+ "'", TestInfo.TESTFILE_1_CHECKSUM, res);
        
        // Check the CorrectMessage.
        RemoteFile corFile = RemoteFileFactory.getCopyfileInstance(TestInfo.CORRECTMESSAGE_TESTFILE);
        CorrectMessage corMsg = new CorrectMessage(theCs, arcReposQ, TestInfo.CORRECTFILE_1_CHECKSUM, corFile);
        cfs.visit(corMsg);
        conn.waitForConcurrentTasksToFinish();
        
        // make sure, that the checksum of the file has changed.
        csMsg = new GetChecksumMessage(theCs, arcReposQ, TestInfo.CORRECTMESSAGE_TESTFILE.getName(), "THREE");
        cfs.visit(csMsg);
        conn.waitForConcurrentTasksToFinish();

        // Checks that the new file has been uploaded.
        res = csMsg.getChecksum();
        assertEquals("The checksum for the correct should be '" 
        	+ TestInfo.CORRECTFILE_1_CHECKSUM + "', but was '" + res + "'", 
        	TestInfo.CORRECTFILE_1_CHECKSUM, res);
    }
    
    public void testNoInitialFile() throws IOException {
	// Delete the checksum archive file.
	String archiveFilename = Settings.get(ArchiveSettings.CHECKSUM_FILENAME);
	File archiveFile = new File(archiveFilename);
	archiveFile.delete();

	// Restart the checksum file server.
	cfs = ChecksumFileServer.getInstance();
	cfs.cleanup();
	cfs = ChecksumFileServer.getInstance();
	
	// set the listener.
        conn.setListener(arcReposQ, listener);

	GetAllChecksumsMessage gacMsg = new GetAllChecksumsMessage(theCs, 
		arcReposQ, Replica.getReplicaFromId("ONE").getId());
	cfs.visit(gacMsg);
        conn.waitForConcurrentTasksToFinish();

        File tmp = File.createTempFile("tmp1", "tmp", TestInfo.BASE_FILE_DIR);
        gacMsg.getData(tmp);
	List<ChecksumEntry> content = ChecksumEntry.parseChecksumJob(tmp);
	
	assertNotNull("An empty map should be retrievable, not null.", content);
	assertEquals("The retrieved map should be empty.",content.size(), 0);
	
	GetAllFilenamesMessage gafMsg = new GetAllFilenamesMessage(theCs, 
		arcReposQ, Replica.getReplicaFromId("ONE").getId());
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
	cfs.visit(upMsg);
	conn.waitForConcurrentTasksToFinish();
	
	gafMsg = new GetAllFilenamesMessage(theCs, arcReposQ, 
		Replica.getReplicaFromId("ONE").getId());
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
}
