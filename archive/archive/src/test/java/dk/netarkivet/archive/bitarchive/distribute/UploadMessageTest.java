package dk.netarkivet.archive.bitarchive.distribute;

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.RemoteFileFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

public class UploadMessageTest {
    ReloadSettings rs = new ReloadSettings();

    @Before
    public void setUp() {
        rs.setUp();
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
    }

    @After
    public void tearDown() {
        rs.tearDown();
    }

    @Test
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