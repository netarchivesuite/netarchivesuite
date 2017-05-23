/*
 * #%L
 * Netarchivesuite - harvester - test
 * %%
 * Copyright (C) 2005 - 2017 The Royal Danish Library, 
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dk.netarkivet.harvester.harvesting.metadata.MetadataEntry;

/**
 * Testdata for this package.
 */
public class Heritrix1ControllerTestInfo {
    // General dirs:
    protected static final File RESOURCE_DIR = new File("src/test/resources");
    protected static final File WORKING_DIR = new File("target/test-output");

    static final File ORIGINALS_DIR = new File(RESOURCE_DIR, "originals");

    public static final File CRAWLDIR_ORIGINALS_DIR = new File(RESOURCE_DIR, "crawldir");
    static final File UNFINISHED_CRAWLDIR = new File(RESOURCE_DIR, "unfinished_crawl_dir");
    static final File ORDER_AND_SEEDS_ORIGINALS_DIR = new File(RESOURCE_DIR, "originals");

    // Single files inside ORDER_AND_SEEDS_ORGINALS_DIR:
    // (We should ALWAYS move these to WORKING_DIR and reference the copies!)
    static final File ORDER_FILE = new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "order.xml");
    static final File ORDER_FILE_WITH_DEDUPLICATION_DISABLED = new File(ORDER_AND_SEEDS_ORIGINALS_DIR,
            "order-without-deduplication.xml");
    static final File DEDUP_ORDER_FILE = new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "order-deduplicator.xml");

    static final File EMPTY_ORDER_FILE = new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "empty_order.xml");
    static final File BAD_ORDER_FILE = new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "badorder.xml");
    static final File MISSING_DISK_FIELD_ORDER_FILE = new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "missing_disk_field.xml");
    static final File MISSING_ARCS_PATH_ORDER_FILE = new File(ORDER_AND_SEEDS_ORIGINALS_DIR,
            "missing_arcs_path_field.xml");
    static final File MISSING_SEEDS_FILE_ORDER_FILE = new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "missing_seeds_field.xml");
    static final File MISSING_PREFIX_FIELD_ORDER_FILE = new File(ORDER_AND_SEEDS_ORIGINALS_DIR,
            "missing_prefix_field.xml");

    static final File SEEDS_FILE = new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "seeds.txt");

    // Single files inside WORKING_DIR:
    static final File HERITRIX_TEMP_DIR = new File(WORKING_DIR, "heritrix_out");
    static final File HERITRIX_LOG_DIR = new File(HERITRIX_TEMP_DIR, "logs");
    static final File HERITRIX_CRAWL_LOG_FILE = new File(HERITRIX_LOG_DIR, "crawl.log");

    // Single files inside the CRAWLDIR_ORIGINALS_DIR:
    static final File ORIGINAL_ARCS_DIR = new File(CRAWLDIR_ORIGINALS_DIR, "arcs");
    static final File METADATA_TEST_DIR = new File(ORIGINAL_ARCS_DIR, "realistically-named-arcs");
    static final String ARC_HARVEST_ID = "117";
    static final String ARC_JOB_ID = "42";
    static final File METADATA_TEST_DIR_INCONSISTENT = new File(ORIGINAL_ARCS_DIR, "inconsistently-named-arcs");
    static final File ARC_REAL_DIR = new File(METADATA_TEST_DIR, "arcs");
    static final File CDX_DIR = new File(CRAWLDIR_ORIGINALS_DIR, "cdxs");
    static final File CDX_WORKING_DIR = new File(WORKING_DIR, "cdxs");
    static final File ARC_FILE_0 = new File(ARC_REAL_DIR,
            "42-117-20051212141240-00000-sb-test-har-001.statsbiblioteket.dk.arc");
    static final File ARC_FILE_1 = new File(ARC_REAL_DIR,
            "42-117-20051212141241-00001-sb-test-har-001.statsbiblioteket.dk.arc");
    static final File CDX_FILE = new File(CDX_DIR,
            "42-117-20051212141241-00001-sb-test-har-001.statsbiblioteket.dk.cdx");

    static final File ARC_FILE2 = new File(ORIGINAL_ARCS_DIR, "NetarchiveSuite-netarkivet.arc.gz");
    static final File CDX_FILE2 = new File(CDX_DIR, "NetarchiveSuite-netarkivet.arc.gz.cdx");

    static final long JOB_ID = 42;
    static final long HARVEST_ID = 142L;
    public static final MetadataEntry sampleEntry = new MetadataEntry("metadata://netarkivet.dk", "text/plain",
            "DETTE ER NOGET METADATA");
    public static final List<MetadataEntry> emptyMetadata = new ArrayList<MetadataEntry>();
    public static final List<MetadataEntry> oneMetadata = new ArrayList<MetadataEntry>();

     static final File NON_EXISTING_FILE = new File(new File(WORKING_DIR, "harvestreports"), "must-not-exist.log");

    public static final String FST_FILENAME = "42-117-20051212141241-00001-sb-test-har-001.statsbiblioteket.dk.arc";
    public static final String SND_FILENAME = "42-117-20051212141240-00000-sb-test-har-001.statsbiblioteket.dk.arc";

}
