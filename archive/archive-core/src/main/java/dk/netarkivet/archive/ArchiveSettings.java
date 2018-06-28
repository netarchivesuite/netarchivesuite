/*
 * #%L
 * Netarchivesuite - archive
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
package dk.netarkivet.archive;

import dk.netarkivet.common.utils.Settings;

/**
 * Settings specific to the archive module of NetarchiveSuite.
 */
public class ArchiveSettings {
    /** The default place in classpath where the settings file can be found. */
    private static final String DEFAULT_SETTINGS_CLASSPATH = "dk/netarkivet/archive/settings.xml";

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

    /**
     * <b>settings.archive.arcrepository.baseDir</b>: <br>
     * Absolute/relative path to where the "central list of files and checksums" (admin.data) is written. Used by
     * ArcRepository and BitPreservation.
     */
    public static String DIRS_ARCREPOSITORY_ADMIN = "settings.archive.arcrepository.baseDir";

    /**
     * <b>settings.archive.arcrepository.uploadRetries</b>: <br>
     * The maximum number of times an arc file is attempted uploaded if the initial upload fails.
     */
    public static String ARCREPOSITORY_UPLOAD_RETRIES = "settings.archive.arcrepository.uploadRetries";

    /**
     * <b>settings.archive.bitarchive.minSpaceLeft</b>: <br>
     * The minimum amount of bytes left *in any dir* that we will allow a bitarchive machine to accept uploads with.
     * When no dir has more space than this, the bitarchive machine stops listening for uploads. This value should at
     * the very least be greater than the largest ARC file you expect to receive.
     */
    public static String BITARCHIVE_MIN_SPACE_LEFT = "settings.archive.bitarchive.minSpaceLeft";

    /**
     * <b>settings.archive.bitarchive.minSpaceRequired</b>: <br>
     * The minimum amount of bytes required left *in all dirs* after we have accepted an upload. If no dir has enough
     * space to store a received file and still have at least this much space left, the operation will result in an
     * error.
     */
    public static String BITARCHIVE_MIN_SPACE_REQUIRED = "settings.archive.bitarchive.minSpaceRequired";

    /**
     * <b>settings.archive.bitarchive.baseFileDir</b>: <br>
     * These are the directories where ARC files are stored (in a subdir). If more than one is given, they are used from
     * one end. This setting may be repeated, to define multiple directories. Note: This value should always be so much
     * lower than minSpaceRequired, that the difference matches at least the size of files we expect to receive.
     */
    public static String BITARCHIVE_SERVER_FILEDIR = "settings.archive.bitarchive.baseFileDir";

    /**
     * <b>settings.archive.bitarchive.readOnlyAllowed</b>: <br>
     * true/false - The possibility to have ready-only directories in your list (default false) - If true the
     * application is not throwing a PermissionDenied Exception
     */
    public static String BITARCHIVE_READ_ONLY_ALLOWED = "settings.archive.bitarchive.readOnlyAllowed";

    /**
     * <b>settings.archive.bitarchive.heartbeatFrequency</b>: <br>
     * The frequency in milliseconds of heartbeats that are sent by each BitarchiveServer to the BitarchiveMonitor.
     */
    public static String BITARCHIVE_HEARTBEAT_FREQUENCY = "settings.archive.bitarchive.heartbeatFrequency";

    /**
     * <b>settings.archive.bitarchive.acceptableHeartbeatDelay</b>: <br>
     * If we haven't heard from a bit archive within this many milliseconds, we don't expect it to be online and won't
     * wait for them to reply on a batch job. This number should be significantly greater than heartbeatFrequency to
     * account for temporary network congestion.
     */
    public static String BITARCHIVE_ACCEPTABLE_HEARTBEAT_DELAY = "settings.archive.bitarchive.acceptableHeartbeatDelay";

    /**
     * <b>settings.archive.bitarchive.batchMessageTimeout</b>: <br>
     * The BitarchiveMonitorServer will listen for BatchEndedMessages for this many milliseconds before it decides that
     * a batch job is taking too long and returns just the replies it has received at that point.
     */
    public static String BITARCHIVE_BATCH_JOB_TIMEOUT = "settings.archive.bitarchive.batchMessageTimeout";

    /**
     * <b>settings.archive.bitarchive.thisCredentials</b>: <br>
     * Credentials to enter in the GUI for "deleting" ARC files in this bit archive.
     */
    public static String ENVIRONMENT_THIS_CREDENTIALS = "settings.archive.bitarchive.thisCredentials";

    /**
     * <b>settings.archive.bitpreservation.baseDir</b>: <br>
     * Absolute or relative path to dir containing results of file-list-batch-jobs and checksumming batch jobs for bit
     * preservation.
     */
    public static String DIR_ARCREPOSITORY_BITPRESERVATION = "settings.archive.bitpreservation.baseDir";

