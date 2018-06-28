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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.Constants;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.FullHarvest;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDAO;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.datamodel.HarvestChannelDAO;
import dk.netarkivet.harvester.datamodel.HarvestDefinition;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.HeritrixTemplate;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.PartialHarvest;
import dk.netarkivet.harvester.datamodel.Schedule;
import dk.netarkivet.harvester.datamodel.SeedList;
import dk.netarkivet.harvester.datamodel.TemplateDAO;
import dk.netarkivet.harvester.datamodel.eav.EAV;

/**
 * A base class for {@link JobGenerator} implementations. It is recommended to extend this class to implement a new job
 * generator.
 * <p>
 * The base algorithm iterates over domain configurations within the harvest definition, and according to the
 * configuration ({@link HarvesterSettings#JOBGEN_DOMAIN_CONFIG_SUBSET_SIZE}, constitutes a subset of domain
 * configurations from which one or more jobs will be generated.
 */
abstract class AbstractJobGenerator implements JobGenerator {

    /** Logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(AbstractJobGenerator.class);

    /**
     * How many domain configurations to process in one go.
     */
    private final long DOMAIN_CONFIG_SUBSET_SIZE = Settings.getLong(HarvesterSettings.JOBGEN_DOMAIN_CONFIG_SUBSET_SIZE);

    /** Is deduplication enabled or disabled in the settings? */
    private final boolean DEDUPLICATION_ENABLED = Settings.getBoolean(HarvesterSettings.DEDUPLICATION_ENABLED);

    @Override
    public int generateJobs(HarvestDefinition harvest) {
        log.info("Generating jobs for harvestdefinition #{} using class '{}'", harvest.getOid(), this.getClass());
        int jobsMade = 0;
        final Iterator<DomainConfiguration> domainConfigurations = harvest.getDomainConfigurations();
        log.info("Now ready to iterate over the domainConfigurations for harvestdefinition #{}", harvest.getOid());
        harvest.setNumEvents(harvest.getNumEvents() + 1);
        if (harvest.isSnapShot()) {
            HarvestDefinitionDAO.getInstance().update(harvest);
        }
        while (domainConfigurations.hasNext()) {
            List<DomainConfiguration> subset = new ArrayList<DomainConfiguration>();
            while (domainConfigurations.hasNext() && subset.size() < DOMAIN_CONFIG_SUBSET_SIZE) {
                subset.add(domainConfigurations.next());
            }

            final Comparator<DomainConfiguration> domainConfigurationSubsetComparator = getDomainConfigurationSubsetComparator(
                    harvest);
            log.trace("Sorting domains with instance of " + domainConfigurationSubsetComparator.getClass().getName());
            Collections.sort(subset, domainConfigurationSubsetComparator);
            log.trace("{} domainconfigs now sorted and ready to processing for harvest #{}", subset.size(),
                    harvest.getOid());
            if (subset.size() == 0) {
                log.warn("Processing a domain config subset of zero size for HD #{}.", harvest.getOid());
            }
            jobsMade += processDomainConfigurationSubset(harvest, subset.iterator());
            if (jobsMade == 0) {
                log.warn("Created 0 jobs for HD #{} from domain cfg subset size {}.", harvest.getOid(), subset.size());
            } else {
                log.info("Now created {} jobs for HD #{} from domain cfg subset size {}.", jobsMade, harvest.getOid(), subset.size());
            }
        }

        if (!harvest.isSnapShot()) {
            PartialHarvest focused = (PartialHarvest) harvest;
            Schedule schedule = focused.getSchedule();
            int numEvents = harvest.getNumEvents();

            // Calculate next event
            Date now = new Date();
            Date nextEvent = schedule.getNextEvent(focused.getNextDate(), numEvents);

            // Refuse to schedule event in the past
            if (nextEvent != null && nextEvent.before(now)) {
                int eventsSkipped = 0;
                while (nextEvent != null && nextEvent.before(now)) {
                    nextEvent = schedule.getNextEvent(nextEvent, numEvents);
                    ++eventsSkipped;
                }
                log.warn("Refusing to schedule harvest definition '{}' in the past. Skipped {} events. "
                        + "Old nextDate was {} new nextDate is {}", harvest.getName(), eventsSkipped,
                        focused.getNextDate(), nextEvent);
            }

            // Set next event
            focused.setNextDate(nextEvent);
            if (log.isTraceEnabled()) {
                log.trace("Next event for harvest definition {} happens: {}", harvest.getName(),
                        (nextEvent == null ? "Never" : nextEvent.toString()));
            }
        }

        log.info("Finished generating {} jobs for harvestdefinition #{}", jobsMade, harvest.getOid());
        return jobsMade;
    }

