package dk.netarkivet.archive.arcrepository.distribute;

import java.io.File;

import junit.framework.TestCase;
import dk.netarkivet.archive.distribute.ArchiveMessageHandler;
import dk.netarkivet.archive.distribute.ArchiveMessageVisitor;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/**
 * Basic unit tests for the StoreMessage.
 */
public class StoreMessageTester extends TestCase {
    ReloadSettings rs = new ReloadSettings();
    UseTestRemoteFile rm = new UseTestRemoteFile();
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS, TestInfo.WORKING);

    public void setUp() {
        rs.setUp();
        rm.setUp();
        mtf.setUp();
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
    }

    public void tearDown() {
        rm.tearDown();
        rs.tearDown();
        mtf.tearDown();
    }

    public void testInvalidArguments() {

        try {
            new StoreMessage( (ChannelID) null, new File(""));
            fail("Should throw ArgumentNotValid on null Channel");
        } catch (ArgumentNotValid e) {
            // expected case
        }

        try {
            new StoreMessage(Channels.getTheRepos(), (File) null);
            fail("Should throw ArgumentNotValid on null file");
        } catch (ArgumentNotValid e) {
            // expected case
        }
    }
    
    public void testValid() {
        StoreMessage msg = new StoreMessage(Channels.getError(), 
                TestInfo.ARCFILE);
        
        ArchiveMessageVisitor amv = new ArchiveMessageHandler() {
            @Override
            public void visit(StoreMessage msg) {
                // ??
                assertEquals("Should be replied to the error queue.",
                        msg.getReplyTo().getName(), Channels.getError().getName());
                assertEquals("Should be sent to the repos queue.",
                        msg.getTo().getName(), Channels.getTheRepos().getName());
                assertEquals("Should be the file '" + TestInfo.ARCFILE.getName() + "'.",
                        msg.getArcfileName(), TestInfo.ARCFILE.getName());
            }
        };
        
        msg.accept(amv);
        // test getters
        assertEquals(Channels.getError(), msg.getReplyTo());
        assertEquals(TestInfo.ARCFILE.getName(), msg.getArcfileName());
    }

}
