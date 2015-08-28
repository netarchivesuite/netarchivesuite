package dk.netarkivet.common.distribute.arcrepository.bitrepository;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provide the hostname of the machine on which the program is running.
 */
public class HostName {
    /** Logging mechanism. */
    private static Logger logger = LoggerFactory.getLogger(HostName.class.getName());
    
    /**
     * Default constructor
     */
    private HostName() {
        super();
    }

    /**
     * Get the hostname of the machine.
     * @return the hostname as a {@link String}
     */
    public static String getHostName() {
        String hostName;
        try {
            //Trying to get hostname through InetAddress
            final InetAddress iAddress = InetAddress.getLocalHost();
            hostName = iAddress.getHostName();
            
            //Trying to do better and get Canonical hostname
            
            final String canonicalHostName = iAddress.getCanonicalHostName();         
            hostName = canonicalHostName;
            
            // TODO does the above always work?		

            if (StringUtils.isNotEmpty(hostName)) {
                logger.info("Local hostname (provided  by iAddress): " + hostName);
                return hostName;
            }
            
        } catch (UnknownHostException  e) {
            logger.info("Failed finding hostname the standard Java way, returning: localhost");
        }
	return "localhost";
    }
}
