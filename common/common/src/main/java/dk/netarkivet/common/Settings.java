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
package dk.netarkivet.common;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import dk.netarkivet.common.utils.SettingsStructure;
import dk.netarkivet.common.utils.StringTree;

/**
 * Settings common to the entire NetarchiveSuite.
 * Currently contains most settings.
 * TODO: Split settings into settings for all classes.
 */
public class Settings {
    /**
     * The property name specifying the file name of the settings file.
     * If the property is unset, uses DEFAULT_FILEPATH.
     */
    public static final String SETTINGS_FILE_NAME_PROPERTY
            = "dk.netarkivet.settings.file";

    /** The default place where the settings file can be found. */
    static final String DEFAULT_FILEPATH = "./conf/settings.xml";

    /** The singleton Settings object initialized at load time. */
    public static final SettingsStructure SETTINGS_STRUCTURE
            = new SettingsStructure(SETTINGS_FILE_NAME_PROPERTY,
                                    DEFAULT_FILEPATH);

    /**
     * The fields of this class that don't actually correspond to settings,
     * or are pluggable settings not always present.
     */
    public static final List<String> EXCLUDED_FIELDS = Arrays.asList(
            "DEFAULT_FILEPATH", "DEFAULT_XSD_FILEPATH",
            "HTTPREMOTEFILE_PORT_NUMBER");

    /**
     * Utility method. Provides static access to getter in settingsStructure.
     *
     * @see SettingsStructure#get(String)
     */
    public static String get(String s) {
        return SETTINGS_STRUCTURE.get(s);
    }

    /**
     * Utility method. Provides static access to getter in settingsStructure.
     *
     * @see SettingsStructure#getInt(String)
     */
    public static int getInt(String s) {
        return SETTINGS_STRUCTURE.getInt(s);
    }

    /**
     * Utility method. Provides static access to getter in settingsStructure.
     *
     * @see SettingsStructure#getLong(String)
     */
    public static long getLong(String s) {
        return SETTINGS_STRUCTURE.getLong(s);
    }

    /**
     * Utility method. Provides static access to getter in settingsStructure.
     *
     * @see SettingsStructure#get(String)
     */
    public static String[] getAll(String s) {
        return SETTINGS_STRUCTURE.getAll(s);
    }

    /**
     * Utility method. Provides static access to getter in settingsStructure.
     *
     * @see SettingsStructure#getSettingsFile()
     */
    public static File getSettingsFile() {
        return SETTINGS_STRUCTURE.getSettingsFile();
    }

    /**
     * Utility method. Provides static access to getter in settingsStructure.
     *
     * @see SettingsStructure#getEdition()
     */
    public static int getEdition() {
        return SETTINGS_STRUCTURE.getEdition();
    }

    /** Get a tree view of a part of the settings.
     *
     * @param path Dotted path to a unique element in the tree.
     * @return The part of the setting structure below the element given.
     */
    public static StringTree<String> getTree(String path) {
        return SETTINGS_STRUCTURE.getTree(path);
    }

    /**
     * Utility method. Provides static access to conditionalReload in
     * settingsStructure.
     *
     * @see SettingsStructure#conditionalReload()
     */
    public static void conditionalReload() {
        SETTINGS_STRUCTURE.conditionalReload();
    }

    /**
     * Utility method. Provides static access to reload in settingsStructure.
     *
     * @see SettingsStructure#reload()
     */
    public static void reload() {
        SETTINGS_STRUCTURE.reload();
    }

    /**
     * Utility method. Provides static access to setter in settingsStructure.
     *
     * @see SettingsStructure#set(String,String...)
     */
    public static void set(String s, String... values) {
        SETTINGS_STRUCTURE.set(s, values);
    }

    /**
     * Utility method. Provides static access to create in settingsStructure.
     *
     * @see SettingsStructure#create(String,String...)
     */
    public static void create(String s, String... values) {
        SETTINGS_STRUCTURE.create(s, values);
    }

    /* The setting names used should be declared and documented here */

    /** Common temporary directory for all applications. */
    public static final String DIR_COMMONTEMPDIR = "settings.common.tempDir";

    /** The class to use for RemoteFile objects. */
    public static final String REMOTE_FILE_CLASS
            = "settings.common.remoteFile.class";

    /** The default FTP-server used. */
    public static final String FTP_SERVER_NAME
            = "settings.common.remoteFile.serverName";

