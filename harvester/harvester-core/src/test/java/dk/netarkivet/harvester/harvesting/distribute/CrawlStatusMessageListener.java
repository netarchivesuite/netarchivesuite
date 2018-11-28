/*
 * #%L
 * Netarchivesuite - harvester - test
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package dk.netarkivet.harvester.harvesting.distribute;

import java.util.ArrayList;

import javax.jms.Message;
import javax.jms.MessageListener;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.NetarkivetMessage;
import dk.netarkivet.harvester.datamodel.JobStatus;

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
            jobids.add(Long.valueOf(csm.getJobID()));
            messages.add(csm);
            synchronized (this) {
                notifyAll();
            }
        }
    }
}
