/* File:   $Id$
 * Revision: $Revision$
 * Author:   $Author$
 * Date:     $Date$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */
package dk.netarkivet.wayback.aggregator;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.wayback.WaybackSettings;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The <code>AggregationWorker</code> singleton contains the schedule and file
 * bookkeeping functionality needed in the aggregation of indexes.
 *
 * The <code>AggregationWorker</code> has the responsibility of ensuring each
 * index in the raw index files ends up appearing exactly once in the index
 * files used by Wayback. If this isn't possibility the fallback is to allow
 * duplicate occurrences of index lines ensuring index lines appears at least
 * once.
 */
public class AggregationWorker implements CleanupIF {
    private Log log = LogFactory.getLog(getClass().getName());

    private static AggregationWorker instance = null;
    private IndexAggregator aggregator = new IndexAggregator();
    private static File temporaryDir = Settings.getFile(
            WaybackSettings.WAYBACK_AGGREGATOR_TEMP_DIR);
    private static File indexInputDir = Settings.getFile(
            WaybackSettings.WAYBACK_AGGREGATOR_INPUT_DIR);
    private static File indexOutputDir = Settings.getFile(
            WaybackSettings.WAYBACK_AGGREGATOR_OUTPUT_DIR);

    static File tempIntermediateIndexFile = new File(temporaryDir,
                                                     "temp_intermediate.index");

    private int maxIndexFileSize = Settings.getInt(
            WaybackSettings.WAYBACK_AGGREGATOR_MAX_INTERMEDIATE_INDEX_FILE_SIZE);

    private TimerTask aggregatorTask = null;

    /**
     * The Files to store sorted indexes until they have been merge into a
     * intermediate index files.
     */
    public final static File TEMP_FILE_INDEX = new File(temporaryDir,
                                                        "temp.index");
    /**
     * The intermediate Wayback index file currently used to merge new indexes
     * into. If the intermediate files size exceeds the WaybackSettings#WAYBACK_AGGREGATOR_INTERMEDIATE_INDEX_FILE_SIZE_LIMIT
     */
    public final static File INTERMEDIATE_INDEX_FILE = new File(indexOutputDir,
                                                                "wayback_intermediate.index");
    /**
     * The final Wayback index file currently used to intermediate indexes into.
     * A new working file is created and used, when the current file size + new
     * indexes would exceed WaybackSettings#WAYBACK_AGGREGATOR_FINAL_INDEX_FILE_SIZE_LIMIT
     */
    public final static File FINAL_INDEX_FILE = new File(indexOutputDir,
                                                         "wayback.index");

    /**
     * Factory method which creates a singleton aggregator and sets it running.
     * It has the side effect of creating the output directories for the indexer
     * if these do not already exist.
     *
     * A temp directory is create if it doesn't exist. The aggregator wouldn't
     * run if am temp directory is already present, as this might indicate a
     * running aggregator
     *
     * @return the indexer.
     */
    public static synchronized AggregationWorker getInstance() {
        if (instance == null) {
            instance = new AggregationWorker();
        }
        return instance;
    }

    /**
     * Creates an Aggregator and starts the aggregation thread. Only one
     * aggregator will be allowed to run at a time, {@see #getInstance()}.
     */
    private AggregationWorker() {
        initialize();
        startAggregationThread();
    }

    /**
     * Starts the aggregation task. Only allowed to be called once to avoid
     * aggregation race conditions
     */
    private void startAggregationThread() {
        if (aggregatorTask == null) {
            aggregatorTask = new TimerTask() {
                @Override
                public void run() {
                    runAggregation();
                }
            };
            Timer aggregatorThreadTimer = new Timer("AggregatorThread");
            aggregatorThreadTimer.schedule
                    (aggregatorTask, 0, Settings.getLong(
                            WaybackSettings.WAYBACK_AGGREGATOR_AGGREGATION_INTERVAL));
        } else {
            throw new IllegalStateException(
                    "An attempt has been made to start a second aggregation job");
        }
    }

    protected void runAggregation() {
        String[] fileNamesToProcess = indexInputDir.list();

        File[] filesToProcess = new File[fileNamesToProcess.length];

        for (int i = 0; i < fileNamesToProcess.length; i++) {
            filesToProcess[i] = new File(indexInputDir, fileNamesToProcess[i]);
        }

        if (fileNamesToProcess.length == 0) {
            if (log.isDebugEnabled()) {
                log.debug("No new raw index files found, skipping aggregation");
            }
            return;
        }

        if (log.isInfoEnabled()) {
            log.info("Starting Agregation of the following raw index files: "
                     + fileNamesToProcess);
        }

        aggregator.sortAndMergeFiles(filesToProcess, TEMP_FILE_INDEX);
        if (log.isDebugEnabled()) {
            log.debug("Sorted raw indexes into temporary index file ");
        }

        // If no Intermediate Index file exist we just promote the temp index file
        // to working file. Normally the Intermediate Index file exists and we
        // need to merge the new indexes into this.
        if (!INTERMEDIATE_INDEX_FILE.exists()) {
            TEMP_FILE_INDEX.renameTo(INTERMEDIATE_INDEX_FILE);
        } else {
            aggregator.mergeFiles(new File[]{TEMP_FILE_INDEX,
                                             INTERMEDIATE_INDEX_FILE},
                                  tempIntermediateIndexFile);
            tempIntermediateIndexFile.renameTo(INTERMEDIATE_INDEX_FILE);
            if (log.isDebugEnabled()) {
                log.debug(
                        "Merged temporary index file into intermediate index file");
            }
        }

        // Clean the up
        for (File inputFile : filesToProcess) {
            inputFile.delete();    
        }
        TEMP_FILE_INDEX.delete();

    }

    public void cleanup() {
        FileUtils.removeRecursively(temporaryDir);
    }

    protected void initialize() {
        FileUtils.createDir(indexOutputDir);
        if (temporaryDir.exists()) {
            throw new IllegalStateException(
                    "An temporary Aggregator dir ("
                    + Settings.get(WaybackSettings.WAYBACK_AGGREGATOR_TEMP_DIR)
                    + ") already exists "
                    + " indication a instance of the aggregator is already running. "
                    + " Please ensure this is not the case, remove the temp directory and"
                    + " restart the Aggregator"
                    + "");
        }
        FileUtils.createDir(temporaryDir);
    }
}