    /** The default FTP-server port used. */
    public static final String FTP_SERVER_PORT
            = "settings.common.remoteFile.serverPort";

    /** The default FTP username. */
    public static final String FTP_USER_NAME
            = "settings.common.remoteFile.userName";

    /** The default FTP password. * */
    public static final String FTP_USER_PASSWORD
            = "settings.common.remoteFile.userPassword";

    /**
     * The number of times FTPRemoteFile should try before giving up a copyTo
     * operation.
     */
    public static final String FTP_COPYTO_RETRIES
            = "settings.common.remoteFile.retries";

    /** HTTP remotefile port number. */
    public static final String HTTPREMOTEFILE_PORT_NUMBER
            = "settings.common.remoteFile.port";

    /** The keystore file used for HTTPS remotefiles. */
    public static final String HTTPSREMOTEFILE_KEYSTORE_FILE
            = "settings.common.remoteFile.certificateKeyStore";

    /** The password used for HTTPS remotefile keystore.
     * Refer to the installation manual for how to build a keystore.
     */
    public static final String HTTPSREMOTEFILE_KEYSTORE_PASSWORD
            = "settings.common.remoteFile.certificateKeyStorePassword";

    /** The password used for HTTPS remotefile private key. */
    public static final String HTTPSREMOTEFILE_KEY_PASSWORD
            = "settings.common.remoteFile.certificatePassword";

    /** Selects the broker class to be used. Must be subclass of
     * dk.netarkivet.common.distribute.JMSConnection. */
    public static final String JMS_BROKER_CLASS = "settings.common.jms.class";

    /** The JMS broker host contacted by the JMS connection. */
    public static final String JMS_BROKER_HOST = "settings.common.jms.broker";

    /** The port the JMS connection should use. */
    public static final String JMS_BROKER_PORT = "settings.common.jms.port";

    /**
     * The name of the environment in which this code is running, e.g.
     * PROD, RELEASETEST, NHC, ... Common prefix to all JMS channels.
     */
    public static final String ENVIRONMENT_NAME
            = "settings.common.jms.environmentName";

    /**
     * The *unique* (per host) port number that may or may not be used to serve
     * http, but is frequently used to identify the process.
     */
    public static final String HTTP_PORT_NUMBER = "settings.common.http.port";

    /**
     * The class that implements the ArcRepositoryClient.  This class will
     * be instantiated by the ArcRepositoryClientFactory.
     */
    public static final String ARC_REPOSITORY_CLIENT
            = "settings.common.arcrepositoryClient.class";

    /**
     * How many milliseconds we will wait before giving up on a lookup request
     * to the Arcrepository
     */
    public static final String ARCREPOSITORY_GET_TIMEOUT
            = "settings.common.arcrepositoryClient.getTimeout";

    /**
     * Number of times to try sending a store message before failing,
     * including the first attempt.
     */
    public static final String ARCREPOSITORY_STORE_RETRIES
            = "settings.common.arcrepositoryClient.storeRetries";

    /**
     * Timeout in milliseconds before retrying when calling
     * ArcRepositoryClient.store()
     */
    public static final String ARCREPOSITORY_STORE_TIMEOUT
            = "settings.common.arcrepositoryClient.storeTimeout";

    /**
     * The class instantiated to give access to indices.  Will be created
     * by IndexClientFactory.
     */
    public static final String INDEXSERVER_CLIENT
            = "settings.common.indexClient.class";

    /**
     * The amount of time, in milliseconds, we should wait for replies when
     * issuing a call to generate an index over som jobs.
     */
    public static final String INDEXREQUEST_TIMEOUT
            = "settings.common.indexClient.indexRequestTimeout";

    /**
     * The name of the directory where cache data global to the entire
     * machine can be stored.  Various kinds of caches should be stored in
     * subdirectories of this.
     */
    public static final String CACHE_DIR = "settings.common.cacheDir";

    /** The number of milliseconds we wait for processes to react to
     *  shutdown requests. */
    public static final String PROCESS_TIMEOUT
            = "settings.common.processTimeout";

    /** Default seed list to use when new domains are created. */
    public static final String DEFAULT_SEEDLIST
            = "settings.harvester.datamodel.domain.defaultSeedlist";

    /**
     * The name of a configuration that is created by default and which is
     * initially used for snapshot harvests.
     */
    public static final String DOMAIN_DEFAULT_CONFIG
            = "settings.harvester.datamodel.domain.defaultConfig";

