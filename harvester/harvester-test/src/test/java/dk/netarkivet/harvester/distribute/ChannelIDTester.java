package dk.netarkivet.harvester.distribute;

import dk.netarkivet.harvester.datamodel.*;
import junit.framework.TestCase;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;

/**
 * Tests the part of ChannelID class that relates to the harvesting module.
 * The rest of ChannelID is tested in dk.netarkivet.common.distribute.ChannelIDTester
 */
public class ChannelIDTester extends TestCase {
    /**
     * Test that each channel is equal only to itself.
     */
    public void testChannelIdentity(){
        ChannelID harvestJobChannel = HarvesterChannels.getHarvestJobChannelId(
                new HarvestChannel("FOCUSED", false, true, ""));
        ChannelID[] channelArray =
         {Channels.getAllBa(), harvestJobChannel, Channels.getAnyBa(),
          Channels.getError(), Channels.getTheRepos(), Channels.getTheBamon(),
          Channels.getTheSched(), Channels.getThisReposClient()};
        for (int i = 0; i<channelArray.length; i++){
            for (int j = 0; j<channelArray.length; j++){
                if (i == j) {
                    assertEquals("Two different instances of same queue "
                            +channelArray[i].getName(), channelArray[i],
                            channelArray[j]);
                    assertEquals("Two instances of same channel have different " +
                            "names: "
                            + channelArray[i].getName() + " and " +
                            channelArray[j].getName(), channelArray[i].getName(),
                            channelArray[j].getName() ) ;
                }
                else {
                    assertNotSame("Two different queues are the same object "
                            +channelArray[i].getName() + " "
                            + channelArray[j].getName(), channelArray[i],
                            channelArray[j]);
                    assertNotSame("Two different channels have same name",
                            channelArray[i].getName(), channelArray[j].getName());
                }
            }
        }
    }
}
