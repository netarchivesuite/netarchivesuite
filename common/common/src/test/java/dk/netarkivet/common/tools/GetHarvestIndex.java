/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.common.tools;

import java.util.HashSet;
import java.util.Set;

import dk.netarkivet.common.distribute.indexserver.Index;
import dk.netarkivet.common.distribute.indexserver.IndexClientFactory;
import dk.netarkivet.common.distribute.indexserver.JobIndexCache;

/**
 * A tool to get harvestindices on demand.
 *
 * Usage: java dk.netarkivet.common.tools.GetHarvestIndex [jobid]+
 *
 */
public class GetHarvestIndex {

    
    /**
     * Find the jobIds in the argumentList
     * @param argv
     * @return
     */
    private static Set<Long> findJobIds(String[] argv) {
        Set<Long> jobIds = new HashSet<Long>();
        for (String arg: argv) {
            jobIds.add(Long.valueOf(arg));
        }
        return jobIds;
    }
    
    /**
     * 
     * @param argv
     */
    public static void main(String[] argv) {
        if (argv.length < 1) {
            System.err.println("Too few arguments.");
            dieWithUsage();
        }
        
        Set<Long> jobIDs = findJobIds(argv);
        JobIndexCache cache = IndexClientFactory.getDedupCrawllogInstance();
        Index<Set<Long>> index = cache.getIndex(jobIDs);
        }


    private static void dieWithUsage() {
        System.out.println("Usage: java " + GetHarvestIndex.class.getName()
                + "[jobid]+");
        System.exit(1);
    }
}
