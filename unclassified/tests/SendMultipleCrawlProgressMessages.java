
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage;
import dk.netarkivet.harvester.harvesting.distribute.CrawlProgressMessage.CrawlStatus;

/**
 * Send multiple CrawlProgressMessages to HARVEST_MONITOR.
 */
public class SendMultipleCrawlProgressMessages {

    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println(
                    "arguments missing. Expected <HARVEST-ID> <JOB-ID> <numberOfmessages>");
            System.exit(1);
        }
        long harvestId = Long.parseLong(args[0]);
        long jobId = Long.parseLong(args[1]); 
        long messagesToSend = Long.parseLong(args[2]); 
        JMSConnection con = JMSConnectionFactory.getInstance();
        for (long current=0; current < messagesToSend; current++) {
            
            NetarkivetMessage msg = getCrawlProgressMessage(harvestId, jobId);           
            con.send(msg);
        }
        con.cleanup();
    }

    private static NetarkivetMessage getCrawlProgressMessage(long harvestId,
            long jobId) {
        CrawlProgressMessage msg = new CrawlProgressMessage(harvestId, jobId, "Running");
        msg.setStatus(CrawlStatus.CRAWLER_ACTIVE);
        return msg;
    }
}