    /**
     * Name of order xml template used for domains if nothing
     * else is specified (e.g. newly created configrations use this)
     */
    public static final String DOMAIN_DEFAULT_ORDERXML
            = "settings.harvester.datamodel.domain.defaultOrderxml";

    /**
     * Default download rate for domain configuration.
     * Not currently enforced.
     */
    public static final String DOMAIN_CONFIG_MAXRATE
            = "settings.harvester.datamodel.domain.defaultMaxrate";

    /** Valid top level domains, like .co.uk, .dk, .org. Repeats. */
    public static final String TLDS = "settings.harvester.datamodel.domain.tld";

    /** The class that defines DB-specific methods */
    public static final String DB_SPECIFICS_CLASS
            = "settings.harvester.datamodel.database.specificsclass";

    /**
     * URL to use to connect to the database.  If absent or empty, the URL
     * will be constructed in a derby-specific way based on DB_NAME and
     * HARVESTDEFINITION_BASEDIR.
     */
    public static final String DB_URL
            = "settings.harvester.datamodel.database.url";

    /**
     * The earliest time of day backup will be initiated, 0..24 hours.  At
     * a time shortly after this, a consistent backup copy of the database
     * will be created.
     */
    public static final String DB_BACKUP_INIT_HOUR
            = "settings.harvester.datamodel.database.backupInitHour";

    /**
     * Used when calculating expected size of a harvest of some configuration
     * during job-creation process.
     * This defines how great a possible factor we will permit a harvest to
     * be larger then the expectation, when basing the expectation on a previous
     * completed job.
     */
    public static final String ERRORFACTOR_PERMITTED_PREVRESULT
            = "settings.harvester.scheduler.errorFactorPrevResult";

    /**
     * Used when calculating expected size of a harvest of some configuration
     * during job-creation process.
     * This defines how great a possible factor we will permit a harvest to
     * be larger then the expectation, when basing the expectation on previous
     * uncompleted harvests or no harvest data at all.
     */
    public static final String ERRORFACTOR_PERMITTED_BESTGUESS
            = "settings.harvester.scheduler.errorFactorBestGuess";

    /**
     * How many bytes the average object is expected to be on domains where
     * we don't know any better.  This number should grow over time, as of
     * end of 2005 empirical data shows 38000.
     */
    public static final String EXPECTED_AVERAGE_BYTES_PER_OBJECT
            = "settings.harvester.scheduler.expectedAverageBytesPerObject";

    /** Initial guess of #objects in an unknown domain */
    public static final String MAX_DOMAIN_SIZE
            = "settings.harvester.scheduler.maxDomainSize";

    /**
     * The maximum allowed relative difference in expected number of objects
     * retrieved in a single job definition.  Set to MAX_LONG for no splitting.
     */
    public static final String JOBS_MAX_RELATIVE_SIZE_DIFFERENCE
            = "settings.harvester.scheduler.jobs.maxRelativeSizeDifference";

    /**
     * Size differences for jobs below this threshold are ignored,
     * regardless of the limits for the relative size difference.  Set to
     * MAX_LONG for no splitting.
     */
    public static final String JOBS_MIN_ABSOLUTE_SIZE_DIFFERENCE
            = "settings.harvester.scheduler.jobs.minAbsoluteSizeDifference";

    /**
     * When this limit is exceeded no more configurations may be added to a job.
     * Set to MAX_LONG for no splitting.
     */
    public static final String JOBS_MAX_TOTAL_JOBSIZE
            = "settings.harvester.scheduler.jobs.maxTotalSize";

    /**
     * How many domain configurations we will process in one go before
     * making jobs out of them.  This amount of domains will be stored in
     * memory at the same time.  Set to MAX_LONG for no job splitting.
     */
    public static final String MAX_CONFIGS_PER_JOB_CREATION
            = "settings.harvester.scheduler.configChunkSize";

    /**
     * Each job gets a subdir of this dir. Job data is written and
     * Heritrix writes to that subdir.
     */
    public static final String HARVEST_CONTROLLER_SERVERDIR
            = "settings.harvester.harvesting.serverDir";


    /**
     * Check, that the serverdir has an adequate amount of bytes
     * available before accepting any harvest-jobs.
     */
    public static final String HARVEST_SERVERDIR_MINSPACE
    		= "settings.harvester.harvesting.minSpaceLeft";

