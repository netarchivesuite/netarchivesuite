package dk.netarkivet.harvester.distribute;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import junit.framework.TestCase;

/**
 * Unittests of the class dk.netarkivet.common.distribute.Channels.
 */
public class HarvesterChannelsTester extends TestCase {
    /**
     * Test if static Channels.isTopic(String name) works.
     * Only names containing substring "ALL_BA" is considered a name
     * for a topic.
     */
    public void testIsTopic() {
        ChannelID[]queues = new ChannelID[]{
                HarvesterChannels.getHarvestJobChannelId(
                        new HarvestChannel("FOCUSED", false, true, "")),
                HarvesterChannels.getHarvestJobChannelId(
                		new HarvestChannel("SNAPSHOT", true, true, "")),
                HarvesterChannels.getHarvestMonitorChannel()
        };
        for (ChannelID queue : queues) {
           String queueName = queue.getName();
           assertFalse(queueName + " is not a topic",
                   Channels.isTopic(queueName));
        }


        ChannelID[]topics = new ChannelID[]{
                HarvesterChannels.getHarvesterStatusChannel()
        };

        for (ChannelID topic : topics) {
            String topicName = topic.getName();
            assertTrue(topicName + " is a topic",
                    Channels.isTopic(topicName));
         }
    }
}
