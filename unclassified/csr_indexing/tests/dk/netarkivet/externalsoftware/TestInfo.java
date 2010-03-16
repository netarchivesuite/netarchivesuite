/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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

package dk.netarkivet.externalsoftware;

import java.io.File;

/**
 * Information about directories, and files used by the tests in
 * the dk.netarkivet.externalsoftware package.
 */
public class TestInfo {
    /**
     *  The data is stored at tests/dk/netarkivet/harvester/harvesting/data to
     *  avoid duplicate templates.
     */
    static final File TEST_DIR = new File("tests/dk/netarkivet/harvester/harvesting/data/launcher");
    static final File WORKING_DIR = new File(TEST_DIR, "working");
    static final File ORIGINALS_DIR = new File(TEST_DIR, "originals");
    static final File ORDER_FILE = new File(ORIGINALS_DIR, "order.xml");
    static final File DEFAULT_ORDERXML_FILE = new File(ORIGINALS_DIR, "default_orderxml.xml");
    static final File DEDUPFETCH_ORDERXML_FILE = new File(ORIGINALS_DIR, "dedupfetch_orderxml.xml");
    static final File ORDER_FILE_MAX_OBJECTS = new File(ORIGINALS_DIR, "orderMaxObjectsPrDomain.xml");
    static final File SEEDS_FILE_MAX_OBJECTS = new File(ORIGINALS_DIR, "seedsMaxObjectsPrDomain.txt");
    static final File EMPTY_ORDER_FILE = new File(ORIGINALS_DIR, "empty_order.xml");
    static final File BAD_ORDER_FILE = new File(ORIGINALS_DIR, "badorder.xml");
    static final File MISSING_DISK_FIELD_ORDER_FILE = new File(ORIGINALS_DIR, "missing_disk_field.xml");
    static final File MISSING_ARCS_PATH_ORDER_FILE = new File(ORIGINALS_DIR, "missing_arcs_path_field.xml");
    static final File MISSING_SEEDS_FILE_ORDER_FILE = new File(ORIGINALS_DIR, "missing_seeds_field.xml");
    static final File MISSING_PREFIX_FIELD_ORDER_FILE = new File(ORIGINALS_DIR, "missing_prefix_field.xml");
    static final File HERITRIX_SETTINGS_SCHEMA_FILE = new File(ORIGINALS_DIR, "heritrix_settings.xsd");
    static final File MAX_OBJECTS_ORDER_FILE =
            new File(ORIGINALS_DIR, "max_objects_order.xml");
    static final File MAX_OBJECTS_PR_DOMAIN_ORDER_FILE =
            new File(ORIGINALS_DIR, "max_objects_pr_domain_order.xml");
    static final File COOKIES_ORDER_FILE =
            new File(ORIGINALS_DIR, "cookies_order.xml");
    static final File RESTRICTED_URL_ORDER_FILE =
    //        new File(ORIGINALS_DIR, "restricted_url_order.xml");
            new File(ORIGINALS_DIR, "restricted_url_order_org.xml"); // taken from tests/dk/netarkivet/externalsoftware/data
    static final File SEEDS_FILE = new File(ORIGINALS_DIR, "seeds.txt");
    static final File SEEDS_FILE2 = new File(ORIGINALS_DIR, "seeds2.txt");
    static final File SEEDS_DEFAULT = new File(ORIGINALS_DIR, "seeds-default.txt");
    static final File COOKIE_SEEDS_FILE = new File(ORIGINALS_DIR, "cookie_seeds.txt");
    static final File HERITRIX_TEMP_DIR = new File(WORKING_DIR, "heritrix_out");
    static final File HERITRIX_LOG_DIR = new File(HERITRIX_TEMP_DIR, "logs");
    static final File HERITRIX_PROGRESS_LOG_FILE = new File(HERITRIX_LOG_DIR, "progress-statistics.log");
    static final File HERITRIX_CRAWL_LOG_FILE = new File(HERITRIX_LOG_DIR, "crawl.log");
    static final File HERITRIX_ARCS_DIR = new File(HERITRIX_TEMP_DIR, "arcs");
    static final String SEARCH_FOR_THIS_URL = "http://netarkivet.dk/robots.txt";
    static final int MAX_OBJECTS = 10;
    public static final long JOBID = 42;
    public static final long HARVESTID = 23;
    
    static final File EMPTY_CRAWLLOG_FILE = new File(ORIGINALS_DIR, "empty_crawl.log");
    static final File DEDUPLICATOR_ORDERXML_FILE = new File(ORIGINALS_DIR, "deduplicator_orderxml.xml");
    
    static final File TEST_LAUNCH_HARVEST_DIR =
        new File("tests/dk/netarkivet/harvester/harvesting/data/launcher/originals/netarkivet/testLaunchHarvest");
}
