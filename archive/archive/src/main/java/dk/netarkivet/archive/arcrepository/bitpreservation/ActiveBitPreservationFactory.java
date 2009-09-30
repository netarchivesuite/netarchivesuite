/* File:        $Id$
 * Revision:    $Revision$
 * Date:        $Date$
 * Author:      $Author$
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
package dk.netarkivet.archive.arcrepository.bitpreservation;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.common.utils.SettingsFactory;

/**
 * A factory for the ActiveBitPreservation interface. <br/>
 * Creates an instance of the ActiveBitPreservation from on the setting
 * settings.archive.bitpreservation.class.
 * 
 * @see dk.netarkivet.archive.arcrepository.bitpreservation.ActiveBitPreservation
 */
public class ActiveBitPreservationFactory 
        extends SettingsFactory<ActiveBitPreservation>{

    /**
     * Method for retrieving the current ActiveBitPreservation instance defined
     * in the settings. 
     * 
     * @return The ActiveBitPreservation defined in the settings.
     */
    public static ActiveBitPreservation getInstance() {
        return SettingsFactory.getInstance(
        	ArchiveSettings.CLASS_ARCREPOSITORY_BITPRESERVATION);
    }
}
