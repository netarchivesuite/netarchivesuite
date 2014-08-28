
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.monitor.registry.distribute.RegisterHostMessage;

/**
 * Send multiple RegisterHostMessages to COMMON_MONITOR.
 */
public class SendMultipleRegisterMessages {

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println(
                    "arguments missing. Expected <numberOfmessages>");
            System.exit(1);
        }
        long messagesToSend = Long.parseLong(args[0]); 
        JMSConnection con = JMSConnectionFactory.getInstance();
        for (long current=0; current < messagesToSend; current++) {
            NetarkivetMessage msg = new RegisterHostMessage("unknown-host", 8010, 8020);           
            con.send(msg);
        }
        con.cleanup();
    }
}
