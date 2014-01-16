/**
 * 
 */
package dk.netarkivet.harvester.scheduler;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.harvesting.HarvestController;
import dk.netarkivet.harvester.harvesting.distribute.HarvestChannelValidityRequest;

/**
 * Keeps track of the number of {@link HarvestChannelValidityRequest}s that have been
 * received per channel, which allows to know if a {@link HarvestController}s are
 * registered to a given {@link HarvestChannel}. 
 */
public class HarvestChannelRegistry {

	/** The class logger. */
    private static final Log LOG = LogFactory.getLog(HarvestChannelRegistry.class.getName());
	
	private Map<String, Integer> harvesterChannelRegistry = new HashMap<String, Integer>();

	public synchronized void register(final String channelName) {
		Integer count = harvesterChannelRegistry.get(channelName);
		count = count == null ? 1 : count + 1;
		LOG.info("Registered harvest channel '" + channelName + "' (total " + count + ")");
		harvesterChannelRegistry.put(channelName, count);
	}
	
	public synchronized boolean isRegistered(final String channelName) {
		return harvesterChannelRegistry.containsKey(channelName);
	}

}
