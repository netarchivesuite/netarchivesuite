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
package dk.netarkivet.harvester.harvesting;

import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.Job;

/**
 * Implements another way of prefixing archive files in Netarchivesuite. I.e. collectionName-jobid-harvestid
 */
public class CollectionPrefixNamingConvention implements ArchiveFileNaming {

    /** The default place in classpath where the settings file can be found. */
    private static String defaultSettingsClasspath = "dk/netarkivet/harvester/harvesting/"
            + "CollectionPrefixNamingConventionSettings.xml";

    /*
     * The static initialiser is called when the class is loaded. It will add default values for all settings defined in
     * this class, by loading them from a settings.xml file in classpath.
     */
    static {
        Settings.addDefaultClasspathSettings(defaultSettingsClasspath);
    }

    /** The name of the collection embedded in the names. */
    private static String CollectionName = Settings.get(HarvesterSettings.HERITRIX_PREFIX_COLLECTION_NAME);

    public CollectionPrefixNamingConvention() {
    }

    @Override
    public String getPrefix(Job theJob) {
        return CollectionName + "-" + theJob.getJobID() + "-" + theJob.getOrigHarvestDefinitionID();
    }

}
