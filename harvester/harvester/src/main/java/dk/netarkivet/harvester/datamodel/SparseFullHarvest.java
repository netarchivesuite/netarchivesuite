/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Sparse version of FullHarvest to be used for GUI purposes only. Immutable.
 * For GUI purposes only.
 *
 * @see FullHarvest
 */
public class SparseFullHarvest {
    /**
     * ID of this harvest.
     */
    private final Long oid;
    /**
     * Name of this harvest.
     */
    private final String name;
    /**
     * Comments on this harvest.
     */
    private final String comments;
    /**
     * Number of times this harvest has run.
     */
    private final int numEvents;
    /**
     * True if harvest is active.
     */
    private final boolean active;
    /**
     * Current edition of harvest.
     */
    private final long edition;
    /**
     * The maximum number of objects retrieved from each domain during a
     * snapshot harvest.
     */
    private long maxCountObjects;
    /**
     * The maximum number of bytes retrieved from each domain during a snapshot
     * harvest.
     */
    private long maxBytes;
    /**
     * The ID for the harvestdefinition, this FullHarvest is based upon.
     */
    private Long previousHarvestDefinitionOid;

    /**
     * Create new instance of SparseFullHarvest.
     *
     * @param oid                 id of this  harvest.
     * @param harvestDefName      the name of the harvest definition.
     * @param comments            comments.
     * @param numEvents           Number of times this harvest has run.
     * @param active              Whether this harvest definition is active.
     * @param edition             DAO edition of harvest.
     * @param maxCountObjects     Limit for how many objects can be harvested
     * @param maxBytes            Limit for how many bytes can be harvested
     * @param previousFullHarvest This id of the harvestDefinition used to
     *                            create this Fullharvest definition. May be
     *                            null for none
     * @throws ArgumentNotValid if oid, name or comments is null, or name is
     *                          empty.
     */
    public SparseFullHarvest(Long oid, String harvestDefName,
                             String comments,
                             int numEvents,
                             boolean active,
                             long edition, long maxCountObjects,
                             long maxBytes,
                             Long previousFullHarvest) {
        ArgumentNotValid.checkNotNull(oid, "oid");
        ArgumentNotValid.checkNotNullOrEmpty(harvestDefName, "harvestDefName");
        ArgumentNotValid.checkNotNull(comments, "comments");

        this.oid = oid;
        this.name = harvestDefName;
        this.comments = comments;
        this.numEvents = numEvents;
        this.active = active;
        this.edition = edition;
        this.maxCountObjects = maxCountObjects;
        this.maxBytes = maxBytes;
        this.previousHarvestDefinitionOid = previousFullHarvest;
    }

    /**
     * Get ID of HarvestDefinition which this is based on, or null for none.
     *
     * @return The previous HarvestDefinition, or null for none
     */
    public Long getPreviousHarvestDefinitionOid() {
        return previousHarvestDefinitionOid;
    }

    /**
     * Get the maximum number of objects that this fullharvest will harvest per
     * domain, -1 for no limit.
     *
     * @return Total download limit in objects per domain.
     */
    public long getMaxCountObjects() {
        return maxCountObjects;
    }

    /**
     * Get the maximum number of bytes that this fullharvest will harvest per
     * domain, -1 for no limit.
     *
     * @return Total download limit in bytes per domain.
     */
    public long getMaxBytes() {
        return maxBytes;
    }

    /**
     * Name of harvest definition.
     *
     * @return Name of harvest definition.
     */
    public String getName() {
        return name;
    }

    /**
     * Comments for harvest definition.
     *
     * @return Comments for harvest definition.
     */
    public String getComments() {
        return comments;
    }

    /**
     * Number of times this harvest has run.
     *
     * @return Number of times this harvest has run.
     */
    public int getNumEvents() {
        return numEvents;
    }

    /**
     * ID of this harvest.
     *
     * @return ID of this harvest.
     */
    public Long getOid() {
        return oid;
    }

    /**
     * Whether harvest is active.
     *
     * @return Whether harvest is active.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * DAO edition of harvest definition.
     *
     * @return DAO edition of harvest definition.
     */
    public long getEdition() {
        return edition;
    }
}
