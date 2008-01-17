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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * A Sax parser specialized to parsing it-config.xml.
 * This parser translates Sax events into method calls
 * on a given builder object.
 *
 * In its first versions, only handles tags relevant
 * for the JMX agent.
 * Note 1: We only expect to find the JMX-portnr in it-config.
 * The RMI port.
 */
public class ItConfigParser extends DefaultHandler {
    /** The builder used to process the host information
     * contained in the config-file.
     */
    private DeploymentBuilder theBuilder;
    /**
     * Buffer used to collect character values.
     */
    private StringBuilder sb = new StringBuilder();

    /** the current location. */
    private String currentLocation;

    /** The object responsible for handling the JMX connections on
     * a specific host. */
    private DeploymentBuilder.HostBuilder theCurrentHostBuilder;

    /**
     * Registers the builder to be used for handling events.
     * @param builder The builder object that this parser directs.
     */
    public ItConfigParser(DeploymentBuilder builder) {
        ArgumentNotValid.checkNotNull(builder, "DeploymentBuilder builder");
        this.theBuilder = builder;
    }

    /**
     * If tag is "host", gets a new Host from the internal builder.
     * Always resets the buffer.
     * if tag is "location", sets the currentLocation to the qname arg.
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
     * java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(String uri, String name, String qname,
                             Attributes atts) {
        sb = new StringBuilder();
        if (qname.equalsIgnoreCase("location")) {
            if (currentLocation == null) {
                currentLocation = atts.getValue("name"); // reset current location
                theBuilder.setLocation(currentLocation);
            } else {
                throw new ArgumentNotValid("Inconsistency:"
                                           + "location already defined as: "
                                           + currentLocation);
            }
        } else if (qname.equalsIgnoreCase("host")) {
            theCurrentHostBuilder = theBuilder.newHostBuilder();
            String hostname = atts.getValue("name");
            theCurrentHostBuilder.setName(hostname);
        }
    }

    /**
     * If tag is location, reset currentLocation.
     * If tag is "host", calls done() on the current Host object.
     * If tag is "jmxport", parses cached characters as an integer
     * and adds the port number to the current Host.
     * If tag is "name", sets the name of the current Host to the
     * cached characters.
     * If tag is jmxMonitorRolePassword, call the setJmxPassword()
     * on the DeploymentBuilder.
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    public void endElement(String uri, String name, String qname) {
        ArgumentNotValid.checkNotNullOrEmpty(qname, "String qname");
        if (qname.equalsIgnoreCase("location")) {
            currentLocation = null; // reset current location
        } else if (qname.equalsIgnoreCase("host")) {
            theCurrentHostBuilder.done();
        } else if (qname.equalsIgnoreCase("jmxport")) {
            try {
                int jmxport = Integer.parseInt(sb.toString());
                theCurrentHostBuilder.addJmxPort(jmxport);
            } catch (NumberFormatException e) {
                throw new IOFailure("Unable to parse value of jmxport as an integer: "
                        +  sb.toString());
            }
        } else if (qname.equalsIgnoreCase("jmxMonitorRolePassword")) {
            theBuilder.setJmxPassword(sb.toString().trim());
        } else {
          // Do nothing
        }
    }

    /**
     * Caches the characters for use in endElement().
     * @param ch incoming array of characters
     * @param start index of 1st character
     * @param length total number of characters to append
     * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
     * @throws SAXException
     *
     */
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        ArgumentNotValid.checkNotNull(ch, "char[] ch");
        ArgumentNotValid.checkNotNegative(start, "int start");
        ArgumentNotValid.checkNotNegative(length, "int length");
        sb.append(ch, start, length);
    }
}
