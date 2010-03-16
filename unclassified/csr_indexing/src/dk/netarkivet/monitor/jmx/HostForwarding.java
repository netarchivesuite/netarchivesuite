/* File:        $Id$
* Revision:    $Revision$
* Author:      $Author$
* Date:        $Date$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/

package dk.netarkivet.monitor.jmx;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.distribute.monitorregistry.HostEntry;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.management.SingleMBeanObject;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.monitor.MonitorSettings;
import dk.netarkivet.monitor.registry.MonitorRegistry;

/**
 * Handles the forwarding of other hosts' MBeans matching a specific regular
 * query and interface to a given mbean server. The interface should be of type
 * T.
 *
 * @param <T>
 */
public class HostForwarding<T> {

    /** The log. */
    public static final Log log = LogFactory.getLog(
            HostForwarding.class);

    /** List of all known and established JMX connections for this object. */
    private Map<String, Set<HostEntry>> knownJmxConnections =
            new HashMap<String, Set<HostEntry>>();

    /** The query to the MBeanserver to get the MBeans. */
    public final String mBeanQuery;
    /** The MBean server we register the forwarded mbeans in. */
    private final MBeanServer mBeanServer;
    /** The interface the remote mbeans should implement. */
    private final Class<T> asInterface;

    /**
     * The username for JMX read from either a System property, the overriding
     * settings given by the installer, or the default value stored in
     * src/dk/netarkivet/monitor/settings.xml.
     */
    private String jmxUsername;

    /**
     * The password for JMX read from either a System property, the overriding
     * settings given by the installer, or the default value stored in
     * src/dk/netarkivet/monitor/settings.xml.
     */
    private String jmxPassword;

    /**
     * The instances of host forwardings, to ensure mbeans are only forwarded
     * once.
     */
    private static Map<String, HostForwarding> instances
            = new HashMap<String, HostForwarding>();

    /** The factory used for producing connections to remote mbean servers. */
    private final JMXProxyConnectionFactory connectionFactory;

    /**
     * Initialise forwarding MBeans. This will connect to all hosts mentioned in
     * settings, and register proxy beans for each bean on remote servers
     * matching the given query. The remote beans should implement the given
     * interface.
     *
     * @param asInterface The interface remote beans should implement.
     * @param mBeanServer The MBean server the proxy beans should be registered
     *                    in.
     * @param mBeanQuery  The query that returns the mbeans that should be
     *                    proxied.
     */
    private HostForwarding(Class<T> asInterface, MBeanServer mBeanServer,
                           String mBeanQuery) {
        this.mBeanServer = mBeanServer;
        this.asInterface = asInterface;
        this.mBeanQuery = mBeanQuery;
        this.connectionFactory = new CachingProxyConnectionFactory(
                new RmiProxyConnectionFactory());

        updateJmx();
    }

    /**
     * Get a host forwarding instance. As a side effect of this, all mbeans
     * matching a query from remote hosts, are proxied and registered in the
     * given mbean server. Only one HostForwarding instance will be made for
     * each query string. Any subsequent call with the same query string will
     * simply return the previously initiated instance.
     *
     * @param asInterface The interface remote mbeans should implement.
     * @param mBeanServer The MBean server to register proxy mbeans in.
     * @param query       The query for which we should proxy matching mbeans on
     *                    remote servers.
     * @param <T>         The type of HostForwarding to return.
     *
     * @return This host forwarding instance.
     */
    public static synchronized <T> HostForwarding getInstance(
            Class<T> asInterface, MBeanServer mBeanServer, String query) {

        if (instances.get(query) == null) {
            instances.put(query, new HostForwarding<T>(asInterface, mBeanServer,
                                                       query));
        }
        HostForwarding hf = instances.get(query);
        hf.updateJmx();
        return hf;
    }

    /**
     * Gets the list of hosts and corresponding JMX ports from the monitor
     * registry. For all unknown hosts, it registers proxies to all Mbeans
     * registered on the remote MBeanservers in the given MBeanserver.
     */
    private synchronized void updateJmx() {
        // Update username/password setting, if either of the settings
        // MonitorSettings.JMX_PASSWORD_SETTING,
        // MonitorSettings.JMX_USERNAME_SETTING have changed.

        boolean changed = updateJmxUsernameAndPassword();
        if (changed) {
            log.info("Settings '"
                     + MonitorSettings.JMX_USERNAME_SETTING
                     + "' and '" + MonitorSettings.JMX_PASSWORD_SETTING +
                     "' has been updated with value from a System property "
                     + "or one of the files: "
                     + StringUtils.conjoin(",", Settings.getSettingsFiles()));
        }

        List<HostEntry> newJmxHosts = new ArrayList<HostEntry>();
        for (Map.Entry<String, Set<HostEntry>> entries
                : getCurrentHostEntries().entrySet()) {
            String host = entries.getKey();
            // Take a copy of the host entries, to avoid concurrent
            // modifications.
            Set<HostEntry> hostEntries
                    = new HashSet<HostEntry>(entries.getValue());
            if (knownJmxConnections.containsKey(host)) {
                Set<HostEntry> registeredJmxPortsOnHost
                        = knownJmxConnections.get(host);
                for (HostEntry he : hostEntries) {
                    if (!registeredJmxPortsOnHost.contains(he)) {
                        log.debug("Adding new jmx host '" + he + "'");
                        newJmxHosts.add(he);
                        registeredJmxPortsOnHost.add(he);
                    } else {
                        log.trace("Updating last seen time for jmx host '"
                                  + he + "'");
                        for (HostEntry existing : registeredJmxPortsOnHost) {
                            if (existing.equals(he)) {
                                existing.setTime(he.getTime());
                            }
                        }
                    }
                }
                knownJmxConnections.put(host,
                                        registeredJmxPortsOnHost);
            } else {
                log.debug("Adding new jmx hosts '" + hostEntries + "'");
                newJmxHosts.addAll(hostEntries);
                knownJmxConnections.put(
                        host, new HashSet<HostEntry>(hostEntries));
            }
        }
        if (newJmxHosts.size() > 0) {
            log.info("Found " + newJmxHosts.size() + " new JMX hosts");
            registerRemoteMbeans(newJmxHosts);
        }
    }

