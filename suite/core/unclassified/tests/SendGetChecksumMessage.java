import dk.netarkivet.archive.checksum.distribute.GetChecksumMessage;
import dk.netarkivet.archive.distribute.ArchiveMessage;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;

/**
 * Program that sends a lot of batch messages to bitarchives
 * to provoke OOM errors.
 * Used for testing a production bug in 3.12.1
 */
public class SendGetChecksumMessage {

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println(
                    "arguments missing. Expected <repId> <numberOfmessages>");
            System.exit(1);
        }
        String repId = args[0];
        long messagesToSend = Long.parseLong(args[1]);        
        JMSConnection con = JMSConnectionFactory.getInstance();
        for (long current=0; current < messagesToSend; current++) {
            String filename = "TESTFILE-" 
                + System.currentTimeMillis() + "." + current + ".arc";
            System.out.println("Sending Getchecksum-message (" + current 
                    + ") for presumed nonexisting file " + filename);
            sendGetChecksumMessage(filename, repId, con);
        }
        
        con.cleanup();
    }
    
    public static void sendGetChecksumMessage(String filename, String repId, JMSConnection con) {
        ChannelID error = Channels.getError();
        ChannelID bamon = Channels.getBaMonForReplica(repId);
        ArchiveMessage msg = new GetChecksumMessage(bamon, error, filename, repId); 
        con.send(msg);
    }
    

}
