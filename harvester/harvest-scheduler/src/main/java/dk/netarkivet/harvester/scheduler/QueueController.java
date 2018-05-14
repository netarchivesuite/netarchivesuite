package dk.netarkivet.harvester.scheduler;

import java.util.Enumeration;

import javax.jms.JMSException;
import javax.jms.QueueBrowser;
import javax.jms.QueueSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionSunMQ;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.Settings;

public class QueueController {

    /** The logger to use. */
    private static final Logger log = LoggerFactory.getLogger(QueueController.class);
    
    /** Connection to JMS provider. */
    private JMSConnection jmsConnection;
    /** Limit to the number of calls to getCount before resetting the jmsconnection. */
    private static int CALL_LIMIT = 100;
    /** Setting to override the CALL_LIMIT of 100. */ 
    private static String QUEUECOUNTER_RESET_LIMIT = "settings.harvester.scheduler.queuecounterResetLimit";
    /** JMSBroker host */
    
    
    private int callCounter;
    public QueueController() {
        this.jmsConnection = new JMSConnectionSunMQ();
        this.callCounter = 0;
        try {
            CALL_LIMIT = Settings.getInt(QUEUECOUNTER_RESET_LIMIT);
            log.debug("Setting {} overrided in settings to {}", QUEUECOUNTER_RESET_LIMIT, CALL_LIMIT);
        } catch (UnknownID e) {
            log.debug("Setting {} not overrided in settings. Keeping hardwired value of {}", QUEUECOUNTER_RESET_LIMIT, CALL_LIMIT);
        }
    }
    
    /**
     * Retrieve the number of current messages defined by the given queueID. 
     * @param queueID a given QueueID
     * @return the number of current messages defined by the given queueID
     */
    synchronized int getCount(ChannelID queueID) {
        QueueSession qSession;
        QueueBrowser qBrowser;
        int submittedCounter = 0;
        try {
            if (callCounter >= CALL_LIMIT) {
                callCounter=0;
                jmsConnection.cleanup();
                jmsConnection = new JMSConnectionSunMQ();
                log.debug("Resetting the JMSConnection for the QueueController class");
            } else {
                callCounter++;
                log.debug("Now reached the {} call to the getCount method. Argument queueID is {}", callCounter, queueID);
            }
            
            qSession = jmsConnection.getQueueSession();
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
            qSession.close();
        } catch (JMSException e) {
            log.warn("JMSException thrown: ", e);
            jmsConnection.onException(e); // See if we want to reconnect now
        } catch (Throwable e1) {
            log.warn("Unexpected exception of type {} thrown: ", e1.getClass().getName(), e1);
        }

        return submittedCounter;
    }
}
