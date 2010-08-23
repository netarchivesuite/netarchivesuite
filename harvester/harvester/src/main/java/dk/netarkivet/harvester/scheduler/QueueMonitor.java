package dk.netarkivet.harvester.scheduler;

import java.util.LinkedList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.QueueBrowser;
import javax.jms.QueueSession;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;

/* File:    $Id: $
* Revision: $Revision: $
* Author:   $Author: $
* Date:     $Date: $
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

/**
 * Provides functionality for monitoring queues
 */
public class QueueMonitor {
    private final ChannelID[] queues2Monitor;
    private JMSConnection jmsConnection;
    
    /**
     * Creates a <code>QueueMonitor</code> instance monitoring the indicated queues
     * @param queues2Monitor The list of queues to monitor
     */
    public QueueMonitor(ChannelID[] queues2Monitor) {
        this.queues2Monitor = queues2Monitor;
        jmsConnection = JMSConnectionFactory.getInstance();
    }
    
    public ChannelID[] listEmptyQueues() throws JMSException {
        List<ChannelID> emptyQueueList = new LinkedList<ChannelID>();
        for(ChannelID queueID:queues2Monitor) {
            QueueBrowser queueSession = jmsConnection.createQueueBrowser(queueID);
        }
        
        return emptyQueueList.toArray(new ChannelID[emptyQueueList.size()]);
    }
}
