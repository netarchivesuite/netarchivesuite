package dk.netarkivet.harvester.harvesting.report;

import java.util.Map;

import dk.netarkivet.harvester.datamodel.StopReason;
import dk.netarkivet.harvester.harvesting.distribute.DomainStats;

public class DomainStatsReport {
	
	private Map<String, DomainStats> domainStats;
	private StopReason defaultStopReason;

	public DomainStatsReport(Map<String, DomainStats> domainStats, StopReason defaultReason) {
		this.domainStats = domainStats;
		this.defaultStopReason = defaultReason; 
	}
	
	public StopReason getDefaultStopReason() {
		return defaultStopReason;
	}

	public Map<String, DomainStats> getDomainstats() {
		return domainStats;
	}
	
}
