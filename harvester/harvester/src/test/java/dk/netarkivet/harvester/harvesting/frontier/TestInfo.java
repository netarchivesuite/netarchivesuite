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
