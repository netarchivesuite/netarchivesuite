/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

package dk.netarkivet.archive.indexserver;

import java.util.regex.Pattern;

/**
 * This class implements the low-level cache for crawl log Lucene indexing.
 * It will get the crawl logs for individual jobs as files.
 *
 */

public class CrawlLogDataCache extends RawMetadataCache {
    /**
     * Create a new CrawlLogDataCache.  For a given job ID, this will fetch
     * and cache crawl.log files from metadata files
     * (&lt;ID&gt;-metadata-[0-9]+.arc).
     *
     */
    public CrawlLogDataCache() {
        super("crawllog",
                Pattern.compile("metadata://[^/]*/crawl/logs/crawl\\.log.*"),
                Pattern.compile("text/plain"));
    }
}