    /**
     * <b>settings.archive.admin.class</b>: <br>
     * The path to the settings for the adminstration instance class.
     */
    public static String ADMIN_CLASS = "settings.archive.admin.class";

    /**
     * <b>settings.archive.admin.database.baseUrl</b>: <br>
     * Setting for giving the base URL to the database used by the ReplicaCacheDatabase class. It has the default value:
     * 'jdbc:derby'. Do not retrieve this directly, use in stead the function
     * dk.netarkivet.archive.arcrepositoryadmin.DBConnect.getArchiveUrl().
     * <p>
     * If a specific url is wanted, and not constructed from the 4 different parts, just assign the entire URL to this
     * setting and set the other settings to the empty string.
     */
    public static String BASEURL_ARCREPOSITORY_ADMIN_DATABASE = "settings.archive.admin.database.baseUrl";

    /**
     * <b>settings.archive.admin.database.machine</b>: <br>
     * Setting for giving the machine of the external database used by the ReplicaCacheDatabase class. It is default:
     * 'localhost'. If this is empty, then it will be ignored. Do not retrieve this directly, use in stead the function
     * dk.netarkivet.archive.arcrepositoryadmin.DBConnect.getArchiveUrl().
     */
    public static String MACHINE_ARCREPOSITORY_ADMIN_DATABASE = "settings.archive.admin.database.machine";

    /**
     * <b>settings.archive.admin.database.port</b>: <br>
     * Setting for giving the port of the external database used by the ReplicaCacheDatabase class. It is default:
     * '1527'. If this is empty, then it will be ignored. Do not retrieve this directly, use in stead the function
     * dk.netarkivet.archive.arcrepositoryadmin.DBConnect.getArchiveUrl().
     */
    public static String PORT_ARCREPOSITORY_ADMIN_DATABASE = "settings.archive.admin.database.port";

    /**
     * <b>settings.archive.admin.database.dir</b>: <br>
     * Setting for giving the machine of the external database used by the ReplicaCacheDatabase class. It is default:
     * 'admindb'. If this is empty, then it will be ignored. Do not retrieve this directly, use in stead the function
     * dk.netarkivet.archive.arcrepositoryadmin.DBConnect.getArchiveUrl().
     */
    public static String DIR_ARCREPOSITORY_ADMIN_DATABASE = "settings.archive.admin.database.dir";

    /**
     * <b>settings.archive.admin.database.class</b>: <br>
     * Setting for which class is used for handling the database for the DatabaseBasedActiveBitPreservation class.
     */
    public static String CLASS_ARCREPOSITORY_ADMIN_DATABASE = "settings.archive.admin.database.class";

    /**
     * <b>settings.archive.admin.database.reconnectMaxRetries</b>:<br/>
     * Setting for the maximum number of attempts to reconnect to the admin database.
     */
    public static String RECONNECT_MAX_TRIES_ADMIN_DATABASE = "settings.archive.admin.database.reconnectMaxRetries";

    /**
     * <b>settings.archive.admin.database.reconnectRetryDelay</b>:<br/>
     * Settings for the delay between the attempts to reconnect to the admin database.
     */
    public static String RECONNECT_DELAY_ADMIN_DATABASE = "settings.archive.admin.database.reconnectRetryDelay";

    /**
     * <b>settings.archive.admin.database.validityCheckTimeout</b>: <br>
     * Timeout in seconds to check for the validity of a JDBC connection on the server. This is the time in seconds to
     * wait for the database operation used to validate the connection to complete. If the timeout period expires before
     * the operation completes, this method returns false. A value of 0 indicates a timeout is not applied to the
     * database operation.
     * <p>
     * {@link java.sql.Connection#isValid(int)}
     */
    public static String DB_CONN_VALID_CHECK_TIMEOUT = "settings.archive.admin.database.validityCheckTimeout";

    /**
     * <b>settings.archive.admin.database.pool.minSize</b>: <br>
     * Configure the minimum size of the DB connection pool. Default value is 5.
     */
    public static String DB_POOL_MIN_SIZE = "settings.archive.admin.database.pool.minSize";

    /**
     * <b>settings.archive.admin.database.pool.maxSize</b>: <br>
     * Configure the maximum size of the DB connection pool. Default value is 10.
     */
    public static String DB_POOL_MAX_SIZE = "settings.archive.admin.database.pool.maxSize";

    /**
     * <b>settings.archive.admin.database.pool.acquireInc</b>: <br>
     * Configure the increment size DB connection pool. Default value is 5 (half the max size).
     */
    public static String DB_POOL_ACQ_INC = "settings.archive.admin.database.pool.acquireInc";

