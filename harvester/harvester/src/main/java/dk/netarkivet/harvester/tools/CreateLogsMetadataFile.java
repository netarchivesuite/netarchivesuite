/* $Id$
 * $Date$
 * $Revision$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.harvester.tools;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import dk.netarkivet.common.tools.SimpleCmdlineTool;
import dk.netarkivet.common.tools.ToolRunnerBase;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.harvester.harvesting.HarvestDocumentation;


/**
 * Create a metadata arcfile for the logs and other archive-worthy
 * job-information found in a given jobsdir.
 * A jobsdir is only valid, if it contains a harvestInfo.xml
 * Requires a jobid-harvestid.txt file created by
 * CreateHarvestMappingsFromAdminData.
 * The metadata arcfile are named: <jobid>-metadata-2.arc file
 * (note: <jobid>-metadata-1.arc is already used)
 *
 */
public class CreateLogsMetadataFile extends ToolRunnerBase {

    /** Main method.  Creates and runs the tool object responsible for
     * creating the logs metadata file for a job.
     *
     * @param argv Arguments to the tool: pathToJobidHarvestid.txt jobsDir
     *
     *
     */
    public static void main(String[] argv) {
        new CreateLogsMetadataFile().runTheTool(argv);
    }


    /** Create the tool instance.
    *
    * @return A new tool object.
    */
   protected SimpleCmdlineTool makeMyTool() {
       return new CreateLogsMetadataFileTool();
   }

   /** The actual tool object that creates CDX files.
    */
   private static class CreateLogsMetadataFileTool
           implements SimpleCmdlineTool {

       /** HashMap containing jobid-harvestid mappings. */
       private HashMap<String, String> hm;

       /** Check that valid arguments are given.
        *
        * @param args The args given on the command line.
        * @return True if the args are legal.
        */
       public boolean checkArgs(String... args) {
           if (args.length < 2) {
               System.err.println("Too few arguments");
               return false;
           }
           if (args.length > 2) {
               System.err.println("Too many arguments: '"
                       + StringUtils.conjoin("', '", Arrays.asList(args))
                       + "'");
               return false;
           }
           if (!(new File(args[0])).isFile()) {
               System.err.println(
                       "The first argument is not a file or " 
                       + "it does not exist: '"
                       + StringUtils.conjoin("', '", Arrays.asList(args))
                       + "'");
               return false;
           }
           if (!(new File(args[1])).isDirectory()) {
               System.err.println(
                       "The second argument is not a directory or "
                       + "it does not exist: '"
                       + StringUtils.conjoin("', '", Arrays.asList(args))
                       + "'");
               return false;
           }
           if (!(new File(args[1], "harvestInfo.xml").exists())) {
               System.err.println("The second argument is not a valid jobsdir. "
                       + "It does not contain a harvestInfo.xml file: "
                       + StringUtils.conjoin("', '", Arrays.asList(args))
                       + "'");
               return false;
           }
           return true;
       }

       /**
       * Create required resources here.
       * Establish the jobid-harvestid HashMap
       * Resources created here should be released in tearDown, which is
       * guaranteed to be run.
       *
       * @param args The arguments that were given on the command line
       * (not used here)
       */
      public void setUp(String... args) {
          File inputFile = new File(args[0]);
          List<String> idmappings = FileUtils.readListFromFile(inputFile);
          hm = new HashMap<String, String>();
          for (String mapping: idmappings) {
              String[] components = mapping.split(",");
              hm.put(components[0], components[1]);
          }
          System.out.println(String.format(
                  "%d jobid-harvestid mappings found",
                  hm.keySet().size())
          );
      }

      /**
       * Closes all resources we are using.
       * This is guaranteed to be called at shutdown.
       */
      public void tearDown() {}

      /** Return a string describing the parameters needed by the
       * CreateLogsMetadataFile tool.
       *
       * @return String with description of parameters.
       */
      public String listParameters() {
          return "path-to-jobid-harvestid.txt jobsdir";
      }

      /** The workhorse method of this tool. Here is the real action:
       *  - Retrieve the JobID by parsing the name of the crawlDir.
       *  - Create a <jobid>-metadata-2.arc by calling
       *        HarvestDocumentation.documentOldJob.
       *  - Delete all files added to the newly created metadata arcfile.
       * @param args Arguments given on the command line.
       */
      public void run(String... args) {
          File crawlDir = new File(args[1]);
          String jobIdString = crawlDir.getName().split("_")[0];
          String harvestIdString = hm.get(jobIdString.trim());
          if (harvestIdString == null) {
              System.err.println(
                    String
                    .format("Unable to lookup Jobid %s in jobid-harvestid.txt",
                              jobIdString));
              System.exit(1);
          }
          long jobId = Long.parseLong(jobIdString);
          long harvestId = Long.parseLong(harvestIdString);

          List<File> filesAdded = HarvestDocumentation.documentOldJob(
                  crawlDir, jobId, harvestId);

          // Delete the added files
          for(File fileAdded: filesAdded) {
              FileUtils.remove(fileAdded);
          }

      }
   }
}
