/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.harvester.harvesting.distribute;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobStatus;
import dk.netarkivet.harvester.datamodel.JobUtils;
import dk.netarkivet.harvester.harvesting.metadata.MetadataEntry;
import dk.netarkivet.testutils.ReflectUtils;

/**
 * Contains test information about all harvestdefinition test data.
 */
public class TestInfo {
    public static final File DATA_DIR = new File("tests/dk/netarkivet/harvester/harvesting/distribute/data/");
    public static final File LOG_FILE = new File(new File("tests/testlogs"), "netarkivtest.log");
    public static final int WAIT_TIME = 6000;
    public static ChannelID CLIENT_ID = Channels.getTheSched();
    public static ChannelID SERVER_ID = Channels.getThisReposClient();
    public static final File SERVER_DIR = new File(TestInfo.DATA_DIR, "server");
    public static final String DUMMY_SERVER_ID = "hc_test_dummy_server";
    public static final MetadataEntry sampleEntry = new MetadataEntry(
            "metadata://netarkivet.dk",
            "text/plain",
            "THIS IS SOME METADATA");
    public static final List<MetadataEntry> emptyMetadata = new ArrayList<MetadataEntry>();
    public static final List<MetadataEntry> oneMetadata = new ArrayList<MetadataEntry>();
    public static final String prefix = "ID";
    public static final String suffix = "X";

    /**
     * The properties-file containing properties for logging in unit-tests.
     */
    public static final File TESTLOGPROP = new File("tests/dk/netarkivet/testlog.prop");
    static File WORKING_DIR = new File(TestInfo.DATA_DIR, "working");
    static File ORIGINALS_DIR = new File(TestInfo.DATA_DIR, "originals");
    static File ORDER_FILE = new File(TestInfo.WORKING_DIR, "order.xml");
    static File SEEDS_FILE = new File(TestInfo.WORKING_DIR, "seeds.txt");
    static final File ARCHIVE_DIR = new File(TestInfo.WORKING_DIR, "bitarchive1");
    static final File SERVER_TEMP_DIR = new File(TestInfo.ARCHIVE_DIR, "temp");

    private static final File LEFTOVER_JOB_DIR_1 = new File(TestInfo.WORKING_DIR,"testserverdir1");
    static final File LEFTOVER_CRAWLDIR_1 = new File(TestInfo.LEFTOVER_JOB_DIR_1,"crawldir");
    static final int FILES_IN_LEFTOVER_JOB_DIR_1 = 1;
    private static final File LEFTOVER_JOB_DIR_2 = new File(TestInfo.WORKING_DIR,"testserverdir2");
    static final File LEFTOVER_CRAWLDIR_2 = new File(TestInfo.LEFTOVER_JOB_DIR_2,"crawldir");
    static final String[] FILES_IN_LEFTOVER_JOB_DIR_2 = {
        "42-117-20051212141240-00000-sb-test-har-001.statsbiblioteket.dk.arc",
        "42-117-20051212141240-00001-sb-test-har-001.statsbiblioteket.dk.arc",
        "42-117-20051212141242-00002-sb-test-har-001.statsbiblioteket.dk.arc"
    };
    static final String LEFTOVER_JOB_DIR_2_SOME_FILE_PATTERN =
        "(" + TestInfo.FILES_IN_LEFTOVER_JOB_DIR_2[0]
        + "|" + TestInfo.FILES_IN_LEFTOVER_JOB_DIR_2[1]
        + "|" + TestInfo.FILES_IN_LEFTOVER_JOB_DIR_2[2] + ")";
    // used by HarvestControllerServerTester#testStoreHarvestInformation()
    static final File LEFTOVER_JOB_DIR_3 = new File(TestInfo.WORKING_DIR,"testserverdir3");
    static final File LEFTOVER_CRAWLDIR_3 = new File(TestInfo.LEFTOVER_JOB_DIR_3,"crawldir");
    static final File TEST_CRAWL_DIR = new File ("tests/dk/netarkivet/harvester/harvesting/data/crawldir");
    static final File CRAWL_DIR_COPY = new File ("tests/dk/netarkivet/harvester/harvesting/data/copyOfCrawldir");
    
    static final String HarvestInfofilename = "harvestInfo.xml";
    
    
    /**
     * Load resources needed by unit tests.
     */
    public TestInfo() {
    }

    /** Get a simple job with high priority.
     *  @return a simple job with high priority
     */
    static Job getJob() {
        return JobUtils.getHighPriorityJob(ORDER_FILE, JobStatus.NEW, "default_orderxml");
    }

    

}
