/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package dk.netarkivet.archive.arcrepositoryadmin;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * lc forgot to comment this!
 *
 */

public class TestInfo {
    /** The main test data directory */
    private static final File DATA_DIR =
            new File("tests/dk/netarkivet/archive/arcrepositoryadmin/data");

    static final File TEST_DIR =
            new File(DATA_DIR, "working");
    /**
     * The test log directory
     */
    static final File LOG_DIR =
            new File("tests/testlogs/netarkivtest.log");
    /**
     * The first archive directory to work on.
     */
    static final File ARCHIVE_DIR1 =
            new File(TEST_DIR, "bitarchive1");
    /**
     * The second archive directory to work on.
     */
    static final File ARCHIVE_DIR2 =
            new File(TEST_DIR, "bitarchive2");
    /**
     * The directory storing the arcfiles in the already existing bitarchive - including credentials and admin-files
     */
    static final File ORIGINALS_DIR =
            new File(DATA_DIR, "originals");


    /**
     * List of files that can be used in the scripts (content of the ORIGINALS_DIR)
     */
    static final List GETTABLE_FILENAMES =
            Arrays.asList(new String[]{"get1.ARC",
                                       "get2.ARC"});

    static final File NON_EMPTY_ADMIN_DATA_DIR_ORIG =
            new File(DATA_DIR, "admindata");
    static final File VERSION_03_ADMIN_DATA_DIR_ORIG = new File(DATA_DIR, "admindata-0.3");
    static final File NON_EMPTY_ADMIN_DATA_DIR = new File(TEST_DIR, "admindata");

    static final String[] files = {"some.arc", "are.arc", "equal.arc",
                                   "other.arc", "files.arc"};
}
