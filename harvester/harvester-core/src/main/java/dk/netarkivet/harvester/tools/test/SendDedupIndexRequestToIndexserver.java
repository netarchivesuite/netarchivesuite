/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
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
package dk.netarkivet.harvester.tools.test;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.indexserver.RequestType;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.harvester.indexserver.distribute.IndexRequestMessage;

/**
 * Program that sends a a IndexRequestMessage to the indexserver,
 * so that it starts generating a deduplication index for a snapshot harvest
 * Argument: File containing a set of numbers, one number per line.
 */
public class SendDedupIndexRequestToIndexserver {

    /**
     * @param args
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
            if (!jobIdString.equals("")){
                jobIds.add(Long.valueOf(jobIdString));
            }
        }
        return jobIds;
    }

    public static void sendDedupRequestIndexMessage(Set<Long> jobSet) {
        IndexRequestMessage irm = new IndexRequestMessage(
                RequestType.DEDUP_CRAWL_LOG, jobSet, null);
        JMSConnection con = JMSConnectionFactory.getInstance();
        con.send(irm);
        con.cleanup();
    }
}
