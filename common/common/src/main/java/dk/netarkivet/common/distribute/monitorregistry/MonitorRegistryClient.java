package dk.netarkivet.common.distribute.monitorregistry;

/**
 * Client for registering JMX monitoring at registry.
 */
public interface MonitorRegistryClient {
    /** Register this host for monitoring.
     * @param hostName The name of the host.
     * @param jmxPort The port for JMX communication.
     * @param rmiPort The RMI port for JMX communication.
     */
    void register(String hostName, int jmxPort, int rmiPort);
}
