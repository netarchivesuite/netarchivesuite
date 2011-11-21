/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2011 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dk.netarkivet.archive.indexserver.distribute.IndexRequestMessage;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.indexserver.RequestType;
import dk.netarkivet.common.utils.FileUtils;

/**
 * Send an {@link IndexRequestMessage} of type {@link RequestType#DEDUP_CRAWL_LOG}
 * to the indexserver with a list of JobIDs. 
 * This program will not wait to receive the index, just request it.
 */
public class SendDedupIndexRequestToIndexserver {

    /**
     * Main program that sends a {@link IndexRequestMessage} to the indexserver
     * with a list of JobIDs. 
     * @param args The file with the jobIDs
     */
    public static void main(String[] args) {
            if (args.length != 1) {
                System.err.println(
                        "arguments missing. Expected <jobId-file>");
                System.exit(1);
            }
            File jobsidFile = new File(args[0]);
            Set<Long> jobs = getJobs(jobsidFile);
            System.out.println(
                    "Sending dedupIndexRequest to Indexserver for #jobs: "
                    + jobs.size());
            sendDedupRequestIndexMessage(jobs);
        }
        
        private static Set<Long> getJobs(File jobsidFile) {
            Set<Long> jobIds = new HashSet<Long>();
            List<String> jobIdsStrings = FileUtils.readListFromFile(jobsidFile);
            for (String jobIdString: jobIdsStrings) {
                if (!jobIdString.isEmpty()) {
                    jobIds.add(Long.valueOf(jobIdString));
                }
            }
            return jobIds;
        }

        public static void sendDedupRequestIndexMessage(Set<Long> jobSet) {
            IndexRequestMessage irm = new IndexRequestMessage(
                    RequestType.DEDUP_CRAWL_LOG, jobSet);
            JMSConnection con = JMSConnectionFactory.getInstance();
            con.send(irm);
            con.cleanup();
        }
}
