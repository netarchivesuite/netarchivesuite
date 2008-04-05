/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.harvester.datamodel;

import javax.servlet.jsp.PageContext;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.harvester.webinterface.EventHarvest;


/**
 * This class contains the specific properties and operations
 * of harvest definitions which are not snapshot harvest definitions.
 * I.e. this class models definitions of event and selective harvests.
 *
 */
public class PartialHarvest extends HarvestDefinition {
    private final Log log = LogFactory.getLog(getClass());

    /** A local class that ensure that the list of configurations contains
     * no duplicates.  It provides an equals that makes configurations equal
     * on <domainname, configname> tuple.
     */
    private static class ConfigKey {
        final String domainName;
        final String configName;
        ConfigKey(DomainConfiguration dc) {
            this.domainName = dc.getDomain().getName();
            this.configName = dc.getName();
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ConfigKey)) return false;

            final ConfigKey configKey = (ConfigKey) o;

            if (!configName.equals(configKey.configName)) return false;
            if (!domainName.equals(configKey.domainName)) return false;

            return true;
        }

        public int hashCode() {
            int result;
            result = domainName.hashCode();
            result = 29 * result + configName.hashCode();
            return result;
        }
    }
    /** Set of domain configurations being harvested by this harvest.
     * Entries in this set are unique on configuration name + domain name.
     */
    private Map<ConfigKey, DomainConfiguration> domainConfigurations
            = new HashMap<ConfigKey, DomainConfiguration>();

    /** The schedule used by this PartialHarvest. */
    private Schedule schedule;

    /**
     * The next date this harvest definition should run, null if never again.
     */
    private Date nextDate;

    /**
     * Create new instance of a PartialHavest configured according
     * to the properties of the supplied DomainConfiguration.
     *
     * @param domainConfigurations a list of domain configurations
     * @param schedule             the harvest definition schedule
     * @param harvestDefName       the name of the harvest definition
     * @param comments             comments
     */
    public PartialHarvest(List<DomainConfiguration> domainConfigurations,
                          Schedule schedule,
                          String harvestDefName,
                          String comments) {

        ArgumentNotValid.checkNotNull(schedule, "schedule");
        ScheduleDAO.getInstance().read(schedule.getName());

        ArgumentNotValid.checkNotNullOrEmpty(harvestDefName, "harvestDefName");
        ArgumentNotValid.checkNotNull(comments, "comments");
        ArgumentNotValid.checkNotNull(domainConfigurations,
                "domainConfigurations");

        this.numEvents = 0;
        addConfigurations(domainConfigurations);
        this.schedule = schedule;
        this.harvestDefName = harvestDefName;
        this.comments = comments;
        this.nextDate = schedule.getFirstEvent(new Date());
    }

    /**
     * Generates jobs in files from this harvest definition, and updates the
     * schedule for when the harvest definition should happen next time.
     *
     * Create Jobs from the domainconfigurations in this harvestdefinition
     * and the current value of the limits in Settings.
     * Multiple jobs are generated if different order.xml-templates are used,
     * or if the size of the job is inappropriate.
     *
     * The following settings are used:
     * {@link Settings#JOBS_MAX_RELATIVE_SIZE_DIFFERENCE}:
     * The maximum relative difference between the smallest and largest
     * number of objects expected in a job
     * <p/>
     * {@link Settings#JOBS_MIN_ABSOLUTE_SIZE_DIFFERENCE}: 
     * Size differences below this threshold are ignored even if
     * the relative difference exceeds {@link Settings#JOBS_MAX_RELATIVE_SIZE_DIFFERENCE}
     * <p/>
     * {@link Settings#JOBS_MAX_TOTAL_JOBSIZE}:
     * The upper limit on the total number of objects that a job may
     * retrieve
     *
     * Also updates the harvest definition to schedule the next event using
     * the defined schedule. Will skip events if the next event would be in the
     * past when using the schedule definition.
     *
     * @return Number of jobs created
     */
    int createJobs() {
        //Generate jobs
        int jobsMade = super.createJobs();

        //Calculate next event
        Date now = new Date();
        Date nextEvent = schedule.getNextEvent(getNextDate(), getNumEvents());

        //Refuse to schedule event in the past
        if (nextEvent != null && nextEvent.before(now)) {
            int eventsSkipped = 0;
            while (nextEvent != null && nextEvent.before(now)) {
                nextEvent = schedule.getNextEvent(nextEvent, getNumEvents());
                eventsSkipped++;
            }
            log.warn("Refusing to schedule harvest definition '"
                        + getName() + "' in the past. Skipped "
                        + eventsSkipped + " events. Old nextDate was "
                        + nextDate
                        + " new nextDate is " + nextEvent);
        }

        //Set next event
        setNextDate(nextEvent);
        log.trace("Next event for harvest definition " + getName()
                  + " happens: "
                  + (nextEvent == null ? "Never" : nextEvent.toString()));

        return jobsMade;
    }

    /**
     * Get a new Job suited for this type of HarvestDefinition.
     *
     * @param cfg The configuration to use when creating the job
     * @return a new job
     */
    protected Job getNewJob(DomainConfiguration cfg) {
        return Job.createJob(getOid(), cfg, numEvents);
    }

    /**
     * Returns the schedule defined for this harvest definition.
     *
     * @return schedule
     */
    public Schedule getSchedule() {
        return schedule;
    }

    /**
     * Set the schedule to be used for this harvestdefinition.
     *
     * @param schedule A schedule for when to try harvesting.
     */
    public void setSchedule(Schedule schedule) {
        ArgumentNotValid.checkNotNull(schedule, "schedule");
        this.schedule = schedule;
        if (nextDate != null) {
            setNextDate(schedule.getFirstEvent(nextDate));
        }
    }

    /**
     * Get the next date this harvest definition should be run.
     *
     * @return The next date the harvest definition should be run or null, if
     *         the harvest definition should never run again.
     */
    public Date getNextDate() {
        return nextDate;
    }

    /**
     * Set the next date this harvest definition should be run.
     *
     * @param nextDate The next date the harvest definition should be run.
     *                 May be null, meaning never again.
     */
    public void setNextDate(Date nextDate) {
        this.nextDate = nextDate;
    }

    /**
     * Returns a List of domain configurations for this harvest definition.
     *
     * @return List containing information about the domain configurations
     */
    public Iterator<DomainConfiguration> getDomainConfigurations() {
        return domainConfigurations.values().iterator();
    }

    /**
     * Set the list of configurations that this hd uses.
     *
     * @param configs List<DomainConfiguration> the configurations that this
     *                harvestdefinition will use.
     */
    public void setDomainConfigurations(List<DomainConfiguration> configs) {
        ArgumentNotValid.checkNotNull(configs, "configs");

        domainConfigurations.clear();
        addConfigurations(configs);
    }

    private void addConfigurations(List<DomainConfiguration> configs) {
        for (DomainConfiguration dc : configs) {
            addConfiguration(dc);
        }
    }

    private void addConfiguration(DomainConfiguration dc) {
        domainConfigurations.put(new ConfigKey(dc), dc);
    }

    /**
     * Reset the harvest definition to no harvests and next date being the
     * first possible for the schedule.
     */
    public void reset() {
        numEvents = 0;
        nextDate = schedule.getFirstEvent(new Date());
    }

    /**
     * Check if this harvest definition should be run, given the time now.
     *
     * @param now The current time
     * @return true if harvest definition should be run
     */
    public boolean runNow(Date now) {
        ArgumentNotValid.checkNotNull(now, "now");
        if (!getActive()) {
            return false; // inactive definitions are never run
        }
        return nextDate != null && now.compareTo(nextDate) >= 0;
    }

    /**
     * Returns whether this HarvestDefinition represents a snapshot harvest.
     *
     * @return false (always)
     */
    public boolean isSnapShot() {
        return false;
    }

    /**
     * Always returns no limit.
     * @return 0, meaning no limit.
     */
    protected long getMaxCountObjects() {
        return Constants.HERITRIX_MAXOBJECTS_INFINITY;
    }

    /** Always returns no limit.
     * @return -1, meaning no limit.
     */
    protected long getMaxBytes() {
        return Constants.HERITRIX_MAXBYTES_INFINITY;
    }

    /**
     * Takes a seed list and creates any necessary domains, configurations, and
     * seedlists to enable them to be harvested with the given template and
     *  other parameters.
     * @see EventHarvest#addConfigurations(PageContext,I18n,PartialHarvest) for details
     * @param seeds a newline-separated list of the seeds to be added
     * @param templateName the name of the template to be used
     * @param maxLoad the maximum load. If <0, the default is used
     * @param maxObjects the maximum number of objects per domain. If <0,
     *      the default is used
     */
    public void addSeeds(String seeds, String templateName, long maxLoad, long maxObjects) {
        ArgumentNotValid.checkNotNullOrEmpty(seeds, "seeds");
        ArgumentNotValid.checkNotNullOrEmpty(templateName, "templateName");
        if (!TemplateDAO.getInstance().exists(templateName)) {
            throw new UnknownID("No such template: " + templateName);
        }
        // Generate components for the name for the configuration and seedlist
        String maxLoadS = "";
        if (maxLoad >= 0) {
            maxLoadS += maxLoad;
        }
        String maxObjectsS = "";
        if (maxObjects >= 0) {
            maxObjectsS += maxObjects;
        }
        String name = harvestDefName + "_" + templateName + "_"
                      + maxLoadS + "_" + maxObjectsS;

        // Note: Matches any sort of newline (unix/mac/dos), but won't get empty
        // lines, which is fine for this purpose
        String[] seedArray = seeds.split("[\n\r]+");
        Map<String, Set<String>> acceptedSeeds
                = new HashMap<String, Set<String>>();
        StringBuilder invalidMessage =
                new StringBuilder("Unable to create an event harvest.\n"
                                  + "The following seeds are invalid:\n");
        boolean valid = true;
        //validate:

        for (String seed: seedArray) {
            seed = seed.trim();
            if (seed.length() != 0) {
                if (!(seed.startsWith("http://")
                      || seed.startsWith("https://"))) {
                    seed = "http://" + seed;
                }
                URL url = null;
                try {
                    url = new URL(seed);
                } catch (MalformedURLException e) {
                    valid = false;
                    invalidMessage.append(seed);
                    invalidMessage.append('\n');
                    continue;
                }
                String host = url.getHost();
                String domainName = Domain.domainNameFromHostname(host);
                if (domainName == null) {
                    valid = false;
                    invalidMessage.append(seed);
                    invalidMessage.append('\n');
                    continue;
                }

                Set<String> seedsForDomain = acceptedSeeds.get(domainName);
                if (seedsForDomain == null) {
                    seedsForDomain = new HashSet<String>();
                }
                seedsForDomain.add(seed);
                acceptedSeeds.put(domainName, seedsForDomain);
            }
        }

        if (!valid) {
            throw new ArgumentNotValid(invalidMessage.toString());
        }

        for (Map.Entry<String, Set<String>> entry : acceptedSeeds.entrySet()) {
            String domainName = entry.getKey();
            Domain domain;

            // Need a seedlist to include in the configuration when we
            // create it. This will be replaced lated.
            SeedList seedlist = new SeedList(name, "");
            List<SeedList> seedListList = new ArrayList<SeedList>();
            seedListList.add(seedlist);

            //Find or create the domain
            if (DomainDAO.getInstance().exists(domainName)) {
                domain = DomainDAO.getInstance().read(domainName);
                if (!domain.hasSeedList(name)) {
                    domain.addSeedList(seedlist);
                }
            } else {
                domain = Domain.getDefaultDomain(domainName);
                domain.addSeedList(seedlist);
                DomainDAO.getInstance().create(domain);
            }
            //Find or create the DomainConfiguration
            DomainConfiguration dc = null;
            if (domain.hasConfiguration(name)) {
                dc = domain.getConfiguration(name);
            } else {
                dc = new DomainConfiguration(name, domain, seedListList,
                                             new ArrayList<Password>());
                dc.setOrderXmlName(templateName);
                if (maxLoad >= 0) {
                    dc.setMaxRequestRate((int) maxLoad);
                }
                if (maxObjects >= 0) {
                    dc.setMaxObjects((int) maxObjects);
                }
                domain.addConfiguration(dc);
            }

            //Find the SeedList and add this seed to it
            seedlist = domain.getSeedList(name);
            List<String> currentSeeds = seedlist.getSeeds();
            entry.getValue().addAll(currentSeeds);

            List<String> allSeeds = new ArrayList<String>();

            allSeeds.addAll(entry.getValue());
            domain.updateSeedList(new SeedList(name, allSeeds));


            //Add the configuration to the harvest config set.
            addConfiguration(dc);
            DomainDAO.getInstance().update(domain);
        }

        HarvestDefinition thisInDAO =
                HarvestDefinitionDAO.getInstance().
                        getHarvestDefinition(this.harvestDefName);
        if (thisInDAO == null) {
            HarvestDefinitionDAO.getInstance().create(this);
        } else {
            HarvestDefinitionDAO.getInstance().update(this);
        }
    }

}
