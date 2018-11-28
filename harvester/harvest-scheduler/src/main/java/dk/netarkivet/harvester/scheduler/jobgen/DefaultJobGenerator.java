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
package dk.netarkivet.harvester.scheduler.jobgen;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.HarvestDefinition;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobDAO;
import dk.netarkivet.harvester.datamodel.NumberUtils;
import dk.netarkivet.harvester.datamodel.eav.EAV;

/**
 * The legacy job generator implementation. Aims at generating jobs that execute in a predictable time by taking
 * advantage of previous crawls statistics.
 */
public class DefaultJobGenerator extends AbstractJobGenerator {

    /** Logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(DefaultJobGenerator.class);

    private static Long CONFIG_COUNT_SNAPSHOT = null;

    public DefaultJobGenerator() {
        try {
            CONFIG_COUNT_SNAPSHOT = Settings.getLong(HarvesterSettings.JOBGEN_FIXED_CONFIG_COUNT_SNAPSHOT);
            if (CONFIG_COUNT_SNAPSHOT <= 0) {
                log.info("The parameter {} has the value {} and is therefore ignored during job splitting for "
                        + "snapshot jobs.", HarvesterSettings.JOBGEN_FIXED_CONFIG_COUNT_SNAPSHOT, CONFIG_COUNT_SNAPSHOT);
            } else {
                log.info("Snapshot jobs will be split at an absolute maximum of {} configurations ({}).",
                        CONFIG_COUNT_SNAPSHOT, HarvesterSettings.JOBGEN_FIXED_CONFIG_COUNT_SNAPSHOT);
            }
        } catch (UnknownID u) {
            log.info("The parameter {} is not set so there is no absolute limit to the number of configurations per "
                    + "snapshot job.", HarvesterSettings.JOBGEN_FIXED_CONFIG_COUNT_SNAPSHOT);
        }
    }

    /**
     * Compare two configurations using the following order: 1) Harvest template 2) Byte limit 3) expected number of
     * object a harvest of the configuration will produce. The comparison will put the largest configuration first (with
     * respect to 2) and 3))
     */
    public static class CompareConfigsDesc implements Comparator<DomainConfiguration> {

        private long objectLimit;
        private long byteLimit;

        public CompareConfigsDesc(long objectLimit, long byteLimit) {
            this.objectLimit = objectLimit;
            this.byteLimit = byteLimit;
        }

        public int compare(DomainConfiguration cfg1, DomainConfiguration cfg2) {
            log.trace("Comparing " + cfg1 + " " + cfg2);
            // Compare order xml names
            int cmp = cfg1.getOrderXmlName().compareTo(cfg2.getOrderXmlName());
            if (cmp != 0) {
                return cmp;
            }
            log.trace("Comparing EAV attributes now");
            int result = EAV.compare(cfg1.getAttributesAndTypes(), cfg2.getAttributesAndTypes());
            log.trace("Comparison of EAV attributes gave result " + result);
            if (result != 0) {
                return result;
            }
            // Compare byte limits
            long bytelimit1 = NumberUtils.minInf(cfg1.getMaxBytes(), byteLimit);
            long bytelimit2 = NumberUtils.minInf(cfg2.getMaxBytes(), byteLimit);
            cmp = NumberUtils.compareInf(bytelimit2, bytelimit1);
            if (cmp != 0) {
                return cmp;
            }
            // Compare expected sizes
            long expectedsize1 = cfg1.getExpectedNumberOfObjects(objectLimit, byteLimit);
            long expectedsize2 = cfg2.getExpectedNumberOfObjects(objectLimit, byteLimit);
            long res = expectedsize2 - expectedsize1;
            if (res != 0L) {
                return res < 0L ? -1 : 1;
            }
            return 0;
        }
    }

    /**
     * Job limits read from settings during construction.
     */
    private final long LIM_MAX_REL_SIZE = Long.parseLong(Settings
            .get(HarvesterSettings.JOBS_MAX_RELATIVE_SIZE_DIFFERENCE));
    private final long LIM_MIN_ABS_SIZE = Long.parseLong(Settings
            .get(HarvesterSettings.JOBS_MIN_ABSOLUTE_SIZE_DIFFERENCE));
    private final long LIM_MAX_TOTAL_SIZE = Long.parseLong(Settings.get(HarvesterSettings.JOBS_MAX_TOTAL_JOBSIZE));
    /** Constant : exclude {@link DomainConfiguration}s with a budget of zero (bytes or objects). */
    private final boolean EXCLUDE_ZERO_BUDGET = Settings
            .getBoolean(HarvesterSettings.JOBGEN_FIXED_CONFIG_COUNT_EXCLUDE_ZERO_BUDGET);

