/* $Id$
 * $Revision$
 * $Author$
 * $Date$
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
package dk.netarkivet.harvester.harvesting.distribute;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobPriority;


/**
 * Proxy for remote scheduler.
 * Establishes a JMS connection, and gives an interfaces for sending crawl
 * requests.
 */
public class HarvestControllerClient {
    /** A String to write to log when sending a message.     */
    public static final String sendMessage = "Send crawl request: ";

    /** the logger to use.     */
    protected final Log log = LogFactory.getLog(getClass());

    /** Connection to JMS provider. */
    private JMSConnection jmsConnection;

    /**
    * Establish the connection to the server
    * Adds a listener to the clientId-messagequeue
    * Set the client itself as messagehandler
    * This clients can communicate with multiple HarvestControllerServers -
    * as the destination.
    *
    * @throws dk.netarkivet.common.exceptions.IOFailure if no JMS connection
    * could be established
    */
    private HarvestControllerClient() throws IOFailure {
        jmsConnection = JMSConnectionFactory.getInstance();
    }

    /**
     * Construction factory.
     * @throws dk.netarkivet.common.exceptions.IOFailure
     *          If there is a problem making the connection.
     * @return a HarvesterControllerClient instance.
     */
    public static HarvestControllerClient getInstance() throws IOFailure{
        return new HarvestControllerClient();
    }

    /**
     * Submit an doOneCrawl request to a HarvestControllerServer with correct
     * priority.
     * @param job the specific job to send
     * @param metadata pre-harvest metadata to store in arcfile.
     * @throws ArgumentNotValid the job parameter is null
     * @throws IOFailure if unable to send the doOneCrawl request to a
     * harvestControllerServer
     */
    public void doOneCrawl(Job job, List<MetadataEntry> metadata)
            throws ArgumentNotValid, IOFailure {
        ArgumentNotValid.checkNotNull(job, "job");
        ArgumentNotValid.checkNotNull(metadata, "metadata");
        JobPriority p = job.getPriority();
        ChannelID result;
        switch(p) {
            case LOWPRIORITY:
                result = Channels.getAnyLowpriorityHaco();
                break;
            case HIGHPRIORITY:
                result = Channels.getAnyHighpriorityHaco();
                break;
            default:
                throw new UnknownID("Job " + job + " has illegal priority "
                        + p);
        }
        DoOneCrawlMessage nMsg =
                new DoOneCrawlMessage(job, result, metadata);
        log.debug(sendMessage + nMsg);
        jmsConnection.send(nMsg);
    }

    /**
     * Closes client cleanly.
     */
    public void close() {
        jmsConnection = null;
        log.info("Client closing down");
    }
}
