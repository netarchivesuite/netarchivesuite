/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.netarkivet.harvester;

import java.util.Arrays;
import java.util.List;

import dk.netarkivet.common.utils.Settings;

/**
 * Settings specific to the harvester module of NetarchiveSuite.
 */
public class HarvesterSettings {
    /** The default place in classpath where the settings file can be found. */
    private static final String DEFAULT_SETTINGS_CLASSPATH
            = "dk/netarkivet/harvester/settings.xml";

    static {
        Settings.addDefaultClasspathSettings(
                DEFAULT_SETTINGS_CLASSPATH
        );
    }

    /**
     * The fields of this class that don't actually correspond to settings.
     */
    public static List<String> EXCLUDED_FIELDS = Arrays.asList(
            "DEFAULT_SETTINGS_CLASSPATH");

    /** Default seed list to use when new domains are created. */
    public static String DEFAULT_SEEDLIST
            = "settings.harvester.datamodel.domain.defaultSeedlist";
    /**
     * The name of a configuration that is created by default and which is
     * initially used for snapshot harvests.
     */
    public static String DOMAIN_DEFAULT_CONFIG
            = "settings.harvester.datamodel.domain.defaultConfig";
    /**
     * Name of order xml template used for domains if nothing
     * else is specified (e.g. newly created configrations use this)
     */
    public static String DOMAIN_DEFAULT_ORDERXML
            = "settings.harvester.datamodel.domain.defaultOrderxml";
    /**
     * Default download rate for domain configuration.
     * Not currently enforced.
     */
    public static String DOMAIN_CONFIG_MAXRATE
            = "settings.harvester.datamodel.domain.defaultMaxrate";
    /** Default byte limit for domain configuration. */
    public static String DOMAIN_CONFIG_MAXBYTES
            = "settings.harvester.datamodel.domain.defaultMaxbytes";
    /**
     * Used when calculating expected size of a harvest of some configuration
     * during job-creation process.
     * This defines how great a possible factor we will permit a harvest to
     * be larger then the expectation, when basing the expectation on a previous
     * completed job.
     */
    public static String ERRORFACTOR_PERMITTED_PREVRESULT
            = "settings.harvester.scheduler.errorFactorPrevResult";
    /**
     * Used when calculating expected size of a harvest of some configuration
     * during job-creation process.
     * This defines how great a possible factor we will permit a harvest to
     * be larger then the expectation, when basing the expectation on previous
     * uncompleted harvests or no harvest data at all.
     */
    public static String ERRORFACTOR_PERMITTED_BESTGUESS
            = "settings.harvester.scheduler.errorFactorBestGuess";
    /**
     * How many bytes the average object is expected to be on domains where
     * we don't know any better.  This number should grow over time, as of
     * end of 2005 empirical data shows 38000.
     */
    public static String EXPECTED_AVERAGE_BYTES_PER_OBJECT
            = "settings.harvester.scheduler.expectedAverageBytesPerObject";
    /** Initial guess of #objects in an unknown domain */
    public static String MAX_DOMAIN_SIZE
            = "settings.harvester.scheduler.maxDomainSize";
    /**
     * The maximum allowed relative difference in expected number of objects
     * retrieved in a single job definition.  Set to MAX_LONG for no splitting.
     */
    public static String JOBS_MAX_RELATIVE_SIZE_DIFFERENCE
            = "settings.harvester.scheduler.jobs.maxRelativeSizeDifference";
    /**
     * Size differences for jobs below this threshold are ignored,
     * regardless of the limits for the relative size difference.  Set to
     * MAX_LONG for no splitting.
     */
    public static String JOBS_MIN_ABSOLUTE_SIZE_DIFFERENCE
            = "settings.harvester.scheduler.jobs.minAbsoluteSizeDifference";
    /**
     * When this limit is exceeded no more configurations may be added to a job.
     * Set to MAX_LONG for no splitting.
     */
    public static String JOBS_MAX_TOTAL_JOBSIZE
            = "settings.harvester.scheduler.jobs.maxTotalSize";
    /**
     * How many domain configurations we will process in one go before
     * making jobs out of them.  This amount of domains will be stored in
     * memory at the same time.  Set to MAX_LONG for no job splitting.
     */
    public static String MAX_CONFIGS_PER_JOB_CREATION
            = "settings.harvester.scheduler.configChunkSize";
    /**
     * Each job gets a subdir of this dir. Job data is written and
     * Heritrix writes to that subdir.
     */
    public static String HARVEST_CONTROLLER_SERVERDIR
            = "settings.harvester.harvesting.serverDir";
    /**
     * Check, that the serverdir has an adequate amount of bytes
     * available before accepting any harvest-jobs.
     */
    public static String HARVEST_SERVERDIR_MINSPACE
    		= "settings.harvester.harvesting.minSpaceLeft";
    /**
     * The directory in which data from old jobs is kept after uploading.  Each
     * directory from serverDir will be moved to here if any data remains,
     * either due to failed uploads or because it wasn't attempted uploaded.
     */
    public static String HARVEST_CONTROLLER_OLDJOBSDIR
            = "settings.harvester.harvesting.oldjobsDir";
    /** Pool to take jobs from */
    public static String HARVEST_CONTROLLER_PRIORITY
            = "settings.harvester.harvesting.queuePriority";
    /**
     * The timeout setting for aborting a crawl based on crawler-inactivity.
     * If the crawler is inactive for this amount of seconds the crawl will
     * be aborted.
     * The inactivity is measured on the crawlController.activeToeCount().
     */
    public static String INACTIVITY_TIMEOUT_IN_SECS
            = "settings.harvester.harvesting.heritrix.inactivityTimeout";
    /**
     * The timeout value (in seconds) used in HeritrixLauncher for aborting
     * crawl.
     * when no bytes are being received from web servers.
     */
    public static String CRAWLER_TIMEOUT_NON_RESPONDING
            = "settings.harvester.harvesting.heritrix.noresponseTimeout";
    /** The name used to access the Heritrix GUI */
    public static String HERITRIX_ADMIN_NAME
            = "settings.harvester.harvesting.heritrix.adminName";
    /** The password used to access the Heritrix GUI */
    public static String HERITRIX_ADMIN_PASSWORD
            = "settings.harvester.harvesting.heritrix.adminPassword";
    /** Port used to access the Heritrix web user interface.
     *  This port must not be used by anything else on the machine. */
    public static String HERITRIX_GUI_PORT
            = "settings.harvester.harvesting.heritrix.guiPort";
    /** The port that Heritrix uses to expose its JMX interface.  This port
     *  must not be used by anything else on the machine, but does not need to
     *  be accessible from other machines unless you want to be able to use
     *  jconsole to access Heritrix directly. Note that apart from pausing
     *  a job, modifications done directly on Heritrix may cause unexpected
     *  breakage. */
    public static String HERITRIX_JMX_PORT
            = "settings.harvester.harvesting.heritrix.jmxPort";
    /** The heap size to use for the Heritrix sub-process.  This should
     * probably be fairly large.
     */
    public static String HERITRIX_HEAP_SIZE
            = "settings.harvester.harvesting.heritrix.heapSize";
    /**
     * The file used to signal that the harvest controller is running.
     * Sidekick starts HarvestController if this file is not present
     */
    public static String HARVEST_CONTROLLER_ISRUNNING_FILE
            = "settings.harvester.harvesting.isrunningFile";
}
