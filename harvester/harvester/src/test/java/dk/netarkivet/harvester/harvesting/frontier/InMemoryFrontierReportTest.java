/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
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