    /**
     * Update JMX username and password.
     *
     * @return true if the username and/or the password were changed.
     */
    private boolean updateJmxUsernameAndPassword() {
        boolean changed = false;
        String newJmxUsername = Settings.get(
                MonitorSettings.JMX_USERNAME_SETTING);

        String newJmxPassword = Settings.get(
                MonitorSettings.JMX_PASSWORD_SETTING);

        if (jmxUsername == null || !jmxUsername.equals(newJmxUsername)) {
            jmxUsername = newJmxUsername;
            changed = true;
        }

        if (jmxPassword == null || !jmxPassword.equals(newJmxPassword)) {
            jmxPassword = newJmxPassword;
            changed = true;
        }
        return changed;
    }

    /**
     * Get current list of host-JMX port mappings. This lists the mappings from
     * the registry server.
     *
     * @return current list of host-JMX port mappings.
     */
    public static Map<String, Set<HostEntry>> getCurrentHostEntries() {
        return MonitorRegistry.getInstance().getHostEntries();
    }

    /**
     * Register all remote Mbeans on the given MBeanServer. The username, and
     * password are the same for all JMX-connections. For hosts which cannot be
     * connected to, an mbean is registered in the same domain, which tries to
     * reconnect on any invocation, and returns the status of the attempt as a
     * string.
     *
     * @param hosts the list of remote Hosts.
     */
    private void registerRemoteMbeans(List<HostEntry> hosts) {
        for (HostEntry hostEntry : hosts) {
            log.debug("Forwarding mbeans '" + this.mBeanQuery + "' for host: "
                      + hostEntry);
            try {
                createProxyMBeansForHost(hostEntry);
            } catch (Exception e) {
                log.warn("Failure connecting to remote JMX MBeanserver ("
                         + hostEntry + ")", e);
                try {
                    // This creates a proxy object that calls the handler on any
                    // invocation of any method on the object.
                    NoHostInvocationHandler handler
                            = new NoHostInvocationHandler(hostEntry);
                    Class<T> proxyClass = (Class<T>) Proxy.getProxyClass(
                            asInterface.getClassLoader(),
                            new Class[]{asInterface});
                    T noHostMBean = proxyClass.getConstructor(
                            InvocationHandler.class).newInstance(handler);
                    SingleMBeanObject<T> singleMBeanObject
                            = new SingleMBeanObject<T>(
                            queryToDomain(mBeanQuery),
                            noHostMBean, asInterface, mBeanServer);
                    Hashtable<String, String> names = singleMBeanObject
                            .getNameProperties();
                    names.put("name",
                              "error_host_" + hostEntry.getName()
                              + "_" + hostEntry.getJmxPort());
                    names.put("index", Integer.toString(0));
                    names.put("hostname", hostEntry.getName());
                    handler.setSingleMBeanObject(singleMBeanObject);
                    singleMBeanObject.register();
                } catch (Exception e1) {
                    log.warn("Failure registering error mbean for hostentry: "
                             + hostEntry, e1);
                }
            }
        }
    }

    /**
     * Connects to the given host, and lists all mbeans matching the query. For
     * each of these mbeans, registers a proxymbean, that on any invocation will
     * connect to the remote host, and return the result of invoking the method
     * on the remote object.
     *
     * @param hostEntry The host to connect to.
     *
     * @throws IOFailure if remote host cannot be connected to.
     */
    private synchronized void createProxyMBeansForHost(
            HostEntry hostEntry) {
        Set<ObjectName> remoteObjectNames;
        JMXProxyConnection connection = connectionFactory.getConnection(
                hostEntry.getName(),
                hostEntry.getJmxPort(),
                hostEntry.getRmiPort(),
                jmxUsername, jmxPassword);

        remoteObjectNames = connection.query(mBeanQuery);
        for (ObjectName name : remoteObjectNames) {
            try {
                // This creates a proxy object that calls the handler on any
                // invocation of any method on the object.
                ProxyMBeanInvocationHandler handler
                        = new ProxyMBeanInvocationHandler(
                        name, hostEntry);
                Class<T> proxyClass = (Class<T>) Proxy.getProxyClass(
                        asInterface.getClassLoader(),
                        new Class[]{asInterface});
                T mbean = proxyClass.getConstructor(
                        InvocationHandler.class).newInstance(handler);

                SingleMBeanObject<T> singleMBeanObject
                        = new SingleMBeanObject<T>(name, mbean, asInterface,
                                                   mBeanServer);
                singleMBeanObject.register();
            } catch (Exception e) {
                log.warn("Error registering mbean", e);
            }
        }
    }

