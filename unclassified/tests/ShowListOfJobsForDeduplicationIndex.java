/* $Id: ShowIndexStats.java 2388 2012-06-25 11:42:54Z svc $
 * $Revision: 2388 $
 * $Date: 2012-06-25 13:42:54 +0200 (Mon, 25 Jun 2012) $
 * $Author: svc $
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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

import java.util.Set;

import dk.netarkivet.harvester.dao.HarvestDefinitionDAO;


public class ShowListOfJobsForDeduplicationIndex {

    /**
     * @param args
     */
    public static void main(String[] args) {
        Long harvestId = Long.getLong(args[0]);
        HarvestDefinitionDAO dao = HarvestDefinitionDAO.getInstance();
        Set<Long> jobSet = dao.getJobIdsForSnapshotDeduplicationIndex(harvestId);
        System.out.println("# jobs used in deduplication index for harvest # " + harvestId + " is " + jobSet.size());
        for(Long id: jobSet){
            System.out.println(id);
        }

    }

}
