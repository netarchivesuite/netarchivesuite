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
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
            if (obj == null || !DomainConfigurationKey.class.equals(obj.getClass())) {
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
     * Simple marker class to improve code readability.
     * 
     * Maps jobs currently being filled, for a given harvest definition, with domain 
     * configurations by harvest template name. These jobs keep getting new 
     * configurations until no more configurations are left to process or the 
     * configured size has been reached.
     */
    @SuppressWarnings("serial")
	private class HarvestJobGenerationState extends HashMap<DomainConfigurationKey,Job> {
    	
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
     * Constant : exclude {@link DomainConfiguration}s with a budget of zero (bytes or objects).
     */
    private static boolean EXCLUDE_ZERO_BUDGET = Settings.getBoolean(
            HarvesterSettings.JOBGEN_FIXED_CONFIG_COUNT_EXCLUDE_ZERO_BUDGET);

    /**
     * The singleton instance.
     */
    public static FixedDomainConfigurationCountJobGenerator instance;

    /**
     * Maps jobs currently being filled with domain configurations by harvest template
     * name. These jobs keep getting new configurations until no more configurations are
     * left to process or the configured size has been reached.
     */
    private Map<Long, HarvestJobGenerationState> state;

    /**
     * The job DAO instance (singleton).
     */
    private JobDAO dao = JobDAO.getInstance();
    
    /** Logger for this class. */
    private Log log = LogFactory.getLog(getClass());
    
    private FixedDomainConfigurationCountJobGenerator() {
    	this.state = new HashMap<Long, HarvestJobGenerationState>();
    }

    /**
     * @return the singleton instance, builds it if necessary.
     */
    public synchronized static FixedDomainConfigurationCountJobGenerator getInstance() {
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
                (job.isSnapshot() ? CONFIG_COUNT_SNAPSHOT : CONFIG_COUNT_FOCUSED);
    }

    @Override
    public int generateJobs(HarvestDefinition harvest) {
        
    	HarvestJobGenerationState jobsUnderConstruction = getStateForHarvest(harvest);

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
            dropStateForHarvest(harvest);
        }
    }

    @Override
    protected int processDomainConfigurationSubset(
            HarvestDefinition harvest,
            Iterator<DomainConfiguration> domainConfSubset) {
    	
    	HarvestJobGenerationState jobsUnderConstruction = getExistingStateForHarvest(harvest);
        int jobsComplete = 0;
        while (domainConfSubset.hasNext()) {
            DomainConfiguration cfg = domainConfSubset.next();
            
            // Should we exclude a configuration with a budget of zero?
            if (EXCLUDE_ZERO_BUDGET
            		&& (0 == cfg.getMaxBytes() || 0 == cfg.getMaxObjects())) {
            	log.info("[JobGen] Config '" + cfg.getName() + "' for '" + cfg.getDomainName() + "'" 
            			+ " excluded (0"
            			+ (cfg.getMaxBytes() == 0 ? " bytes" : " objects")
            			+ ")");
            	continue;
            }
            
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
    	HarvestJobGenerationState jobsUnderConstruction = getExistingStateForHarvest(harvest);
        Job job = getNewJob(harvest, cfg);
        jobsUnderConstruction.put(new DomainConfigurationKey(cfg), job);
        return job;
    }
    
    private synchronized HarvestJobGenerationState getStateForHarvest(
    		final HarvestDefinition harvest,
    		final boolean failIfNotExists) {
    	
    	long harvestId = harvest.getOid();
    	HarvestJobGenerationState harvestState = this.state.get(harvestId);
    	if (harvestState == null) {
    		if (failIfNotExists) {
    			throw new NoSuchElementException(
    					"No job generation state for harvest " + harvestId);
    		}
    		harvestState = new HarvestJobGenerationState();
    		this.state.put(harvestId, harvestState);
    	}
    	
    	return harvestState;
    }
    
    private HarvestJobGenerationState getStateForHarvest(final HarvestDefinition harvest) {
    	return getStateForHarvest(harvest, false);
    }
    
    private HarvestJobGenerationState getExistingStateForHarvest(final HarvestDefinition harvest) {
    	return getStateForHarvest(harvest, true);
    }
    
    private synchronized void dropStateForHarvest(final HarvestDefinition harvest) {
    	long harvestId = harvest.getOid();
    	HarvestJobGenerationState harvestState = this.state.remove(harvestId);
    	if (harvestState == null) {
    		throw new NoSuchElementException("No job generation state for harvest " + harvestId);
    	}
    }

}