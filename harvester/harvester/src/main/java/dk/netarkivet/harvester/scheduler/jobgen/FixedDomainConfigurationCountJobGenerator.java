/* File:             $Id$
 * Revision:         $Revision$
 * Author:           $Author$
 * Date:             $Date$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.harvester.scheduler.jobgen;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.HarvestDefinition;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;

/**
 * Job generator implementation. Generates jobs with a fixed number of domain
 * configurations. Configuration allows to choose a different count for partial and
 * full harvests. The last job generated may have less configurations in it, as job
 * generation happens on a per-harvest basis.
 *
 * @see HarvesterSettings#JOBGEN_FIXED_CONFIG_COUNT_SNAPSHOT
 * @see HarvesterSettings#JOBGEN_FIXED_CONFIG_COUNT_FOCUSED
 *
 */
public class FixedDomainConfigurationCountJobGenerator extends AbstractJobGenerator {

    /**
     * A compound key used to split domain configurations in jobs.
     */
    private class DomainConfigurationKey {

        /**
         * The name of the Heritrix crawl order template.
         */
        private final String orderXmlName;
        /**
         * The crawl budget in URI.
         */
        private final long maxObjects;
        /**
         * The crawl budget in bytes.
         */
        private final long maxBytes;

        /**
         * Constructor from a domain configuration.
         * @param cfg the related {@link DomainConfiguration}
         */
        DomainConfigurationKey(DomainConfiguration cfg) {
            this.orderXmlName = cfg.getOrderXmlName();
            long cMaxBytes = cfg.getMaxBytes();
            long cMaxObjects = cfg.getMaxObjects();
            if (cMaxBytes == 0 || cMaxObjects == 0) {
                // All domain configurations with a zero budget (either size or URI count
                // end up in the same group
                this.maxBytes = 0;
                this.maxObjects = 0;
            } else {
                this.maxBytes = cMaxBytes;
                this.maxObjects = cMaxObjects;
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (maxBytes ^ (maxBytes >>> 32));
            result = prime * result + (int) (maxObjects ^ (maxObjects >>> 32));
            result = prime * result
                    + ((orderXmlName == null) ? 0 : orderXmlName.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (!DomainConfigurationKey.class.equals(obj.getClass())) {
                return false;
            }
            DomainConfigurationKey dc = (DomainConfigurationKey) obj;
            return orderXmlName.equals(dc.orderXmlName)
                    && maxBytes == dc.maxBytes
                    && maxObjects == dc.maxObjects;
        }

        @Override
        public String toString() {
            return orderXmlName + ":" + maxObjects + ":" + maxBytes;
        }
    }

    /**
     * Compare two configurations in alphabetical order of their name.
     */
    private static class ConfigNamesComparator
    implements Comparator<DomainConfiguration> {

        @Override
        public int compare(DomainConfiguration dc1, DomainConfiguration dc2) {
            return dc1.getName().compareTo(dc2.getName());
        }

    }

    /**
     * Constant : how many {@link DomainConfiguration}s we want in a focused harvest job.
     */
    private static long CONFIG_COUNT_FOCUSED = Settings.getLong(
            HarvesterSettings.JOBGEN_FIXED_CONFIG_COUNT_FOCUSED);

    /**
     * Constant : how many {@link DomainConfiguration}s we want in a snapshot harvest job.
     */
    private static long CONFIG_COUNT_SNAPSHOT = Settings.getLong(
            HarvesterSettings.JOBGEN_FIXED_CONFIG_COUNT_SNAPSHOT);

    /**
     * The singleton instance.
     */
    public static FixedDomainConfigurationCountJobGenerator instance;

    /**
     * Maps jobs currently being filled with domain configurations by harvest template
     * name. These jobs keep getting new configurations until no more configurations are
     * left to process or the configured size has been reached.
     */
    private Map<DomainConfigurationKey, Job> jobsUnderConstruction;

    /**
     * True if the {@link HarvestDefinition} being processed is a full harvest,
     * false otherwise.
     */
    private boolean isSnapshotHarvest;

    /**
     * The job DAO instance (singleton).
     */
    private JobDAO dao = JobDAO.getInstance();

    /**
     * @return the singleton instance, builds it if necessary.
     */
    public static FixedDomainConfigurationCountJobGenerator getInstance() {
        if (instance == null) {
            instance = new FixedDomainConfigurationCountJobGenerator();
        }
        return instance;
    }

    @Override
    protected Comparator<DomainConfiguration> getDomainConfigurationSubsetComparator(
            HarvestDefinition harvest) {
        return new ConfigNamesComparator();
    }

    @Override
    protected boolean checkSpecificAcceptConditions(Job job, DomainConfiguration cfg) {
        return job.getDomainConfigurationMap().size() <
                (isSnapshotHarvest ? CONFIG_COUNT_SNAPSHOT : CONFIG_COUNT_FOCUSED);
    }

    @Override
    public synchronized int generateJobs(HarvestDefinition harvest) {
        isSnapshotHarvest = harvest.isSnapShot();
        jobsUnderConstruction = new HashMap<DomainConfigurationKey, Job>();

        try {
            int jobsComplete = super.generateJobs(harvest);

            // Look if we have jobs that have not reached their limit, but are complete
            // as we have finished processing the harvest
            if (!jobsUnderConstruction.isEmpty()) {
                for (Job job : jobsUnderConstruction.values()) {
                    // The job is ready, post-process and store it in DB
                    editJobOrderXml(job);
                    dao.create(job);

                    // Increment counter
                    jobsComplete++;
                }
            }

            return jobsComplete;
        } finally {
            jobsUnderConstruction.clear(); // make sure to free resources
        }
    }

    @Override
    protected int processDomainConfigurationSubset(
            HarvestDefinition harvest,
            Iterator<DomainConfiguration> domainConfSubset) {
        int jobsComplete = 0;
        while (domainConfSubset.hasNext()) {
            DomainConfiguration cfg = domainConfSubset.next();
            DomainConfigurationKey domainConfigKey = new DomainConfigurationKey(cfg);
            Job match = jobsUnderConstruction.get(domainConfigKey);
            if (match == null) {
                match = initNewJob(harvest, cfg);
            } else {
                if (canAccept(match, cfg)) {
                    match.addConfiguration(cfg);
                } else {
                    // The job is ready, post-process and store it in DB
                    editJobOrderXml(match);
                    dao.create(match);

                    // Increment counter
                    jobsComplete++;

                    // Start construction of a new job
                    initNewJob(harvest, cfg);
                }
            }
        }
        return jobsComplete;
    }

    /**
     * Initializes a new job.
     * @param harvest the {@link HarvestDefinition} being processed.
     * @param cfg the first {@link DomainConfiguration} for this job.
     * @return the {@link Job} instance
     */
    private Job initNewJob(HarvestDefinition harvest, DomainConfiguration cfg) {
        Job job = getNewJob(harvest, cfg);
        jobsUnderConstruction.put(new DomainConfigurationKey(cfg), job);
        return job;
    }

}


