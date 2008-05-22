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

import java.util.Date;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FilterIterator;

/**
 * This class contains the specific
 * properties and operations of snapshot harvest definitions.
 *
 */
public class FullHarvest extends HarvestDefinition {
    private final Log log = LogFactory.getLog(getClass());

    /**
     * The maximum number of objects retrieved from each domain
     * during a snapshot harvest.
     */
    private long maxCountObjects;

    /**
     * The maximum number of bytes retrieved from each domain during a
     * snapshot harvest.
     */
    private long maxBytes;

    /** The ID for the harvestdefinition, this FullHarvest is
     *  based upon.
     */
    private Long previousHarvestDefinitionOid;

    /**
     * Create new instance of FullHarvest configured according
     * to the properties of the supplied DomainConfiguration.
     *
     * @param harvestDefName the name of the harvest definition
     * @param comments       comments
     * @param previousHarvestDefinitionOid This harvestDefinition is used to
     * create this Fullharvest definition.
     * @param maxCountObjects Limit for how many objects can be
     * @param maxBytes
     */
    public FullHarvest(String harvestDefName,
                       String comments,
                       Long previousHarvestDefinitionOid,
                       long maxCountObjects, long maxBytes) {
        ArgumentNotValid.checkNotNullOrEmpty(harvestDefName, "harvestDefName");
        ArgumentNotValid.checkNotNull(comments, "comments");

        this.previousHarvestDefinitionOid = previousHarvestDefinitionOid;
        this.harvestDefName = harvestDefName;
        this.comments = comments;
        this.maxCountObjects = maxCountObjects;
        this.numEvents = 0;
        this.maxBytes = maxBytes;
    }

    /**
     * Get a new Job suited for this type of HarvestDefinition.
     *
     * @param cfg The configuration to use when creating the job
     * @return a new job
     */
    protected Job getNewJob(DomainConfiguration cfg) {
        return Job.createSnapShotJob(getOid(), cfg, this.getMaxCountObjects(),
                this.getMaxBytes(), numEvents);
    }

    /**
     * Get the previous HarvestDefinition which is used to base this.
     *
     * @return The previous HarvestDefinition
     */
    public HarvestDefinition getPreviousHarvestDefinition() {
        if (previousHarvestDefinitionOid != null) {
            return HarvestDefinitionDAO.getInstance().read(
                    previousHarvestDefinitionOid);
        }
        return null;
    }

    /** Set the previous HarvestDefinition which is used to base this.
     *  @param prev
     */
    public void setPreviousHarvestDefinition(Long prev) {
        previousHarvestDefinitionOid = prev;
    }

    /**
     * @return Returns the maxCountObjects.
     */
    public long getMaxCountObjects() {
        return maxCountObjects;
    }

    /**
     * @param maxCountObjects The maxCountObjects to set.
     */
    public void setMaxCountObjects(long maxCountObjects) {
        this.maxCountObjects = maxCountObjects;
    }

    /** Get the maximum number of bytes that this fullharvest will harvest
     * per domain, 0 for no limit.
     * @return Total download limit in bytes per domain.
     */
    public long getMaxBytes() {
        return maxBytes;
    }

    /** Set the limit for how many bytes this fullharvest will harvest per
     * domain, or -1 for no limit.
     *
     * @param maxBytes Number of bytes to stop harvesting at.
     */
    public void setMaxBytes(long maxBytes) {
        this.maxBytes = maxBytes;
    }

    /**
     * Returns an iterator of domain configurations for this harvest definition.
     *
     * @return Iterator containing information about the domain configurations
     */
    public Iterator<DomainConfiguration> getDomainConfigurations() {
        if (previousHarvestDefinitionOid == null) {
            //The first snapshot harvest
            HarvestDefinitionDAO hdDao = HarvestDefinitionDAO.getInstance();
            return hdDao.getSnapShotConfigurations();
        }

        //An iterative snapshop harvest
        final DomainDAO dao = DomainDAO.getInstance();
        //Get what has been harvested
        Iterator<HarvestInfo> i =
            dao.getHarvestInfoBasedOnPreviousHarvestDefinition(
                getPreviousHarvestDefinition());
        // Filter out HarvestInfo objects for domains that either
        // 1) are completed
        // 2) have reached their maxBytes limit
        //   (and the maxBytes limit has not changed since time of harvest)
        // 3) that are current aliases of another domain
        //
        // and get domain configurations for the rest.
        return new FilterIterator<HarvestInfo,DomainConfiguration>(i) {
            protected DomainConfiguration filter(HarvestInfo harvestInfo) {
                if (harvestInfo.getStopReason()
                     == StopReason.DOWNLOAD_COMPLETE) {
                    // Don't include the ones that finished.
                    return null;
                }

                DomainConfiguration config
                = getConfigurationFromPreviousHarvest(harvestInfo, dao);
                if (harvestInfo.getStopReason()
                        == StopReason.CONFIG_SIZE_LIMIT) {
                    // Check if MaxBytes limit for DomainConfiguration have been raised
                    // since previous harvest.  If this is the case, return the configuration
                    int compare = NumberUtils.compareInf(config.getMaxBytes(),
                            harvestInfo.getSizeDataRetrieved());
                    if (compare < 1){
                        return null;
                    } else {
                        return config;
                    }
                }

                if (config.getDomain().getAliasInfo() != null
                    && !config.getDomain().getAliasInfo().isExpired()) {
                    //Don't include aliases
                    return null;
                } else {
                    return config;
                }
            }
        };
    }


    /** Get the configuration used in a previous harvest.  If the
     * configuration in the harvestinfo cannot be found (deleted), uses
     * the default configuration.
     *
     * @param harvestInfo A harvest info object from a previous harvest.
     * @param dao The dao to read configurations from.
     * @return A configuration if found and the download in this harvestinfo
     * was complete, null otherwise
     */
    private DomainConfiguration getConfigurationFromPreviousHarvest
            (final HarvestInfo harvestInfo, DomainDAO dao) {
        //For each bit of harvest info that did not complete
        try {
            Domain domain = dao.read(harvestInfo.getDomainName());
            //Read the domain
            DomainConfiguration configuration;
            //Read the configuration
            try {
                configuration = domain.getConfiguration(
                        harvestInfo.getDomainConfigurationName());
            } catch (UnknownID e) {
                //If the old configuration cannot be found, fall
                //back on default configuration
                configuration = domain.getDefaultConfiguration();
                log.debug("Previous configuration '"
                        + harvestInfo.getDomainConfigurationName()
                        + "' for harvesting domain '"
                        + harvestInfo.getDomainName()
                        + "' not found. Using default '"
                        + configuration.getName()
                        + "' instead.", e);
            }
            //Add the configuration to the list to harvest
            return configuration;
        } catch (UnknownID e) {
            //If the domain doesn't exist, warn
            log.debug("Previously harvested domain '"
                      + harvestInfo.getDomainName()
                      + "' no longer exists. Ignoring this domain.",
                      e);
        } catch (IOFailure e) {
            //If the domain can't be read, warn
            log.debug("Previously harvested domain '"
                      + harvestInfo.getDomainName()
                      + "' can't be read. Ignoring this domain.",
                      e);
        }
        return null;
    }

    /**
     * Check if this harvest definition should be run, given the time now.
     *
     * @param now The current time
     * @return true if harvest definition should be run
     */
    public boolean runNow(Date now) {
        return getActive() && (numEvents < 1);
    }

    /**
     * Returns whether this HarvestDefinition represents a snapshot harvest.
     *
     * @return Returns true
     */
    public boolean isSnapShot() {
        return true;
    }

}
