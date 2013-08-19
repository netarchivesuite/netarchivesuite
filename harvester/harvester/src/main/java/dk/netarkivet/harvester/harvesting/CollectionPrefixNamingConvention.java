/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
package dk.netarkivet.harvester.harvesting;

import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.datamodel.Job;

/** 
 * Implements another way of prefixing archive files in Netarchivesuite. 
 * I.e. collectionName-jobid-harvestid
 */
public class CollectionPrefixNamingConvention implements ArchiveFileNaming {
    
    /** The default place in classpath where the settings file can be found. */
    private static String defaultSettingsClasspath
            = "dk/netarkivet/harvester/harvesting/"
                +"CollectionPrefixNamingConventionSettings.xml";

    /*
     * The static initialiser is called when the class is loaded.
     * It will add default values for all settings defined in this class, by
     * loading them from a settings.xml file in classpath.
     */
    static {
        Settings.addDefaultClasspathSettings(defaultSettingsClasspath);
    }
    /** The setting for the collectionName. */
    private static String COLLECTION_SETTING = "settings.harvester.harvesting.heritrix"
            + ".archiveNaming.collectionName";
    /** The name of the collection embedded in the names. */
    private static String CollectionName = Settings.get(COLLECTION_SETTING);
    
    
    public CollectionPrefixNamingConvention() {
    }

    @Override
    public String getPrefix(Job theJob) {
        return CollectionName + "-" 
                + theJob.getJobID() + "-" + theJob.getOrigHarvestDefinitionID();
    }
      
}
