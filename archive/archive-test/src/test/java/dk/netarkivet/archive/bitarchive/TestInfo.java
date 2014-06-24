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
    static final long BITARCHIVE_BATCH_JOB_TIMEOUT = 7*24*60*60*1000;
}
