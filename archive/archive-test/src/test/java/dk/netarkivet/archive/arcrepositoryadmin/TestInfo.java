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

package dk.netarkivet.archive.arcrepositoryadmin;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Useful constants and variables for running unit tests for classes in package arcrepositoryadmin.
 */
public class TestInfo {

    /** The main test data directory. */
    private static final File DATA_DIR = new File("tests/dk/netarkivet/archive/arcrepositoryadmin/data");

    static final File TEST_DIR = new File(DATA_DIR, "working");
    /**
     * The first archive directory to work on.
     */
    static final File ARCHIVE_DIR1 = new File(TEST_DIR, "bitarchive1");
    /**
     * The second archive directory to work on.
     */
    static final File ARCHIVE_DIR2 = new File(TEST_DIR, "bitarchive2");
    /**
     * The directory storing the arcfiles in the already existing bitarchive - including credentials and admin-files.
     */
    static final File ORIGINALS_DIR = new File(DATA_DIR, "originals");

    /**
     * List of files that can be used in the scripts (content of the ORIGINALS_DIR).
     */
    static final List<String> GETTABLE_FILENAMES = Arrays.asList(new String[] {"get1.ARC", "get2.ARC"});

    static final File NON_EMPTY_ADMIN_DATA_DIR_ORIG = new File(DATA_DIR, "admindata");
    static final File VERSION_03_ADMIN_DATA_DIR_ORIG = new File(DATA_DIR, "admindata-0.3");
    static final File NON_EMPTY_ADMIN_DATA_DIR = new File(TEST_DIR, "admindata");

    static final String[] files = {"some.arc", "are.arc", "equal.arc", "other.arc", "files.arc"};

    /** The directory containing the database for DatabaseBasedActiveBitPreservationTester */
    public static final File DATABASE_DIR = new File(TEST_DIR, "adminDB");
    /** The complete database URL for the DatabaseBasedActiveBitPreservationTester. */
    public static final String DATABASE_URL = "jdbc:derby:" + DATABASE_DIR.getAbsolutePath();

    /** The file for the archive database. */
    static final File DATABASE_FILE = new File("archivedatabasedir", "archivedb.jar");

    public static final File TEST_FILE_1 = new File(TEST_DIR, "test1.arc");

}
