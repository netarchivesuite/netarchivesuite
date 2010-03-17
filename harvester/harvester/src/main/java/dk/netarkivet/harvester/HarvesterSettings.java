/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.netarkivet.harvester;

import java.util.regex.Pattern;

import dk.netarkivet.common.utils.Settings;

/** Settings specific to the harvester module of NetarchiveSuite. */
public class HarvesterSettings {
    /** The default place in classpath where the settings file can be found. */
    private static final String DEFAULT_SETTINGS_CLASSPATH
            = "dk/netarkivet/harvester/settings.xml";

    /*
     * The static initialiser is called when the class is loaded.
     * It will add default values for all settings defined in this class, by
     * loading them from a settings.xml file in classpath.
     */
    static {
        Settings.addDefaultClasspathSettings(
                DEFAULT_SETTINGS_CLASSPATH

        );
    }

    // NOTE: The constants defining setting names below are left non-final on
    // purpose! Otherwise, the static initialiser that loads default values
    // will not run.

    /**
     * <b>settings.harvester.datamodel.domain.defaultSeedlist</b>: <br> Default
     * name of the seedlist to use when new domains are created.
     */
    public static String DEFAULT_SEEDLIST
            = "settings.harvester.datamodel.domain.defaultSeedlist";

    /**
     * <b>settings.harvester.datamodel.domain.defaultConfig</b>: <br> The name
     * of a configuration that is created by default and which is initially used
     * for snapshot harvests.
     */
    public static String DOMAIN_DEFAULT_CONFIG
            = "settings.harvester.datamodel.domain.defaultConfig";

    /**
     * <b>settings.harvester.datamodel.domain.defaultOrderxml</b>: <br> Name of
     * order xml template used for domains if nothing else is specified. The
     * newly created configurations use this. This template must exist before
     * harvesting can commence
     */
    public static String DOMAIN_DEFAULT_ORDERXML
            = "settings.harvester.datamodel.domain.defaultOrderxml";

    /**
     * <b>settings.harvester.datamodel.domain.defaultMaxrate</b>: <br> Default
     * download rate for domain configuration. Not currently enforced.
     */
    public static String DOMAIN_CONFIG_MAXRATE
            = "settings.harvester.datamodel.domain.defaultMaxrate";

    /**
     * <b>settings.harvester.datamodel.domain.defaultMaxbytes</b>: <br> Default
     * byte limit for domain configuration.
     */
    public static String DOMAIN_CONFIG_MAXBYTES
            = "settings.harvester.datamodel.domain.defaultMaxbytes";

    /**
     * <b>settings.harvester.datamodel.domain.defaultMaxobjects</b>: <br>
     * Default object limit for domain configuration.
     */
    public static String DOMAIN_CONFIG_MAXOBJECTS
            = "settings.harvester.datamodel.domain.defaultMaxobjects";

    /**
     * <b>settings.harvester.scheduler.errorFactorPrevResult</b>: <br> Used when
     * calculating expected size of a harvest of some configuration during
     * job-creation process. This defines how great a possible factor we will
     * permit a harvest to be larger then the expectation, when basing the
     * expectation on a previous completed job.
     */
    public static String ERRORFACTOR_PERMITTED_PREVRESULT
            = "settings.harvester.scheduler.errorFactorPrevResult";

    /**
     * <b>settings.harvester.scheduler.errorFactorBestGuess</b>: <br> Used when
     * calculating expected size of a harvest of some configuration during
     * job-creation process. This defines how great a possible factor we will
     * permit a harvest to be larger then the expectation, when basing the
     * expectation on previous uncompleted harvests or no harvest data at all.
     */
    public static String ERRORFACTOR_PERMITTED_BESTGUESS
            = "settings.harvester.scheduler.errorFactorBestGuess";

    /**
     * <b>settings.harvester.scheduler.expectedAverageBytesPerObject</b>: <br>
     * How many bytes the average object is expected to be on domains where we
     * don't know any better.  This number should grow over time, as of end of
     * 2005 empirical data shows 38000.
     */
    public static String EXPECTED_AVERAGE_BYTES_PER_OBJECT
            = "settings.harvester.scheduler.expectedAverageBytesPerObject";

    /**
     * <b>settings.harvester.scheduler.maxDomainSize</b>: <br> The initial guess
     * of the domain size (number of objects) of an unknown domain.
     */
    public static String MAX_DOMAIN_SIZE
            = "settings.harvester.scheduler.maxDomainSize";

