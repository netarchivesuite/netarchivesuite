/* File:    $Id$
 * Revision: $Revision$
 * Author:   $Author$
 * Date:     $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
 * Foundation,dk.netarkivet.harvester.schedulerFloor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.harvester.scheduler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.lifecycle.ComponentLifeCycle;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.datamodel.HarvestChannelDAO;
import dk.netarkivet.harvester.distribute.HarvesterMessageHandler;
import dk.netarkivet.harvester.harvesting.distribute.HarvestChannelValidityRequest;
import dk.netarkivet.harvester.harvesting.distribute.HarvestChannelValidityResponse;
import dk.netarkivet.harvester.harvesting.distribute.HarvesterReadyMessage;

/**
 * Handles the reception of status messages from the harvesters. Will call the 
 * {@link #visit(HarvesterReadyMessage)} method when a Ready message is 
 * received.
 */
public class HarvesterStatusReceiver extends HarvesterMessageHandler
implements ComponentLifeCycle {  
    /** @see HarvesterStatusReceiver#visit(dk.netarkivet.harvester.harvesting.distribute.HarvesterReadyMessage)  */
    private final JobDispatcher jobDispatcher;
    /** Connection to JMS provider. */
    private final JMSConnection jmsConnection; 
    /** The logger to use.    */
    private final Log log = LogFactory.getLog(getClass());   
    
    /**
     * The DAO handling {@link HarvestChannel}s
     */
    private final HarvestChannelDAO harvestChannelDao = HarvestChannelDAO.getInstance();
    
    /**
     * @param jobDispatcher The <code>JobDispatcher</code> to delegate the 
     * dispatching of new jobs to, when a 'Ready for job' event is received.
     * @param jmsConnection The JMS connection by which 
     * {@link HarvesterReadyMessage} is received.
     */
    public HarvesterStatusReceiver(
            JobDispatcher jobDispatcher,
            JMSConnection jmsConnection) {
        ArgumentNotValid.checkNotNull(jobDispatcher, "jobDispatcher");
        ArgumentNotValid.checkNotNull(jmsConnection, "jmsConnection");
        this.jobDispatcher = jobDispatcher;
        this.jmsConnection = jmsConnection;
    }

    @Override
    public void start() {
        jmsConnection.setListener(
                Channels.getHarvesterStatusChannel(), this);
        jmsConnection.setListener(
                Channels.getHarvestChannelValidityRequestChannel(), this);
    }

    @Override
    public void shutdown() {
        jmsConnection.removeListener(
                Channels.getHarvesterStatusChannel(), this);
    }
   
     /** 
      * Tells the dispatcher that it may dispatch a new job.
      * @param message The message containing the relevant harvester information.
      * 
     */
    @Override
    public void visit(HarvesterReadyMessage message) {
        ArgumentNotValid.checkNotNull(message, "message");
        log.trace("Received ready message from " + message.getApplicationInstanceId());        
        HarvestChannel channel = harvestChannelDao.getByName(message.getHarvestChannelName());
        jobDispatcher.submitNextNewJob(channel);
    }
    
    @Override
	public void visit(HarvestChannelValidityRequest msg) {
    	ArgumentNotValid.checkNotNull(msg, "msg");
    	
    	String channelName = msg.getHarvestChannelName();
    	HarvestChannelDAO dao = HarvestChannelDAO.getInstance();
    	
    	boolean isSnapshot = true;
    	boolean isValid = true;
    	try {
    		HarvestChannel chan = dao.getByName(channelName);
    		isSnapshot = chan.isSnapshot();
    	} catch (UnknownID e) {
    		isValid = false;
    	}
    
    	// Send the reply
    	jmsConnection.send(new HarvestChannelValidityResponse(channelName, isValid, isSnapshot));
    	log.info("Sent a message to notify that harvest channel '" + channelName + "' is "
    			+ (isValid ? "valid." :  "invalid."));
    }

}
