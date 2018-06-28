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

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.common.utils.TimeUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.Domain;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.HarvestInfo;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.SparseFullHarvest;
import dk.netarkivet.harvester.datamodel.StopReason;
import dk.netarkivet.harvester.harvesting.distribute.DomainStats;

/**
 * This implementation of the harvest report has the same pre-processing as {@link LegacyHarvestReport}, but is intended
 * to be used with a crawl order that sets budget using "queue-total-budget" instead of the QuotaEnforcer (@see
 * {@link HarvesterSettings#OBJECT_LIMIT_SET_BY_QUOTA_ENFORCER}). Hence post-processing cannot rely any more on
 * annotations added by QuotaEnforcer anymore and thus simply compares actual document counts to crawl and configuration
 * budgets.
 */
@SuppressWarnings({"serial"})
public class BnfHarvestReport extends AbstractHarvestReport{

    /** The logger for this class. */
    private static final Logger LOG = LoggerFactory.getLogger(BnfHarvestReport.class);

    /**
     * Constructor for this class.
     *
     * @param dsr A DomainStatsReport
     * @throws IOFailure If the processing of the files goes wrong
     */
    public BnfHarvestReport(DomainStatsReport dsr) throws IOFailure {
    	super(dsr);
    }

    /**
     * Post-processing happens on the scheduler side when ARC files have been uploaded.
     *
     * @param job the actual job.
     */
    @Override
    public void postProcess(Job job) {
        ArgumentNotValid.checkNotNull(job, "job");

        LOG.info("Starting post-processing of harvest report for job {}", job.getJobID());
        long startTime = System.currentTimeMillis();

        long harvestObjectLimit = -1L;
        long harvestByteLimit = -1L;

        // First find if it's a full harvest job,
        // and if so get actual byte and object limits.
        if (job.isSnapshot()) {
            HarvestDefinitionDAO dao = HarvestDefinitionDAO.getInstance();
            String harvestName = dao.getHarvestName(job.getOrigHarvestDefinitionID());
            SparseFullHarvest harvest = dao.getSparseFullHarvest(harvestName);

            harvestObjectLimit = harvest.getMaxCountObjects();
            harvestByteLimit = harvest.getMaxBytes();
        }

        DomainDAO domDao = DomainDAO.getInstance();
        Map<String, String> domConfMap = job.getDomainConfigurationMap();

        // Process only domains from the harvest definition.
        final Set<String> harvestDomainNames = new HashSet<String>();
        harvestDomainNames.addAll(getDomainNames());
        harvestDomainNames.retainAll(domConfMap.keySet());

        for (String domainName : harvestDomainNames) {
            Domain domain = domDao.read(domainName);
            String confName = domConfMap.get(domainName);
            DomainConfiguration conf = domain.getConfiguration(confName);

            long confByteLimit = conf.getMaxBytes();
            long confObjectLimit = conf.getMaxObjects();

            DomainStats ds = getOrCreateDomainStats(domainName);
            long actualByteCount = ds.getByteCount();
            long actualObjectCount = ds.getObjectCount();

            StopReason finalStopReason = ds.getStopReason();

            if (harvestByteLimit > 0 && (actualByteCount >= harvestByteLimit)) {
                finalStopReason = StopReason.SIZE_LIMIT;
            } else if (harvestObjectLimit > 0 && (actualObjectCount >= harvestObjectLimit)) {
                finalStopReason = StopReason.OBJECT_LIMIT;
            } else if (confByteLimit > 0 && (actualByteCount >= confByteLimit)) {
                finalStopReason = StopReason.CONFIG_SIZE_LIMIT;
            } else if (confObjectLimit > 0 && (actualObjectCount >= confObjectLimit)) {
                finalStopReason = StopReason.CONFIG_OBJECT_LIMIT;
            }

            ds.setStopReason(finalStopReason);

            // Create the HarvestInfo object
            HarvestInfo hi = new HarvestInfo(job.getOrigHarvestDefinitionID(), job.getJobID(), domainName, confName,
                    new Date(), actualByteCount, actualObjectCount, finalStopReason);

            // Add HarvestInfo to Domain and make data persistent
            // by updating DAO
            domain.getHistory().addHarvestInfo(hi);
            domDao.update(domain);
        }

        if (LOG.isInfoEnabled()) {
            long time = System.currentTimeMillis() - startTime;
            LOG.info("Finished post-processing of harvest report for job {}, operation took {}", job.getJobID(),
                    StringUtils.formatDuration(time / TimeUtils.SECOND_IN_MILLIS));
        }

    }
}
