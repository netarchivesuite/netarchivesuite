/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2011 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.harvester.harvesting.report;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.common.utils.TimeUtils;
import dk.netarkivet.harvester.datamodel.Domain;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.HarvestInfo;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.NumberUtils;
import dk.netarkivet.harvester.datamodel.StopReason;
import dk.netarkivet.harvester.harvesting.HeritrixFiles;

/**
 * Class responsible for generating a domain harvest report from
 * crawl logs created by
 * Heritrix and presenting the relevant information to clients.
 *
 */
public class LegacyHarvestReport extends AbstractHarvestReport {

    /** The logger for this class. */
    private static final Log LOG = LogFactory.getLog(
            LegacyHarvestReport.class);

    /**
     * The constructor gets the data in a crawl.log file,
     * and parses the file. The crawl.log is described in the Heritrix
     * user-manual, section 8.2.1:
     * http://crawler.archive.org/articles/user_manual/analysis.html#logs
     * Note: Invalid lines are logged and then ignored.
     *
     * Each url listed in the file is assigned to a domain,
     * the total object count and byte count per domain is calculated.
     * Finally, a StopReason is found for each domain:
     *  When the response is CrawlURI.S_BLOCKED_BY_QUOTA (
     *  currently = -5003), the StopReason is set to
     *  StopReason.SIZE_LIMIT,
     *      if the annotation equals "Q:group-max-all-kb"  or
     *  StopReason.OBJECT_LIMIT,
     *      if the annotation equals "Q:group-max-fetch-successes".
     *
     *
     * @param hFiles the Heritrix reports and logs.
     */
    public LegacyHarvestReport(HeritrixFiles hFiles) {
        super(hFiles);
    }
    /** Default constructor. */
    public LegacyHarvestReport() {
        super();
    }

    /**
     * Post-processing happens on the scheduler side when ARC files
     * have been uploaded.
     * @param job the actual job.
     */
    @Override
    public void postProcess(Job job) {

        if (LOG.isInfoEnabled()) {
            LOG.info("Starting post-processing of harvest report for job "
                    + job.getJobID());
        }
        long startTime = System.currentTimeMillis();

        // Get the map from domain names to domain configurations
        Map<String, String> configurationMap = job.getDomainConfigurationMap();

        // For each domain harvested, check if it corresponds to a
        // domain configuration for this Job and if so add a new HarvestInfo
        // to the DomainHistory of the corresponding Domain object.
        // TODO  Information about the domains harvested by the crawler
        // without a domain configuration for this job is deleted!
        // Should this information be saved in some way (perhaps stored
        // in metadata.arc-files?)

        final Set<String> domainNames = new HashSet<String>();
        domainNames.addAll(getDomainNames());
        domainNames.retainAll(configurationMap.keySet());
        final DomainDAO dao = DomainDAO.getInstance();
        for (String domainName : domainNames) {
            Domain domain = dao.read(domainName);

            // Retrieve crawl data from log and add it to HarvestInfo
            StopReason stopReason = getStopReason(domainName);
            long countObjectRetrieved = getObjectCount(domainName);
            long bytesReceived = getByteCount(domainName);

            //If StopReason is SIZE_LIMIT, we check if it's the harvests' size
            //limit, or rather a configuration size limit.

            //A harvest is considered to have hit the configuration limit if
            //1) The limit is lowest, or
            //2) The number of harvested bytes is greater than the limit

            // Note: Even though the per-config-byte-limit might have changed
            // between the time we calculated the job and now, it's okay we
            // compare with the new limit, since it gives us the most accurate
            // result for whether we want to harvest any more.
            if (stopReason == StopReason.SIZE_LIMIT) {
                long maxBytesPerDomain = job.getMaxBytesPerDomain();
                long configMaxBytes = domain.getConfiguration(
                        configurationMap.get(domainName)).getMaxBytes();
                if (NumberUtils.compareInf(configMaxBytes, maxBytesPerDomain)
                    <= 0
                    || NumberUtils.compareInf(configMaxBytes, bytesReceived)
                       <= 0) {
                    stopReason = StopReason.CONFIG_SIZE_LIMIT;
                }
            } else if (stopReason == StopReason.OBJECT_LIMIT) {
                long maxObjectsPerDomain = job.getMaxObjectsPerDomain();
                long configMaxObjects = domain.getConfiguration(
                        configurationMap.get(domainName)).getMaxObjects();
                if (NumberUtils.compareInf(
                        configMaxObjects, maxObjectsPerDomain) <= 0) {
                    stopReason = StopReason.CONFIG_OBJECT_LIMIT;
                }
            }
            // Create the HarvestInfo object
            HarvestInfo hi = new HarvestInfo(
                    job.getOrigHarvestDefinitionID(), job.getJobID(),
                    domain.getName(), configurationMap.get(domain.getName()),
                    new Date(), bytesReceived, countObjectRetrieved,
                    stopReason);

            // Add HarvestInfo to Domain and make data persistent
            // by updating DAO
            domain.getHistory().addHarvestInfo(hi);
            dao.update(domain);
        }

        if (LOG.isInfoEnabled()) {
            long time = System.currentTimeMillis() - startTime;
            LOG.info("Finished post-processing of harvest report for job "
                    + job.getJobID() + ", operation took "
                    + StringUtils.formatDuration(
                            time / TimeUtils.SECOND_IN_MILLIS));
        }

    }

}
