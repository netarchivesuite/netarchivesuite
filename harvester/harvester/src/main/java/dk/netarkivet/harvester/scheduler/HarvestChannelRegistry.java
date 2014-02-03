/**
 * 
 */
package dk.netarkivet.harvester.scheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.harvesting.HarvestController;
import dk.netarkivet.harvester.harvesting.distribute.HarvesterRegistrationRequest;

/**
 * Keeps track of the number of {@link HarvesterRegistrationRequest}s that have been
 * received per channel, which allows to know if a {@link HarvestController}s are
 * registered to a given {@link HarvestChannel}. 
 */
public class HarvestChannelRegistry {

	/** The class logger. */
    private static final Log LOG = LogFactory.getLog(HarvestChannelRegistry.class.getName());
	
	private Map<String, Set<String>> harvesterChannelRegistry = 
			new HashMap<String, Set<String>>();

	public synchronized void register(
			final String channelName,
			final String harvesterInstanceId) {
		
		// First remove any reference to this instanceId
        // This is done in case a Harvester has been unexpectedly shut down and restarted
        clearHarvester(harvesterInstanceId);
		
		Set<String> instanceIds = harvesterChannelRegistry.get(channelName);
		if (instanceIds == null) {
			instanceIds = new TreeSet<String>();
		}
		instanceIds.add(harvesterInstanceId);
		harvesterChannelRegistry.put(channelName, instanceIds);
		
		LOG.info("Harvester " + harvesterInstanceId + " registered on channel " + channelName);
		logStatus();
	}
	
	public synchronized boolean isRegistered(final String channelName) {
		return harvesterChannelRegistry.containsKey(channelName);
	}
	
	private void logStatus() {
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
	
	/**
	 * Clears any registration data for a given harvester instance id.
	 * @param harvesterInstanceId a harvester instance id
	 */
	private void clearHarvester(final String harvesterInstanceId) {
		ArrayList<String> keysToRemove = new ArrayList<String>();
		for (String channel : harvesterChannelRegistry.keySet())  {
			Set<String> instanceIds = harvesterChannelRegistry.get(channel);
			if (instanceIds.contains(harvesterInstanceId)) {
				instanceIds.remove(harvesterInstanceId);
				if (instanceIds.isEmpty()) {
					keysToRemove.add(channel);
				}
				LOG.info("Cleared former registration of '" + channel + "' to '" 
						+ harvesterInstanceId + "'");
			}			
		}
		for (String key : keysToRemove) {
			harvesterChannelRegistry.remove(key);
		}
	}

}
