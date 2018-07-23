/*
 * #%L
 * Netarchivesuite - common
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

package dk.netarkivet.common.distribute.indexserver;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.SettingsFactory;

/**
 * A factory for IndexClients.
 * <p>
 * Implementation note: This currently assumes that only one implementation exists, pointed out by the setting
 * settings.common.indexClient.class, but that the cache variant in question is selected by a parameter to its
 * getInstance method.
 */
public class IndexClientFactory extends SettingsFactory<JobIndexCache> {

    /**
     * Get a cache of CDX files for a set of jobs.
     *
     * @return A cache implementation for CDX files.
     */
    public static JobIndexCache getCDXInstance() {
        return SettingsFactory.getInstance(CommonSettings.INDEXSERVER_CLIENT, RequestType.CDX);
    }

    /**
     * Get a cache of Lucene index files for a set of jobs. This index is intended for deduplication and may contain a
     * subset of the actual entries for the given jobs in the archive to preserve space and time.
     *
     * @return A cache implementation for Lucene index files for deduplication.
     */
    public static JobIndexCache getDedupCrawllogInstance() {
        return SettingsFactory.getInstance(CommonSettings.INDEXSERVER_CLIENT, RequestType.DEDUP_CRAWL_LOG);
    }

    /**
     * Get a cache of Lucene index files for a set of jobs. This index is intended for a viewer, and contains entries
     * for all the records for the given job.
     *
     * @return A cache implementation for Lucene index files for viewing.
     */
    public static JobIndexCache getFullCrawllogInstance() {
        return SettingsFactory.getInstance(CommonSettings.INDEXSERVER_CLIENT, RequestType.FULL_CRAWL_LOG);
    }

}
