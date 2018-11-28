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
package dk.netarkivet.common;

import dk.netarkivet.common.utils.Settings;

/**
 * Settings common to the entire NetarchiveSuite.
 */
public class CommonSettings {
    /** The default place in classpath where the settings file can be found. */
    private static final String DEFAULT_SETTINGS_CLASSPATH = "dk/netarkivet/common/settings.xml";

    /*
     * The static initialiser is called when the class is loaded. It will add default values for all settings defined in
     * this class, by loading them from a settings.xml file in classpath.
     */
    static {
        Settings.addDefaultClasspathSettings(DEFAULT_SETTINGS_CLASSPATH);
    }

    // NOTE: The constants defining setting names below are left non-final on
    // purpose! Otherwise, the static initialiser that loads default values
    // will not run.

    /** The path in the XML-tree for the settings branch. (Used by deploy). */
    public static String SETTINGS = "settings";

    /**
     * <b>settings.common.environmentName</b>: <br>
     * The name of the environment in which this code is running. E.g. PROD, RELEASETEST. It is used as a Common prefix
     * to all JMS channels created in a NetarchiveSuite installation.
     */
    public static String ENVIRONMENT_NAME = "settings.common.environmentName";

    /**
     * <b>settings.common.tempDir</b>: <br>
     * Common temporary directory for all applications. Some subdirs of this directory must be set to have AllPermision
     * in the conf/security.conf file, or the web pages won't work.
     */
    public static String DIR_COMMONTEMPDIR = "settings.common.tempDir";

    /**
     * <b>settings.common.remoteFile.class</b>: <br>
     * The class to use for RemoteFile objects (for transferring files around). This class must implement the
     * dk.netarkivet.common.distribute.RemoteFile interface.
     */
    public static String REMOTE_FILE_CLASS = "settings.common.remoteFile.class";

    /**
     * <b>settings.common.jms.class</b>: <br>
     * Selects the broker class to be used. Must be subclass of dk.netarkivet.common.distribute.JMSConnection.
     */
    public static String JMS_BROKER_CLASS = "settings.common.jms.class";

    /**
     * <b>settings.common.jms.retries</b>: <br>
     * Selects the number of times the JMS connection tries to reconnect to the broker.
     */
    public static String JMS_BROKER_RETRIES = "settings.common.jms.retries";

    /**
     * <b>settings.common.http.port</b>: <br>
     * The *unique* (per host) port number that may or may not be used to serve http.
     */
    public static String HTTP_PORT_NUMBER = "settings.common.http.port";

    /**
     * <b>settings.common.arcrepositoryClient.class</b>: <br>
     * The class that implements the ArcRepositoryClient. The class must implement the interface
     * dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClient This class will be instantiated by the
     * ArcRepositoryClientFactory.
     */
    public static String ARC_REPOSITORY_CLIENT = "settings.common.arcrepositoryClient.class";

    /**
     * <b>settings.common.batch.maxExceptions</b>: <br>
     * The maximum number of exceptions to be stored for a batch job.
     */
    public static String MAX_NUM_BATCH_EXCEPTIONS = "settings.common.batch.maxExceptions";

    /**
     * <b>settings.common.batch.loggingInterval</b>: <br/>
     * The time between logging the status of a batch job.
     */
    public static String BATCH_LOGGING_INTERVAL = "settings.common.batch.loggingInterval";

    /**
     * <b>settings.common.batch.defaultBatchTimeout</b>: <br/>
     * The default timeout for batchjobs. This will be used for batchjobs which has the batchJobTimeout set to -1
     * (inherited value from FileBatchJob).
     */
    public static String BATCH_DEFAULT_TIMEOUT = "settings.common.batch.defaultBatchTimeout";

    /**
     * <b>settings.common.monitorregistryClient.class</b>: <br>
     * Which class to use for monitor registry. Must implement the interface
     * dk.netarkivet.common.distribute.monitorregistry.MonitorRegistryClient.
     */
    public static String MONITOR_REGISTRY_CLIENT = "settings.common.monitorregistryClient.class";

    /**
     * <b>settings.common.monitorregistryClient.reregisterdelay</b>: <br>
     * Delay between every reregistering in minutes, e.g. 1 for one minute.
     */
    public static String MONITOR_REGISTRY_CLIENT_REREGISTERDELAY = "settings.common.monitorregistryClient.reregisterdelay";

    /**
     * <b>settings.common.indexClient.class</b>: <br>
     * The class instantiated to give access to indices. The class must implement the interface
     * dk.netarkivet.common.distribute.indexserver.JobIndexCache The class instantiations are manufactored by
     * IndexClientFactory.
     */
    public static String INDEXSERVER_CLIENT = "settings.common.indexClient.class";

