package dk.netarkivet.harvester.harvesting.report;

import java.io.Serializable;
import java.util.Map;

import dk.netarkivet.harvester.datamodel.StopReason;
import dk.netarkivet.harvester.harvesting.distribute.DomainStats;

/** 
 * Used together with the HarvestReportGenerator to generate a HarvestReport.
 * @author svc
 *
 */
public class DomainStatsReport implements Serializable {
	
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
