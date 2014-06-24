package dk.netarkivet.common.utils;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import dk.netarkivet.common.distribute.JMSConnectionSunMQ;

public class TestInfo {
    public static final File BASEDIR = new File("./tests/dk/netarkivet/common/utils");
    public static final File TEMPDIR = new File(BASEDIR, "working");
    public static final File DATADIR = new File(BASEDIR, "data");
    public static final File TESTXML = new File(TEMPDIR, "test.xml");
    public static final File NEWXML = new File(TEMPDIR, "savedtest.xml");
    public static final File INVALIDXML = new File(TEMPDIR, "invalid.xml");
    public static final String SETTINGSFILENAME =
            new File(TEMPDIR, "settings.xml").getAbsolutePath();
    public static final String PORT = JMSConnectionSunMQ.JMS_BROKER_PORT;
    
    public static final String PORTVALUE = "7676";
    public static final String PROCESS_TIMEOUT_VALUE = "5000";
    public static final String TIMEOUT = "settings.common.arcrepositoryClient.timeout";
    public static final String FIVE_HUNDRED_MEGA_FILE_ZIPPED = "500-mega.zip";
    public static final String FIVE_HUNDRED_MEGA_FILE_UNZIPPED = "500-mega";
    public static final File MD5_EMPTY_FILE = new File(TEMPDIR, "MD5-empty-file");
    public static final String UNUSED_PROPERTY = "settings.never.ever.define.this.property";

    static final File FILEUTILS_DATADIR = new File(BASEDIR, "fileutils_data");
    static final File CDX_FILTER_TEST_DATA_DIR = new File(FILEUTILS_DATADIR,"cdx-filter");
    static final Set<String> CDX_FILTER_TEST_CDX_FILES =
        new HashSet<String>(java.util.Arrays.asList(new String[]{
                "Reader1.cdx",
                "Reader2.cdx"
        }));
    public static final File XML_FILE_1 = new File(TestInfo.TEMPDIR, "test1.xml");
    public static final File XML_FILE_2 = new File(TestInfo.TEMPDIR, "test2.xml");
    public static final String XML_FILE_1_XPATH_1 = "/test/file/attachHere";
    public static final String LAST_LINE_TEXT = "last line";
    public static final File FILE_WITH_NEWLINE_AT_END 
        = new File(TEMPDIR, "newlinedfile.txt");
    public static final File FILE_WITH_NO_NEWLINE_AT_END 
        = new File(TEMPDIR, "nonewlinedfile.txt");
    public static final File FILE_WITH_ONE_LINE = new File(TEMPDIR, "onelinedfile.txt");
    public static final File EMPTY_FILE = new File(TEMPDIR, "emptyfile.txt");
    public static File LOG_FILE = new File("tests/testlogs/netarkivtest.log");
    public static final File ZIPDIR = new File(TEMPDIR, "zipdir");
    public static final File ZIPPED_DIR = new File(TestInfo.ZIPDIR, "zippedDir.zip");
    public static final File ZIPPED_DIR_WITH_SUBDIRS 
        = new File(TestInfo.ZIPDIR, "sub/zippedDirWithSubdirs.zip");

    public static final File NON_EXISTING_FILE = new File("/no/such/file");
    public static final File SETTINGS_FILE = new File(TEMPDIR, "settings.xml");

    }
