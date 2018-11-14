/*
 * #%L
 * Netarchivesuite - harvester - test
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

package dk.netarkivet.harvester.indexserver;

import java.io.File;

/**
 * TestInfo class holding variables used by the tests for the indexserver package.
 */
public class TestInfo {

    static final File BASE_DIR = new File("tests/dk/netarkivet/harvester/indexserver/data");
    static final File ORIGINALS_DIR = new File(BASE_DIR, "originals");
    static final File METADATA_DIR = new File(ORIGINALS_DIR, "metadata");
    static final File WORKING_DIR = new File(BASE_DIR, "working");
    static final File ARCFILES_DIR = new File(WORKING_DIR, "arcfiles");
    static final File CRAWLLOGS_DIR = new File(WORKING_DIR, "crawllogs");
    static final File CDXCACHE_DIR = new File(new File(WORKING_DIR, "cache"), "cdxindex");
    static final File CDXDATACACHE_DIR = new File(new File(WORKING_DIR, "cache"), "cdxdata");

    static final File METADATA_FILE_3 = new File(ARCFILES_DIR, "3-metadata-2.arc");
    static final File METADATA_FILE_4 = new File(ARCFILES_DIR, "4-metadata-1.arc");
    static final File ARC_FILE_1 = new File(ARCFILES_DIR, "42-23-20060707123215-00001-udvikling.kb.dk.arc.gz");
    static final File ARC_FILE_2 = new File(ARCFILES_DIR, "42-23-20060707123215-00000-udvikling.kb.dk.arc.gz");
    static final File CRAWL_LOG_1 = new File(CRAWLLOGS_DIR, "crawl-1.log");
    static final File CRAWL_LOG_2 = new File(CRAWLLOGS_DIR, "crawl-2.log");
    static final File CRAWL_LOG_3 = new File(CRAWLLOGS_DIR, "crawl-3.log");
    static final File CRAWL_LOG_4 = new File(CRAWLLOGS_DIR, "crawl-4.log");
    static final File CRAWL_LOG_1_SORTED = new File(CRAWLLOGS_DIR, "crawl-1.log.sorted");
    static final File CRAWL_LOG_4_SORTED = new File(CRAWLLOGS_DIR, "crawl-4.log.sorted");
    static final File CRAWL_LOG = new File(CRAWLLOGS_DIR, "crawl-42.log");
    static final File CDX_CACHE_1 = new File(CDXCACHE_DIR, "1-cache");
    static final File CDX_CACHE_4 = new File(CDXCACHE_DIR, "4-cache");
    static final File CDX_CACHE_1_SORTED = new File(CDXDATACACHE_DIR, "cdxdata-1-cache.sorted");
    static final File CDX_CACHE_4_SORTED = new File(CDXDATACACHE_DIR, "cdxdata-4-cache.sorted");
    /**
     * Number of items in CRAWL_LOG_1 that CDXOriginCrawlLogIterator should return when using 1-cache.
     */
    static final int VALID_ENTRIES_IN_CRAWL_LOG_1 = 30;
    public static final int VALID_ENTRIES_IN_CRAWL_LOG_4 = 13;

}
