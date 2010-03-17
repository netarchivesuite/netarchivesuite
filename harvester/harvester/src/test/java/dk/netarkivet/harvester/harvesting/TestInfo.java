/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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

package dk.netarkivet.harvester.harvesting;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;

import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobPriority;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.StopReason;
import dk.netarkivet.harvester.harvesting.distribute.MetadataEntry;
import dk.netarkivet.testutils.ReflectUtils;

/**
 * Testdata for this package.
 */
public class TestInfo {

    static final StopReason DEFAULT_STOPREASON = StopReason.DOWNLOAD_COMPLETE;

    //General dirs:
    private static final File BASEDIR = new File("tests/dk/netarkivet/harvester/harvesting/data");
    
    private static final File FUTURE_BASEDIR = new File("tests/dk/netarkivet/harvester/data");
    private static final File TEMPLATES_DIR = new File(FUTURE_BASEDIR, "originals/order_templates");
    
    private static final File ONE_LEVEL_ORDER_FILE = new File(TEMPLATES_DIR, "OneLevel-order.xml");
    
    static final File ORIGINALS_DIR = new File(BASEDIR, "originals");
    
    static final File WORKING_DIR = new File(BASEDIR, "working");
    static final File CRAWLDIR_ORIGINALS_DIR = new File(BASEDIR, "crawldir");
    static final File UNFINISHED_CRAWLDIR = new File(BASEDIR,
                                                     "unfinished_crawl_dir");
    static final File ORDER_AND_SEEDS_ORIGINALS_DIR = new File(new File(BASEDIR, "launcher"), "originals");

