package dk.netarkivet.archive.bitarchive;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;
import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * BitArchiveAdminData test class.
 * Checking if directories for the bitarchive will be given back in the right order
 */

public class BitarchiveAdminTesterONB extends TestCase {
    private BitarchiveAdmin ad;
    private static final String ARC_FILE_NAME = "testfile.arc";
    ReloadSettings rs = new ReloadSettings();

    public BitarchiveAdminTesterONB(String s) {
        super(s);
    }

    public void setUp() {
        rs.setUp();
    }

    public void tearDown() {
        if (ad != null) {
            ad.close();
        }
        rs.tearDown();
    }


    public void testGetFirstBitarchiveDirectory01() throws IOException {
        final String[] TESTDIRS = {
            	"/mnt/brz/brz_datasrc/wa001"
            };
    	
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();
        
    	File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
    	assertEquals("wa001", path.getParentFile().getParentFile().getName());
    }
    
    public void testGetFirstBitarchiveDirectory02() throws IOException {
        final String[] TESTDIRS = {
            	"/mnt/brz/brz_datasrc/wa001",
            	"/mnt/brz/brz_datasrc/wa002"
            };
    	
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();
        
    	File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
    	assertEquals("wa001", path.getParentFile().getParentFile().getName());
    }
    
    public void testGetFirstBitarchiveDirectory03() throws IOException {
        final String[] TESTDIRS = {
            	"/mnt/brz/brz_datasrc/wa001",
            	"/mnt/brz/brz_datasrc/wa002",
            	"/mnt/brz/brz_datasrc/wa003"
            };
    	
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();
        
    	File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
    	assertEquals("wa001", path.getParentFile().getParentFile().getName());
    }
    
    public void testGetFirstBitarchiveDirectory04() throws IOException {
        final String[] TESTDIRS = {
            	"/mnt/brz/brz_datasrc/wa001",
            	"/mnt/brz/brz_datasrc/wa002",
            	"/mnt/brz/brz_datasrc/wa003",
            	"/mnt/brz/brz_datasrc/wa004"
            };
    	
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();
        
    	File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
    	assertEquals("wa001", path.getParentFile().getParentFile().getName());
    }    

    public void testGetFirstBitarchiveDirectory05() throws IOException {
        final String[] TESTDIRS = {
            	"/mnt/brz/brz_datasrc/wa001",
            	"/mnt/brz/brz_datasrc/wa002",
            	"/mnt/brz/brz_datasrc/wa003",
            	"/mnt/brz/brz_datasrc/wa004",
            	"/mnt/brz/brz_datasrc/wa005"
            };
    	
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();
        
    	File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
    	assertEquals("wa001", path.getParentFile().getParentFile().getName());
    }    

    public void testGetFirstBitarchiveDirectory06() throws IOException {
        final String[] TESTDIRS = {
            	"/mnt/brz/brz_datasrc/wa001",
            	"/mnt/brz/brz_datasrc/wa002",
            	"/mnt/brz/brz_datasrc/wa003",
            	"/mnt/brz/brz_datasrc/wa004",
            	"/mnt/brz/brz_datasrc/wa005",
            	"/mnt/brz/brz_datasrc/wa006"
            };
    	
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();
        
    	File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
    	assertEquals("wa001", path.getParentFile().getParentFile().getName());
    }    

    public void testGetFirstBitarchiveDirectory07() throws IOException {
        final String[] TESTDIRS = {
            	"/mnt/brz/brz_datasrc/wa001",
            	"/mnt/brz/brz_datasrc/wa002",
            	"/mnt/brz/brz_datasrc/wa003",
            	"/mnt/brz/brz_datasrc/wa004",
            	"/mnt/brz/brz_datasrc/wa005",
            	"/mnt/brz/brz_datasrc/wa006",
            	"/mnt/brz/brz_datasrc/wa007"
            };
    	
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();
        
    	File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
    	assertEquals("wa001", path.getParentFile().getParentFile().getName());
    }    

