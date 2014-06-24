package dk.netarkivet.wayback.aggregator;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.wayback.WaybackSettings;
import java.io.File;

public class AggregationWorkerTest extends AggregatorTestCase {
    AggregationWorker worker;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        worker = AggregationWorker.getInstance();
        FileUtils.removeRecursively(Settings.getFile(WaybackSettings.WAYBACK_AGGREGATOR_TEMP_DIR));
        worker.initialize();
    }

    /**
     * Verifies that a simple aggregation of two unsorted index files behave correctly
     * the first time the aggregator is run. No intermediate index file merging
     * is performed at this time.
     *
     * Disabled, see https://sbforge.org/jira/browse/NAS-2326.
     */
    public void disabledTestFirstAggregationRun() {
        File[] inputFiles = prepareSourceIndex(new String[] {inputFile1Name, inputFile2Name});

        TestIndex testIndex = new TestIndex();
        testIndex.addIndexesFromFiles(inputFiles);

        worker.runAggregation();

        assertNull("Unexpected content of aggregated index", testIndex.compareToIndex(AggregationWorker.INTERMEDIATE_INDEX_FILE));
        assertTrue("InputFiles remain after aggregation", new File(inputDirName).list().length == 0);
        assertTrue("Temporary intermediate index file remains after aggregation", !AggregationWorker.tempIntermediateIndexFile.exists());
        assertTrue("Temporary index file remains after aggregation", !AggregationWorker.TEMP_FILE_INDEX.exists());
    }

    /**
     * Verifies that a aggregation is run correctly the second time it is run.
     */
    public void disabledTestSecondAggregationRun() {
        disabledTestFirstAggregationRun();

        File[] inputFiles = prepareSourceIndex(new String[] {inputFile3Name});

        TestIndex testIndex = new TestIndex();
        testIndex.addIndexesFromFiles(inputFiles);
        testIndex.addIndexesFromFiles(new File[] { AggregationWorker.INTERMEDIATE_INDEX_FILE });

        worker.runAggregation();

        assertNull("Unexpected content of aggregated index", testIndex.compareToIndex(AggregationWorker.INTERMEDIATE_INDEX_FILE));
        assertTrue("InputFiles remain after aggregation", new File(inputDirName).list().length == 0);
        assertTrue("Temporary intermediate index file remains after aggregation", !AggregationWorker.tempIntermediateIndexFile.exists());
        assertTrue("Temporary index file remains after aggregation", !AggregationWorker.TEMP_FILE_INDEX.exists());
    }

    /**
     * Verifies that the aggregator merges the IntermediateIndexFile into the
     * main index file when the WaybackSettings#INTERMEDIATE_INDEX_FILE_LIMIT is
     * exceeded. The old Intermediate Index file should have been removed in this
     * process
     */
    public void disabledTestMaxIntermediateIndexFileLimit() {
        disabledTestFirstAggregationRun();
        File[] inputFiles = prepareSourceIndex(new String[] {inputFile109KName});

        TestIndex testIndex = new TestIndex();
        testIndex.addIndexesFromFiles(inputFiles);
        testIndex.addIndexesFromFiles(new File[] { AggregationWorker.INTERMEDIATE_INDEX_FILE });

        worker.runAggregation();

        assertNull("Unexpected content of aggregated index", testIndex.compareToIndex(AggregationWorker.FINAL_INDEX_FILE));
        assertTrue("InputFiles remain after aggregation", new File(inputDirName).list().length == 0);
        assertTrue("Temporary intermediate index file remains after aggregation", !AggregationWorker.tempIntermediateIndexFile.exists());
        assertTrue("Temporary index file remains after aggregation", !AggregationWorker.TEMP_FILE_INDEX.exists());
        assertTrue("Intermediate file not cleared after merge to final index file", AggregationWorker.INTERMEDIATE_INDEX_FILE.length() == 0);
    }

    /**
     * Verifies that the aggregator always merges the IntermediateIndexFile into the
     * main index file when the WaybackSettings#INTERMEDIATE_INDEX_FILE_LIMIT is
     * set to 0
     */
    public void testZeroIntermediateIndexFileLimit() {
        System.setProperty(WaybackSettings.WAYBACK_AGGREGATOR_MAX_INTERMEDIATE_INDEX_FILE_SIZE, "0");

        File[] inputFiles = prepareSourceIndex(new String[] {inputFile1Name, inputFile2Name});

        TestIndex testIndex = new TestIndex();
        testIndex.addIndexesFromFiles(inputFiles);

        worker.runAggregation();

        assertNull("Unexpected content of aggregated index", testIndex.compareToIndex(AggregationWorker.FINAL_INDEX_FILE));
    }

    /**
     * Verifies that the aggregator switches to a new main wayback index file when
     * the WaybackSettings#FINAL_INDEX_FILE_LIMIT is going to be exceed, an starts
     * to use this file as the main index file. The old final index file will be
     * renamed to ${finalIndexFileName}.1
     */
    public void disabledTestMaxFinalIndexFileLimit() {
        disabledTestMaxIntermediateIndexFileLimit();

        File[] inputFiles = prepareSourceIndex(new String[] {inputFile155KName});
        
        TestIndex testIndex = new TestIndex();
        testIndex.addIndexesFromFiles(inputFiles);

        worker.runAggregation();

        assertNull("Unexpected content of aggregated index after roll-over", testIndex.compareToIndex(AggregationWorker.FINAL_INDEX_FILE));
        File oldIndexFile = new File(AggregationWorker.indexOutputDir, "wayback.index.1");;
        assertTrue("No wayback.index.1 present", oldIndexFile.exists());

    }
}
