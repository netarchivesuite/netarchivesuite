/* $Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

package dk.netarkivet.deploy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.webinterface.HTMLUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates the contents for the monitor_settings.xml, containing a list of
 * hostnames, and a list of JMX-ports associated with each hostname.
 * e.g.:
 * TODO perhaps include the JmxUsername ('monitorRole') in outputXML,
 * TODO perhaps include location information the the outputXML
 * <?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<settings xmlns=\"http://www.netarkivet.dk/schemas/settings\" "
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSc hema-instance\">\n
 * <monitor>
    <jmxMonitorRolePassword>DetErIkkeVoresSkyld</jmxMonitorRolePassword>
    <numberOfHosts>13</numberOfHosts>
    <host1>
         <name>kb-prod-adm-001.kb.dk</name>
         <jmxport>8100</jmxport>
         <jmxport>8101</jmxport>
    </host1> ..

    <host13>
         <name>kb-prod-adm-001.kb.dk</name>
         <jmxport>8100</jmxport>
         <jmxport>8101</jmxport>
    </host13>
    </monitor>
 */
public class JmxHostsDeploymentBuilder implements DeploymentBuilder {
    
    private static Log log = LogFactory.getLog(JmxHostsDeploymentBuilder.class.getName());
    /** XML header for the monitor_settings.xml file. */
    private static final String MONITOR_SETTTINGS_HEADER =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<settings "
            + "xmlns=\"http://www.netarkivet.dk/schemas/monitor_settings\" "
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n";
    /** XML footer for the monitor_settings.xml file. */
    private static final String MONITOR_SETTTINGS_FOOTER = "</settings>\n";
    
    /** This contains the end result of this builder. */
    private final StringBuilder result;
    /** The general JMX password for the deployment. */
    private String jmxPassword;

    /** a map containing a list for each location of hostnames having jmxports. */
    private final Map<String, Map<String,List<Integer>>> locationServerMap =
        new HashMap<String, Map<String,List<Integer>>>();
    
    /** The location of the current host. */
    private String currentLocation;

    /**
     * Register the factory used for establishing connections to remote servers.
     * @param result the StringBuilder to write to
     */
    public JmxHostsDeploymentBuilder(StringBuilder result) {
        ArgumentNotValid.checkNotNull(result, "StringBuilder result");
        this.result = result;
    }

    /**
     * Configures the common password (for the 'monitorRole' user)
     * to use for establishing connections to all remote applications.
     * @param password The given JMX password
     */
    public void setJmxPassword(String password) {
        ArgumentNotValid.checkNotNullOrEmpty(password, "String password");
        this.jmxPassword = password;
    }

    /**
     * Returns an object that registers information about the JMXport
     * and RMIPort for each remote application
     * on the given host.
     * @return an HostBuilder object that registers information
     * about the JMXport and RMIPort
     * for each remote application on the given host.
     */
    public HostBuilder newHostBuilder() {
        return new HostBuilder() {
            private String server;
            public void setName(String serverName) {
                server = serverName;
                Map<String, List<Integer>> serversOnLocation;
                if (locationServerMap.containsKey(currentLocation)) {
                    serversOnLocation = locationServerMap.get(currentLocation);
                } else {
                    serversOnLocation = new HashMap<String, List<Integer>>();
                }                     
                locationServerMap.put(currentLocation, serversOnLocation);
           }
            
            public void addJmxPort(int jmxPortNo) {
                ArgumentNotValid.checkPositive(jmxPortNo, "int jmxPortNo");
                List<Integer> entriesForServer;
                Map<String, List<Integer>> serversOnLocation =
                    locationServerMap.get(currentLocation);
                if (!serversOnLocation.containsKey(server)) {
                    entriesForServer = serversOnLocation.put(server,
                            new ArrayList<Integer>());
                }
                entriesForServer = serversOnLocation.get(server);
                entriesForServer.add(new Integer(jmxPortNo));
                serversOnLocation.put(server, entriesForServer);
                
                // Update the locationServerMap
                locationServerMap.put(currentLocation, serversOnLocation);
            }

            /**
             * Nothing needs to be done here! The action is postponed to
             * JmxProxyDeploymentBuilder.done().
             */
            public void done() {
            }
        };
    }

    /**
     * Remembers the current locations, but currently does not use it.
     * @see dk.netarkivet.deploy.DeploymentBuilder#setLocation(java.lang.String)
     */
    public void setLocation(String location) {
        ArgumentNotValid.checkNotNullOrEmpty(location, "String location");
        this.currentLocation = location;
    }

    /**
     * Done collection information, generate resulting XML.
     * @see dk.netarkivet.deploy.DeploymentBuilder#done()
     */
    public void done() {
        // We have now finished collecting the information.
        // Now the real work begins.
        // Check, that the JMX password is set. We need it now.
        ArgumentNotValid.checkNotNullOrEmpty(jmxPassword,
                "String jmxPassword");
        result.append(MONITOR_SETTTINGS_HEADER);
        result.append("<monitor>\n");
        result.append("<jmxMonitorRolePassword>");
        result.append(HTMLUtils.escapeHtmlValues(jmxPassword));
        result.append("</jmxMonitorRolePassword>\n");
        result.append("<numberOfHosts>");
        int hosts = 0;
        for (String locationName: locationServerMap.keySet()){
            hosts = hosts + locationServerMap.get(locationName).keySet().size();
        }
        result.append(hosts);
        result.append("</numberOfHosts>\n");

        // No need for now to distinguish between hosts from different locations,
        // so location information is presently discarded in this builder.
        
        int i = 0;
        for (String locationName: locationServerMap.keySet()) {
            log.trace("Writing the servers on location '"
                    + locationName + "'.");
            Map<String, List<Integer>> serversOnLocation
                    = locationServerMap.get(locationName);
            
            log.trace("Writing " + serversOnLocation.keySet().size() 
                    + " servers on location '" + locationName + "'.");
            for (String hostName: serversOnLocation.keySet()) {
                List<Integer> jmxPorts = serversOnLocation.get(hostName);
                if (jmxPorts.size() > 0){
                    i++;
                    result.append("<host" + i + ">\n");
                    result.append("<name>");
                    result.append(HTMLUtils.escapeHtmlValues(hostName));
                    result.append("</name>\n");

                    for (Integer jmxPort : jmxPorts) {
                        result.append("<jmxport>");
                        result.append(jmxPort.longValue());
                        result.append("</jmxport>\n");
                    }
                    result.append("</host" + i + ">\n");
                }
            }
        }
        result.append("</monitor>\n");
        result.append(MONITOR_SETTTINGS_FOOTER);
    }
}
