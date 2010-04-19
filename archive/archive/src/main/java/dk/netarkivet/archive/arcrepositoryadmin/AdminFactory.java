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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */

package dk.netarkivet.archive.arcrepositoryadmin;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.common.utils.SettingsFactory;

/**
 * Factory class for the admin instance. 
 * This creates an instance of the admin structure, which is defined by the 
 * settings.
 * @see dk.netarkivet.archive.ArchiveSettings#ADMIN_CLASS
 */
public class AdminFactory extends SettingsFactory<Admin>{
    /**
     * Retrieves the admin instance defined in the settings.
     * 
     * @return The settings defined admin instance.
     */
    public static Admin getInstance() {
        return SettingsFactory.getInstance(
                ArchiveSettings.ADMIN_CLASS);
    }
}
