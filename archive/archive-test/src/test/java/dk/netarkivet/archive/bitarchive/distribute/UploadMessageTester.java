package dk.netarkivet.archive.bitarchive.distribute;

import junit.framework.TestCase;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

public class UploadMessageTester extends TestCase {
    ReloadSettings rs = new ReloadSettings();

    public UploadMessageTester(String sTestName) {
        super(sTestName);
    }

    public void setUp() {
        rs.setUp();
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
    }

    public void tearDown() {
        rs.tearDown();
    }

    public void testInvalidArguments() {
        try {
            new UploadMessage(Channels.getTheBamon(),
                    Channels.getTheRepos(),
                    RemoteFileFactory.getInstance(null, true, false, true));
            fail("Should throw ArgumentNotValid on null file");
        } catch (ArgumentNotValid e) {
            // expected case
        }
    }
}