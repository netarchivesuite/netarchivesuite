/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
package dk.netarkivet.common.distribute.arcrepository;

import java.io.File;
import java.net.URI;

import dk.netarkivet.common.utils.arc.ARCKey;

/**
 * Constants for shared use by viewerproxy unit tests.
 *
 */
public class TestInfo {
    private static final File BASE_DIR = new File("tests/dk/netarkivet/common/distribute/arcrepository/data");
    static final File WORKING_DIR = new File(BASE_DIR, "working");
    static final File ORIGINALS_DIR = new File(BASE_DIR, "originals");
    static final File LOG_FILE = new File("tests/testlogs/netarkivtest.log");
    /**
     * An archive directory to work on.
     */
    static final File ARCHIVE_DIR = new File(WORKING_DIR, "bitarchive1");
    public static final File INDEX_DIR_2_3
            = new File(TestInfo.WORKING_DIR, "2-3-cache");
    public static final File INDEX_DIR_2_4_3_5
            = new File(TestInfo.WORKING_DIR, "2-4-3-5-cache");
    static URI GIF_URL;
    static final File LOG_PATH = new File(WORKING_DIR, "tmp");
    /**The key listed for GIF_URL. */
    static final ARCKey GIF_URL_KEY =
            new ARCKey("2-2-20060731110420-00000-sb-test-har-001.statsbiblioteket.dk.arc",
                    73269);
    public static final File SAMPLE_FILE = new File(WORKING_DIR, "testFile.txt");
    public static final File SAMPLE_FILE_COPY = new File(WORKING_DIR, "testCopy.txt");
    public static final File EMPTY_FILE = new File(WORKING_DIR, "zeroByteFile");
}
