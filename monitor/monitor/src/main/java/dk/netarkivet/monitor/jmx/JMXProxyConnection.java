package dk.netarkivet.monitor.jmx;

import javax.management.ObjectName;
import java.util.Set;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * JMX interface for connection objects that can be used for accessing
 * MBeans on remote servers. Connection method and policies are
 * implementation-dependent.
 */
public interface JMXProxyConnection {
    /**
     * Method to create a proxy to a given MBean on some remote server.
     * Example use:
     *
     * SingleLogRecord logMsg = (SingleLogRecord)
     *     myJMXProxyFactory.createProxy(myObjectName,SingleLogRecord.class);
     *
     * @param name The name of an MBean on some remote server.
     * @param intf The interface that the returned proxy should implement.
     * @param <T>
     * @return an object implementing T. This object forwards all method calls
     * to the named MBean.
     */
    <T> T createProxy(ObjectName name, Class<T> intf);
    /**
     * Get the set of ObjectNames from the remote MBeanserver, that matches
     * the given query.
     * @param query the given query
     * @return the set of ObjectNames, that matches the given query.
     * @throws IOFailure on communication trouble.
     * @throws ArgumentNotValid on null or empty query.
     */
    Set<ObjectName> query(String query);

    /** Returns true if this object still can return usable proxies.
     *
     * @return True if we can return usable proxies.  Otherwise, somebody
     * may have to make a new instance of JMXProxyFactory to get new proxies.
     */
    boolean isLive();
}
