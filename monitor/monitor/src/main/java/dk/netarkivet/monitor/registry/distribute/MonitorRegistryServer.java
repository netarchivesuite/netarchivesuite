package dk.netarkivet.monitor.registry.distribute;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.monitor.distribute.MonitorMessageHandler;
import dk.netarkivet.monitor.registry.MonitorRegistry;

/**
 * The monitor registry server listens on JMS for hosts that wish to register
 * themselves to the service. The registry lists hosts that can be monitored
 * with JMX.
 */
public class MonitorRegistryServer extends MonitorMessageHandler
        implements CleanupIF {
    private static MonitorRegistryServer instance;
    private final Log log = LogFactory.getLog(getClass());

    /**
     * Start listening for registry messages.
     */
    private MonitorRegistryServer() {
        JMSConnectionFactory.getInstance().setListener(
                Channels.getTheMonitorServer(), this);
        log.info("MonitorRegistryServer listening for messages on channel '"
                 + Channels.getTheMonitorServer() + "'");
    }

    /** Get the registry server singleton.
     * @return The registry server.
     */
    public static MonitorRegistryServer getInstance() {
        if (instance == null) {
            instance = new MonitorRegistryServer();
        }
        return instance;
    }

    /**
     * This method registers the sender as a host to be monitored with JMX.
     *
     * @throws ArgumentNotValid on null parameter. 
     */
    public void visit(RegisterHostMessage msg) {
        ArgumentNotValid.checkNotNull(msg, "RegisterHostMessage msg");
        MonitorRegistry.getInstance().register(msg.getHostEntry());
    }

    /** Remove listener on shutdown. */
    public void cleanup() {
        // FIXME These commands fail when shutting down properly. (kill $PID)
        // instead of kill -9 $PID. See NAS-1976
        //JMSConnectionFactory.getInstance().removeListener(
        //        Channels.getTheMonitorServer(), this);
    }
}
