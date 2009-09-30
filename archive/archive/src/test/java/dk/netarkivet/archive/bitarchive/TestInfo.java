/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.archive.bitarchive;

import java.io.File;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;

/**
 * TestInfo associated with package dk.netarkivet.archive.bitarchive.
 * Contains useful constants.
 */
public class TestInfo {
    static final File DATA_DIR =
            new File("tests/dk/netarkivet/archive/bitarchive/data");
    static final File ORIGINALS_DIR = new File(DATA_DIR, "originals");
    static final File WORKING_DIR = new File(DATA_DIR, "working");
    static final File FILE_DIR = new File(WORKING_DIR, "filedir");

    static final File LOGFILE = new File("tests/testlogs", 
            "netarkivtest.log");
    static final File TESTLOGPROP = 
            new File("tests/dk/netarkivet/testlog.prop");
    static final File BATCH_OUTPUT_FILE =
            new File(WORKING_DIR, "batch_output.log");

    static String baAppId = "bitArchiveApp_1";

    static ChannelID QUEUE_1 = Channels.getAnyBa();
    static final long BITARCHIVE_BATCH_JOB_TIMEOUT = 14*24*60*60*1000;
}