    //Single files inside ORDER_AND_SEEDS_ORGINALS_DIR:
    //(We should ALWAYS move these to WORKING_DIR and reference the copies!)
    static final File ORDER_FILE
            = new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "order.xml");
    static final File ORDER_FILE_WITH_DEDUPLICATION_DISABLED 
            = new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "order-without-deduplication.xml");
    static final File DEDUP_ORDER_FILE
            = new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "order-deduplicator.xml");
    static final File DEDUPFETCH_ORDER_FILE
            = new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "order-dedupfetch.xml");
    static final File DEDUP_DEDUPFETCH_ORDER_FILE
            = new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "order-deduplicator-dedupfetch.xml");

    static final File DEFAULT_ORDERXML_FILE = new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "default_orderxml.xml");
    static final File ORDER_FILE_MAX_OBJECTS = new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "orderMaxObjectsPrDomain.xml");
    static final File SEEDS_FILE_MAX_OBJECTS = new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "seedsMaxObjectsPrDomain.txt");
    static final File EMPTY_ORDER_FILE = new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "empty_order.xml");
    static final File BAD_ORDER_FILE = new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "badorder.xml");
    static final File MISSING_DISK_FIELD_ORDER_FILE = new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "missing_disk_field.xml");
    static final File MISSING_ARCS_PATH_ORDER_FILE = new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "missing_arcs_path_field.xml");
    static final File MISSING_SEEDS_FILE_ORDER_FILE = new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "missing_seeds_field.xml");
    static final File MISSING_PREFIX_FIELD_ORDER_FILE = new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "missing_prefix_field.xml");
    static final File MAX_OBJECTS_ORDER_FILE =
            new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "max_objects_order.xml");
    static final File MAX_OBJECTS_PR_DOMAIN_ORDER_FILE =
            new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "max_objects_pr_domain_order.xml");
    static final File COOKIES_ORDER_FILE =
            new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "cookies_order.xml");

    static final File RESTRICTED_URL_ORDER_FILE =
	//            new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "restricted_url_order.xml");
	new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "restricted_url_order_org.xml");

    static final File SEEDS_FILE = new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "seeds.txt");
    static final File SEEDS_FILE2 = new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "seeds2.txt");
    static final File SEEDS_DEFAULT = new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "seeds-default.txt");
    static final File COOKIE_SEEDS_FILE = new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "cookie_seeds.txt");

    //Single files inside WORKING_DIR:
    static final File HERITRIX_TEMP_DIR = new File(WORKING_DIR, "heritrix_out");
    static final File HERITRIX_LOG_DIR = new File(HERITRIX_TEMP_DIR, "logs");
    static final File HERITRIX_PROGRESS_LOG_FILE = new File(HERITRIX_LOG_DIR, "progress-statistics.log");
    static final File HERITRIX_CRAWL_LOG_FILE = new File(HERITRIX_LOG_DIR, "crawl.log");
    static final File HERITRIX_ARCS_DIR = new File(HERITRIX_TEMP_DIR, "arcs");
    static final String SEARCH_FOR_THIS_URL = "http://netarkivet.dk/newsite/organisation/index-da.php";
    static final int MAX_OBJECTS = 10;

    //Single files inside the CRAWLDIR_ORIGINALS_DIR:
    static final File ORIGINAL_ARCS_DIR = new File(CRAWLDIR_ORIGINALS_DIR, "arcs");
    static final File METADATA_TEST_DIR = new File(ORIGINAL_ARCS_DIR,
                                                   "realistically-named-arcs");
    static final String ARC_HARVEST_ID = "117";
    static final String ARC_JOB_ID = "42";
    static final String FST_ARC_TIME = "20051212141240";
    static final String FST_ARC_SERIAL = "00000";
    static final String SND_ARC_TIME = "20051212141241";
    static final String SND_ARC_SERIAL = "00001";
    static final File METADATA_TEST_DIR_INCONSISTENT =
            new File(ORIGINAL_ARCS_DIR, "inconsistently-named-arcs");
    static final String INCONSISTENT_HID_1 = "117";
    static final String INCONSISTENT_JID_1 = "42";
    static final String INCONSISTENT_HID_2 = "117";
    static final String INCONSISTENT_JID_2 = "43";
    static final File ARC_REAL_DIR = new File(METADATA_TEST_DIR, "arcs");
    static final File CDX_DIR = new File(CRAWLDIR_ORIGINALS_DIR, "cdxs");
    static final File CDX_WORKING_DIR = new File(WORKING_DIR, "cdxs");
    static final File ARC_FILE_0 = new File(ARC_REAL_DIR, "42-117-20051212141240-00000-sb-test-har-001.statsbiblioteket.dk.arc");
    static final File ARC_FILE_1 = new File(ARC_REAL_DIR, "42-117-20051212141241-00001-sb-test-har-001.statsbiblioteket.dk.arc");
    static final File CDX_FILE = new File(CDX_DIR, "42-117-20051212141241-00001-sb-test-har-001.statsbiblioteket.dk.cdx");
   
   
    //static final File ARC_FILE2 = new File(ORIGINAL_ARCS_DIR, "IAH-20050506114726-00001-kb-prod-udv-001.kb.dk.arc.gz");
    //static final File CDX_FILE2 = new File(CDX_DIR, "IAH-20050506114726-00001-kb-prod-udv-001.kb.dk.cdx");
    static final File ARC_FILE2 = new File(ORIGINAL_ARCS_DIR, "NetarchiveSuite-netarkivet.arc.gz");
    static final File CDX_FILE2 = new File(CDX_DIR, "NetarchiveSuite-netarkivet.arc.gz.cdx");
    
    
    static final long JOB_ID = 42;
    static final long HARVEST_ID = 142L;
    static final File LOG_FILE = new File("tests/testlogs/netarkivtest.log");
    public static final File HARVEST_INFO_FILE = new File(CRAWLDIR_ORIGINALS_DIR,
                                                          "harvestInfo.xml");
    public static final MetadataEntry sampleEntry = new MetadataEntry(
            "metadata://netarkivet.dk",
            "text/plain",
            "DETTE ER NOGET METADATA");
    public static final List<MetadataEntry> emptyMetadata = new ArrayList<MetadataEntry>();
    public static final List<MetadataEntry> oneMetadata = new ArrayList<MetadataEntry>();

    static final File EMPTY_CRAWLLOG_FILE =
        new File(ORDER_AND_SEEDS_ORIGINALS_DIR, "empty_crawl.log");
    static final File REPORT_FILE = new File(new File(WORKING_DIR, "harvestreports"), "crawl.log");
    static final File LONG_REPORT_FILE = new File(new File(WORKING_DIR, "harvestreports"), "crawl-long.log");
    static final File ADD_LONG_REPORT_FILE = new File(new File(WORKING_DIR, "harvestreports"), "crawl-addslong.log");
    static final File INVALID_REPORT_FILE = new File(new File(WORKING_DIR, "harvestreports"),
                                                     "invalid-crawl.log");
    public static final File STOP_REASON_REPORT_FILE = new File(new File(WORKING_DIR, "harvestreports"),
                                                                "stop-reason-crawl.log");
    static final File NON_EXISTING_FILE = new File(new File(WORKING_DIR, "harvestreports"), "must-not-exist.log");
    static final int NO_OF_TEST_DOMAINS = 2;
    static final String TEST_DOMAIN = "netarkivet.dk";
    static final int NO_OF_OBJECTS_TEST = 37;
    static final int NO_OF_BYTES_TEST = 1162154;
   
    /** Get a simple job with high priority.
     *  @return a simple job with high priority
     */
    static Job getJob() {
        Document d = XmlUtils.getXmlDoc(ONE_LEVEL_ORDER_FILE);
        try {
            Constructor<Job> c = ReflectUtils.getPrivateConstructor(
                    Job.class, Long.class, Map.class, JobPriority.class, Long.TYPE,
                    Long.TYPE, JobStatus.class, String.class, Document.class,
                    String.class, Integer.TYPE);
            String seedList = "www.netarkivet.dk";
            return c.newInstance(42L, Collections.<String, String>emptyMap(),
                                 JobPriority.HIGHPRIORITY, -1L, -1L,
                                 JobStatus.STARTED, "OneLevel-order", d,
                                 seedList, 1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
