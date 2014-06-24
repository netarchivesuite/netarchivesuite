
package dk.netarkivet.testutils;

import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.ArrayList;
import java.util.List;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.NetarkivetMessage;

/**
 * A bare bones MessageListener used for unit testing.
 */
public class GenericMessageListener implements MessageListener {

    /** An ordered list of all messages received by this listener */
    public List<NetarkivetMessage> messagesReceived = new ArrayList<NetarkivetMessage>();

    public void onMessage(Message message) {
        NetarkivetMessage naMsg = JMSConnection.unpack(message);
        messagesReceived.add(naMsg);
    }
}
