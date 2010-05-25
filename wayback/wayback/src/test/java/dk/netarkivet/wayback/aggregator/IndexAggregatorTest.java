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

import dk.netarkivet.wayback.TestInfo;

import junit.framework.TestCase;

/**
 * Verifies that the {code}IndexAggregator{code} class is able to aggregate CDX
 * index files correctly in largere files, and sort the index entries
 */
public class IndexAggregatorTest extends TestCase{
    private static IndexAggregator aggregator = new IndexAggregator();
    private static final String testDataDirectory = TestInfo.DATA_DIR+File.separator+"raw-search-files"+File.separator;
    private static final String inputFile1Name = testDataDirectory+"result1.txt";
    private static final String inputFile2Name = testDataDirectory+"result2.txt";

    private static final String outputFileName = "outputFile.txt";

    public void testAggregation() {
        TestIndex testIndex = new TestIndex();

        testIndex.addIndexesFromFiles(new String[] {inputFile1Name, inputFile2Name});

        aggregator.processFiles(new String[] {inputFile1Name, inputFile2Name}, outputFileName);

        assertNull("Unexpected content of aggregated index", testIndex.compareToIndex(outputFileName));
        
    }
}