    /**
     * Returns the domain from a given query. Used for constructing an
     * error-mbean-name on connection trouble.
     *
     * @param mBeanQuery The query to return the domain from.
     *
     * @return the domain from a given query.
     */
    private String queryToDomain(String mBeanQuery) {
        return mBeanQuery.replaceAll(":.*$", "");
    }

    /**
     * Get the mbean server that proxies to remote mbeans are registered in.
     *
     * @return The mbean server with proxy mbeans.
     */
    public MBeanServer getMBeanServer() {
        return mBeanServer;
    }

    /**
     * An invocation handler for the mbeans registered when a host does not
     * respond. This handler will on any invocation attempt to reconnect, and
     * then return a string with the result. Unsuccessfully connecting, it will
     * unregister the mbean.
     */
    private class NoHostInvocationHandler implements InvocationHandler {
        /** The mbean this invocation handler handles. */
        private SingleMBeanObject singleMBeanObject;
        /** The host we should retry connecting to. */
        private HostEntry hostEntry;

        /**
         * Make a new invocation handler for showing errors and retrying
         * connect.
         *
         * @param hostEntry The host to retry connecting to.
         */
        public NoHostInvocationHandler(HostEntry hostEntry) {
            this.hostEntry = hostEntry;
        }

        /**
         * Remembers the mbean this invocation handler is registered in. Should
         * always be called before actually registering the mbean.
         *
         * @param singleMBeanObject The mbean this object handles.
         */
        public void setSingleMBeanObject(SingleMBeanObject singleMBeanObject) {
            this.singleMBeanObject = singleMBeanObject;
        }

        /**
         * Retries connecting to the host. On success, returns a string with
         * success, and unregisters. On failure, returns a string with failure.
         *
         * @param proxy  The error mbean that invoked this, ignored.
         * @param method The method attempted invoked, ignored.
         * @param args   The arguments for the method, ignored.
         *
         * @return A string with success or failure.
         *
         * @throws Throwable Shouldn't throw exceptions.
         */
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            try {
                createProxyMBeansForHost(hostEntry);
                singleMBeanObject.unregister();
                return "Now proxying JMX beans for host '"
                       + hostEntry.getName() + ":" + hostEntry.getJmxPort()
                       + "'";
            } catch (Exception e) {
                //Still unable to connect. Oh well.
                return "Unable to proxy JMX beans on host '"
                       + hostEntry.getName() + ":" + hostEntry.getJmxPort()
                       + "', last seen active at '"
                       + hostEntry.getTime() + "'\n"
                       + ExceptionUtils.getStackTrace(e);
            }
        }
    }

    /** An invocation handler that forwards invocations to a remote mbean. */
    private class ProxyMBeanInvocationHandler implements InvocationHandler {
        /** The name of the remote mbean. */
        private final ObjectName name;
        /** The host for the remote mbean. */
        private final HostEntry hostEntry;

        /**
         * Make a new forwarding mbean handler.
         *
         * @param name      The name of the remote mbean.
         * @param hostEntry The host for the remote mbean.
         */
        public ProxyMBeanInvocationHandler(
                ObjectName name,
                HostEntry hostEntry) {
            this.name = name;
            this.hostEntry = hostEntry;
        }

        /**
         * Initialises a connection to a remote bean. Then invokes the method on
         * that bean.
         *
         * @param proxy  This proxying object. Ignored.
         * @param method The method invoked. This is called on the remote
         *               mbean.
         * @param args   The arguments to the method. These are given to the
         *               remote mbean.
         *
         * @return Whatever the remote mbean returns.
         *
         * @throws IOFailure On trouble establishing the connection.
         * @throws javax.management.RuntimeMBeanException
         *                   On exceptions in the mbean invocations.
         * @throws Throwable What ever the remote mbean has thrown.
         */
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            // establish or reestablish mbean.
            JMXProxyConnection connection;
            try {
                connection = connectionFactory.getConnection(
                        hostEntry.getName(),
                        hostEntry.getJmxPort(),
                        hostEntry.getRmiPort(),
                        jmxUsername, jmxPassword);
            } catch (Exception e) {
                throw new IOFailure("Could not connect to host '"
                                    + hostEntry.getName() + ":"
                                    + hostEntry.getJmxPort()
                                    + "', last seen active at '"
                                    + hostEntry.getTime() + "'\n", e);
            }
            // call remote method.
            T mBean = connection.createProxy(name, asInterface);
            return method.invoke(mBean, args);
        }
    }
}