    /**
     * The directory in which data from old jobs is kept after uploading.  Each
     * directory from serverDir will be moved to here if any data remains,
     * either due to failed uploads or because it wasn't attempted uploaded.
     */
    public static final String HARVEST_CONTROLLER_OLDJOBSDIR
            = "settings.harvester.harvesting.oldjobsDir";

    /** Pool to take jobs from */
    public static final String HARVEST_CONTROLLER_PRIORITY
            = "settings.harvester.harvesting.queuePriority";

    /**
     * The timeout setting for aborting a crawl based on crawler-inactivity.
     * If the crawler is inactive for this amount of seconds the crawl will
     * be aborted.
     * The inactivity is measured on the crawlController.activeToeCount().
     */
    public static final String INACTIVITY_TIMEOUT_IN_SECS
            = "settings.harvester.harvesting.heritrix.inactivityTimeout";

    /**
     * The timeout value (in seconds) used in HeritrixLauncher for aborting
     * crawl.
     * when no bytes are being received from web servers.
     */
    public static final String CRAWLER_TIMEOUT_NON_RESPONDING
            = "settings.harvester.harvesting.heritrix.noresponseTimeout";

    /** The name used to access the Heritrix GUI */
    public static final String HERITRIX_ADMIN_NAME
            = "settings.harvester.harvesting.heritrix.adminName";

    /** The password used to access the Heritrix GUI */
    public static final String HERITRIX_ADMIN_PASSWORD
            = "settings.harvester.harvesting.heritrix.adminPassword";

    /** Port used to access the Heritrix web user interface.
     *  This port must not be used by anything else on the machine. */
    public static final String HERITRIX_GUI_PORT
            = "settings.harvester.harvesting.heritrix.guiPort";

    /** The port that Heritrix uses to expose its JMX interface.  This port
     *  must not be used by anything else on the machine, but does not need to
     *  be accessible from other machines unless you want to be able to use
     *  jconsole to access Heritrix directly. Note that apart from pausing
     *  a job, modifications done directly on Heritrix may cause unexpected
     *  breakage. */
    public static final String HERITRIX_JMX_PORT
            = "settings.harvester.harvesting.heritrix.jmxPort";

    /** The heap size to use for the Heritrix sub-process.  This should
     * probably be fairly large.
     */
    public static final String HERITRIX_HEAP_SIZE
            = "settings.harvester.harvesting.heritrix.heapSize";

    /**
     * The file used to signal that the harvest controller is running.
     * Sidekick starts HarvestController if this file is not present
     */
    public static final String HARVEST_CONTROLLER_ISRUNNING_FILE
            = "settings.harvester.harvesting.isrunningFile";

    /**
     * The subclass of SiteSection that defines a part of the
     * web interface.
     */
    public static final String SITESECTION_CLASS
            = "settings.common.webinterface.siteSection.class";

    /**
     * The directory or war-file containing the web application
     * for a site section.
     */
    public static final String SITESECTION_WEBAPPLICATION
            = "settings.common.webinterface.siteSection.webapplication";

    /** The URL path for this site section. */
    public static final String SITESECTION_DEPLOYPATH
            = "settings.common.webinterface.siteSection.deployPath";

    /** The entire webinterface structure */
    public static final String WEBINTERFACE_SUBTREE
            = "settings.common.webinterface";

    /** A locale the website is available as. E.g. 'en_US' or 'da' */
    public static final String LANGUAGE_LOCALE
            = "settings.common.webinterface.language.locale";

    /**
     * The native name of the language for the website locale. E.g. 'Dansk' or
     * 'English'.
     */
    public static final String LANGUAGE_NAME
            = "settings.common.webinterface.language.name";

    /**
     * Absolute/relative path to where the "central list of files and
     * checksums" (admin.data) is written. Used by ArcRepository and
     * BitPreservation.
     */
    public static final String DIRS_ARCREPOSITORY_ADMIN
            = "settings.archive.arcrepository.baseDir";

    /**
     * The names of all bit archive locations in the
     * environment, e.g., "KB" and "SB".
     */
    public static final String ENVIRONMENT_LOCATION_NAMES
            = "settings.archive.arcrepository.location.name";

    /** Default bit archive to use for batch jobs (if none is specified) */
    public static final String ENVIRONMENT_BATCH_LOCATION
            = "settings.archive.arcrepository.batchLocation";