    /**
     * <b>settings.common.unixSort.useCommonTempDir</b>: <br>
     * When using the Unix sort command, by default it stores temporary files in the system temp dir, but if this
     * setting is set to true, then it will be configured to use the common temp dir defined by common.settings.tempDir.
     * By default the value is false (use system temp), which is the legacy behavior. This setting is part of the fix
     * for Jira issue NAS-1995.
     *
     * @see #DIR_COMMONTEMPDIR
     */
    public static String UNIX_SORT_USE_COMMON_TEMP_DIR = "settings.common.unixSort.useCommonTempDir";

    /**
     * <b>settings.common.cacheDir</b>: <br>
     * The name of the directory where cache data global to the entire machine can be stored. Various kinds of caches
     * should be stored in subdirectories of this.
     */
    public static String CACHE_DIR = "settings.common.cacheDir";

    // TODO Currently only used by heritrix shutdown - move to harvester
    // settings?
    /**
     * <b>settings.common.processTimeout</b>: <br>
     * The number of milliseconds we wait for processes to react to shutdown requests.
     */
    public static String PROCESS_TIMEOUT = "settings.common.processTimeout";

    /**
     * <b>settings.common.notifications.class</b>: <br>
     * The implementation class for notifications, e.g. error notification. The class must extend
     * dk.netarkivet.common.utils.Notifications
     */
    public static String NOTIFICATIONS_CLASS = "settings.common.notifications.class";

    /**
     * <b>settings.common.mail.server</b>: <br>
     * The mail server to use when sending mails.
     */
    public static String MAIL_SERVER = "settings.common.mail.server";

    /**
     * <b>settings.common.jmx.port</b>: <br>
     * The port to use for JMX.
     */
    public static String JMX_PORT = "settings.common.jmx.port";

    /**
     * <b>settings.common.jmx.rmiPort</b>: <br>
     * The JMX's RMI port to use for internal communication with beans.
     */
    public static String JMX_RMI_PORT = "settings.common.jmx.rmiPort";

    /**
     * <b>settings.common.jmx.passwordFile</b>: <br>
     * The password file, containing information about who may connect to the beans. The file has a format defined by
     * the JMX standard,
     *
     * @see <URL:http://java.sun.com/j2se/1.5.0/docs/guide/management/agent.html#PasswordAccessFiles>
     */
    public static String JMX_PASSWORD_FILE = "settings.common.jmx.passwordFile";

    /**
     * <b>settings.common.jmx.accessFile</b>: <br>
     * The access file, containing information about who have which JMX roles have which access privileges. The file has
     * a format defined by the JMX standard,
     *
     * @see <URL:http://java.sun.com/j2se/1.5.0/docs/guide/management/agent.html#PasswordAccessFiles>
     */
    public static String JMX_ACCESS_FILE = "settings.common.jmx.accessFile";

    /**
     * <b>settings.common.jmx.timeout</b>: <br>
     * How many seconds we will wait before giving up on a JMX connection.
     */
    public static String JMX_TIMEOUT = "settings.common.jmx.timeout";

    /**
     * <b>settings.common.webinterface</b>: <br>
     * The entire webinterface setting structure.
     */
    public static String WEBINTERFACE_SETTINGS = "settings.common.webinterface";

    /**
     * <b>settings.common.webinterface.hideInactiveTemplates</b>;<br/>
     * Boolean valued String. If true, inactive harvest templates are entirely hidden in the GUI - it will be impossible
     * to see them or change their state to "active". If false, the GUI will include functionality to change the
     * status of template from inactive to active and vice-versa.
     */
    public static String HIDE_INACTIVE_TEMPLATES = "settings.common.webinterface.hideInactiveTemplates";

    /**
     * settings.common.webinterface.<b>language</b>: <br>
     * The entire language setting structure under the webinterface setting. Is repeated for each language
     */
    public static String WEBINTERFACE_LANGUAGE = "language";

    /**
     * settings.common.webinterface.language.<b>locale</b>: <br>
     * The locale the GUI is available as under specific language setting.
     */
    public static String WEBINTERFACE_LANGUAGE_LOCALE = "locale";

    /**
     * settings.common.webinterface.language.<b>name</b>: <br>
     * The native name of the language for the locale under specific language setting.
     */
    public static String WEBINTERFACE_LANGUAGE_NAME = "name";

    /**
     * <b>settings.common.webinterface.siteSection.class</b>: <br>
     * The subclass of SiteSection that defines a part of the web interface. Is part of repeated siteSection settings
     * for each part.
     */
    public static String SITESECTION_CLASS = "settings.common.webinterface.siteSection.class";

