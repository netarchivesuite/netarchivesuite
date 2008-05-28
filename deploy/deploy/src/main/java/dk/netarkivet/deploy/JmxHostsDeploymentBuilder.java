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

import java.io.ByteArrayOutputStream;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.StreamUtils;

/**
 * Generates the contents for the monitor_settings.xml, containing a list of
 * hostnames, and a list of JMX-ports associated with each hostname.
 * e.g.:
 * <?xml version="1.0" encoding="UTF-8"?>
 * <settings xmlns="http://www.netarkivet.dk/schemas/monitor_settings">
 *     <monitor>
 *         <jmxMonitorRolePassword>test</jmxMonitorRolePassword>
 *     </monitor>
 * </settings>
 * TODO perhaps include the JmxUsername ('monitorRole') in outputXML,
 */
public class JmxHostsDeploymentBuilder implements DeploymentBuilder {

    /** This contains the end result of this builder. */
    private final StringBuilder result;
    /** The general JMX password for the deployment. */
    private String jmxPassword;
    /** The namespace used in the generated XML document. */
    private static final String MONITOR_SETTINGS_NAMESPACE
            = "http://www.netarkivet.dk/schemas/monitor_settings";

    /**
     * Initialise the builder with the stringbuilder to generate result to.
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
        jmxPassword = password;
    }

    /**
     * Currently the host information is not used by this builder, so just
     * return a hostbuilder that does nothing.
     * @return an HostBuilder object that ignores information about a host.
     */
    public HostBuilder newHostBuilder() {
        return new HostBuilder() {
            public void setName(String serverName) {}
            public void addJmxPort(int jmxPortNo) {}
            public void done() {}
        };
    }

    /**
     * Does nothing, location is not used.
     * @see DeploymentBuilder#setLocation(String)
     */
    public void setLocation(String location) {}

    /**
     * Done collection information, generate resulting XML.
     * @see DeploymentBuilder#done()
     */
    public void done() {
        Document d = DocumentFactory.getInstance().createDocument();
        d.addElement("settings", MONITOR_SETTINGS_NAMESPACE).addElement(
                "monitor", MONITOR_SETTINGS_NAMESPACE).addElement(
                "jmxMonitorRolePassword", MONITOR_SETTINGS_NAMESPACE).setText(
                jmxPassword);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamUtils.writeXmlToStream(d, baos);
        result.append(baos.toString());

    }
}
