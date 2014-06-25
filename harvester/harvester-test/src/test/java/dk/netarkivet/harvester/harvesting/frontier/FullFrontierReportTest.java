/*
 * #%L
 * Netarchivesuite - harvester - test
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.harvester.harvesting.frontier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import junit.framework.TestCase;

public class FullFrontierReportTest extends TestCase {
    
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
    
    public final static void testParseStoreAndDispose() throws IOException {
        for (File reportFile : TestInfo.getFrontierReportSamples()) {
            FullFrontierReport report = 
                FullFrontierReport.parseContentsAsString(
                        "test-" + System.currentTimeMillis(), 
                        FileUtils.readFile(reportFile));
            report.dispose();
            assertFalse(report.getStorageDir().exists());
        }
        
    }
    
    public final static void testAll() throws IOException {
        for (File reportFile : TestInfo.getFrontierReportSamples()) {
            FullFrontierReport report = 
                FullFrontierReport.parseContentsAsString(
                        "test-" + System.currentTimeMillis(), 
                        FileUtils.readFile(reportFile));
            
            BufferedReader in = new BufferedReader(new FileReader(reportFile));
            String inLine = in.readLine(); // discard header line
            while((inLine = in.readLine()) != null) {
                String domainName = inLine.split("\\s+")[0];
                assertEquals(
                        inLine, 
                        FrontierTestUtils.toString(
                                report.getLineForDomain(domainName)));
            }
            
            report.dispose();
            in.close();
            
            assertFalse(report.getStorageDir().exists());
        }
        
    }
    
    

}
