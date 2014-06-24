package dk.netarkivet.common.webinterface;

import java.io.File;

public class TestInfo {

    public static final File DATA_DIR = new File("tests/dk/netarkivet/common/webinterface/data");
    public static final File ORIGINALS_DIR = new File(DATA_DIR, "originals");
    public static final File WORKING_DIR = new File(DATA_DIR, "working");
    public static final File TEMPDIR = new File(DATA_DIR, "working/");
    
    public static final int GUI_WEB_SERVER_PORT = 4242;
    public static final String GUI_WEB_SERVER_WEBBASE = "/jsp";
    public static final String GUI_WEB_SERVER_JSP_DIRECTORY 
        = "tests/dk/netarkivet/common/webinterface/data/jsp";
    public static final String GUI_WEB_SERVER_SITESECTION_CLASS
        = TestSiteSection.class.getName();
}
