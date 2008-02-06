/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.archive.arcrepository.bitpreservation;

import java.io.File;
import java.text.SimpleDateFormat;

import dk.netarkivet.common.distribute.Channels;

/**
 * Created by IntelliJ IDEA.
 * User: csr
 * Date: Mar 7, 2005
 * Time: 1:53:41 PM
 */
public class TestInfo {

    /**
     * The head test directory.
     */
    public static final File TEST_DIR =
            new File("tests/dk/netarkivet/archive/arcrepository/bitpreservation/data");

    public static final File ORIGINALS_DIR =
            new File(TestInfo.TEST_DIR, "originals");
    public static final File WORKING_DIR =
            new File(TEST_DIR, "working");
    public static final File CLOG_DIR =
            new File(WORKING_DIR, "log/controller");
    public static final File REPORT_DIR =
            new File(WORKING_DIR, "log/integrity_reports");
    public static final String CREDENTIALS = "42";
    public static final File GOOD_ARCHIVE_DIR =
            new File(WORKING_DIR, "bitarchive1");
    public static final File FAIL_ARCHIVE_DIR =
            new File(WORKING_DIR, "bitarchive1_to_fail");
    public static final File GOOD_ARCHIVE_FILE_DIR
            = new File(GOOD_ARCHIVE_DIR, "filedir");
    public static final File THE_ARCHIVE_DIR =
            new File(WORKING_DIR, Channels.getTheBamon().getName());
    public static final File ORIGINAL_ARCHIVE =
            new File(ORIGINALS_DIR, "DEV_SB_THE_BAMON");
    public static final String[] REFERENCE_FILES = new String[]{
        "integrity1.ARC", "integrity2.ARC", "integrity11.ARC", "integrity12.ARC"
    };
    public static final File CHECKSUM_ARCHIVE =
            new File(WORKING_DIR, "DEV_SB_THE_BAMON");

    public static final String VALID_LOCATION = "SB";

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final SimpleDateFormat DATE_FORMATTER =
            new SimpleDateFormat(DATE_FORMAT);

    public static final String LOCATION_NAME = "KB";
    public static final String OTHER_LOCATION_NAME = "SB";

    public static final File LOG_FILE =
            new File("tests/testlogs/netarkivtest.log");
    public static final String FILE_IN_ADMIN_DATA = "foobar";
    public static final String FILE_NOT_IN_ADMIN_DATA = "does.not.exist";
    public static final File CORRECT_ADMIN_DATA
            = new File(WORKING_DIR, "correct.admin.data");
    public static final File ADMIN_DATA
            = new File(WORKING_DIR, "admin.data");
}