    /**
     * <b>settings.common.webinterface.siteSection.webapplication</b>: <br>
     * The directory or war-file containing the web application for a site section. Is part of repeated siteSection
     * settings for each part.
     */
    public static String SITESECTION_WEBAPPLICATION = "settings.common.webinterface.siteSection.webapplication";

    /**
     * <b>settings.common.webinterface.harvestStatus.defaultPageSize</b>: <br>
     * The default number of jobs to show in the harvest status section, on one result page.
     */
    public static String HARVEST_STATUS_DFT_PAGE_SIZE = "settings.common.webinterface.harvestStatus.defaultPageSize";
    
    /**
     * <b>settings.common.topLevelDomains.tld</b>: <br>
     * Extra valid top level domain, like .co.uk, .dk, .org., not part of current embedded public_suffix_list.dat file 
     * in common/common-core/src/main/resources/dk/netarkivet/common/utils/public_suffix_list.dat
     * downloaded from https://www.publicsuffix.org/list/public_suffix_list.dat
     */
    public static String TLDS = "settings.common.topLevelDomains.tld";
    
    /**
     * <b>settings.common.topLevelDomains.silentlyIgnoreStarredTlds</b>: <br>
     * If set to true, silently ignores starred TLDS and excluded tlds which are currently not supported by NetarchiveSuite (NAS-2689).
     * default: true
     */
    public static String TLD_SILENTLY_IGNORE_STARRED_TLDS = "settings.common.topLevelDomains.silentlyIgnoreStarredTlds";
        
    // TODO move database settings to harvester settings?
    /**
     * <b>settings.common.database.class</b>: <br>
     * The class that defines DB-specific methods. This class must extend the DBSpecifics class
     */
    public static String DB_SPECIFICS_CLASS = "settings.common.database.class";

    /**
     * If DB_SPECIFICS_CLASS contains this string then a Derby database is in use.
     */
    public static String DB_IS_DERBY_IF_CONTAINS = "Derby";

    /**
     * <b>settings.common.database.baseUrl</b>: <br>
     * The URL to use to connect to the database specified in the DB_SPECIFICS_CLASS setting.
     */
    public static String DB_BASE_URL = "settings.common.database.baseUrl";

    /**
     * <b>settings.common.database.machine</b>: <br>
     * Used for the external harvest definition database. The machine where the harvest definition database is located.
     */
    public static String DB_MACHINE = "settings.common.database.machine";

    /**
     * <b>settings.common.database.port</b>: <br>
     * Used for the external harvest definition database. The port where the external harvest definition database is
     * attached.
     */
    public static String DB_PORT = "settings.common.database.port";

    /**
     * <b>settings.common.database.dir</b>: <br>
     * Used for the external harvest definition database. The directory where the external harvest definition database
     * is located.
     */
    public static String DB_DIR = "settings.common.database.dir";

    /**
     * <b>settings.common.database.validityCheckTimeout</b>: <br>
     * Timeout in seconds to check for the validity of a JDBC connection on the server. This is the time in seconds to
     * wait for the database operation used to validate the connection to complete. If the timeout period expires before
     * the operation completes, this method returns false. A value of 0 indicates a timeout is not applied to the
     * database operation.
     * <p>
     * {@link java.sql.Connection#isValid(int)}
     */
    public static String DB_CONN_VALID_CHECK_TIMEOUT = "settings.common.database.validityCheckTimeout";

    /**
     * <b>settings.common.database.pool.minSize</b>: <br>
     * Configure the minimum size of the DB connection pool. Default value is 5.
     */
    public static String DB_POOL_MIN_SIZE = "settings.common.database.pool.minSize";

    /**
     * <b>settings.common.database.pool.maxSize</b>: <br>
     * Configure the maximum size of the DB connection pool. Default value is 10.
     */
    public static String DB_POOL_MAX_SIZE = "settings.common.database.pool.maxSize";

    /**
     * <b>settings.common.database.pool.acquireInc</b>: <br>
     * Configure the increment size DB connection pool. Default value is 5 (half the max size).
     */
    public static String DB_POOL_ACQ_INC = "settings.common.database.pool.acquireInc";

    /**
     * <b>settings.common.database.pool.maxStm</b>: <br>
     * Configure statement pooling, by setting the global maximum number of pooled prepared statements for a data
     * source. Default value is 0. Note that if both {@link #DB_POOL_MAX_STM} and {@link #DB_POOL_MAX_STM_PER_CONN} are
     * set to zero, statement pooling is fully deactivated.
     *
     * @see <a href="http://www.mchange.com/projects/c3p0/index.html">c3p0 documentation</a>
     */
    public static String DB_POOL_MAX_STM = "settings.common.database.pool.maxStm";

