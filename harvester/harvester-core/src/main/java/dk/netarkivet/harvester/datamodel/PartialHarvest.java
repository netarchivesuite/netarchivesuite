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
package dk.netarkivet.harvester.datamodel;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.jsp.PageContext;

import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antiaction.raptor.dao.AttributeBase;
import com.antiaction.raptor.dao.AttributeTypeBase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.DomainUtils;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.harvester.datamodel.dao.DAOProviderFactory;
import dk.netarkivet.harvester.datamodel.eav.EAV;
import dk.netarkivet.harvester.datamodel.eav.EAV.AttributeAndType;
import dk.netarkivet.harvester.webinterface.EventHarvestUtil;

/**
 * This class contains the specific properties and operations of harvest definitions which are not snapshot harvest
 * definitions. I.e. this class models definitions of event and selective harvests.
 */
public class PartialHarvest extends HarvestDefinition {

    private static final Logger log = LoggerFactory.getLogger(PartialHarvest.class);

    /**
     * Set of domain configurations being harvested by this harvest. Entries in this set are unique on configuration
     * name + domain name.
     */
    private Map<SparseDomainConfiguration, DomainConfiguration> domainConfigurations = new HashMap<SparseDomainConfiguration, DomainConfiguration>();

    /** The schedule used by this PartialHarvest. */
    private Schedule schedule;

    /**
     * The next date this harvest definition should run, null if never again.
     */
    private Date nextDate;