    /** Singleton instance. */
    private static DefaultJobGenerator instance;

    /**
     * @return the singleton instance, builds it if necessary.
     */
    public static synchronized DefaultJobGenerator getInstance() {
        if (instance == null) {
            instance = new DefaultJobGenerator();
        }
        return instance;
    }

    @Override
    protected Comparator<DomainConfiguration> getDomainConfigurationSubsetComparator(HarvestDefinition harvest) {
        return new CompareConfigsDesc(harvest.getMaxCountObjects(), harvest.getMaxBytes());
    }

    /**
     * Create new jobs from a collection of configurations. All configurations must use the same order.xml file.Jobs
     *
     * @param harvest the {@link HarvestDefinition} being processed.
     * @param domainConfSubset the configurations to use to create the jobs
     * @return The number of jobs created
     * @throws ArgumentNotValid if any of the parameters is null or if the cfglist does not contain any configurations
     */
    @Override
    protected int processDomainConfigurationSubset(HarvestDefinition harvest,
            Iterator<DomainConfiguration> domainConfSubset) {
        int jobsMade = 0;
        Job job = null;
        log.debug("Adding domainconfigs with the same order.xml for harvest #{}", harvest.getOid());
        JobDAO dao = JobDAO.getInstance();
        DomainConfiguration previousDomainConf = null;
        while (domainConfSubset.hasNext()) {
            DomainConfiguration cfg = domainConfSubset.next();
            log.trace("Processing " + DomainConfiguration.cfgToString(cfg));
            if (EXCLUDE_ZERO_BUDGET && (0 == cfg.getMaxBytes() || 0 == cfg.getMaxObjects())) {
                log.info("Config '{}' for '{}'" + " excluded (0{})", cfg.getName(), cfg.getDomainName(),
                        (cfg.getMaxBytes() == 0 ? " bytes" : " objects"));
                continue;
            }
            // excluding configs with no active seeds
            if (ignoreConfiguration(cfg)) {
            	log.info("Ignoring config '{}' for domain '{}' - no active seeds !");
            	continue;
            }
            
            if ((job == null) || (!canAccept(job, cfg, previousDomainConf))) {
                if (job != null) {
                    // If we're done with a job, write it out
                    ++jobsMade;
                    dao.create(job);
                }
                job = getNewJob(harvest, cfg);
                log.trace("Created new job for harvest #{} to add configuration {} for domain {}", harvest.getOid(),
                        cfg.getName(), cfg.getDomainName());

            } else {
                job.addConfiguration(cfg);
                log.trace("Added job configuration {} for domain {} to current job for harvest #{}", cfg.getName(),
                        cfg.getDomainName(), harvest.getOid());
            }
            previousDomainConf = cfg;
        }
        if (job != null) {
            ++jobsMade;
            editJobOrderXml(job);
            dao.create(job);
            if (log.isTraceEnabled()) {
                log.trace("Generated job: '{}'", job.toString());
                StringBuilder logMsg = new StringBuilder("Job configurationsDomain:");
                for (Map.Entry<String, String> config : job.getDomainConfigurationMap().entrySet()) {
                    logMsg.append("\n ").append(config.getKey()).append(":").append(config.getValue());
                }
                log.trace(logMsg.toString());
            }
            log.debug("Created {} jobs for harvest #{}", jobsMade, harvest.getOid());
        }
        return jobsMade;
    }