    /**
     * <b>settings.common.database.pool.maxStmPerConn</b>: <br>
     * Configure statement pooling, by setting the global maximum number of pooled prepared statements for a data
     * source. Default value is 0. Note that if both {@link #DB_POOL_MAX_STM} and {@link #DB_POOL_MAX_STM_PER_CONN} are
     * set to zero, statement pooling is fully deactivated.
     *
     * @see <a href="http://www.mchange.com/projects/c3p0/index.html">c3p0 documentation</a>
     */
    public static String DB_POOL_MAX_STM_PER_CONN = "settings.common.database.pool.maxStmPerConn";

    /**
     * <b>settings.common.database.pool.idleConnTestPeriod</b>: <br>
     * Configure idle connection testing period in seconds. Default is 0, which means no idle connection testing
     *
     * @see <a href="http://www.mchange.com/projects/c3p0/index.html#idleConnectionTestPeriod"/>
     */
    public static String DB_POOL_IDLE_CONN_TEST_PERIOD = "settings.common.database.pool.idleConnTestPeriod";

    /**
     * <b>settings.common.database.pool.idleConnTestOnCheckin</b>: <br>
     * Configure if a connection validity should be checked when returned to the pool. Default is false.
     *
     * @see <a href="http://www.mchange.com/projects/c3p0/index.html#testConnectionOnCheckin"/>
     */
    public static String DB_POOL_IDLE_CONN_TEST_ON_CHECKIN = "settings.common.database.pool.idleConnTestOnCheckin";

    /**
     * <b>settings.common.database.pool.idleConnTestQuery</b>: <br>
     * The SQL query to be used when testing an idle connection. Default is empty, which means using c3p0 defaults.
     *
     * @see <a href="http://www.mchange.com/projects/c3p0/index.html#preferredTestQuery"/>
     */
    public static String DB_POOL_IDLE_CONN_TEST_QUERY = "settings.common.database.pool.idleConnTestQuery";

    /**
     * <b>settings.common.database.pool.maxConnectionAge</b>: <br>
     * The maxConnectionAge in seconds for each connection in the pool.
     * The default is one hour (3600 seconds)
     *
     * @see <a href="http://www.mchange.com/projects/c3p0/index.html#maxConnectionAge"/>
     */
    public static String DB_POOL_MAX_CONNECTION_AGE = "settings.common.database.pool.maxConnectionAge";
    
    /**
     * The username for login to the harvest database.
     */
    public static String DB_USERNAME = "settings.common.database.username";

    /**
     * The password for login to the harvest database.
     */
    public static String DB_PASSWORD = "settings.common.database.password";

    /**
     * <b>settings.common.repository.limitForRecordDatatransferInFile</b>: <br>
     * When the length record exceeds this number, the contents of the record will be transferred using a RemoteFile.
     */
    public static String BITARCHIVE_LIMIT_FOR_RECORD_DATATRANSFER_IN_FILE = "settings.common.repository.limitForRecordDatatransferInFile";

    /**
     * <b>settings.common.replicas</b>: <br>
     * The entire settings for all replicas in the environment. NOTE: settings for checksum replicas are not use yet
     */
    public static String REPLICAS_SETTINGS = "settings.common.replicas";

    /**
     * settings.common.replicas.<b>replica</b>: <br>
     * The path to settings belonging to an individual replica, placed under the replicas setting.
     */
    public static String REPLICA_TAG = "replica";

    /**
     * settings.common.replicas.replica.<b>replicaId</b>: <br>
     * The tags for identifier of the replica, placed under the replica tag. The replica id is used internally in e.g.
     * naming of channels.
     */
    public static String REPLICAID_TAG = "replicaId";
    /**
     * settings.common.replicas.replica.<b>replicaName</b>: <br>
     * The tags for name of the replica, placed under the replica tag. The replica name is used in interfaces like the
     * GUI or command-line batch-programs. The name can be the same value as the id.
     */
    public static String REPLICANAME_TAG = "replicaName";
    /**
     * settings.common.replicas.replica.<b>replicaType</b>: <br>
     * The tags for type of the replica, placed under the replica tag. The type is used to identify whether it is a
     * bitarchive or a checksum replica. NOTE: checksum replicas are not implemented yet Possible values are defined in
     * ReplicaType
     */
    public static String REPLICATYPE_TAG = "replicaType";

    /**
     * <b>settings.common.replicas.replica.replicaId</b>: <br>
     * The identifiers of all replicas in the environment.
     */
    public static String REPLICA_IDS = REPLICAS_SETTINGS + "." + REPLICA_TAG + "." + REPLICAID_TAG;