    /**
     * The minimum amount of bytes left *in any dir* that we will allow a
     * bitarchive machine to accept uploads with.  When no dir has more space
     * than this, the bitarchive machine stops listening for uploads.  This
     * values should at the very least be greater than the largest ARC file
     * you expect to receive.
     */
    public static final String BITARCHIVE_MIN_SPACE_LEFT
            = "settings.archive.bitarchive.minSpaceLeft";

    /**
     * These are the directories where ARC files are stored (in a subdir).
     * If more than one is given, they are used from one end.
     */
    public static final String BITARCHIVE_SERVER_FILEDIR
            = "settings.archive.bitarchive.fileDir";

    /**
     * The frequency in milliseconds of heartbeats that are sent by each
     * BitarchiveServer to the BitarchiveMonitor.
     */
    public static final String BITARCHIVE_HEARTBEAT_FREQUENCY
            = "settings.archive.bitarchive.heartbeatFrequency";

    /**
     * If we haven't heard from a bit archive within this many milliseconds,
     * we don't excpect it to be online and won't wait for them to reply on a
     * batch job.  This number should be significantly greater than
     * heartbeatFrequency to account for temporary network congestion.
     */
    public static final String BITARCHIVE_ACCEPTABLE_HEARTBEAT_DELAY
            = "settings.archive.bitarchive.acceptableHeartbeatDelay";

    /**
     * The BitarchiveMonitorServer will listen for BatchEndedMessages for this
     * many milliseconds before it decides that a batch job is taking too long
     * and returns just the replies it has received at that point.
     */
    public static final String BITARCHIVE_BATCH_JOB_TIMEOUT
            = "settings.archive.bitarchive.batchMessageTimeout";

    /** For archiving applications, which bit archive are you part of? */
    public static final String ENVIRONMENT_THIS_LOCATION
            = "settings.archive.bitarchive.thisLocation";

    /**
     * Credentials to enter in the GUI for "deleting" ARC files in
     * this bit archive.
     */
    public static final String ENVIRONMENT_THIS_CREDENTIALS
            = "settings.archive.bitarchive.thisCredentials";

    /**
     * When the length record exceeds this number, the contents of the record
     * will be transferred using a RemoteFile. Currently set to 31 MB
     * ( Integer.MAX_VALUE / 64) -->
     */
    public static final String BITARCHIVE_LIMIT_FOR_RECORD_DATATRANSFER_IN_FILE
            = "settings.archive.bitarchive.limitForRecordDatatransferInFile";

    /**
     * Absolute or relative path to dir containing results of
     * file-list-batch-jobs and checksumming batch jobs for bit preservation
     */
    public static final String DIR_ARCREPOSITORY_BITPRESERVATION
            = "settings.archive.bitpreservation.baseDir";

    /**
     * The main directory for the ViewerProxy, used for storing the Lucene
     * index for the jobs being viewed.
     */
    public static final String VIEWERPROXY_DIR = "settings.viewerproxy.baseDir";

    /** The name of the application, fx. "BitarchiveServerApplication". */
    public static final String APPLICATIONNAME
            = "settings.monitor.applicationName";

    /**
     * The number of logmessages from each application visible in the
     * monitor.
     */
    public static final String LOGGING_HISTORY_SIZE
            = "settings.monitor.logging.historySize";

    /**
     * The mail server to use when sending mails. Currently only used for
     * email notifications.
     */
    public static final String MAIL_SERVER = "settings.common.mail.server";

    /** The receiver of email notifications. */
    public static final String MAIL_RECEIVER
            = "settings.common.notifications.receiver";

    /** The sender of email notifications. */
    public static final String MAIL_SENDER
            = "settings.common.notifications.sender";

    /** The implementation class for notifications. */
    public static final String NOTIFICATIONS_CLASS
            = "settings.common.notifications.class";

    /** Which port to use for JMX. */
    public static final String JMX_PORT = "settings.common.jmx.port";

    /** Which port to use for JMX's RMI communication. */
    public static final String JMX_RMI_PORT = "settings.common.jmx.rmiPort";

    /** Which file to look for JMX passwords in. */
    public static final String JMX_PASSWORD_FILE
            = "settings.common.jmx.passwordFile";

    /** Which class to use for monitor registry. Must implement the interface
     * dk.netarkivet.common.distribute.monitorregistry.MonitorRegistryClient. */
    public static final String MONITOR_REGISTRY_CLIENT
            = "settings.common.monitorregistryClient.class";

}
