/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.harvester.harvesting.distribute;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.harvester.datamodel.JobStatus;

import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.ArrayList;

/**
 * Utility class to listen to and record all CrawlStatusMessages
 */
public class CrawlStatusMessageListener implements MessageListener {
    public ArrayList<JobStatus> status_codes = new ArrayList<JobStatus>();
    public ArrayList<Long> jobids = new ArrayList<Long>();
    public ArrayList<CrawlStatusMessage> messages = new ArrayList<CrawlStatusMessage>();

    public void onMessage(Message message) {
        NetarkivetMessage naMsg = JMSConnection.unpack(message);
        if (naMsg instanceof CrawlStatusMessage) {
            CrawlStatusMessage csm = (CrawlStatusMessage) naMsg;
            status_codes.add(csm.getStatusCode());
            jobids.add(new Long(csm.getJobID()));
            messages.add(csm);
            synchronized(this) {
                notifyAll();
            }
        }
    }
}
