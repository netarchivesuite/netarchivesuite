/* $Id$
 * $Revision$
 * $Author$
 * $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import dk.netarkivet.common.exceptions.ArgumentNotValid;


/**
 * Container for the historical information available for a domain.
 *
 */
public class DomainHistory {

    /**
     * Harvest information sorted with newest harvests first.
     */
    private SortedSet<HarvestInfo> harvestInfo;
    /**
     * Sorts HarvestInfo with newest first. Sorting on HarvestID and
     * DomainConfiguration is only to make comparator consistent with equals.
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
            return hi2.getDomainConfigurationName().compareTo(
                    hi1.getDomainConfigurationName());
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
     * @return Iterator of harvest information registered
     *         for this domain. The information is sorted by date with the most
     *         recent information as the first entry.
     */
    public Iterator<HarvestInfo> getHarvestInfo() {
        return harvestInfo.iterator();
    }

    /**
     * Gets the most recent harvestinfo for a specific DomainConfiguration.
     *
     * @param cfgName name of the configuration
     * @return the most recent harvest info or null if no
     *         matching harvestinfo found
     */
    public HarvestInfo getMostRecentHarvestInfo(String cfgName) {
        ArgumentNotValid.checkNotNull(cfgName, "cfgName");

        if (harvestInfo.size() == 0) {
            return null;
        }
        for (HarvestInfo hi: harvestInfo) {
            if (hi.getDomainConfigurationName().equals(cfgName)) {
                return hi;
            }
        }
        return null;
    }

    /**
     * Gets the newest harvestinfo for a specific HarvestDefinition and
     * DomainConfiguration.
     *
     * @param oid     id of the harvest definition
     * @param cfgName the name of the domain configuration
     * @return the harvest info or null if no matching harvestinfo found
     */
    public HarvestInfo getSpecifiedHarvestInfo(Long oid, String cfgName) {
        ArgumentNotValid.checkNotNull(oid, "oid");
        ArgumentNotValid.checkNotNull(cfgName, "cfgName");

        Iterator iter = harvestInfo.iterator();
        HarvestInfo hi;
        while (iter.hasNext()) {
            hi = (HarvestInfo) iter.next();
            if (hi.getHarvestID().equals(oid)
                    && hi.getDomainConfigurationName().equals(cfgName)) {
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
}
