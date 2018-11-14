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
package dk.netarkivet.archive.arcrepository.bitpreservation;

import java.io.File;

/** Test constants and data for bitpreservation package. */
public class TestInfo {

    /** The head test directory. */
    public static final File TEST_DIR = new File("tests/dk/netarkivet/archive/arcrepository/bitpreservation/data");

    /** Subdir with originals. */
    public static final File ORIGINALS_DIR = new File(TEST_DIR, "originals");
    /** Subdir to use for working copy. */
    public static final File WORKING_DIR = new File(TEST_DIR, "working");
    /** The file for the archive database. */
    static final File DATABASE_FILE = new File("archivedatabasedir", "archivedb.jar");

    /** A directory with valid bitarchive files. */
    public static final File GOOD_ARCHIVE_DIR = new File(WORKING_DIR, "bitarchive1");
    /** The filedir for the valid bitarchive. */
    public static final File GOOD_ARCHIVE_FILE_DIR = new File(GOOD_ARCHIVE_DIR, "filedir");
    /** A directory with invalid bitarchive files. */
    public static final File FAIL_ARCHIVE_DIR = new File(WORKING_DIR, "bitarchive1_to_fail");
    /** The filedir for the invalid bitarchive. */
    public static final File FAIL_ARCHIVE_FILE_DIR = new File(GOOD_ARCHIVE_DIR, "filedir");

    /** The directory for containing the files for the checksum replica. */
    public static final File CHECKSUM_ARCHIVE_DIR = new File(WORKING_DIR, "checksum");
    /** The checksum archive file for testing the missing files. */
    public static final File CHECKSUM_ARCHIVE_FILE = new File(CHECKSUM_ARCHIVE_DIR, "checksum_THREE.md5");

    /** The files known to be present in the bitarchive dirs. */
    public static final String[] REFERENCE_FILES = new String[] {"integrity1.ARC", "integrity2.ARC", "integrity11.ARC",
            "integrity12.ARC"};

    /** The name of one replica. */
    public static final String REPLICA_ID_ONE = "ONE";
    /** The name of another replica. */
    public static final String REPLICA_ID_TWO = "TWO";
    /** The name of the checksum replica. */
    public static final String REPLICA_ID_THREE = "THREE";

    /** A valid admin.data file. */
    public static final File CORRECT_ADMIN_DATA = new File(WORKING_DIR, "correct.admin.data");
    /** An invalid admin.data file. */
    public static final File ADMIN_DATA = new File(WORKING_DIR, "admin.data");

    /** File for checksum replica. */
    public static final File CHECKSUM_FILE = new File(GOOD_ARCHIVE_DIR, "cs.content");

    /** The filename of a file in the valid admin data. */
    public static final String FILE_IN_ADMIN_DATA = "foobar";
    /** The filename of a file not in the valid admin data. */
    public static final String FILE_NOT_IN_ADMIN_DATA = "does.not.exist";

    /** The directory containing the database for DatabaseBasedActiveBitPreservationTester */
    public static final File DATABASE_DIR = new File(WORKING_DIR, "bitpreservationdb");
    /** The complete database URL for the DatabaseBasedActiveBitPreservationTester. */
    public static final String DATABASE_URL = "jdbc:derby:" + DATABASE_DIR.getPath();

}
