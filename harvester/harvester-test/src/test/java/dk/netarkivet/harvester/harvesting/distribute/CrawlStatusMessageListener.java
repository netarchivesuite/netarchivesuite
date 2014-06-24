
package dk.netarkivet.harvester.harvesting.distribute;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.harvester.datamodel.JobStatus;

import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.ArrayList;

/**
 * Utility class to listen to and record all CrawlStatusMessages
 */
public class CrawlStatusMessageListener implements MessageListener {
    public ArrayList<JobStatus> status_codes = new ArrayList<JobStatus>();
    public ArrayList<Long> jobids = new ArrayList<Long>();
    public ArrayList<CrawlStatusMessage> messages = new ArrayList<CrawlStatusMessage>();

    public void onMessage(Message message) {
        NetarkivetMessage naMsg = JMSConnection.unpack(message);
        if (naMsg instanceof CrawlStatusMessage) {
            CrawlStatusMessage csm = (CrawlStatusMessage) naMsg;
            status_codes.add(csm.getStatusCode());
            jobids.add(Long.valueOf(csm.getJobID()));
            messages.add(csm);
            synchronized(this) {
                notifyAll();
            }
        }
    }
}
