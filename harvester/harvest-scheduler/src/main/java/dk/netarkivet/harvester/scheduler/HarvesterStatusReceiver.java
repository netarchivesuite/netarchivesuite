/*
 * #%L
 * Netarchivesuite - harvester
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
package dk.netarkivet.harvester.scheduler;

import java.util.Enumeration;

import javax.jms.JMSException;
import javax.jms.QueueBrowser;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.lifecycle.ComponentLifeCycle;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.datamodel.HarvestChannelDAO;
import dk.netarkivet.harvester.distribute.HarvesterChannels;
import dk.netarkivet.harvester.distribute.HarvesterMessageHandler;
import dk.netarkivet.harvester.harvesting.distribute.HarvesterReadyMessage;
import dk.netarkivet.harvester.harvesting.distribute.HarvesterRegistrationRequest;
import dk.netarkivet.harvester.harvesting.distribute.HarvesterRegistrationResponse;

/**
 * Handles the reception of status messages from the harvesters. Will call the {@link #visit(HarvesterReadyMessage)}
 * method when a Ready message is received.
 */
public class HarvesterStatusReceiver extends HarvesterMessageHandler implements ComponentLifeCycle {

    /** The logger to use. */
    private static final Logger log = LoggerFactory.getLogger(HarvesterStatusReceiver.class);

    /** @see HarvesterStatusReceiver#visit(dk.netarkivet.harvester.harvesting.distribute.HarvesterReadyMessage) */
    private final JobDispatcher jobDispatcher;
    /** Connection to JMS provider. */
    private final JMSConnection jmsConnection;

    /** The DAO handling {@link HarvestChannel}s */
    private final HarvestChannelDAO harvestChannelDao;

    private final HarvestChannelRegistry harvestChannelRegistry;
    /** Is the feature to limit the number of submitted messages in each queue enabled? */
	private final Boolean limitSubmittedJobsInQueue;
	/** The number of submitted messages in each queue. Only used, if the above is true */
	private final int submittedJobsInQueueThreshold;

    /**
     * Constructor of the <code>HarvesterStatusReceiver</code>.
     * This constructs also reads from settings, if we're limiting the number of submitted messages in each queue, and its limit.
     * If the setting 'settings.harvester.scheduler.limitSubmittedJobsInQueue' is false, no limit is enforced, otherwise the limit is
     * defined by setting 'settings.harvester.scheduler.submittedJobsInQueueLimit'.
     * @param jobDispatcher The <code>JobDispatcher</code> to delegate the dispatching of new jobs to, when a 'Ready for
     * job' event is received. 
     * @param jmsConnection The JMS connection by which {@link HarvesterReadyMessage} is received.
     * @param harvestChannelDao The specific HarvestChannelDAO instance to use 
     * @param harvestChannelRegistry The specific HarvestChannelRegistry instance to use
     */
    public HarvesterStatusReceiver(JobDispatcher jobDispatcher, JMSConnection jmsConnection,
            HarvestChannelDAO harvestChannelDao, HarvestChannelRegistry harvestChannelRegistry) {
        ArgumentNotValid.checkNotNull(jobDispatcher, "jobDispatcher");
        ArgumentNotValid.checkNotNull(jmsConnection, "jmsConnection");
        ArgumentNotValid.checkNotNull(harvestChannelDao, "harvestChannelDao");
        this.jobDispatcher = jobDispatcher;
        this.jmsConnection = jmsConnection;
        this.harvestChannelDao = harvestChannelDao;
        this.harvestChannelRegistry = harvestChannelRegistry;
        this.limitSubmittedJobsInQueue = Settings.getBoolean(HarvesterSettings.SCHEDULER_LIMIT_SUBMITTED_JOBS_IN_QUEUE);
        this.submittedJobsInQueueThreshold = Settings.getInt(HarvesterSettings.SCHEDULER_SUBMITTED_JOBS_IN_QUEUE_LIMIT);
    }

    @Override
    public void start() {
        jmsConnection.setListener(HarvesterChannels.getHarvesterStatusChannel(), this);
        jmsConnection.setListener(HarvesterChannels.getHarvesterRegistrationRequestChannel(), this);
        log.info("limitSubmittedJobsInQueue: {}", limitSubmittedJobsInQueue);
        if (limitSubmittedJobsInQueue) {
        	log.info("submittedJobsInQueueThreshold: {}", submittedJobsInQueueThreshold);
        }
    }

