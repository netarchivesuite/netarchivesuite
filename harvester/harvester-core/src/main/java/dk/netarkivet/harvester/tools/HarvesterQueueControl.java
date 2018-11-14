package dk.netarkivet.harvester.tools;

import java.util.Enumeration;

import javax.jms.JMSException;
import javax.jms.QueueBrowser;

import com.sun.messaging.jms.Message;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.harvester.distribute.HarvesterChannels;

/** 
 * Use the JMSConnection.createQueueBrowser() method to test number of
 * entries in a specific Queue.
 * If test successful, we will add a check of messages in the queue for the harvestjobchannel before submitting another 
 * message to this queue 
 * 
 * @author svc
 *
 */
public class HarvesterQueueControl {

	public static void main(String[] args) throws JMSException {
		String harvestChannelName = args[0];
		
		ChannelID queueID = HarvesterChannels.getHarvestJobChannelId(harvestChannelName, false);
		System.out.println("Found in queue '" + queueID.getName() + "' #messages: " + getCount(queueID));
	}

	public static int getCount(ChannelID queueID) throws JMSException {
		JMSConnection con = JMSConnectionFactory.getInstance();
		QueueBrowser qBrowser = con.createQueueBrowser(queueID);
		Enumeration msgs = qBrowser.getEnumeration();
		int count=0;
		if ( !msgs.hasMoreElements() ) {
		    //System.out.println("No messages in queue '" + queueID.getName() + "'"):
		} else { 
		    while (msgs.hasMoreElements()) { 
		        Message tempMsg = (Message)msgs.nextElement(); 
		        System.out.println("Message # " + count + ": "+ tempMsg);
		        count++;
		    }
		}
		con.cleanup();
		qBrowser.close();
		return count;
	}
	
}
