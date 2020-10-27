/*
 * #%L
 * Netarchivesuite - archive - test
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
package dk.netarkivet.archive.bitarchive.distribute;

import java.io.File;

public class TestInfo {

    private static final File BAMON_BASEDIR = new File("tests/dk/netarkivet/archive/bitarchive/distribute/data");
    static final File BAMON_ORIGINALS = new File(BAMON_BASEDIR, "originals");
    static final File BAMON_WORKING = new File(BAMON_BASEDIR, "working");
    static final File BAMON_TMP_FILE = new File(BAMON_ORIGINALS, "tmpout.txt");// Non-existing, to put data in

    static final File MONITOR_TEST_DIR = new File("tests/dk/netarkivet/archive/bitarchive/distribute/data/monitor");
    static final File DUMMY_FILE = new File(MONITOR_TEST_DIR, "dummyTestRemoteFile.txt");
    static final File WORKING_DIR = new File(MONITOR_TEST_DIR, "working");
    static final File ORIGINALS_DIR = new File(MONITOR_TEST_DIR, "originals");

    static final String BATCH_OUTPUT_FILE = "batch_output";
    static final String BITARCHIVE_APP_DIR_1 = "tests/dk/netarkivet/archive/bitarchive/distribute/data/working/app1";
    static final String BITARCHIVE_APP_DIR_2 = "tests/dk/netarkivet/archive/bitarchive/distribute/data/working/app2";
    static final String BITARCHIVE_SERVER_DIR_1 = "tests/dk/netarkivet/archive/bitarchive/distribute/data/working/server1";
    static final String BITARCHIVE_SERVER_DIR_2 = "tests/dk/netarkivet/archive/bitarchive/distribute/data/working/server2";

    static final long BITARCHIVE_BATCH_MESSAGE_TIMEOUT = 100;
    static final long BITARCHIVE_HEARTBEAT_FREQUENCY = 50;
    static final long BITARCHIVE_ACCEPTABLE_HEARTBEAT_DELAY = 100;
    static final long JUST_A_BIT_LONGER = 20;

    static final File DATADIR = new File("tests/dk/netarkivet/archive/bitarchive/distribute/data");
    static final File TEMPDIR = new File("tests/dk/netarkivet/archive/bitarchive/distribute/data/working");

    static final File UPLOADMESSAGE_ORIGINALS_DIR = new File(
            "tests/dk/netarkivet/archive/bitarchive/distribute/data/uploadmessage");
    static final File UPLOADMESSAGE_TEMP_DIR = new File(
            "tests/dk/netarkivet/archive/bitarchive/distribute/data/working");
    static final File UPLOADMESSAGE_TESTFILE_1 = new File(UPLOADMESSAGE_TEMP_DIR, "NetarchiveSuite-store1.arc");
    static final File UPLOADMESSAGE_TESTFILE_2 = new File(UPLOADMESSAGE_TEMP_DIR, "NetarchiveSuite-store2.arc");

    static final File BATCH_ALL_CHECKSUM_OUTPUT_FILE = new File(BAMON_WORKING, "batch_all_checksum_output.txt");
    static final File BATCH_ALL_FILENAMES_OUTPUT_FILE = new File(BAMON_WORKING, "batch_all_filenames_output.txt");
    static final File BATCH_ONE_CHECKSUM_OUTPUT_FILE = new File(BAMON_WORKING, "batch_one_checksum_output.txt");

    static final File CORRECT_ARC_FILE = new File(BAMON_WORKING, "file-1.arc");
    static final File BAD_ARC_FILE = new File(BAMON_WORKING, "file-2.arc");

    // Moved from BitArchiveServerTester:
    static final File BA1_MAINDIR = new File(UPLOADMESSAGE_TEMP_DIR, "bitarchive1");
    static final File SERVER1_DIR = new File(UPLOADMESSAGE_TEMP_DIR, "server1");
    // Files for testing RemoveAndGetFile:
    static final String BA1_FILENAME = "NetarchiveSuite-upload1.arc";
    static final File BA1_FILEDIR = new File(BA1_MAINDIR, "filedir");
    static final File BA1_ATTICDIR = new File(BA1_MAINDIR, "atticdir");
    static final File BA1_ORG_FILE = new File(BA1_FILEDIR, BA1_FILENAME);
    static final File BA1_ATTIC_FILE = new File(BA1_ATTICDIR, BA1_FILENAME);
    static final String BA1_CHECKSUM = "d87cc8068fa49f3a4926ce4d1cdf14e1";

}
