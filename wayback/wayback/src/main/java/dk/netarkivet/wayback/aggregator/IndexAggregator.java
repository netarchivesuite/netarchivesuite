/* File:   $Id$
 * Revision: $Revision$
 * Author:   $Author$
 * Date:     $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *  USA
 */

package dk.netarkivet.wayback.aggregator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IndexAggregator {
    
    /* Logger. */
    private Log log = LogFactory.getLog(getClass().getName());

    /**
     * Generates a sorted CDX index file based on the set of unsorted CDX input files
     * @param filesNames A list of the files to aggregate
     * @param outputFile  Name of the outputfile 
     */
    public void processFiles(String[] filesNames, String outputFile) {
        Process p = null;
        StringBuffer cmdStringBuffer = new StringBuffer("sort");
        for (int i = 0 ; i<filesNames.length ; i++) {
            cmdStringBuffer.append(" " + filesNames[i]);
        }

        cmdStringBuffer.append(" -o "+outputFile);
        
        try {
            p = Runtime.getRuntime().exec(cmdStringBuffer.toString());
            p.waitFor();
            if (p.exitValue() == -1);
        } catch (Exception e) {
            log.error("Failed to aggregate indexes ", e);
        }
    }
}
