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
package dk.netarkivet.harvester.harvesting.report;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.StopReason;
import dk.netarkivet.harvester.harvesting.distribute.DomainStats;

/**
 * Base implementation for a harvest report.
 */
@SuppressWarnings({"serial"})
public abstract class AbstractHarvestReport implements HarvestReport{

    /** The logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(AbstractHarvestReport.class);

    /**
     * The default reason why we stopped harvesting this domain. This value is set by looking for a CRAWL ENDED in the
     * crawl.log.
     */
    private StopReason defaultStopReason;

	private DomainStatsReport domainstatsReport;

    /**
     * Default constructor that does nothing. The real construction is supposed to be done in the subclasses by filling
     * out the domainStats map with crawl results.
     */
    public AbstractHarvestReport() {
    }

    /**
     * Constructor from DomainStatsReports.
     *
     * @param dsr the result of parsing the crawl.log for domain statistics
     */
    public AbstractHarvestReport(DomainStatsReport dsr) {
        ArgumentNotValid.checkNotNull(dsr, "DomainStatsReport dsr");
        this.domainstatsReport = dsr;
        this.defaultStopReason = dsr.getDefaultStopReason();
    }

    @Override
    public StopReason getDefaultStopReason() {
        return defaultStopReason;
    }

    /**
     * Returns the set of domain names that are contained in hosts-report.txt (i.e. host names mapped to domains)
     *
     * @return a Set of Strings
     */
    @Override
    public final Set<String> getDomainNames() {
        return Collections.unmodifiableSet(domainstatsReport.getDomainstats().keySet());
    }

    /**
     * Get the number of objects found for the given domain.
     *
     * @param domainName A domain name (as given by getDomainNames())
     * @return How many objects were collected for that domain or Null if none found
     */
    @Override
    public final Long getObjectCount(String domainName) {
        ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
        final DomainStats domainStats = getDomainStats().get(domainName);
        if (domainStats != null) {
            return domainStats.getObjectCount();
        }
        return null;
    }

    /**
     * Get the number of bytes downloaded for the given domain.
     *
     * @param domainName A domain name (as given by getDomainNames())
     * @return How many bytes were collected for that domain or null if information available for this domain.
     */
    @Override
    public final Long getByteCount(String domainName) {
        ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
        final DomainStats domainStats = getDomainStats().get(domainName);
        if (domainStats != null) {
            return domainStats.getByteCount();
        }
        return null;
    }

    /**
     * Get the StopReason for the given domain.
     *
     * @param domainName A domain name (as given by getDomainNames())
     * @return the StopReason for the given domain or null, if no stopreason found for this domain
     * @throws ArgumentNotValid if null or empty domainName
     */
    @Override
    public final StopReason getStopReason(String domainName) {
        ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
        final DomainStats domainStats = getDomainStats().get(domainName);
        if (domainStats != null) {
            return domainStats.getStopReason();
        }
        return null;
    }

    /**
     * Attempts to get an already existing {@link DomainStats} object for that domain, and if not found creates one with
     * zero values.
     *
     * @param domainName the name of the domain to get DomainStats for.
     * @return a DomainStats object for the given domain-name.
     */
    protected DomainStats getOrCreateDomainStats(String domainName) {
        DomainStats dhi = getDomainStats().get(domainName);
        if (dhi == null) {
            dhi = new DomainStats(0L, 0L, defaultStopReason);
            getDomainStats().put(domainName, dhi);
        }

        return dhi;
    }

    private Map<String, DomainStats> getDomainStats() {
    	return this.domainstatsReport.getDomainstats();
    }
    
}