    /**
     * <b>settings.archive.admin.database.pool.maxStm</b>: <br>
     * Configure statement pooling, by setting the global maximum number of pooled prepared statements for a data
     * source. Default value is 0. Note that if both {@link #DB_POOL_MAX_STM} and {@link #DB_POOL_MAX_STM_PER_CONN} are
     * set to zero, statement pooling is fully deactivated.
     *
     * @see <a href="http://www.mchange.com/projects/c3p0/index.html#maxStatements"> c3p0 maxStatements
     * documentation</a>
     */
    public static String DB_POOL_MAX_STM = "settings.archive.admin.database.pool.maxStm";

    /**
     * <b>settings.archive.admin.database.pool.maxStmPerConn</b>: <br>
     * Configure statement pooling, by setting the global maximum number of pooled prepared statements for a data
     * source. Default value is 0. Note that if both {@link #DB_POOL_MAX_STM} and {@link #DB_POOL_MAX_STM_PER_CONN} are
     * set to zero, statement pooling is fully deactivated.
     *
     * @see <a href="http://www.mchange.com/projects/c3p0/index.html#maxStatementsPerConnection"> c3p0
     * maxStatementsPerConnection documentation</a>
     */
    public static String DB_POOL_MAX_STM_PER_CONN = "settings.archive.admin.database.pool.maxStmPerConn";

    /**
     * <b>settings.archive.admin.database.pool.idleConnTestPeriod</b>: <br>
     * Configure idle connection testing period in seconds. Default is 0, which means no idle connection testing
     *
     * @see <a href="http://www.mchange.com/projects/c3p0/index.html#idleConnectionTestPeriod"> c3p0
     * idleConnectionTestPeriod documentation</a>
     */
    public static String DB_POOL_IDLE_CONN_TEST_PERIOD = "settings.archive.admin.database.pool.idleConnTestPeriod";

    /**
     * <b>settings.archive.admin.database.pool.idleConnTestOnCheckin</b>: <br>
     * Configure if a connection validity should be checked when returned to the pool. Default is false.
     *
     * @see <a href="http://www.mchange.com/projects/c3p0/index.html#testConnectionOnCheckin"> c3p0
     * testConnectionOnCheckin documentation</a>
     */
    public static String DB_POOL_IDLE_CONN_TEST_ON_CHECKIN = "settings.archive.admin.database.pool.idleConnTestOnCheckin";

    /**
     * <b>settings.archive.admin.database.pool.idleConnTestQuery</b>: <br>
     * The SQL query to be used when testing an idle connection. Default is empty, which means using c3p0 defaults.
     *
     * @see <a href="http://www.mchange.com/projects/c3p0/index.html#preferredTestQuery"> c3p0 preferredTestQuery
     * documentation</a>
     */
    public static String DB_POOL_IDLE_CONN_TEST_QUERY = "settings.archive.admin.database.pool.idleConnTestQuery";

    /**
     * The user name for login to the admin database. Default value is empty (no username passed to database
     * connection).
     */
    public static String DB_USERNAME = "settings.archive.admin.database.username";

    /**
     * The password name for login to the admin database.Default values is empty (no password passed to database
     * connection).
     */
    public static String DB_PASSWORD = "settings.archive.admin.database.password";

    /**
     * <b>settings.archive.bitpreservation.class</b>: <br>
     * Setting for which instance of ActiveBitPreservation that should be used for preservation.
     */
    public static String CLASS_ARCREPOSITORY_BITPRESERVATION = "settings.archive.bitpreservation.class";

    /**
     * <b>settings.archive.checksum.baseDir</b>: <br>
     * The directory for the checksum file.
     */
    public static String CHECKSUM_BASEDIR = "settings.archive.checksum.baseDir";

    /**
     * <b>settings.archive.checksum.minSpaceLeft</b>: <br>
     * The path to the settings for the minimum amount of space left for the checksum archive to receive new upload
     * messages.
     */
    public static String CHECKSUM_MIN_SPACE_LEFT = "settings.archive.checksum.minSpaceLeft";

    /**
     * <b>settings.archive.checksum.class</b> This decides how to store the checksums, in file, og BDB database. By
     * default, it is stored in a file
     */
    public static String CHECKSUM_ARCHIVE_CLASS = "settings.archive.checksum.archive.class";
    
    /**
     * <b>settings.archive.checksum.usePrecomputedChecksum</b>: This decides whether or not use the pre-computed checksum sent as part of the StoreMessage and UploadMessage
     * The default is false
     */
    public static String CHECKSUM_USE_PRECOMPUTED_CHECKSUM_DURING_UPLOAD= "settings.archive.checksum.usePrecomputedChecksumDuringUpload";
    
    
    /**
     * <b>settings.archive.bitarchive.singleChecksumTimeout</b>: <br>
     * The path to the settings for the maximum time usage for the calculation of the checksum for a single file. Used
     * to set a timelimit to the batchjob for the GetChecksumMessage.
     */
    public static String SINGLE_CHECKSUM_TIMEOUT = "settings.archive.bitarchive.singleChecksumTimeout";
}
