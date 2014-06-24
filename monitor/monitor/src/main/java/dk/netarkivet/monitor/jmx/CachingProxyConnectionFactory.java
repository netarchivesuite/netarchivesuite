
package dk.netarkivet.monitor.jmx;

import java.util.HashMap;
import java.util.Map;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/** Adds caching to another JMXProxyFactoryConnectionFactory. */
public class CachingProxyConnectionFactory implements
                                           JMXProxyConnectionFactory {
    /** The JMXProxyFactoryConnectionFactory, this class acts as a cache for. */
    private final JMXProxyConnectionFactory wrappedFactory;

    /**
     * Encapsulates the unit of information for checking the cache. That is, all
     * information used as arguments for the JMXProxyFactoryConnectionFactory.getConnection
     * method.
     */
    static class CacheKey {
        String server;
        int port, rmiPort;
        String userName, password;

        /**
         * Constructor for this class.
         *
         * @param server   The server name.
         * @param port     The JMX port number.
         * @param rmiPort  The RMI callback number.
         * @param userName The JMX user name.
         * @param password The JMX password.
         */
        public CacheKey(String server, int port, int rmiPort, String userName,
                        String password) {
            this.server = server;
            this.port = port;
            this.rmiPort = rmiPort;
            this.userName = userName;
            this.password = password;
        }

        /**
         * Equals method, that overrides the Object.equals method.
         *
         * @param o anObject
         *
         * @return true, if o is equal to this object; else false
         *
         * @see Object#equals(java.lang.Object)
         */
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            CacheKey cacheKey = (CacheKey) o;

            if (port != cacheKey.port) {
                return false;
            }
            if (rmiPort != cacheKey.rmiPort) {
                return false;
            }
            if (!password.equals(cacheKey.password)) {
                return false;
            }
            if (!server.equals(cacheKey.server)) {
                return false;
            }
            if (!userName.equals(cacheKey.userName)) {
                return false;
            }

            return true;
        }

        /**
         * hashCode method, that overrides the Object.hashCode method.
         *
         * @return the hashcode for this object
         *
         * @see Object#hashCode()
         */
        public int hashCode() {
            int result;
            result = server.hashCode();
            result = 31 * result + port;
            result = 31 * result + rmiPort;
            result = 31 * result + userName.hashCode();
            result = 31 * result + password.hashCode();
            return result;
        }
    }

    private Map<CacheKey, JMXProxyConnection> cache
            = new HashMap<CacheKey, JMXProxyConnection>();

    /**
     * Registers the factory to wrap and initializes connection cache.
     *
     * @param wrappedFactory The factory to add caching to.
     */
    public CachingProxyConnectionFactory(
            JMXProxyConnectionFactory wrappedFactory) {
        this.wrappedFactory = wrappedFactory;
    }

    /**
     * If (server,port,userName) has been seen before, looks up the cached
     * connection associated with these values. Otherwise passes the request on
     * the the wrapped factory, caching the result for future reuse.
     *
     * @see JMXProxyConnectionFactory#getConnection(String, int, int, String,
     *      String)
     */
    public JMXProxyConnection getConnection(String server, int port,
                                            int rmiPort,
                                            String userName, String password) {
        ArgumentNotValid.checkNotNullOrEmpty(server, "server");
        ArgumentNotValid.checkNotNullOrEmpty(userName, "userName");
        ArgumentNotValid.checkNotNullOrEmpty(password, "password");
        CacheKey key = new CacheKey(server, port, rmiPort, userName, password);
        if (cache.containsKey(key)) {
            JMXProxyConnection jmxProxyConnection = cache.get(key);
            if (jmxProxyConnection != null && jmxProxyConnection.isLive()) {
                return jmxProxyConnection;
            }
        }
        JMXProxyConnection newConnection = wrappedFactory.getConnection(
                server, port, rmiPort, userName, password);
        cache.put(key, newConnection);
        return newConnection;
    }

}