    @Override
    protected boolean checkSpecificAcceptConditions(Job job, DomainConfiguration cfg) {
        if (job.isSnapshot()
                && CONFIG_COUNT_SNAPSHOT != null
                && CONFIG_COUNT_SNAPSHOT > 0
                && job.getDomainConfigurationMap().size() >= CONFIG_COUNT_SNAPSHOT
                ) {
            log.debug("Job for HD #{} has now reached the CONFIG_COUNT_SNAPSHOT limit {}", job.getOrigHarvestDefinitionID(), CONFIG_COUNT_SNAPSHOT);
            return false;
        }

        // By default byte limit is used as base criterion for splitting a
        // harvest in config chunks, however the configuration can override
        // this and instead use object limit.
        boolean splitByObjectLimit = Settings.getBoolean(HarvesterSettings.SPLIT_BY_OBJECTLIMIT);
        long forceMaxObjectsPerDomain = job.getForceMaxObjectsPerDomain();
        long forceMaxBytesPerDomain = job.getForceMaxBytesPerDomain();
        if (splitByObjectLimit) {
            if (NumberUtils.compareInf(cfg.getMaxObjects(), forceMaxObjectsPerDomain) < 0
                    || (job.isConfigurationSetsObjectLimit() && NumberUtils.compareInf(cfg.getMaxObjects(),
                            forceMaxObjectsPerDomain) != 0)) {
                log.debug("Job for HD #{} OBJECT_LIMIT of config (domain,config={},{}) incompatible with current job", 
                        job.getOrigHarvestDefinitionID(), cfg.getDomainName(), cfg.getName());
                return false;
            }
        } else {
            if (NumberUtils.compareInf(cfg.getMaxBytes(), forceMaxBytesPerDomain) < 0
                    || (job.isConfigurationSetsByteLimit() && NumberUtils.compareInf(cfg.getMaxBytes(),
                            forceMaxBytesPerDomain) != 0)) {
                log.debug("Job for HD #{} BYTE_LIMIT of config (domain,config={},{}) incompatible with current job", 
                        job.getOrigHarvestDefinitionID(), cfg.getDomainName(), cfg.getName());
                return false;
            }
        }

        long maxCountObjects = job.getMaxCountObjects();
        long minCountObjects = job.getMinCountObjects();

        assert (maxCountObjects >= minCountObjects) : "basic invariant";

        // The expected number of objects retrieved by this job from
        // the configuration based on historical harvest results.
        long expectation = cfg.getExpectedNumberOfObjects(forceMaxObjectsPerDomain, forceMaxBytesPerDomain);

        // Check if total count is exceeded
        long totalCountObjects = job.getTotalCountObjects();
        if ((totalCountObjects > 0) && ((expectation + totalCountObjects) > LIM_MAX_TOTAL_SIZE)) {
            log.debug("Job for HD #{} will exceed LIM_MAX_TOTAL_SIZE({}), if config(domain,config={},{}) with expected object count {} is added", 
                    job.getOrigHarvestDefinitionID(), LIM_MAX_TOTAL_SIZE, cfg.getDomainName(), cfg.getName(), expectation);
            return false;
        }

        // total count OK
        // Check if size within existing limits
        if ((expectation <= maxCountObjects) && (expectation >= minCountObjects)) {
            // total count ok and within current max and min
            return true;
        }

        // Outside current range we need to check the relative difference
        long absDiff;
        long xmaxCountObjects = maxCountObjects;
        long yminCountObjects = minCountObjects;

        // New max or new min ?
        if (expectation > maxCountObjects) {
            xmaxCountObjects = expectation;
        } else {
            assert (expectation < minCountObjects) : "New minimum expected";
            yminCountObjects = expectation;
        }

        absDiff = (xmaxCountObjects - yminCountObjects);

        if ((absDiff == 0) || (absDiff <= LIM_MIN_ABS_SIZE)) {
            return true; // difference too small to matter
        }

        if (yminCountObjects == 0) {
            yminCountObjects = 1; // make sure division succeeds
        }

        float relDiff = (float) xmaxCountObjects / (float) yminCountObjects;
        if (relDiff > LIM_MAX_REL_SIZE) {
            log.debug("Job for HD #{} will be incompatible with LIM_MAX_REL_SIZE({}), if config(domain,config={},{}) with relDiff {} is added", 
                    job.getOrigHarvestDefinitionID(), LIM_MAX_REL_SIZE, cfg.getDomainName(), cfg.getName(), relDiff);
            return false;
        }

        // all tests passed
        return true;
    }

    /** Only to be used by unittests. */
    public static void reset() {
        instance = null;
    }

}
