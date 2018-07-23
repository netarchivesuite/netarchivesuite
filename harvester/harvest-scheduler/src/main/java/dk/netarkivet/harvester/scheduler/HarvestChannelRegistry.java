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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.harvesting.distribute.HarvesterRegistrationRequest;

/**
 * Keeps track of the number of {@link HarvesterRegistrationRequest}s that have been received per channel, which allows
 * to know if a HarvestControllers are registered to a given {@link HarvestChannel}.
 */
public class HarvestChannelRegistry {

    /** The class logger. */
    private static final Logger LOG = LoggerFactory.getLogger(HarvestChannelRegistry.class);

    private Map<String, Set<String>> harvesterChannelRegistry = new HashMap<String, Set<String>>();

    public synchronized void register(final String channelName, final String harvesterInstanceId) {
        // First remove any reference to this instanceId
        // This is done in case a Harvester has been unexpectedly shut down and restarted
        clearHarvester(harvesterInstanceId);

        Set<String> instanceIds = harvesterChannelRegistry.get(channelName);
        if (instanceIds == null) {
            instanceIds = new TreeSet<String>();
        }
        instanceIds.add(harvesterInstanceId);
        harvesterChannelRegistry.put(channelName, instanceIds);

        LOG.info("Harvester {} registered on channel {}", harvesterInstanceId, channelName);
        logStatus();
    }

    public synchronized boolean isRegistered(final String channelName) {
        return harvesterChannelRegistry.containsKey(channelName);
    }
    
    public synchronized boolean isRegisteredToChannel(final String harvesterInstanceId, final String channelName) {
    	if (!isRegistered(channelName)){
    		return false;
    	} else {
    		return harvesterChannelRegistry.get(channelName).contains(harvesterInstanceId);
    	}
    }

    private void logStatus() {
        if (LOG.isInfoEnabled()) {
            String msg = HarvestChannelRegistry.class.getSimpleName() + " status:";
            for (String channel : harvesterChannelRegistry.keySet()) {
                msg += "\n\t- " + channel + " { ";
                for (String id : harvesterChannelRegistry.get(channel)) {
                    msg += id + ", ";
                }
                msg = msg.substring(0, msg.lastIndexOf(",")) + " }";
            }
            LOG.info(msg);
        }
    }

    /**
     * Clears any registration data for a given harvester instance id.
     *
     * @param harvesterInstanceId a harvester instance id
     */
    private void clearHarvester(final String harvesterInstanceId) {
        ArrayList<String> keysToRemove = new ArrayList<String>();
        for (String channel : harvesterChannelRegistry.keySet()) {
            Set<String> instanceIds = harvesterChannelRegistry.get(channel);
            if (instanceIds.contains(harvesterInstanceId)) {
                instanceIds.remove(harvesterInstanceId);
                if (instanceIds.isEmpty()) {
                    keysToRemove.add(channel);
                }
                LOG.info("Cleared former registration of '{}' to '{}'", channel, harvesterInstanceId);
            }
        }
        for (String key : keysToRemove) {
            harvesterChannelRegistry.remove(key);
        }
    }

}
