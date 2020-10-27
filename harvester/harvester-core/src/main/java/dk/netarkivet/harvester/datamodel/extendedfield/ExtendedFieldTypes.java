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

package dk.netarkivet.harvester.datamodel.extendedfield;

/**
 * Class declaring constants for ExtendedFieldTypes and their corresponding table-names. There are two kinds of extended
 * fields: extendedfields for domains, and extendedfields for harvestdefinitions. TODO change into an enum class
 * instead.
 */
public final class ExtendedFieldTypes {

    /**
     * Private default constructor to avoid instantiation.
     */
    private ExtendedFieldTypes() {
    }

    /** constant representing extendedfields for domains. */
    public static final int DOMAIN = 1;
    /** constant representing extendedfields for harvestdefinitions. */
    public static final int HARVESTDEFINITION = 2;
    /**
     * arrays representing the two different types, and which database table they are associated with. used for testing.
     */
    protected static final String[] tableNames = {"", "domains", "harvestdefinitions"};

}
