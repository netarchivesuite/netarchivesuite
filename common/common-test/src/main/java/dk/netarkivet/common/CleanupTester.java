package dk.netarkivet.common;

import java.io.File;
import java.io.FileFilter;

import junit.framework.TestCase;
import dk.netarkivet.common.utils.FileUtils;

public class CleanupTester extends TestCase {

    private String[] dirsToClean = new String[]{"derbyDB/wayback_indexer_db", "oldjobs"};
    
	private File tmpdir;
	
	@Override
    public void setUp() throws Exception {
        super.setUp();
        tmpdir = FileUtils.getTempDir();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Remove files in FileUtils.getTempDir();
     */
    public void testThatMostTmpFilesGone() {
    	
    	File[] files = tmpdir.listFiles(new SvnFileFilter());
    	for (File f: files) {
    		FileUtils.removeRecursively(f);
    	}
    	File tmp = new File("tmp");
    	File tmp1 = new File("tmp1");
    	FileUtils.remove(tmp);
    	FileUtils.remove(tmp1);
    	for (String fileToDelete : dirsToClean) {
    	    File f = new File(fileToDelete);
    	    System.out.println("Ready to delete file " + f.getAbsolutePath());
    	    FileUtils.removeRecursively(f);
    	}
    	
    } 
    
    class SvnFileFilter implements FileFilter {	
    		public boolean accept(File f) {
    			return !f.getName().equalsIgnoreCase(".svn");
    		}
    }
}
