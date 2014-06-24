package dk.netarkivet.monitor.jmx;



/**
 * Common interface for objects that supply JMX connections
 * to remote servers.
 *
 * This interface does not specify which protocol to use for
 * connection, nor whether previously created connections will
 * be cached for reuse.
 */
public interface JMXProxyConnectionFactory {
    /**
     * Establish a JMX connection to a remote server.
     * @param server The name of remote server to connect to.
     * @param port The port to connect to on the remote server.
     * @param rmiPort The RMI-port to use in this connection.
     * @param userName The user name to log in as.
     * @param password The password for the specified user.
     * @return a connection object that can be used for accessing
     * MBeans on the remote server.
     */
    JMXProxyConnection getConnection(
            String server,
            int port,
            int rmiPort,
            String userName,
            String password);
}
