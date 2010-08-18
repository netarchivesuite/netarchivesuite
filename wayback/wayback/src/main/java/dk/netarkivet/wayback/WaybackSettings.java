/*$Id$
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

package dk.netarkivet.wayback;

import dk.netarkivet.common.utils.Settings;

/**
 * Settings specific to the wayback module of NetarchiveSuite.
 */
public class WaybackSettings {
      /** The default place in classpath where the settings
       * file can be found. */
    private static final String DEFAULT_SETTINGS_CLASSPATH
            = "dk/netarkivet/wayback/settings.xml";

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

    /**
     * Setting specifying the name of the class used to canonicalize urls. This
     * class must implement the interface org.archive.wayback.UrlCanonicalizer .
     */
    public static String URL_CANONICALIZER_CLASSNAME = 
            "settings.wayback.urlcanonicalizer.classname";


    /**
     * There now follows a list of hibernate-related properties.
     */

    /**
     * c3p0 is the database connection manager used by hibernate. See c3p0
     * documentation for their meaning.
     */
    public static String C3P0_ACQUIRE_INCREMENT =
            "settings.wayback.hibernate.c3p0.acquireIncrement";
    public static String C3P0_IDLE_PERIOD =
            "settings.wayback.hibernate.c3p0.idleTestPeriod";
    public static String  C3P0_MAX_SIZE =
            "settings.wayback.hibernate.c3p0.maxSize";
    public static String C3P0_MAX_STATEMENTS =
             "settings.wayback.hibernate.c3p0.maxStatements";
    public static String C3P0_MIN_SIZE =
             "settings.wayback.hibernate.c3p0.minSize";
    public static String C3P0_TIMEOUT =
             "settings.wayback.hibernate.c3p0.timeout";
    /**
     * These are the hibernate specific properties. See hibernate documentation
     * for their meaning.
     */
    public static String HIBERNATE_DB_URL =
            "settings.wayback.hibernate.connectionUrl";
    public static String HIBERNATE_DB_DRIVER =
            "settings.wayback.hibernate.dbDriverClass";
    public static String HIBERNATE_REFLECTION_OPTIMIZER =
            "settings.wayback.hibernate.useReflectionOptimizer";
    public static String HIBERNATE_TRANSACTION_FACTORY =
            "settings.wayback.hibernate.transactionFactory";
    public static String HIBERNATE_DIALECT =
            "settings.wayback.hibernate.dialect";
    public static String HIBERNATE_SHOW_SQL =
            "settings.wayback.hibernate.showSql";
    public static String HIBERNATE_FORMAT_SQL =
            "settings.wayback.hibernate.formatSql";
    public static String HIBERNATE_HBM2DDL_AUTO =
            "settings.wayback.hibernate.hbm2ddlAuto";
    public static String HIBERNATE_USERNAME =
            "settings.wayback.hibernate.user";
    public static String HIBERNATE_PASSWORD =
            "settings.wayback.hibernate.password";

    /**
     * The replica to be used by the wayback indexer.
     */
    public static String WAYBACK_REPLICA =
            "settings.wayback.indexer.replicaId";

    /**
     * The directory to which batch output is written during indexing.
     */
    public static String WAYBACK_INDEX_TEMPDIR =
            "settings.wayback.indexer.tempBatchOutputDir";

    /**
     * The directory to which batch output is moved after a batch indexing
     * job is successfully completed.
     */
    public static String WAYBACK_BATCH_OUTPUTDIR =
            "settings.wayback.indexer.finalBatchOutputDir";

    /**
     * The maximum number of times an archive file may generate a batch error
     * during indexing before we give up on it.
     */
    public static String WAYBACK_INDEXER_MAXFAILEDATTEMPTS =
            "settings.wayback.indexer.maxFailedAttempts";

    /**
     * The delay in milliseconds before the producer thread is started.
     */
    public static String WAYBACK_INDEXER_PRODUCER_DELAY =
            "settings.wayback.indexer.producerDelay";

    /**
     * The interval, in milliseconds, between successive runs of the
     * producer thread.
     */
    public static String WAYBACK_INDEXER_PRODUCER_INTERVAL =
            "settings.wayback.indexer.producerInterval";
    
    /**
     * The number of consumer threads to run.
     */
    public static String WAYBACK_INDEXER_CONSUMER_THREADS =
            "settings.wayback.indexer.consumerThreads";
    
    /**
     * A file containing a list of files which have been archived and
     * therefore do not need to be archived again. This key may be unset.
     */
    public static String WAYBACK_INDEXER_INITIAL_FILES =
            "settings.wayback.indexer.initialFiles";

    /**-------------------------Aggregator Settings---------------------------*/
    /** The directory the Aggregator consumes raw index files from */
    public static String WAYBACK_AGGREGATOR_INPUT_DIR =
            "settings.wayback.aggregator.indexFileInputDir";

    /** The directory the Aggregator places the Aggregated and sorted files into */
    public static String WAYBACK_AGGREGATOR_OUTPUT_DIR =
            "settings.wayback.aggregator.indexFileOutputDir";
    
    /** The directory used by the aggregator to store temporary files.*/
    public static String WAYBACK_AGGREGATOR_TEMP_DIR =
            "settings.wayback.aggregator.tempAggregatorDir";

    /** The time to between each scheduled aggregation run (in miliseconds)*/
    public static String WAYBACK_AGGREGATOR_AGGREGATION_INTERVAL =
            "settings.wayback.aggregator.aggregationInterval";

    /** The maximum size of the Intermediate index file in MB. When this limit is reached
     * a new index file is created and new indexes are added to this file. In
     * case of a 0 value, the intermediate index file will always be merged into
     * the main index file.
     */
    public static String WAYBACK_AGGREGATOR_MAX_INTERMEDIATE_INDEX_FILE_SIZE =
            "settings.wayback.aggregator.maxIntermediateIndexFileSize";

    /** The maximum size of the main wayback index file in MB. When this limit is reached
     * a new index file is created and new indexes are added to this file. The
     * old index file will be rename to ${finalIndexFileSizeLimit}.1
     */
    public static String WAYBACK_AGGREGATOR_MAX_MAIN_INDEX_FILE_SIZE =
            "settings.wayback.aggregator.maxMainIndexFileSize";
}