    /**
     * <b>settings.common.useReplicaId</b>: <br>
     * Default bitarchive to use for e.g. batch jobs (if none is specified).
     */
    public static String USE_REPLICA_ID = "settings.common.useReplicaId";

    /**
     * <b>settings.common.thisPhysicalLocation</b>: <br>
     * Physical location of where the application is running. Only use for System state GUI and deploy
     */
    public static String THIS_PHYSICAL_LOCATION = "settings.common.thisPhysicalLocation";

    /**
     * <b>settings.common.applicationName</b>: <br>
     * The name of the application, e.g. "BitarchiveServerApplication". The monitor puts this with each log message.
     */
    public static String APPLICATION_NAME = "settings.common.applicationName";

    /**
     * <b>settings.common.applicationInstanceId</b>: <br>
     * The identifier of the instance of the application. This is used when there are more than one of the same
     * application running on the same machine, e.g. when more harvesters are running on the same machine or more
     * bitarchive applications are running on the same machine.
     */
    public static String APPLICATION_INSTANCE_ID = "settings.common.applicationInstanceId";

    /**
     * <b>settings.common.freespaceprovider.class</b>: <br>
     * The implementation class for free space provider, e.g. dk.netarkivet.common.utils.DefaultFreeSpaceProvider. The
     * class must implement FreeSpaceProvider-Interface.
     */
    public static String FREESPACE_PROVIDER_CLASS = "settings.common.freespaceprovider.class";

    /**
     * <b>settings.common.batch.batchjobs.batchjob.class</b>: <br/>
     * The list of batchjobs to be runnable from the GUI. Must be the complete path to the batchjob classes (e.g.
     * dk.netarkivet.archive.arcrepository.bitpreservation.ChecksumJob). Must inherit FileBatchJob.
     */
    public static String BATCHJOBS_CLASS = "settings.common.batch.batchjobs.batchjob.class";

    /**
     * <b>settings.common.batch.batchjobs.batchjob.jarfile</b>: <br/>
     * The list of the corresponding jar-files containing the batchjob. This will be used for LoadableJarBatchJobs. If
     * no file is specified, it is assumed, that the batchjob exists with the default classpath of the involved
     * applications (BitarchiveMonitor, ArcRepository, GUIWebServer and BitArchive).
     */
    public static String BATCHJOBS_JARFILE = "settings.common.batch.batchjobs.batchjob.jarfile";

    /**
     * <b>settings.common.batch.baseDir</b>: <br/>
     * The directory where the resulting files will be placed when running a batchjob through the GUI interface.
     */
    public static String BATCHJOBS_BASEDIR = "settings.common.batch.baseDir";
    /**
     * <b>settings.common.remoteFile.serverName</b>: <br>
     * The setting for the FTP-server used.
     */
    public static String FTP_SERVER_NAME = "settings.common.remoteFile.serverName";
    /**
     * <b>settings.common.remoteFile.serverPort</b>: <br>
     * The setting for the FTP-server port used.
     */
    public static String FTP_SERVER_PORT = "settings.common.remoteFile.serverPort";
    /**
     * <b>settings.common.remoteFile.userName</b>: <br>
     * The setting for the FTP username.
     */
    public static String FTP_USER_NAME = "settings.common.remoteFile.userName";
    /**
     * <b>settings.common.remoteFile.userPassword</b>: <br>
     * The setting for the FTP password. *
     */
    public static String FTP_USER_PASSWORD = "settings.common.remoteFile.userPassword";
    /**
     * <b>settings.common.remoteFile.retries</b>: <br>
     * The setting for the number of times FTPRemoteFile should try before giving up a copyTo operation or logOn
     * operation.
     */
    public static String FTP_RETRIES_SETTINGS = "settings.common.remoteFile.retries";
    /**
     * <b>settings.common.remoteFile.datatimeout</b>: <br>
     * The setting for the FTP data timeout in seconds. The default value is 600 (10 minutes).
     */
    public static String FTP_DATATIMEOUT_SETTINGS = "settings.common.remoteFile.datatimeout";

    /**
     * <b>settings.common.organization</b>: <br>
     * The name of the organization responsible. The default value is netarkivet.dk
     */
    public static String ORGANIZATION = "settings.common.organization";

    /**
     * Specifies the suffix of a regex which can identify valid metadata files by job number. Thus preceding
     * the value of this setting with .* will find all metadata files.
     */
    public static String METADATAFILE_REGEX_SUFFIX = "settings.common.metadata.fileregexsuffix";
    
}
