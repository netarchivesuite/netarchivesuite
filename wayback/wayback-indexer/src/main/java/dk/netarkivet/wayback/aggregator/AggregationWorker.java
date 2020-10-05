/*
 * #%L
 * Netarchivesuite - wayback
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
package dk.netarkivet.wayback.aggregator;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.CleanupIF;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.wayback.WaybackSettings;

/**
 * The <code>AggregationWorker</code> singleton contains the schedule and file bookkeeping functionality needed in the
 * aggregation of indexes.
 * <p>
 * The <code>AggregationWorker</code> has the responsibility of ensuring each index in the raw index files ends up
 * appearing exactly once in the index files used by Wayback. If this isn't possible the fallback is to allow duplicate
 * occurrences of index lines ensuring index lines appears at least once.
 */
public class AggregationWorker implements CleanupIF {
    /** The AggregationWorker logger. */
    //private Log log = LogFactory.getLog(getClass().getName());
    private final Logger log = LoggerFactory.getLogger(AggregationWorker.class);
    /** The singleton instance. */
    private static AggregationWorker instance = null;
    /** The IndexAggregator instance to use for the actual aggregation work. */
    private IndexAggregator aggregator = new IndexAggregator();
    /** See WaybackSettings.WAYBACK_AGGREGATOR_TEMP_DIR. */
    private static File temporaryDir = Settings.getFile(WaybackSettings.WAYBACK_AGGREGATOR_TEMP_DIR);
    /** See WaybackSettings.WAYBACK_AGGREGATOR_INPUT_DIR). */
    private static File indexInputDir = Settings.getFile(WaybackSettings.WAYBACK_BATCH_OUTPUTDIR);
    /** See WaybackSettings.WAYBACK_AGGREGATOR_OUTPUT_DIR. */
    static File indexOutputDir = Settings.getFile(WaybackSettings.WAYBACK_AGGREGATOR_OUTPUT_DIR);
    /**
     * The file to use for creating temporary intermediate index file, which subsequent are merge into the final
     * intermediate index file.
     */
    static File tempIntermediateIndexFile = new File(temporaryDir, "temp_intermediate.index");
    /**
     * The file to use for creating temporary final index file, which subsequent are merge into the working final index
     * file.
     */
    static File tempFinalIndexFile = new File(temporaryDir, "temp_final.index");
    /** The task which is used to schedule the aggregations. */
    private TimerTask aggregatorTask = null;

    /**
     * The Files to store sorted indexes until they have been merge into a intermediate index files.
     */
    public static final File TEMP_FILE_INDEX = new File(temporaryDir, "temp.index");
    /**
     * The intermediate Wayback index file currently used to merge new indexes into. If the intermediate files size
     * exceeds the WaybackSettings#WAYBACK_AGGREGATOR_INTERMEDIATE_INDEX_FILE_SIZE_LIMIT
     */
    public static final File INTERMEDIATE_INDEX_FILE = new File(indexOutputDir, "wayback_intermediate.index");
    /**
     * The final Wayback index file currently used to intermediate indexes into. A new working file is created and used,
     * when the current file size + new indexes would exceed
     * WaybackSettings#WAYBACK_AGGREGATOR_FINAL_INDEX_FILE_SIZE_LIMIT
     */
    public static final File FINAL_INDEX_FILE = new File(indexOutputDir, "wayback.index");

    /**
     * Factory method which creates a singleton aggregator and sets it running. It has the side effect of creating the
     * output directories for the indexer if these do not already exist.
     * <p>
     * A temp directory is create if it doesn't exist. The aggregator won't run if a temp directory is already present,
     * as this might indicate an instance of the aggregator already running.
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
     * Creates an Aggregator and starts the aggregation thread. Only one aggregator will be allowed to run at a time,
     * {@see #getInstance()}.
     */
    private AggregationWorker() {
        initialize();
        startAggregationThread();
    }