    /**
     * <b>settings.harvester.scheduler.jobs.maxRelativeSizeDifference</b>: <br>
     * The maximum allowed relative difference in expected number of objects
     * retrieved in a single job definition. To avoid job splitting, set the
     * value as Long.MAX_VALUE.
     */
    public static String JOBS_MAX_RELATIVE_SIZE_DIFFERENCE
            = "settings.harvester.scheduler.jobs.maxRelativeSizeDifference";

    /**
     * <b>settings.harvester.scheduler.jobs.minAbsoluteSizeDifference</b>: <br>
     * Size differences for jobs below this threshold are ignored, regardless of
     * the limits for the relative size difference. To avoid job splitting, set
     * the value as Long.MAX_VALUE.
     */
    public static String JOBS_MIN_ABSOLUTE_SIZE_DIFFERENCE
            = "settings.harvester.scheduler.jobs.minAbsoluteSizeDifference";

    /**
     * <b>settings.harvester.scheduler.jobs.maxTotalSize</b>: <br> When this
     * limit is exceeded no more configurations may be added to a job. To avoid
     * job splitting, set the value as Long.MAX_VALUE.
     */
    public static String JOBS_MAX_TOTAL_JOBSIZE
            = "settings.harvester.scheduler.jobs.maxTotalSize";

    /**
     * <b>settings.harvester.scheduler.configChunkSize</b>: <br> How many domain
     * configurations we will process in one go before making jobs out of them.
     * This amount of domains will be stored in memory at the same time.  To
     * avoid job splitting, set this value as Long.MAX_VALUE.
     */
    public static String MAX_CONFIGS_PER_JOB_CREATION
            = "settings.harvester.scheduler.configChunkSize";

    /**
     * <b>settings.harvester.scheduler.splitByObjectLimit</b>: <br> By default
     * the byte limit is used as the base criterion for how many domain
     * configurations are put into one harvest job. However if this parameter is
     * set to "true", then the object limit is used instead as the base
     * criterion.
     */
    public static String SPLIT_BY_OBJECTLIMIT =
            "settings.harvester.scheduler.splitByObjectLimit";

    /**
     * <b>settings.harvester.scheduler.jobtimeouttime</b>:<br /> Time before a
     * STARTED job times out and change status to FAILED. In seconds.
     */
    public static String JOB_TIMEOUT_TIME =
            "settings.harvester.scheduler.jobtimeouttime";

    /**
     * <b>settings.harvester.harvesting.serverDir</b>: <br> Each job gets a
     * subdir of this dir. Job data is written and Heritrix writes to that
     * subdir.
     */
    public static String HARVEST_CONTROLLER_SERVERDIR
            = "settings.harvester.harvesting.serverDir";

    /**
     * <b>settings.harvester.harvesting.minSpaceLeft</b>: <br> The minimum
     * amount of free bytes in the serverDir required before accepting any
     * harvest-jobs.
     */
    public static String HARVEST_SERVERDIR_MINSPACE
            = "settings.harvester.harvesting.minSpaceLeft";

    /**
     * <b>settings.harvester.harvesting.oldjobsDir</b>: <br> The directory in
     * which data from old jobs is kept after uploading. Each directory from
     * serverDir will be moved to here if any data remains, either due to failed
     * uploads or because it wasn't attempted uploaded.
     */
    public static String HARVEST_CONTROLLER_OLDJOBSDIR
            = "settings.harvester.harvesting.oldjobsDir";

    /**
     * <b>settings.harvester.harvesting.queuePriority</b>: <br> Pool to take
     * jobs from. There are two pools to choose from, labelled "HIGHPRIORITY"
     * (pool for selective harvest jobs), and "LOWPRIORITY" (pool for snapshot
     * harvest jobs) respectively.
     *
     * NOTE: this one is also used in SingleMBeanObject parsing information to
     * System state
     */
    public static String HARVEST_CONTROLLER_PRIORITY
            = "settings.harvester.harvesting.queuePriority";

    /**
     * <b>settings.harvester.harvesting.heritrix.inactivityTimeout</b>: <br> The
     * timeout setting for aborting a crawl based on crawler-inactivity. If the
     * crawler is inactive for this amount of seconds the crawl will be aborted.
     * The inactivity is measured on the crawlController.activeToeCount().
     */
    public static String INACTIVITY_TIMEOUT_IN_SECS
            = "settings.harvester.harvesting.heritrix.inactivityTimeout";

    /**
     * <b>settings.harvester.harvesting.heritrix.noresponseTimeout</b>: <br> The
     * timeout value (in seconds) used in HeritrixLauncher for aborting crawl
     * when no bytes are being received from web servers.
     */
    public static String CRAWLER_TIMEOUT_NON_RESPONDING
            = "settings.harvester.harvesting.heritrix.noresponseTimeout";

