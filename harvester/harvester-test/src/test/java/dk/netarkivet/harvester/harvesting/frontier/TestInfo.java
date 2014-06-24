package dk.netarkivet.harvester.harvesting.frontier;

import java.io.File;
import java.io.FileFilter;

public class TestInfo {
    
    //General dirs:
    protected static final File BASEDIR = 
        new File("tests/dk/netarkivet/harvester/harvesting/frontier/data");

    protected static final File WORKDIR =
        new File(BASEDIR, "working");
        
    static File[] getFrontierReportSamples() {
        return BASEDIR.listFiles(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isFile() 
                    && f.getName().startsWith("frontierReport_all_sample_")
                    && f.getName().endsWith(".txt");
            }
        });
    }
}
