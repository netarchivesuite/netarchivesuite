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

import java.io.Serializable;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Class containing Info about a harvestjob.
 */
@SuppressWarnings({"serial"})
public class HarvestDefinitionInfo implements Serializable {

    /** The original harvest name. */
    private final String origHarvestName;

    /** The original harvest description. */
    private final String origHarvestDesc;

    /** The name of the schedule for the original harvest definition. */
    private final String scheduleName;

    /**
     * Builds a harvest definition info object.
     *
     * @param origHarvestName the harvest definition's name
     * @param origHarvestDesc the harvest definition's comments (can be empty string)
     * @param scheduleName the harvest definition's schedule name (only applicable for selective harvests)
     */
    public HarvestDefinitionInfo(String origHarvestName, String origHarvestDesc, String scheduleName) {
        super();
        ArgumentNotValid.checkNotNullOrEmpty(origHarvestName, "origHarvestName");
        ArgumentNotValid.checkNotNull(origHarvestDesc, "origHarvestDesc");
        ArgumentNotValid.checkNotNull(scheduleName, "scheduleName");
        this.origHarvestName = origHarvestName;
        this.origHarvestDesc = origHarvestDesc;
        this.scheduleName = scheduleName;
    }

    /**
     * @return the origHarvestName
     */
    public String getOrigHarvestName() {
        return origHarvestName;
    }

    /**
     * @return the origHarvestDesc
     */
    public String getOrigHarvestDesc() {
        return origHarvestDesc;
    }

    /**
     * @return the origHarvestScheduleName
     */
    public String getScheduleName() {
        return scheduleName;
    }

}