    /**
     * <b>settings.harvester.harvesting.heritrix.adminName</b>: <br> The name
     * used to access the Heritrix GUI.
     */
    public static String HERITRIX_ADMIN_NAME
            = "settings.harvester.harvesting.heritrix.adminName";

    /**
     * <b>settings.harvester.harvesting.heritrix.adminPassword</b>: <br> The
     * password used to access the Heritrix GUI.
     */
    public static String HERITRIX_ADMIN_PASSWORD
            = "settings.harvester.harvesting.heritrix.adminPassword";

    /**
     * <b>settings.harvester.harvesting.heritrix.guiPort</b>: <br> Port used to
     * access the Heritrix web user interface. This port must not be used by
     * anything else on the machine. Note that apart from pausing a job,
     * modifications done directly on Heritrix may cause unexpected breakage.
     */
    public static String HERITRIX_GUI_PORT
            = "settings.harvester.harvesting.heritrix.guiPort";

    /**
     * <b>settings.harvester.harvesting.heritrix.jmxPort</b>: <br> The port that
     * Heritrix uses to expose its JMX interface. This port must not be used by
     * anything else on the machine, but does not need to be accessible from
     * other machines unless you want to be able to use jconsole to access
     * Heritrix directly. Note that apart from pausing a job, modifications done
     * directly on Heritrix may cause unexpected breakage.
     */
    public static String HERITRIX_JMX_PORT
            = "settings.harvester.harvesting.heritrix.jmxPort";

    /**
     * <b>settings.harvester.harvesting.heritrix.jmxUsername</b>: <br> The
     * username used to connect to Heritrix JMX interface The username must
     * correspond to the value stored in the jmxremote.password file (name
     * defined in setting settings.common.jmx.passwordFile).
     */
    public static String HERITRIX_JMX_USERNAME
            = "settings.harvester.harvesting.heritrix.jmxUsername";

    /**
     * <b>settings.harvester.harvesting.heritrix.jmxPassword</b>: <br> The
     * password used to connect to Heritrix JMX interface The password must
     * correspond to the value stored in the jmxremote.password file (name
     * defined in setting settings.common.jmx.passwordFile).
     */
    public static String HERITRIX_JMX_PASSWORD
            = "settings.harvester.harvesting.heritrix.jmxPassword";

    /**
     * <b>settings.harvester.harvesting.heritrix.heapSize</b>: <br> The heap
     * size to use for the Heritrix sub-process.  This should probably be fairly
     * large. It can be specified in the same way as for the -Xmx argument to
     * Java, e.g. 512M, 2G etc.
     */
    public static String HERITRIX_HEAP_SIZE
            = "settings.harvester.harvesting.heritrix.heapSize";

    /**
     * <b>settings.harvester.harvesting.heritrix.javaOpts</b>: <br> Additional
     * JVM options for the Heritrix sub-process. By default there is no
     * additional JVM option.
     */
    public static String HERITRIX_JVM_OPTS =
            "settings.harvester.harvesting.heritrix.javaOpts";

    /**
     * <b>settings.harvester.harvesting.heritrixControllerClass</b>:<br/> The
     * implementation of the HeritrixController interface to be used.
     */
    public static String HERITRIX_CONTROLLER_CLASS =
            "settings.harvester.harvesting.heritrixController.class";

    /**
     * <b>settings.harvester.harvesting.deduplication.enabled</b>:<br/> This
     * setting tells the system whether or not to use deduplication. This
     * setting is true by default.
     */
    public static String DEDUPLICATION_ENABLED =
            "settings.harvester.harvesting.deduplication.enabled";

    /**
     * <b>settings.harvester.harvesting.metadata.heritrixFilePattern</b> This
     * setting allows to filter which Heritrix files should be stored in the
     * metadata ARC.
     *
     * @see Pattern
     */
    public static String METADATA_HERITRIX_FILE_PATTERN =
            "settings.harvester.harvesting.metadata.heritrixFilePattern";

    /**
     * <b>settings.harvester.harvesting.metadata.reportFilePattern</b> This
     * setting allows to filter which Heritrix files that should be stored in
     * the metadata ARC are to be classified as a report.
     *
     * @see Pattern
     */
    public static String METADATA_REPORT_FILE_PATTERN =
            "settings.harvester.harvesting.metadata.reportFilePattern";

    /**
     * <b>settings.harvester.harvesting.metadata.logFilePattern</b> This setting
     * allows to filter which Heritrix log files should be stored in the
     * metadata ARC.
     *
     * @see Pattern
     */
    public static String METADATA_LOG_FILE_PATTERN =
            "settings.harvester.harvesting.metadata.logFilePattern";
}