    /**
     * Create new instance of a PartialHavest configured according to the properties of the supplied
     * DomainConfiguration.
     *
     * @param domainConfigurations a list of domain configurations
     * @param schedule the harvest definition schedule
     * @param harvestDefName the name of the harvest definition
     * @param comments comments
     * @param audience The intended audience for this harvest (could be null)
     */
    public PartialHarvest(List<DomainConfiguration> domainConfigurations, Schedule schedule, String harvestDefName,
            String comments, String audience) {
        super(DAOProviderFactory.getExtendedFieldDAOProvider());
        ArgumentNotValid.checkNotNull(schedule, "schedule");
        ScheduleDAO.getInstance().read(schedule.getName());

        ArgumentNotValid.checkNotNullOrEmpty(harvestDefName, "harvestDefName");
        ArgumentNotValid.checkNotNull(comments, "comments");
        ArgumentNotValid.checkNotNull(domainConfigurations, "domainConfigurations");

        this.numEvents = 0;
        addConfigurations(domainConfigurations);
        this.schedule = schedule;
        this.harvestDefName = harvestDefName;
        this.comments = comments;
        this.nextDate = schedule.getFirstEvent(new Date());
        this.audience = audience;
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
     * @return The next date the harvest definition should be run or null, if the harvest definition should never run
     * again.
     */
    public Date getNextDate() {
        return nextDate;
    }

    /**
     * Set the next date this harvest definition should be run.
     *
     * @param nextDate The next date the harvest definition should be run. May be null, meaning never again.
     */
    public void setNextDate(Date nextDate) {
        this.nextDate = nextDate;
    }

    /**
     * Remove domainconfiguration from this partialHarvest.
     *
     * @param dcKey domainConfiguration key
     */
    public void removeDomainConfiguration(SparseDomainConfiguration dcKey) {
        ArgumentNotValid.checkNotNull(dcKey, "DomainConfigurationKey dcKey");
        if (domainConfigurations.remove(dcKey) == null) {
            log.warn("Unable to delete domainConfiguration '{}' from {}. Reason: didn't exist.", dcKey, this);
        }
    }

    /**
     * Add a new domainconfiguration to this PartialHarvest.
     *
     * @param newConfiguration A new DomainConfiguration
     */
    public void addDomainConfiguration(DomainConfiguration newConfiguration) {
        ArgumentNotValid.checkNotNull(newConfiguration, "DomainConfiguration newConfiguration");
        SparseDomainConfiguration key = new SparseDomainConfiguration(newConfiguration);
        if (domainConfigurations.containsKey(key)) {
            log.warn("Unable to add domainConfiguration '{}' from {}. Reason: does already exist.", newConfiguration,
                    this);
        } else {
            domainConfigurations.put(key, newConfiguration);
        }
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
     * @return the domainconfigurations as a list
     */
    public Collection<DomainConfiguration> getDomainConfigurationsAsList() {
        return domainConfigurations.values();
    }

    /**
     * Set the list of configurations that this PartialHarvest uses.
     *
     * @param configs List<DomainConfiguration> the configurations that this harvestdefinition will use.
     */
    public void setDomainConfigurations(List<DomainConfiguration> configs) {
        ArgumentNotValid.checkNotNull(configs, "configs");

        domainConfigurations.clear();
        addConfigurations(configs);
    }

    /**
     * Add the list of configurations to the configuration associated with this PartialHarvest.
     *
     * @param configs a List of configurations
     */
    private void addConfigurations(List<DomainConfiguration> configs) {
        for (DomainConfiguration dc : configs) {
            addConfiguration(dc);
        }
    }

    /**
     * Add a configuration to this PartialHarvest.
     *
     * @param dc the given configuration
     */
    private void addConfiguration(DomainConfiguration dc) {
        domainConfigurations.put(new SparseDomainConfiguration(dc), dc);
    }

    /**
     * Reset the harvest definition to no harvests and next date being the first possible for the schedule.
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
     *
     * @return 0, meaning no limit.
     */
    public long getMaxCountObjects() {
        return Constants.HERITRIX_MAXOBJECTS_INFINITY;
    }

    /**
     * Always returns no limit.
     *
     * @return -1, meaning no limit.
     */
    public long getMaxBytes() {
        return Constants.HERITRIX_MAXBYTES_INFINITY;
    }

    /**
     * Takes a seed list and creates any necessary domains, configurations, and seedlists to enable them to be harvested
     * with the given template and other parameters. <A href="https://sbforge.org/jira/browse/NAS-1317">JIRA issue
     * NAS-1317</A> addresses this issue. Current naming of the seedlists and domainconfigurations are: one of <br>
     * harvestdefinitionname + "_" + templateName + "_" + "UnlimitedBytes" (if maxbytes is negative)<br>
     * harvestdefinitionname + "_" + templateName + "_" + maxBytes + "Bytes" (if maxbytes is zero or postive).
     *
     * @param seeds a list of the seeds to be added
     * @param templateName the name of the template to be used
     * @param maxBytes Maximum number of bytes to harvest per domain
     * @param maxObjects Maximum number of objects to harvest per domain
     * @param attributeValues  Attributes read from webpage
     * @see EventHarvestUtil#addConfigurations(PageContext, I18n, String) for details
     * @return the list of invalid seeds found during this process.
     */
    public Set<String> addSeeds(Set<String> seeds, String templateName, long maxBytes, int maxObjects, Map<String, String> attributeValues) {
        ArgumentNotValid.checkNotNull(seeds, "seeds");
        ArgumentNotValid.checkNotNullOrEmpty(templateName, "templateName");
        if (!TemplateDAO.getInstance().exists(templateName)) {
            throw new UnknownID("No such template: " + templateName);
        }
        Set<String> invalidSeeds = new HashSet<String>();
        Map<String, Set<String>> acceptedSeeds = new HashMap<String, Set<String>>();
        
        for (String seed : seeds) {
            boolean seedValid = processSeed(seed, acceptedSeeds);
            if (!seedValid) {
                invalidSeeds.add(seed);
            }
        }

        if (invalidSeeds.size() > 0) {
            log.warn("Found the following invalid seeds:" + StringUtils.join(invalidSeeds, ","));
        }

        addSeedsToDomain(templateName, maxBytes, maxObjects, acceptedSeeds, attributeValues);
        return invalidSeeds;
    }

    /**
     * This method is a duplicate of the addSeeds method but for seedsFile parameter
     *
     * @param seedsFile a newline-separated File containing the seeds to be added
     * @param templateName the name of the template to be used
     * @param maxBytes Maximum number of bytes to harvest per domain
     * @param maxObjects Maximum number of objects to harvest per domain
     */
    public Set<String> addSeedsFromFile(File seedsFile, String templateName, long maxBytes, int maxObjects, Map<String,String> attributeValues) {
        ArgumentNotValid.checkNotNull(seedsFile, "seeds");
        ArgumentNotValid.checkTrue(seedsFile.isFile(), "seedsFile does not exist");
        ArgumentNotValid.checkNotNullOrEmpty(templateName, "templateName");
        if (!TemplateDAO.getInstance().exists(templateName)) {
            throw new UnknownID("No such template: " + templateName);
        }
        Set<String> invalidSeeds = new HashSet<String>();
        Map<String, Set<String>> acceptedSeeds = new HashMap<String, Set<String>>();
        
        // validate all the seeds in the file
        // those accepted are entered into the acceptedSeeds datastructure

        // Iterate through the contents of the file
        LineIterator seedIterator = null;
        try {
            seedIterator = new LineIterator(new FileReader(seedsFile));
            while (seedIterator.hasNext()) {
                String seed = seedIterator.next();
                boolean seedValid = processSeed(seed, acceptedSeeds);
                if (!seedValid) {
                    invalidSeeds.add(seed);
                }
            }
        } catch (IOException e) {
            throw new IOFailure("Unable to process seedsfile ", e);
        } finally {
            LineIterator.closeQuietly(seedIterator);
        }
        
        if (invalidSeeds.size() > 0) {
            log.warn("Found the following invalid seeds:" + StringUtils.join(invalidSeeds, ","));
        }
        
        addSeedsToDomain(templateName, maxBytes, maxObjects, acceptedSeeds, attributeValues);
        return invalidSeeds;
    }

    /**
     * Process each seed.
     *
     * @param seed The given seed.
     * @param acceptedSeeds The set of accepted seeds
     * @return true, if the processed seed is valid or empty.
     */
    private boolean processSeed(String seed, Map<String, Set<String>> acceptedSeeds) {
        seed = seed.trim();
        if (seed.length() != 0 && !seed.startsWith("#") && !seed.startsWith("//")) { // ignore empty lines and comments
            
            if (!(seed.toLowerCase().startsWith("http://") || seed.toLowerCase().startsWith("https://"))) {
                seed = "http://" + seed;
            }
            URL url = null;
            try {
                url = new URL(seed);
            } catch (MalformedURLException e) {
                return false;
            }
            String host = url.getHost();
            String domainName = DomainUtils.domainNameFromHostname(host);
            if (domainName == null) {
                return false;
            }

            Set<String> seedsForDomain = acceptedSeeds.get(domainName);
            if (seedsForDomain == null) {
                seedsForDomain = new HashSet<String>();
                acceptedSeeds.put(domainName, seedsForDomain);
            }
            seedsForDomain.add(seed);
        }
        return true;
    }

    /**
     * Generate domain configurations for the accepted seeds.
     *
     * @param templateName The Heritrix template to be used.
     * @param maxBytes The number of max bytes allowed
     * @param maxObjects The number of max objected allowed
     * @param acceptedSeeds The set of accepted seeds
     */
    private void addSeedsToDomain(String templateName, long maxBytes, int maxObjects,
            Map<String, Set<String>> acceptedSeeds, Map<String, String> attributeValues) {
        // Generate components for the name for the configuration and seedlist
        final String maxbytesSuffix = "Bytes";
        String maxBytesS = "Unlimited" + maxbytesSuffix;
        if (maxBytes >= 0) {
            maxBytesS = Long.toString(maxBytes);
            maxBytesS = maxBytesS + maxbytesSuffix;
        }

        final String maxobjectsSuffix = "Objects";
        String maxObjectsS = "Unlimited" + maxobjectsSuffix;
        if (maxObjects >= 0) {
            maxObjectsS = Long.toString(maxObjects);
            maxObjectsS = maxObjectsS + maxobjectsSuffix;
        }

        String name = harvestDefName + "_" + templateName + "_" + maxBytesS + "_" + maxObjectsS;

        Set<DomainConfiguration> newDcs = new HashSet<DomainConfiguration>();
        for (Map.Entry<String, Set<String>> entry : acceptedSeeds.entrySet()) {
            String domainName = entry.getKey();
            Domain domain;
            List<SeedList> seedListList = new ArrayList<SeedList>();
            SeedList seedlist;
            // Find or create the domain
            if (DomainDAO.getInstance().exists(domainName)) {
                domain = DomainDAO.getInstance().read(domainName);
                
                // If a config with this name exists already for the dommain, add a "_" + timestamp to the end of the name to be make it unique.
                // This will probably happen rarely.
                // This name is used for both the configuration and corresponding seed
                if (domain.hasConfiguration(name)) {
                    String oldName = name;
                    name = name + "_" + System.currentTimeMillis();
                    log.info("configuration '{}' for domain '{}' already exists. Change name for config and corresponding seed to ", 
                            oldName, name, domain.getName());
                }
                seedlist =  new SeedList(name, ""); // Assure that the seedname is the same as the configname.
                seedListList.add(seedlist);
                domain.addSeedList(seedlist);
                                
            } else {
                seedlist =  new SeedList(name, ""); // Assure that the seedname is the same as the configname.
                seedListList.add(seedlist);
                log.info("Creating domain {} in DomainDAO", domainName);
                domain = Domain.getDefaultDomain(domainName);
                domain.addSeedList(seedlist);
                DomainDAO.getInstance().create(domain);
            }
            
            DomainConfiguration dc = new DomainConfiguration(name, domain, seedListList, new ArrayList<Password>());
            dc.setOrderXmlName(templateName);
            dc.setMaxBytes(maxBytes);
            dc.setMaxObjects(maxObjects);
            domain.addConfiguration(dc);
            log.info("Adding seeds til new configuration '{}' (id={}) for domain '{}' ", name, dc.getID(), domain.getName());


            // Find the SeedList and add this seed to it
            seedlist = domain.getSeedList(name);
            List<String> currentSeeds = seedlist.getSeeds();
            entry.getValue().addAll(currentSeeds);

            List<String> allSeeds = new ArrayList<String>();

            allSeeds.addAll(entry.getValue());
            domain.updateSeedList(new SeedList(name, allSeeds));

            // Add the configuration to the list of new configs for
            // this harvest.
            newDcs.add(dc);
            DomainDAO.getInstance().update(domain);
            log.info("Created configuration '{}' for domain {} with ID {}", dc.getName(), dc.getDomainName(), dc.getID());
            saveAttributes(dc, attributeValues);
        }

        boolean thisInDAO = HarvestDefinitionDAO.getInstance().exists(this.harvestDefName);
        if (thisInDAO) { // We have previously created this harvestdefinition in the HarvestDefinitionDAO.
            HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
            for (DomainConfiguration dc : newDcs) {
                addConfiguration(dc);
                hddao.addDomainConfiguration(this, new SparseDomainConfiguration(dc));
            }
            hddao.update(this);
        } else { // not yet created in the HarvestDefinitionDAO
            for (DomainConfiguration dc : newDcs) {
                addConfiguration(dc);
            }
            HarvestDefinitionDAO.getInstance().create(this);
        }
    }

    private void saveAttributes(DomainConfiguration dc, Map<String, String> attributeValues) {
        if (dc.getID() == null) {
             log.warn("Attributes not saved to database. Id of domainConfiguration not yet available");
             return;
        }
        // EAV
        try {
            long entity_id = dc.getID();
            log.info("Saving attributes for domain config id {} and name {} and domain {}", entity_id, dc.getName(), dc.getDomainName());
            EAV eav = EAV.getInstance();
            List<AttributeAndType> attributeTypes = eav.getAttributesAndTypes(EAV.DOMAIN_TREE_ID, (int)entity_id);
            log.debug("3 attributes available for entity {}", entity_id);
            AttributeAndType attributeAndType;
            AttributeTypeBase attributeType;
            AttributeBase attribute;
            for (int i=0; i<attributeTypes.size(); ++i) {
                attributeAndType = attributeTypes.get(i);
                attributeType = attributeAndType.attributeType;
                log.debug("Examining attribute {}",attributeType.name);
                attribute = attributeAndType.attribute;
                if (attribute == null) {
                    attribute = attributeType.instanceOf();
                    attribute.entity_id = (int)entity_id;
                }
                switch (attributeType.viewtype) {
                case 1:
                    String paramValue = attributeValues.get(attributeType.name);
                    int intValue;
                    if (paramValue != null) {
                      intValue = Integer.decode(paramValue);
                    } else {
                      intValue = attributeType.def_int;
                    }
                    log.info("Setting attribute {} to value {}", attributeType.name, intValue);
                    attribute.setInteger(intValue);
                    break;
                case 5:
                case 6:
                    paramValue = attributeValues.get(attributeType.name);
                    int intVal = 0;
                    if (paramValue != null && !"0".equals(paramValue)) {
                        intVal = 1;
                    } 
                    log.debug("Set intVal = 1 for attribute {} when receiving paramValue={}", attributeType.name, paramValue);
                    attribute.setInteger(intVal);
                    break;
                }
                eav.saveAttribute(attribute);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Unable to store EAV data!", e);
        }
    }
}
