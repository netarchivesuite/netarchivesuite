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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.LogManager;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.wayback.TestInfo;
import dk.netarkivet.wayback.WaybackSettings;

import junit.framework.TestCase;

public class AggregatorTestCase extends TestCase {
    protected static final String testWorkingDirectory = "target"+File.separator+"test-data"+File.separator;

    protected static final String testSourceIndexDir = TestInfo.DATA_DIR+ File.separator+"raw-index-files"+File.separator;
    public static final String inputDirName = testWorkingDirectory+"inputDir";
    protected static final String tempDirName = testWorkingDirectory+"tempDir";
    protected static final String outputDirName = testWorkingDirectory+"outPutDir";


    protected static final String inputFile1Name = "index1.txt";
    protected static final String inputFile2Name = "index2.txt";
    protected static final String inputFile3Name = "index3.txt";     
    protected static final String inputFile109KName = "index109K.txt";
    protected static final String inputFile155KName = "index155K.txt";

    private final ReloadSettings originalSettings = new ReloadSettings();

    /** See http://kb-prod-udv-001.kb.dk/twiki/bin/view/Netarkiv/LoggingInUnittests*/
    private static final File TESTLOGPROP = new File("tests/dk/netarkivet/testlog.prop");
    private static final File LOGFILE = new File("tests/testlogs/netarkivtest.log");


    @Override
    public void setUp() throws Exception {
        super.setUp();

        try {
            LogManager.getLogManager().readConfiguration(new FileInputStream(TESTLOGPROP));
        } catch (IOException e) {
            fail("Could not load the testlog.prop file");
        }

        originalSettings.setUp();

        System.setProperty(WaybackSettings.WAYBACK_AGGREGATOR_INPUT_DIR, inputDirName);
        System.setProperty(WaybackSettings.WAYBACK_AGGREGATOR_TEMP_DIR, tempDirName);
        System.setProperty(WaybackSettings.WAYBACK_AGGREGATOR_OUTPUT_DIR, outputDirName);
        System.setProperty(WaybackSettings.WAYBACK_AGGREGATOR_AGGREGATION_INTERVAL, "1000000000"); //Never run the scheduled aggregation
        System.setProperty(WaybackSettings.WAYBACK_AGGREGATOR_MAX_INTERMEDIATE_INDEX_FILE_SIZE, "15");
        System.setProperty(WaybackSettings.WAYBACK_AGGREGATOR_MAX_MAIN_INDEX_FILE_SIZE, "200");

        FileUtils.removeRecursively(new File(testWorkingDirectory));
        new File(inputDirName).mkdirs();
        new File(outputDirName).mkdirs();
    }

    @Override
    public void tearDown() throws Exception {
        originalSettings.tearDown();
        super.tearDown();
    }

    /**
     * Moves index files from the source directory to the input directory, from
     * which the index files are consumed by the aggregator.
     *
     * @param indexFileNames The array of file names from the
     * <code>testSourceIndexDir</code> to 'prepare'
     * @return A array of files identifying the prepared input files
     */
    protected File[] prepareSourceIndex(String[] indexFileNames) {
        File[] inputFiles = new File[indexFileNames.length];
        for (int i=0;i <indexFileNames.length;i++) {
            File finalFile = new File(inputDirName, indexFileNames[i]);
            FileUtils.copyFile(new File(testSourceIndexDir, indexFileNames[i]), finalFile);
            inputFiles[i] = finalFile;
        }
        return inputFiles;
    }
    
    public void testNothing() {
        assertTrue("This is here to stop junit complaining that there are no "
                   + "tests in this class", true);
    }


}
