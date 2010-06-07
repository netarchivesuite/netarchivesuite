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

/**
 * Verifies that the <code>IndexAggregator</code> class is able to aggregate CDX
 * index files correctly in larger files, and sort the index entries
 */
public class IndexAggregatorTest extends AggregatorTestCase {
    private static IndexAggregator aggregator = new IndexAggregator();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        new File(tempDirName).mkdirs();
    }


    public void testAggregation() {
        File[] inputFiles = prepareSourceIndex(new String[] {inputFile1Name, inputFile2Name});
        TestIndex testIndex = new TestIndex();

        testIndex.addIndexesFromFiles(inputFiles);

        aggregator.sortAndMergeFiles(inputFiles, AggregationWorker.TEMP_FILE_INDEX);

        String compareResult = testIndex.compareToIndex(AggregationWorker.TEMP_FILE_INDEX);
        assertNull("Unexpected content of aggregated index: "+compareResult, compareResult);          
    }

    /**
     * The <code>IndexAggregator</code> should be able to handle situations with
     * no index files. No index files should be created in this case
     */
    public void testAggregationNoFiles() {
        File[] inputFiles = prepareSourceIndex(new String[] {});
        TestIndex testIndex = new TestIndex();

        testIndex.addIndexesFromFiles(inputFiles);

        aggregator.sortAndMergeFiles(inputFiles, AggregationWorker.TEMP_FILE_INDEX);

        assertTrue ("temp index file found after agrregation with no new source index files",
                    !AggregationWorker.TEMP_FILE_INDEX.exists());
    }

    public void testAggregationSingleFile() {
        File[] inputFiles = prepareSourceIndex(new String[] {inputFile1Name});
        TestIndex testIndex = new TestIndex();

        testIndex.addIndexesFromFiles(inputFiles);

        aggregator.sortAndMergeFiles(inputFiles, AggregationWorker.TEMP_FILE_INDEX);

        assertNull("Unexpected content of aggregated index single file", testIndex.compareToIndex(AggregationWorker.TEMP_FILE_INDEX));
    }

    public void testMerging() {
        File[] inputFiles1 = prepareSourceIndex(new String[] { inputFile1Name });
        File[] inputFiles2 = prepareSourceIndex(new String[] { inputFile2Name });

        File tempFile1 = new File(testWorkingDirectory,"tempFile1");
        File tempFile2 = new File(testWorkingDirectory,"tempFile2");

        aggregator.sortAndMergeFiles(inputFiles1, tempFile1);
        aggregator.sortAndMergeFiles(inputFiles2, tempFile2);

        TestIndex testIndex = new TestIndex();
        testIndex.addIndexesFromFiles(inputFiles1);
        testIndex.addIndexesFromFiles(inputFiles2);

        aggregator.mergeFiles(new File[] {tempFile1, tempFile2}, AggregationWorker.INTERMEDIATE_INDEX_FILE);

        assertNull("Unexpected content of merged index", testIndex.compareToIndex(AggregationWorker.INTERMEDIATE_INDEX_FILE));
    }
}