    /**
     * Starts the aggregation task. Only allowed to be called once to avoid aggregation race conditions.
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
            aggregatorThreadTimer.schedule(aggregatorTask, 0,
                    Settings.getLong(WaybackSettings.WAYBACK_AGGREGATOR_AGGREGATION_INTERVAL));
        } else {
            throw new IllegalStateException("An attempt has been made to start a second aggregation job");
        }
    }

    /**
     * Runs the actual aggregation. See package description for details.
     * <p>
     * Is synchronized so several subsequent scheduled runs of the method will have to run one at a time.
     */
    protected synchronized void runAggregation() {
        String[] fileNamesToProcess = indexInputDir.list();
        if (fileNamesToProcess == null) {
            log.warn("Input directory '" + indexInputDir.getAbsolutePath()
                    + "' was not found: skipping this aggregation");
            return;
        }

        if (fileNamesToProcess.length == 0) {
            if (log.isDebugEnabled()) {
                log.debug("No new raw index files found in '" + indexInputDir.getAbsolutePath()
                        + "' skipping aggregation");
            }
            return;
        }

        File[] filesToProcess = new File[fileNamesToProcess.length];

        // Convert filename array to file handle array
        for (int i = 0; i < fileNamesToProcess.length; i++) {
            File file = new File(indexInputDir, fileNamesToProcess[i]);
            if (file.isFile()) {
                filesToProcess[i] = new File(indexInputDir, fileNamesToProcess[i]);
            } else {
                throw new ArgumentNotValid("Encountered non-regular file '" + file.getName()
                        + "' in the index input directory '" + indexInputDir.getAbsolutePath() + "'");
            }
        }

        aggregator.sortAndMergeFiles(filesToProcess, TEMP_FILE_INDEX);
        if (log.isDebugEnabled()) {
            log.debug("Sorted raw indexes into temporary index file ");
        }

        // If no Intermediate Index file exist we just promote the temp index
        // file to working file.
        // Normally the Intermediate Index file exists and we
        // need to merge the new indexes into this.
        if (!INTERMEDIATE_INDEX_FILE.exists()) {
            TEMP_FILE_INDEX.renameTo(INTERMEDIATE_INDEX_FILE);
        } else {
            aggregator.mergeFiles(new File[] {TEMP_FILE_INDEX, INTERMEDIATE_INDEX_FILE}, tempIntermediateIndexFile);
            tempIntermediateIndexFile.renameTo(INTERMEDIATE_INDEX_FILE);
            if (log.isDebugEnabled()) {
                log.debug("Merged temporary index file into intermediate index " + "file '"
                        + INTERMEDIATE_INDEX_FILE.getAbsolutePath() + "'.");
            }
        }

        handlePossibleIntemediateIndexFileLimit();

        // Delete the files which have been processed to avoid processing them
        // again
        for (File inputFile : filesToProcess) {
            inputFile.delete();
        }
        TEMP_FILE_INDEX.delete();

    }

    /**
     * Call the handleFinalIndexFileMerge is case of a exceeded
     * WaybackSettings.WAYBACK_AGGREGATOR_MAX_INTERMEDIATE_INDEX_FILE_SIZE and ?.
     */
    private void handlePossibleIntemediateIndexFileLimit() {
        if (INTERMEDIATE_INDEX_FILE.length() > 1024 * Settings
                .getLong(WaybackSettings.WAYBACK_AGGREGATOR_MAX_INTERMEDIATE_INDEX_FILE_SIZE)) {
            handleFinalIndexFileMerge();
        }
    }

    /**
     * See package description for the concrete handling of larger index files.
     */
    private void handleFinalIndexFileMerge() {
        if (INTERMEDIATE_INDEX_FILE.length() + FINAL_INDEX_FILE.length() > 1024 * Settings
                .getLong(WaybackSettings.WAYBACK_AGGREGATOR_MAX_MAIN_INDEX_FILE_SIZE)) {
             renameFinalIndexFile();
        }

        if (!FINAL_INDEX_FILE.exists()) {
            INTERMEDIATE_INDEX_FILE.renameTo(FINAL_INDEX_FILE);
            if (log.isDebugEnabled()) {
                log.debug("Promoting Intermediate Index file to final index " + "file '"
                        + FINAL_INDEX_FILE.getAbsolutePath() + "'.");
            }
        } else {
            aggregator.mergeFiles(new File[] {FINAL_INDEX_FILE, INTERMEDIATE_INDEX_FILE}, tempFinalIndexFile);
            if (log.isDebugEnabled()) {
                log.debug("Merged intermediate file into final index file");
            }

            tempFinalIndexFile.renameTo(FINAL_INDEX_FILE);

            INTERMEDIATE_INDEX_FILE.delete();
        }

        try {
            INTERMEDIATE_INDEX_FILE.createNewFile();
        } catch (IOException e) {
            log.error("Failed to create new Intermediate Index file", e);
        }
    }

    /**
     * Give the FINAL_INDEX_FILE (wayback.index) a unique new name.
     */
    private void renameFinalIndexFile() {
        String timestampString = (new SimpleDateFormat("yyyyMMdd-HHmm")).format(new Date());
        String newFileName = "wayback." + timestampString +".cdx";
        File fileToRename = new File(indexOutputDir, FINAL_INDEX_FILE.getName());
        File newFile = new File(indexOutputDir, newFileName);
        if (newFile.exists()) {
            //This should be rare outside tests
            newFileName = UUID.randomUUID().toString() + "." + newFileName;
            newFile = new File(indexOutputDir, newFileName);
        }
        fileToRename.renameTo(newFile);
    }

    @Override
    public void cleanup() {
        FileUtils.removeRecursively(temporaryDir);
    }

    /**
     * Creates the needed working directories. Also checks whether a temp directory exists, which might be an indication
     * of a unclean shutdown.
     */
    protected void initialize() {
        FileUtils.createDir(indexOutputDir);
        if (temporaryDir.exists()) {
            log.warn("An temporary Aggregator dir ("
                    + Settings.getFile(WaybackSettings.WAYBACK_AGGREGATOR_TEMP_DIR).getAbsolutePath()
                    + ") already exists. This indicates that the previous "
                    + "running aggregator wasn't shutdown cleanly. "
                    + "The temp dirs will be removed and the aggregation " + "on the indexes will be restarted");
        }
        FileUtils.removeRecursively(temporaryDir);
        FileUtils.createDir(temporaryDir);
    }
}
