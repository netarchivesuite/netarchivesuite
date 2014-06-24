package dk.netarkivet.harvester.harvesting.frontier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;

import junit.framework.TestCase;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

public class InMemoryFrontierReportTest extends TestCase {
    
    ReloadSettings rs = new ReloadSettings();

    /* (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        rs.setUp();
        super.setUp();
        
        Settings.set(
                CommonSettings.CACHE_DIR, 
                TestInfo.WORKDIR.getAbsolutePath());
    }
    
    public void tearDown() throws Exception {
        
        File[] testDirs = TestInfo.WORKDIR.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        
        for (File dir : testDirs) {
            FileUtils.removeRecursively(dir);
        }
        
        super.tearDown();
        rs.tearDown();
    }
    
    public final static void testAll() throws IOException {
        for (File reportFile : TestInfo.getFrontierReportSamples()) {
            InMemoryFrontierReport report = parse(reportFile);
            
            BufferedReader in = new BufferedReader(new FileReader(reportFile));
            String inLine = in.readLine(); // discard header line
            while((inLine = in.readLine()) != null) {
                String domainName = inLine.split("\\s+")[0];
                assertEquals(
                        inLine, 
                        FrontierTestUtils.toString(
                                report.getLineForDomain(domainName)));
            }
            
            in.close();
        }
        
    }
    
    private static InMemoryFrontierReport parse(File reportFile) 
    throws IOException {
        
        InMemoryFrontierReport report = new InMemoryFrontierReport(
                "test-" + System.currentTimeMillis());
        
        BufferedReader in = new BufferedReader(new FileReader(reportFile));
        String inLine = in.readLine(); // discard header line
        while((inLine = in.readLine()) != null) {
            report.addLine(new FrontierReportLine(inLine));
        }
        
        in.close();
        
        return report;
    }
    
    

}