    /**
     * Instantiates a new job.
     *
     * @param cfg the {@link DomainConfiguration} being processed
     * @param harvest the {@link HarvestDefinition} being processed
     * @return an instance of {@link Job}
     */
    public Job getNewJob(HarvestDefinition harvest, DomainConfiguration cfg) {
        HarvestChannelDAO harvestChannelDao = HarvestChannelDAO.getInstance();
        HarvestChannel channel = harvestChannelDao.getChannelForHarvestDefinition(harvest.getOid());
        if (channel == null) {
            log.info("No channel mapping registered for harvest id {}, will use default.", harvest.getOid());
            channel = harvestChannelDao.getDefaultChannel(harvest.isSnapShot());
        }
        HeritrixTemplate orderXMLdoc = loadOrderXMLdoc(cfg.getOrderXmlName());
        Job newJob;
        if (harvest.isSnapShot()) {
            newJob = new Job(harvest.getOid(), cfg, orderXMLdoc, channel, harvest.getMaxCountObjects(),
                    harvest.getMaxBytes(), ((FullHarvest) harvest).getMaxJobRunningTime(), harvest.getNumEvents());
        } else {
            newJob = new Job(harvest.getOid(), cfg, orderXMLdoc, channel, Constants.HERITRIX_MAXOBJECTS_INFINITY,
                    Constants.HERITRIX_MAXBYTES_INFINITY, Constants.HERITRIX_MAXJOBRUNNINGTIME_INFINITY,
                    harvest.getNumEvents());
        }

        return newJob;
    }

    /**
     * Returns a comparator used to sort the subset of {@link #DOMAIN_CONFIG_SUBSET_SIZE} configurations that are
     * scanned at each iteration.
     *
     * @param harvest the {@link HarvestDefinition} being processed.
     * @return a comparator
     */
    protected abstract Comparator<DomainConfiguration> getDomainConfigurationSubsetComparator(HarvestDefinition harvest);

    /**
     * Create new jobs from a collection of configurations. All configurations must use the same order.xml file.Jobs
     *
     * @param harvest the {@link HarvestDefinition} being processed.
     * @param domainConfSubset the configurations to use to create the jobs
     * @return The number of jobs created
     * @throws ArgumentNotValid if any of the parameters is null or if the cfglist does not contain any configurations
     */
    protected abstract int processDomainConfigurationSubset(HarvestDefinition harvest,
            Iterator<DomainConfiguration> domainConfSubset);

    @Override
    public boolean canAccept(Job job, DomainConfiguration cfg, DomainConfiguration previousCfg) {
        log.trace("Comparing current cfg {} with previous cfg {} when adding configs to HD #{}", cfg, previousCfg, job.getOrigHarvestDefinitionID());
        if (!checkAddDomainConfInvariant(job, cfg, previousCfg)) {
            log.debug("Unable to add incompatible config(domain,configname='{}','{}') to current job for HD #{}", cfg.getDomainName(), cfg.getName(), job.getOrigHarvestDefinitionID());
            return false;
        }
        boolean specificAcceptConditionsIsOk = checkSpecificAcceptConditions(job, cfg);
        if (!specificAcceptConditionsIsOk) {
            log.debug("Unable to add config(domain,configname='{}','{}') to current job for HD #{}. The specific accept conditions fail", cfg.getDomainName(), cfg.getName(), job.getOrigHarvestDefinitionID());
        }
        return specificAcceptConditionsIsOk;
    }
    
