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
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import junit.framework.TestCase;
import dk.netarkivet.harvester.harvesting.frontier.TestInfo;

public class FrontierReportLineTest extends TestCase {
    
    public final static void testParseReports() throws IOException {
        
        String[] files = new String[] {
                "frontierReport_all_sample_small.txt",
                "frontierReport_all_sample_atlas502.txt"
        };
        
        for (String file : files) {
            File report = new File(TestInfo.BASEDIR, file);
            BufferedReader br = new BufferedReader(new FileReader(report));
        
            int lineNum = 1;
            String line = br.readLine(); // Discard header line
            while ((line = br.readLine()) != null) {
                lineNum++;
                FrontierReportLine hfrLine = 
                    new FrontierReportLine(line);
                assertEquals(file + ", " +"l." + lineNum, 
                        line, 
                        FrontierTestUtils.toString(hfrLine));
            }
            
            br.close();
        }
        
    }
    
    public static final void testParseDoubleValues() {
        
        String line = "000webhost.com 4 8 0 1(1.2) 2010-06-02T12:36:09.208Z - " 
            + "5/-1 1 http://www.000webhost.com/ " 
            + "http://www.000webhost.com/images/banners/160x600/banner1.gif";
        
        FrontierReportLine hfrLine = 
            new FrontierReportLine(line);
        
        assertEquals(line, FrontierTestUtils.toString(hfrLine));
    }
    
}