    public void testGetFirstBitarchiveDirectory08() throws IOException {
        final String[] TESTDIRS = {
            	"/mnt/brz/brz_datasrc/wa001",
            	"/mnt/brz/brz_datasrc/wa002",
            	"/mnt/brz/brz_datasrc/wa003",
            	"/mnt/brz/brz_datasrc/wa004",
            	"/mnt/brz/brz_datasrc/wa005",
            	"/mnt/brz/brz_datasrc/wa006",
            	"/mnt/brz/brz_datasrc/wa007",
            	"/mnt/brz/brz_datasrc/wa008"
            };
    	
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();
        
    	File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
    	assertEquals("wa001", path.getParentFile().getParentFile().getName());
    }    


    public void testGetFirstBitarchiveDirectory09() throws IOException {
        final String[] TESTDIRS = {
            	"/mnt/brz/brz_datasrc/wa002",
            	"/mnt/brz/brz_datasrc/wa003",
            	"/mnt/brz/brz_datasrc/wa004",
            	"/mnt/brz/brz_datasrc/wa005",
            	"/mnt/brz/brz_datasrc/wa006",
            	"/mnt/brz/brz_datasrc/wa007",
            	"/mnt/brz/brz_datasrc/wa008"
            };
    	
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();
        
    	File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
    	assertEquals("wa002", path.getParentFile().getParentFile().getName());
    }
    
    public void testGetFirstBitarchiveDirectory10() throws IOException {
        final String[] TESTDIRS = {
            	"/mnt/brz/brz_datasrc/wa003",
            	"/mnt/brz/brz_datasrc/wa004",
            	"/mnt/brz/brz_datasrc/wa005",
            	"/mnt/brz/brz_datasrc/wa006",
            	"/mnt/brz/brz_datasrc/wa007",
            	"/mnt/brz/brz_datasrc/wa008"
            };
    	
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();
        
    	File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
    	assertEquals("wa003", path.getParentFile().getParentFile().getName());
    }
    
    public void testGetFirstBitarchiveDirectory11() throws IOException {
        final String[] TESTDIRS = {
            	"/mnt/brz/brz_datasrc/wa004",
            	"/mnt/brz/brz_datasrc/wa005",
            	"/mnt/brz/brz_datasrc/wa006",
            	"/mnt/brz/brz_datasrc/wa007",
            	"/mnt/brz/brz_datasrc/wa008"
            };
    	
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();
        
    	File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
    	assertEquals("wa004", path.getParentFile().getParentFile().getName());
    }
    
    public void testGetFirstBitarchiveDirectory12() throws IOException {
        final String[] TESTDIRS = {
            	"/mnt/brz/brz_datasrc/wa005",
            	"/mnt/brz/brz_datasrc/wa006",
            	"/mnt/brz/brz_datasrc/wa007",
            	"/mnt/brz/brz_datasrc/wa008"
            };
    	
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();
        
    	File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
    	assertEquals("wa005", path.getParentFile().getParentFile().getName());
    }    

    public void testGetFirstBitarchiveDirectory13() throws IOException {
        final String[] TESTDIRS = {
            	"/mnt/brz/brz_datasrc/wa006",
            	"/mnt/brz/brz_datasrc/wa007",
            	"/mnt/brz/brz_datasrc/wa008"
            };
    	
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();
        
    	File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
    	assertEquals("wa006", path.getParentFile().getParentFile().getName());
    }    

    public void testGetFirstBitarchiveDirectory14() throws IOException {
        final String[] TESTDIRS = {
            	"/mnt/brz/brz_datasrc/wa007",
            	"/mnt/brz/brz_datasrc/wa008"
            };
    	
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();
        
    	File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
    	assertEquals("wa007", path.getParentFile().getParentFile().getName());
    }    

    public void testGetFirstBitarchiveDirectory15() throws IOException {
        final String[] TESTDIRS = {
            	"/mnt/brz/brz_datasrc/wa008"
            };
    	
        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();
        
    	File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
    	assertEquals("wa008", path.getParentFile().getParentFile().getName());
    }    
}