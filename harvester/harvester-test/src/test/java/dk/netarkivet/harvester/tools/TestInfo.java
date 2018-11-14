/*
 * #%L
 * Netarchivesuite - harvester - test
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

package dk.netarkivet.harvester.tools;

import java.io.File;

public class TestInfo {
    static final File WORKING_DIR = new File("./tests/dk/netarkivet/common/tools/working/");
    static final File DATA_DIR = new File("./tests/dk/netarkivet/common/tools/data/originals");

    static final File METADATA_DIR = new File(dk.netarkivet.harvester.tools.TestInfo.DATA_DIR, "admindatadir");
    static final File OLDJOBS_DIR = new File(dk.netarkivet.harvester.tools.TestInfo.DATA_DIR, "oldjobs");
    static final File JOBID_HARVESTID_MAPPING_FILE = new File(dk.netarkivet.harvester.tools.TestInfo.DATA_DIR,
            "jobid-harvestid.txt");

    static final String TEST_ENTRY_FILENAME = "2-2-20060731110420-00000-sb-test-har-001.statsbiblioteket.dk.arc";
    static final long TEST_ENTRY_OFFSET = 8459;

    static final File TEXT_FILE_1 = new File(dk.netarkivet.harvester.tools.TestInfo.WORKING_DIR, "testfile01.txt");
    static final File TEXT_FILE_2 = new File(dk.netarkivet.harvester.tools.TestInfo.WORKING_DIR, "testfile02.txt");

    /** data files used by createIndex tests. */
    static final File CACHE_DIR = new File(WORKING_DIR, "cache");
    static final File CACHE_TEMP_DIR = new File(WORKING_DIR, "tempCache");
    static final File CACHE_1_FILE = new File(CACHE_TEMP_DIR, "cache_file_1");
    static final File CACHE_OUTPUT_DIR = new File(WORKING_DIR, "outCache");
    static final File CACHE_ZIP_FILE = new File(CACHE_OUTPUT_DIR, "cache_file_1.gz");
}
