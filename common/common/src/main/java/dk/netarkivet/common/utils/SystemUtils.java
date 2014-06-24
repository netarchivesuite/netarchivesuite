package dk.netarkivet.common.utils;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.IOFailure;

/** Miscellanous utilities for getting system resources. */
public class SystemUtils {
    /** Hostname for this machine used when no name can be found, or when the
     * actual name doesn't matter. */
    public static final String LOCALHOST = "localhost";
    /** Name of standard Java property containing class path.  Why these names
     * aren't actually defined as constants anywhere eludes me. */
    private static final String CLASS_PATH_PROPERTY = "java.class.path";
    
    private static final Log log = LogFactory.getLog(SystemUtils.class);

    /**
     * Looks up the IP number of the local host.
     * Note that Java does not guarantee that the result is
     * IPv4 or IPv6.
     * @return the found IP; returns "UNKNOWNIP" if it could not
     * be found.
     */
    public static String getLocalIP() {
        String result;
        try {
            result = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            result = "UNKNOWNIP";
        }
        return result;
    }

    /**
     * Get the first hostname available for this machine, or "localhost" if none
     * are available.
     *
     * @return A hostname, as returned by
     * InetAddress.getLocalHost().getCanonicalHostName()()
     */
    public static String getLocalHostName() {
        String hostname = LOCALHOST;
        try {
        	InetAddress localhost = InetAddress.getLocalHost();
        	String localhostName = localhost.getCanonicalHostName();
        	String localhostIp = localhost.getHostAddress();
        	if (log.isTraceEnabled()) {
        	    log.trace("[getLocalHostName] Resolved: " + localhostName + " (" + localhostIp + ")");
        	}
        	return localhostName;
        } catch (UnknownHostException e) {
            // If no interfaces, use default;
            log.warn("Unable to resolve localhostname. Returning the default '" + LOCALHOST + "'");
        }
        return hostname;
    }

    /** Check that a given port is not in use.  If this method returns
     * normally, the port is safe to bind.
     *
     * @param port Port to check
     * @throws IOFailure if the port cannot be bound.
     */
    public static void checkPortNotUsed(int port) {
        try {
            ServerSocket s = new ServerSocket(port, 1);
            s.close();
        } catch (BindException e) {
            throw new IOFailure("Port " + port + " already in use, or "
                                + "port is out of range", e);
        } catch (IOException e) {
            throw new IOFailure("IO error testing port " + port, e);
        }
    }

    /** Get the current class path entries.  Note that this does not work
     * if we've been invoked with java -jar, as that option silently ignores
     * classpaths.
     *
     * @return List of directories/jar files in the current class path.
     */
    public static List<String> getCurrentClasspath() {
        String propertyValue = System.getProperty(CLASS_PATH_PROPERTY);
        if (propertyValue != null) {
            final String[] pathArray = propertyValue.split(File.pathSeparator);
            return new ArrayList<String>(Arrays.asList(pathArray));
        } else {
            return new ArrayList<String>();
        }
    }
}
