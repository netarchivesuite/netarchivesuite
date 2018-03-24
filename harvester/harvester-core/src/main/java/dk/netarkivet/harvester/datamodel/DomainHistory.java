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

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Container for the historical information available for a domain.
 */
public class DomainHistory {

    /**
     * Harvest information sorted with newest harvests first.
     */
    private SortedSet<HarvestInfo> harvestInfo;
    /**
     * Sorts HarvestInfo with newest first. Sorting on HarvestID and DomainConfiguration is only to make comparator
     * consistent with equals.
     */
    private static final Comparator<HarvestInfo> DATE_COMPARATOR = new Comparator<HarvestInfo>() {
        public int compare(HarvestInfo hi1, HarvestInfo hi2) {
            int i = hi2.getDate().compareTo(hi1.getDate());
            if (i != 0) {
                return i;
            }
            int i2 = hi2.getHarvestID().compareTo(hi1.getHarvestID());
            if (i2 != 0) {
                return i2;
            }
            return hi2.getDomainConfigurationName().compareTo(hi1.getDomainConfigurationName());
        }
    };

    /**
     * Create new DomainHistory instance.
     */
    public DomainHistory() {
        this.harvestInfo = new TreeSet<HarvestInfo>(DATE_COMPARATOR);
    }

    /**
     * Get all harvest information domain history.
     *
     * @return Iterator of harvest information registered for this domain. The information is sorted by date with the
     * most recent information as the first entry.
     */
    public Iterator<HarvestInfo> getHarvestInfo() {
        return harvestInfo.iterator();
    }

    /**
     * Gets the most recent harvestinfo for a specific DomainConfiguration.
     *
     * @param cfgName name of the configuration
     * @return the most recent harvest info or null if no matching harvestinfo found
     */
    public HarvestInfo getMostRecentHarvestInfo(String cfgName) {
        ArgumentNotValid.checkNotNull(cfgName, "cfgName");

        if (harvestInfo.size() == 0) {
            return null;
        }
        for (HarvestInfo hi : harvestInfo) {
            if (hi.getDomainConfigurationName().equals(cfgName)) {
                return hi;
            }
        }
        return null;
    }

    /**
     * Gets the newest harvestinfo for a specific HarvestDefinition and DomainConfiguration.
     *
     * @param oid id of the harvest definition
     * @param cfgName the name of the domain configuration
     * @return the harvest info or null if no matching harvestinfo found
     */
    public HarvestInfo getSpecifiedHarvestInfo(Long oid, String cfgName) {
        ArgumentNotValid.checkNotNull(oid, "oid");
        ArgumentNotValid.checkNotNull(cfgName, "cfgName");

        Iterator<HarvestInfo> iter = harvestInfo.iterator();
        HarvestInfo hi;
        while (iter.hasNext()) {
            hi = iter.next();
            if (hi.getHarvestID().equals(oid) && hi.getDomainConfigurationName().equals(cfgName)) {
                return hi;
            }
        }
        return null;
    }

    /**
     * Add new harvestinformation to the domainHistory.
     *
     * @param hi the harvest information to add
     */
    public void addHarvestInfo(HarvestInfo hi) {
        ArgumentNotValid.checkNotNull(hi, "hi");
        harvestInfo.add(hi);
    }

    /**
     * Return the most recent harvestresult for the configuration identified by name that was a complete harvest of the
     * domain.
     *
     * @param configName The name of the configuration
     * @param history The domainHistory for a domain
     * @return the most recent harvestresult for the configuration identified by name that was a complete harvest of the
     * domain.
     */
    public static HarvestInfo getBestHarvestInfoExpectation(String configName, DomainHistory history) {
        ArgumentNotValid.checkNotNullOrEmpty(configName, "String configName");
        ArgumentNotValid.checkNotNull(history, "DomainHistory history");
        // Remember best expectation
        HarvestInfo best = null;

        // loop through all harvest infos for this configuration. The iterator is
        // sorted by date with most recent first
        Iterator<HarvestInfo> i = history.getHarvestInfo();
        while (i.hasNext()) {
            HarvestInfo hi = i.next();
            if (hi.getDomainConfigurationName().equals(configName)) {
                // Remember this expectation, if it harvested at least
                // as many objects as the previously remembered
                if ((best == null) || (best.getCountObjectRetrieved() <= hi.getCountObjectRetrieved())) {
                    best = hi;
                }
                // if this harvest completed, stop search and return best
                // expectation,
                if (hi.getStopReason() == StopReason.DOWNLOAD_COMPLETE) {
                    return best;
                }
            }
        }

        // Return maximum uncompleted harvest, or null if never harvested
        return best;
    }

}
