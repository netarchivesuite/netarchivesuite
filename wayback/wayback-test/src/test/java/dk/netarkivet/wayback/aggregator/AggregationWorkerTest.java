/*
 * #%L
 * Netarchivesuite - wayback - test
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *       the National Library of France and the Austrian National Library.
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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.wayback.WaybackSettings;

public class AggregationWorkerTest extends AggregatorTestCase {
    AggregationWorker worker;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        worker = AggregationWorker.getInstance();
        FileUtils.removeRecursively(Settings.getFile(WaybackSettings.WAYBACK_AGGREGATOR_TEMP_DIR));
        worker.initialize();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Verifies that a simple aggregation of two unsorted index files behave correctly the first time the aggregator is
     * run. No intermediate index file merging is performed at this time.
     * <p>
     */
    @Test
    public void testFirstAggregationRun() {
        File[] inputFiles = prepareSourceIndex(new String[] {inputFile1Name, inputFile2Name});

        TestIndex testIndex = new TestIndex();
        testIndex.addIndexesFromFiles(inputFiles);

        worker.runAggregation();

        assertNull("Unexpected content of aggregated index",
                testIndex.compareToIndex(AggregationWorker.INTERMEDIATE_INDEX_FILE));
        assertTrue("InputFiles remain after aggregation", new File(inputDirName).list().length == 0);
        assertTrue("Temporary intermediate index file remains after aggregation",
                !AggregationWorker.tempIntermediateIndexFile.exists());
        assertTrue("Temporary index file remains after aggregation", !AggregationWorker.TEMP_FILE_INDEX.exists());
    }

    /**
     * Verifies that a aggregation is run correctly the second time it is run.
     */
    @Test
    public void testSecondAggregationRun() {
        testFirstAggregationRun();

        File[] inputFiles = prepareSourceIndex(new String[] {inputFile3Name});

        TestIndex testIndex = new TestIndex();
        testIndex.addIndexesFromFiles(inputFiles);
        testIndex.addIndexesFromFiles(new File[] {AggregationWorker.INTERMEDIATE_INDEX_FILE});

        worker.runAggregation();

        assertNull("Unexpected content of aggregated index",
                testIndex.compareToIndex(AggregationWorker.INTERMEDIATE_INDEX_FILE));
        assertTrue("InputFiles remain after aggregation", new File(inputDirName).list().length == 0);
        assertTrue("Temporary intermediate index file remains after aggregation",
                !AggregationWorker.tempIntermediateIndexFile.exists());
        assertTrue("Temporary index file remains after aggregation", !AggregationWorker.TEMP_FILE_INDEX.exists());
    }

    /**
     * Verifies that the aggregator merges the IntermediateIndexFile into the main index file when the
     * WaybackSettings#INTERMEDIATE_INDEX_FILE_LIMIT is exceeded. The old Intermediate Index file should have been
     * removed in this process
     */
    @Test
    public void testMaxIntermediateIndexFileLimit() {
        testFirstAggregationRun();
        File[] inputFiles = prepareSourceIndex(new String[] {inputFile109KName});

        TestIndex testIndex = new TestIndex();
        testIndex.addIndexesFromFiles(inputFiles);
        testIndex.addIndexesFromFiles(new File[] {AggregationWorker.INTERMEDIATE_INDEX_FILE});

        worker.runAggregation();

        assertNull("Unexpected content of aggregated index",
                testIndex.compareToIndex(AggregationWorker.FINAL_INDEX_FILE));
        assertTrue("InputFiles remain after aggregation", new File(inputDirName).list().length == 0);
        assertTrue("Temporary intermediate index file remains after aggregation",
                !AggregationWorker.tempIntermediateIndexFile.exists());
        assertTrue("Temporary index file remains after aggregation", !AggregationWorker.TEMP_FILE_INDEX.exists());
        assertTrue("Intermediate file not cleared after merge to final index file",
                AggregationWorker.INTERMEDIATE_INDEX_FILE.length() == 0);
    }

    /**
     * Verifies that the aggregator always merges the IntermediateIndexFile into the main index file when the
     * WaybackSettings#INTERMEDIATE_INDEX_FILE_LIMIT is set to 0.
     */
    @Test
    public void testZeroIntermediateIndexFileLimit() {
        System.setProperty(WaybackSettings.WAYBACK_AGGREGATOR_MAX_INTERMEDIATE_INDEX_FILE_SIZE, "0");

        File[] inputFiles = prepareSourceIndex(new String[] {inputFile1Name, inputFile2Name});

        TestIndex testIndex = new TestIndex();
        testIndex.addIndexesFromFiles(inputFiles);

        worker.runAggregation();

        assertNull("Unexpected content of aggregated index",
                testIndex.compareToIndex(AggregationWorker.FINAL_INDEX_FILE));
    }

    /**
     * Verifies that the aggregator switches to a new main wayback index file when the
     * WaybackSettings#FINAL_INDEX_FILE_LIMIT is going to be exceed, an starts to use this file as the main index file.
     * The old final index file will be renamed to a file containing a timestamp in the filename.
     */
    @Test
    public void testMaxFinalIndexFileLimit() {
        testMaxIntermediateIndexFileLimit();

        File[] inputFiles = prepareSourceIndex(new String[] {inputFile155KName});

        TestIndex testIndex = new TestIndex();
        testIndex.addIndexesFromFiles(inputFiles);

        worker.runAggregation();

        assertNull("Unexpected content of aggregated index after roll-over",
                testIndex.compareToIndex(AggregationWorker.FINAL_INDEX_FILE));

        // JAVA 8 required for lambda
        //FilenameFilter renamedFileFilter = (dir, name) -> name.matches("wayback.*[0-9]+.*cdx");
        FilenameFilter renamedFileFilter = new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.matches("wayback.*[0-9]+.*cdx");
			}
		};
        File[] renamedFiles = AggregationWorker.indexOutputDir.listFiles(renamedFileFilter);
        assertTrue("Should exist a renamed file.", renamedFiles.length > 0);

    }

    /**
     * Tests that the renaming strategy really does create new files each time. By setting the
     * maximum sizes to 0, we force the creation of a new final file with each aggregation.
     */
    @Test
    public void testRunMultipleRenames() {
        System.setProperty(WaybackSettings.WAYBACK_AGGREGATOR_MAX_INTERMEDIATE_INDEX_FILE_SIZE, "0");
        System.setProperty(WaybackSettings.WAYBACK_AGGREGATOR_MAX_MAIN_INDEX_FILE_SIZE, "0");
        prepareSourceIndex(new String[] {inputFile1Name, inputFile2Name});
        worker.runAggregation();   //creates first wayback.index
        prepareSourceIndex(new String[] {inputFile3Name});
        worker.runAggregation();   //creates first renamed index
        prepareSourceIndex(new String[] {inputFile109KName});
        worker.runAggregation();
        prepareSourceIndex(new String[] {inputFile155KName});
        worker.runAggregation();
     
     // JAVA 8 required for lambda
        //FilenameFilter renamedFileFilter = (dir, name) -> name.matches(".*wayback.*[0-9]+.*cdx");
        FilenameFilter renamedFileFilter = new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return name.matches(".*wayback.*[0-9]+.*cdx");
			}
		};
        File[] renamedFiles = AggregationWorker.indexOutputDir.listFiles(renamedFileFilter);
        assertTrue("Should exist more than one renamed file.", renamedFiles.length == 3 );
    }

}
