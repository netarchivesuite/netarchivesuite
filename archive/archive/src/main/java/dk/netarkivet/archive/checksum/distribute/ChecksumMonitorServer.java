/* File:        $Id$
 * Date:        $Date$
 * Revision:    $Revision$
 * Author:      $Author$
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
package dk.netarkivet.archive.checksum.distribute;

import java.util.Observable;
import java.util.Observer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.distribute.ArchiveMessageHandler;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.utils.CleanupIF;

public class ChecksumMonitorServer extends ArchiveMessageHandler 
        implements Observer, CleanupIF {

    /**
     * The current instance. Used to avoid several parallel instances running 
     * at the same time.
     */
    private static ChecksumMonitorServer instance;
    
    /**
     * Logger.
     */
    private static final Log log = LogFactory.getLog(
	    ChecksumMonitorServer.class);

    /**
     * The jms connection used.
     */
    private final JMSConnection con = JMSConnectionFactory.getInstance();

    /**
     * Constructor.
     */
    private ChecksumMonitorServer() {
        con.setListener(Channels.getTheBamon(), this);
    }
    
    /**
     * Method for retrieving the current instance. If no instance is currently
     * running, then a new instance is created.
     * 
     * @return The current running instance.
     */
    public ChecksumMonitorServer getInstance() {
        if (instance == null) {
            instance = new ChecksumMonitorServer();
        }
        return instance;
    }

    @Override
    public void update(Observable o, Object arg) {
        // TODO What should this instance do?
        log.warn("The Observable '" + o + "' and the object '" + arg
                + "' was given as input. ");
    }

    /**
     * Close down this BitarchiveMonitor.
     */
    public void close() {
        log.info("ChecksumMonitorServer closing down.");
        cleanup();
        log.info("ChecksumMonitorServer closed down");
    }

    /**
     * Closes this BitarchiveMonitorServer cleanly.
     */
    public void cleanup() {
        if (instance != null) {
            con.removeListener(Channels.getTheBamon(), this);
            instance = null;
        }
    }
}