    @Override
    public void shutdown() {
        jmsConnection.removeListener(HarvesterChannels.getHarvesterStatusChannel(), this);
    }

    /**
     * Tells the dispatcher that it may dispatch a new job.
     *
     * @param message The message containing the relevant harvester information.
     */
    @Override
    public void visit(HarvesterReadyMessage message) {
        ArgumentNotValid.checkNotNull(message, "message");
        log.trace("Received ready message from {} on host {}", message.getApplicationInstanceId(), message.getHostName() );
        HarvestChannel channel = harvestChannelDao.getByName(message.getHarvestChannelName());
        if (!harvestChannelRegistry.isRegistered(message.getHarvestChannelName())) {
        	log.info("Reregistering the harvester '{}' to channel '{}'", message.getApplicationInstanceId(),message.getHarvestChannelName()); 
        	harvestChannelRegistry.register(message.getHarvestChannelName(), message.getApplicationInstanceId());
        } else if (!harvestChannelRegistry.isRegisteredToChannel(message.getApplicationInstanceId(), message.getHarvestChannelName())) {
        	harvestChannelRegistry.register(message.getHarvestChannelName(), message.getApplicationInstanceId());
        };
        if (limitSubmittedJobsInQueue) {
        	// Check If already a Message in the JMS queue for this channel
        	ChannelID relevantChannelId = HarvesterChannels.getHarvestJobChannelId(channel);
        	int currentCount = getCount(relevantChannelId);
        	if (currentCount < submittedJobsInQueueThreshold) {
        		jobDispatcher.submitNextNewJob(channel);
        	} else {
        		log.debug("No jobs submitted to channel {} after receiving ready message from {}. "
        				+ "Already {} jobs submitted to channel ", relevantChannelId, message.getApplicationInstanceId(),
        				currentCount);
        	}
        } else { // If no limit, always submit new job, if a job in status NEW exists scheduled for this channel
        	jobDispatcher.submitNextNewJob(channel);
        }
    }

    @Override
    public void visit(HarvesterRegistrationRequest msg) {
        ArgumentNotValid.checkNotNull(msg, "msg");

        String harvesterInstanceId = msg.getInstanceId();
        String channelName = msg.getHarvestChannelName();

        boolean isSnapshot = true;
        boolean isValid = true;
        try {
            HarvestChannel chan = harvestChannelDao.getByName(channelName);
            isSnapshot = chan.isSnapshot();
        } catch (UnknownID e) {
        	log.warn("The channel '{}' is unknown by the channels table, wherefore the HarvesterRegistrationRequest is denied. The known channels are ", channelName, 
        			StringUtils.join(harvestChannelDao.getAll(true), ","));
            isValid = false;
        }

        if (isValid) {
            harvestChannelRegistry.register(channelName, harvesterInstanceId);
        }

        // Send the reply
        jmsConnection.send(new HarvesterRegistrationResponse(channelName, isValid, isSnapshot));
        log.info("Sent a message to host {} to notify that harvest channel '{}' is {}", msg.getHostname(), channelName, (isValid ? "valid."
                : "invalid."));
    }
    
    /**
     * Retrieve the number of current messages defined by the given queueID. 
     * @param queueID a given QueueID
     * @return the number of current messages defined by the given queueID
     */
    private int getCount(ChannelID queueID) {
    	QueueBrowser qBrowser;
    	int count=0;
    	try {
    		qBrowser = jmsConnection.createQueueBrowser(queueID);
    		Enumeration msgs = qBrowser.getEnumeration();

    		if ( !msgs.hasMoreElements() ) {
    			return 0;
    		} else { 
    			while (msgs.hasMoreElements()) { 
    				msgs.nextElement();
    				count++;
    			}
    		}
    		qBrowser.close();
    	} catch (JMSException e) {
    		log.warn("JMSException thrown: ", e);
    	} catch (Throwable e1) {
    		log.warn("Unexpected exception of type {} thrown: ", e1.getClass().getName(), e1);
    	}

    	return count;
    }
    
}
