/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.monitor.webinterface;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.RuntimeMBeanException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.monitor.jmx.HostForwarding;
import dk.netarkivet.monitor.logging.SingleLogRecord;

/**
 * Implementation of StatusEntry, that receives its data from the 
 * MBeanServer (JMX).
 */
public class JMXStatusEntry implements StatusEntry {
    /** The ObjectName assigned to the MBean for this JMXStatusEntry. */
    private ObjectName mBeanName;
    /** JMX Query to retrieve the logmessage associated with this Entry.*/
    private static final String LOGGING_QUERY
        = "dk.netarkivet.common.logging:*";
    /** JMX Attribute containing the logmessage itself. */
    private static final String JMXLogMessageAttribute = "RecordString";
    /** MBeanserver used by this class. */
    private static final MBeanServer mBeanServer
            = MBeanServerFactory.createMBeanServer();
    
   /** Internationalisation object. */
    private static final I18n I18N
        = new I18n(dk.netarkivet.monitor.Constants.TRANSLATIONS_BUNDLE);
    
    /**
     * Constructor for the JMXStatusEntry. 
     * @param mBeanName The ObjectName to be assigned to the MBean representing 
     * this JMXStatusEntry.
     */
    public JMXStatusEntry(ObjectName mBeanName) {
        ArgumentNotValid.checkNotNull(mBeanName, "ObjectName mBeanName");
        this.mBeanName = mBeanName;
    }
    
    /**
     * @return the location designated by the key {@link JMXSummaryUtils#JMXLocationProperty}
     */
    public String getLocation() {
        return mBeanName.getKeyProperty(JMXSummaryUtils.JMXLocationProperty);
    }

    /**
     * @return the hostname designated by the key {@link JMXSummaryUtils#JMXHostnameProperty}
     */
    public String getHostName() {
        return mBeanName.getKeyProperty(JMXSummaryUtils.JMXHostnameProperty);
    }
    
    /**
     * @return the http-port designated by the key {@link JMXSummaryUtils#JMXHttpportProperty}
     */
    public String getHTTPPort() {
        return mBeanName.getKeyProperty(JMXSummaryUtils.JMXHttpportProperty);
    }

    /**
     * @return the application name designated by the key {@link JMXSummaryUtils#JMXApplicationnameProperty}
     */
    public String getApplicationName() {
        return mBeanName.getKeyProperty(JMXSummaryUtils.JMXApplicationnameProperty);
    }

    /**
     * @return the index designated by the key {@link JMXSummaryUtils#JMXIndexProperty}
     */
    public String getIndex() {
        return mBeanName.getKeyProperty(JMXSummaryUtils.JMXIndexProperty);
    }

    /** Gets the log message from this status entry.  This implementation
     * actually talks to an MBeanServer to get the log message.  Will return an
     * explanation if remote host does not respond, throws exception or returns
     * null.
     * @param l the current Locale
     * @throws ArgumentNotValid if the current Locale is null
     * @return A log message.
     */
    public String getLogMessage(Locale l) {
        ArgumentNotValid.checkNotNull(l, "l");
        // Make sure mbeans are forwarded
        HostForwarding.getInstance(SingleLogRecord.class,
                                   mBeanServer,
                                   LOGGING_QUERY);
        try {
            String logMessage = (String)
                    mBeanServer.getAttribute(mBeanName, JMXLogMessageAttribute);
            if (logMessage == null) {
                return HTMLUtils.escapeHtmlValues(
                        I18N.getString(l,
                                       "errormsg;remote.host.returned.null.log.record"));
            } else {
                return logMessage;
            }
        } catch (RuntimeMBeanException e) {
            return HTMLUtils.escapeHtmlValues(
                    I18N.getString(l,
                                   "errormsg;jmx.error.while.getting.log.recordrd")
                    + "\n" 
                    + I18N.getString(l,
                                     "errormsg;probably.host.is.not.respondingng")
                    + "\n" 
                    + ExceptionUtils.getStackTrace(e));
        } catch (Exception e) {
            return HTMLUtils.escapeHtmlValues(
                    I18N.getString(l,
                                   "errormsg;remote.jmx.bean.generated.exceptionon")
                    + "\n" + ExceptionUtils.getStackTrace(e));
        }
    }

    /** Compares two entries according to first their location, then their
     * machine name, then their ports, and then
     * their application name, and then their index.
     * @param o The object to compare with
     * @return A negative number if this entry comes first, a positive if it
     * comes second and 0 if they are equal.
     */
    public int compareTo(StatusEntry o) {
        int c;

        if (getLocation() != null && o.getLocation() != null) {
            c = getLocation().compareTo(o.getLocation());
            if (c != 0) {
                return c;
            }
        } else if (getLocation() == null) {
            return -1;
        } else {
            return 1;
        }

        if (getHostName() != null && o.getHostName() != null) {
            c = getHostName().compareTo(o.getHostName());
            if (c != 0) {
                return c;
            }
        } else if (getHostName() == null) {
            return -1;
        } else {
            return 1;
        }

        if (getHTTPPort() != null && o.getHTTPPort() != null) {
            c = getHTTPPort().compareTo(o.getHTTPPort());
            if (c != 0) {
                return c;
            }
        } else if (getHTTPPort() == null) {
            return -1;
        } else {
            return 1;
        }

        if (getApplicationName() != null && o.getApplicationName() != null) {
            c = getApplicationName().compareTo(o.getApplicationName());
            if (c != 0) {
                return c;
            }
        } else if (getApplicationName() == null) {
            return -1;
        } else {
            return 1;
        }

        Integer i1;
        Integer i2;
        try {
            i1 = Integer.valueOf(getIndex());
        } catch (NumberFormatException e) {
            i1 = null;
        }
        try {
            i2 = Integer.valueOf(o.getIndex());
        } catch (NumberFormatException e) {
            i2 = null;
        }

        if (i1 != null && i2 != null) {
            c = i1.compareTo(i2);
            if (c != 0) {
                return c;
            }
        } else if (i1 == null) {
            return -1;
        } else {
            return 1;
        }

        return 0;
    }

    /** Query the JMX system for system status mbeans.
     *
     * @param query A JMX request, e.g.
     * dk.netarkivet.logging:location=KB,httpport=8080,*
     * @return A list of status entries for the mbeans that match the query.
     * @throws MalformedObjectNameException If the query has wrong format.
     */
    public static List<StatusEntry> queryJMX(String query)
            throws MalformedObjectNameException {
        // Make sure mbeans are forwarded
        HostForwarding.getInstance(SingleLogRecord.class,
                                   mBeanServer,
                                   LOGGING_QUERY);
        // The "null" in this case is used to indicate no further filters on the
        // query.
        Set<ObjectName> resultSet = (Set<ObjectName>) mBeanServer.queryNames
                (new ObjectName(query), null);
        List<StatusEntry> entries = new ArrayList<StatusEntry>();
        for (ObjectName objectName : resultSet) {
            entries.add(new JMXStatusEntry(objectName));
        }
        Collections.sort(entries);
        return entries;
    }
}