    /**
     * Called by {@link JobGenerator#canAccept(Job, DomainConfiguration, DomainConfiguration)}. Tests the implementation-specific conditions to accept
     * the given {@link DomainConfiguration} in the given {@link Job}. It is assumed that
     * {@link #checkAddDomainConfInvariant(Job, DomainConfiguration, DomainConfiguration)} has already passed.
     *
     * @param job the {@link Job} n=being built
     * @param cfg the {@link DomainConfiguration} to test
     * @return true if the configuration passes the conditions.
     */
    protected abstract boolean checkSpecificAcceptConditions(Job job, DomainConfiguration cfg);

    /**
     * Once the job has been filled with {@link DomainConfiguration}s, performs the following operations:
     * <ol>
     * <li>Edit the harvest template to add/remove deduplicator configuration.</li>
     * <li></li>
     * </ol>
     *
     * @param job the job
     */
    protected void editJobOrderXml(Job job) {
        HeritrixTemplate doc = job.getOrderXMLdoc();
        doc.enableOrDisableDeduplication(DEDUPLICATION_ENABLED);
    }

    /**
     * Tests that:
     * <ol>
     * <li>The given domain configuration and job are not null.</li>
     * <li>The EAV attributes are the same for the the given domain configuration and the previous one if any.
     * <li>The job does not already contain the given domain configuration.</li>
     * <li>The domain configuration has the same orderXmlName as the first inserted domain config.</li>
     * <li>If the previous configuration is not null, check that all attributes are identical between the two configurations</li>
     * </ol>
     *
     * @param job a given Job
     * @param cfg a given DomainConfiguration
     * @param previousCfg if not null, the domain configuration added to the job immediately prior to this one
     * @return true, if the given DomainConfiguration can be inserted into the given job
     */
    private boolean checkAddDomainConfInvariant(Job job, DomainConfiguration cfg, DomainConfiguration previousCfg) {
        ArgumentNotValid.checkNotNull(job, "job");
        ArgumentNotValid.checkNotNull(cfg, "cfg");

        if (previousCfg != null && EAV.compare(cfg.getAttributesAndTypes(), previousCfg.getAttributesAndTypes())!=0 ) {
            log.debug("Attributes have changed between configurations {} and {}",
                    DomainConfiguration.cfgToString(previousCfg), DomainConfiguration.cfgToString(cfg));
            return false;
        }

        // check if domain in DomainConfiguration cfg is not already in this job
        // domainName is used as key in domainConfigurationMap
        if (job.getDomainConfigurationMap().containsKey(cfg.getDomainName())) {
            log.debug("Job already has a configuration for Domain '{}'.", cfg.getDomainName());
            return false;
        }

        // check if template is same as this job.
        String orderXMLname = job.getOrderXMLName();
        if (!orderXMLname.equals(cfg.getOrderXmlName())) {
            log.debug("This Job only accept configurations using the harvest template '{}'."
                    + " This configuration uses the harvest template '{}'.", orderXMLname, cfg.getOrderXmlName());
            return false;
        }

        return true;
    }

    private HeritrixTemplate loadOrderXMLdoc(String orderXmlName) {
    	HeritrixTemplate orderXMLdoc = TemplateDAO.getInstance().read(orderXmlName);
        GlobalCrawlerTrapListDAO.getInstance().addGlobalCrawlerTraps(orderXMLdoc);
        return orderXMLdoc;
    }
    
    @Override
    public boolean ignoreConfiguration(DomainConfiguration cfg) {
        boolean noValidSeeds = true;
        Iterator<SeedList> lists = cfg.getSeedLists();
        while (noValidSeeds && lists.hasNext() ) {
            SeedList sList = lists.next();
            for (String seed: sList.getSeeds()) {
                String trimmedSeed = seed.trim(); // trim before testing
                if (trimmedSeed.length() > 0 && !trimmedSeed.startsWith("#")) { // Found a valid seed 
                    return false; // At least one valid seed in config. Don't ignore this config
                }
            }
        }
        return noValidSeeds;
    }

}
