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

package dk.netarkivet.harvester.tools;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.indexserver.Index;
import dk.netarkivet.common.distribute.indexserver.IndexClientFactory;
import dk.netarkivet.common.distribute.indexserver.JobIndexCache;

/**
 * A tool to ask indices from indexserver on demand.
 * <p>
 * Usage: java dk.netarkivet.archive.tools.CreateIndex --type cdx|dedup|crawllog [jobid]+
 */
@SuppressWarnings({"unused"})
public class CreateIndex {
    /**
     * Private constructor to avoid instantiation of this class.
     */
    private CreateIndex() {
    }

    /** Option for selecting the type of index required. */
    private static final String INDEXTYPE_OPTION = "t";

    /** Option for selecting the jobids to be used in the index. */
    private static final String JOBIDS_OPTION = "l";

    /**
     * The main method that does the parsing of the commandline, and makes the actual index request.
     *
     * @param args the arguments
     */
    public static void main(String[] args) {
        Options options = new Options();
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = null;
        Option indexType = new Option("t", "type", true, "Type of index");
        Option jobList = new Option("l", "jobids", true, "list of jobids");
        indexType.setRequired(true);
        jobList.setRequired(true);
        options.addOption(indexType);
        options.addOption(jobList);

        try {
            // parse the command line arguments
            cmd = parser.parse(options, args);
        } catch (MissingOptionException e) {
            System.err.println("Some of the required parameters are missing: " + e.getMessage());
            dieWithUsage();
        } catch (ParseException exp) {
            System.err.println("Parsing of parameters failed: " + exp.getMessage());
            dieWithUsage();
        }

        String typeValue = cmd.getOptionValue(INDEXTYPE_OPTION);
        String jobidsValue = cmd.getOptionValue(JOBIDS_OPTION);
        String[] jobidsAsStrings = jobidsValue.split(",");
        Set<Long> jobIDs = new HashSet<Long>();
        for (String idAsString : jobidsAsStrings) {
            jobIDs.add(Long.valueOf(idAsString));
        }

        JobIndexCache cache = null;
        String indexTypeAstring = "";
        if (typeValue.equalsIgnoreCase("CDX")) {
            indexTypeAstring = "CDX";
            cache = IndexClientFactory.getCDXInstance();
        } else if (typeValue.equalsIgnoreCase("DEDUP")) {
            indexTypeAstring = "DEDUP";
            cache = IndexClientFactory.getDedupCrawllogInstance();
        } else if (typeValue.equalsIgnoreCase("CRAWLLOG")) {
            indexTypeAstring = "CRAWLLOG";
            cache = IndexClientFactory.getFullCrawllogInstance();
        } else {
            System.err.println("Unknown indextype '" + typeValue + "' requested.");
            dieWithUsage();
        }

        System.out.println("Creating " + indexTypeAstring + " index for ids: " + jobIDs);
        Index<Set<Long>> index = cache.getIndex(jobIDs);
        JMSConnectionFactory.getInstance().cleanup();
    }

    /**
     * Method for terminating this instance, with writing out the usage. This is used when the arguments are incorrect.
     */
    private static void dieWithUsage() {
        System.err.println("Usage: java " + CreateIndex.class.getName()
                + " -type cdx|dedup|crawllog -jobids jobid[,jobid]*");
        System.exit(1);
    }
}
