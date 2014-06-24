package dk.netarkivet.harvester.distribute;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import junit.framework.TestCase;

public class IndexReadyMessageTester extends TestCase {

    public void testConstructor() {
        ChannelID replyTo = Channels.getTheIndexServer();
        ChannelID to = Channels.getTheSched();
        IndexReadyMessage irm = new IndexReadyMessage(42L, true, to, replyTo);
        assertTrue(42L == irm.getHarvestId());
        assertTrue(true == irm.getIndexOK());
        
        irm = new IndexReadyMessage(43L, false, to, replyTo);
        assertTrue(43L == irm.getHarvestId());
        assertTrue(false == irm.getIndexOK());
        
        
    }
}
