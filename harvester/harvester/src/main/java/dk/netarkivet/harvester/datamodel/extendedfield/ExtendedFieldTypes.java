/* File:        $Id$Id$
 * Revision:    $Revision$Revision$
 * Author:      $Author$Author$
 * Date:        $Date$Date$
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

package dk.netarkivet.harvester.datamodel.extendedfield;

/**
 * Class declaring constants for ExtendedFieldTypes and their 
 * corresponding table-names. There are two kinds of extended fields:
 * extendedfields for domains, and extendedfields for harvestdefinitions.
 * TODO change into an enum class instead.
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
     * arrays representing the two different types, and which database table
     * they are associated with. used for testing.
     */
    protected static String[] tableNames = {
        "", "domains", "harvestdefinitions"};
}
