import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.harvester.distribute.IndexReadyMessage;

/**
 * Send a {@link IndexReadyMessage} to the HarvestJobManager to
 * inform, that an deduplication index is ready for at certain harvest ID.
 * 
 */
public class SendIndexReadyMessage {

    /**
     * @param args The harvestID
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println(
                    "arguments missing. Expected <harvestid>");
            System.exit(1);
        }
        Long harvestId = Long.parseLong(args[0]);
        JMSConnection con = JMSConnectionFactory.getInstance();
        ChannelID to = Channels.getTheSched();
        ChannelID replyTo = Channels.getError();
        boolean indexisready = true;
        IndexReadyMessage msg = new IndexReadyMessage(harvestId, indexisready, to, replyTo);
        
        con.send(msg);
        con.cleanup();
    }
}
