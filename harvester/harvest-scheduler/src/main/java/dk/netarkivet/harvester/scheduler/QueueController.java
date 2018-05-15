package dk.netarkivet.harvester.scheduler;

import java.util.Enumeration;

import javax.jms.JMSException;
import javax.jms.QueueBrowser;
import javax.jms.QueueSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;

/**
 * Helper class to test the status of the number of submitted jobs on our JMS Queues.
 * Uses the same QueueSession for all calls to getCount() to avoid memory-leak caused by accumulation
 * of imqConsumerReader threads.
 */
public class QueueController {

    /** The logger to use. */
    private static final Logger log = LoggerFactory.getLogger(QueueController.class);
    
    /** Connection to JMS provider. */
    private JMSConnection jmsConnection; 
    /** The current qSession. */
    QueueSession qSession = null;
    
    public QueueController() {
        this.jmsConnection = JMSConnectionFactory.getInstance();
    }
    
    /**
     * Retrieve the number of current messages defined by the given queueID. 
     * @param queueID a given QueueID
     * @return the number of current messages defined by the given queueID
     */
    synchronized int getCount(ChannelID queueID) {
        QueueBrowser qBrowser;
        int submittedCounter = 0;
        try {
            if (qSession == null) {
                qSession = jmsConnection.getQueueSession();
                log.info("Created a new QueueSession");
            }
            qBrowser = jmsConnection.createQueueBrowser(queueID, qSession);
            Enumeration msgs = qBrowser.getEnumeration();

            if ( !msgs.hasMoreElements() ) {
                return 0;
            } else { 
                while (msgs.hasMoreElements()) { 
                    msgs.nextElement();
                    submittedCounter++;
                }
            }
            qBrowser.close();
            qBrowser = null;
        } catch (JMSException e) {
            log.warn("JMSException thrown: ", e);
            jmsConnection.onException(e); // See if we want to reconnect now
            qSession = null;
        } catch (Throwable e1) {
            log.warn("Unexpected exception of type {} thrown: ", e1.getClass().getName(), e1);
        }

        return submittedCounter;
    }
}
