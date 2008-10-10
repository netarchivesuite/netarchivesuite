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

package dk.netarkivet.archive.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.Location;
import dk.netarkivet.common.distribute.arcrepository.ViewerArcRepositoryClient;
import dk.netarkivet.common.tools.SimpleCmdlineTool;
import dk.netarkivet.common.tools.ToolRunnerBase;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.arc.LoadableFileBatchJob;
import dk.netarkivet.common.utils.arc.LoadableJarBatchJob;
import dk.netarkivet.common.utils.arc.FileBatchJob;
import dk.netarkivet.common.utils.arc.FileBatchJob.ExceptionOccurrence;

import org.apache.commons.cli.*;

/**
 * A command-line tool to run batch jobs in the bitarchive.
 *
 * Usage:
 *  java dk.netarkivet.archive.tools.RunBatch 
 *       with arguments as defined in local class BatchParameters 
 *
 * where -C<classfile> is a file containing a FileBatchJob implementation
 *       -R<regexp> is a regular expression that will be matched against
 *              file names in the archive, by default .*
 *       -B<location> is the bitarchive location this should be run on, by
 *              default taken from settings.
 *       -O<outputfile> is a file where the output from the batch job will be
 *              written.  By default, it goes to stdout.
 *       -E<errorFile> is a file where the errors from the batch job will be
 *              written. By default, it goes to stderr.
 * Example:
 *
 * java dk.netarkivet.archive.tools.RunBatch -CFindMime.class -R10-*.arc \
 *                                           -BSB -omimes
 *
 * TODO: There are mede preparations for inclusion of jar files in the argument
 *       list
 *        
 * Note that you probably want to set the HTTP port setting
 * ({@literal CommonSettings#HTTP_PORT_NUMBER}) to something other than its 
 * default value to avoid clashing with other channel listeners.
 */
public class RunBatch extends ToolRunnerBase {
    /**
     * Main method.  Runs a batch job in the bitarchive.
     * Setup, teardown and run is delegated to the RunBatchTool class.
     * Management of this, exception handling etc. is delegated to
     * ToolRunnerBase class.
     *
     * @param argv command line parameters as defined in local class 
     *        BatchParameters
     * required:
     *   the name of a class-file containing an implementation of FileBatchJob
     *   TODO: name of jar file which includes class file, if class file is  
     *         given indirectly via jar file.
     */
    public static void main(String[] argv) {
        RunBatch instance = new RunBatch();
        instance.runTheTool(argv);
    }

    /** Create an instance of the actual RunBatchTool.
     * @return an instance of RunBatchTool.
     */
    protected SimpleCmdlineTool makeMyTool() {
        return new RunBatchTool();
    }

    /** The implementation of SimpleCmdlineTool for RunBatch. */
    private static class RunBatchTool implements SimpleCmdlineTool {
        /**
         * This instance is declared outside of run method to ensure reliable
         * teardown in case of exceptions during execution.
         */
        private ViewerArcRepositoryClient arcrep;
        
        /** Default regexp that matches everything. */
        private static final String DEFAULT_REGEXP = ".*";
        
        /** The regular expression that will be matched against
            file names in the archive, by default .*
        */
        private String regexp = DEFAULT_REGEXP;
        
        /** Bitarchive location where batchjob is to be run. Set to setting 
         *  batch location as default */
        private Location batchLocation = Location.get(Settings.get(
                CommonSettings.ENVIRONMENT_BATCH_LOCATION)
        );

        /**
         * The outputfile, if any was given.
         */
        private File outputFile;
        
        /** The errorfile, if any was given. */
        private File errorFile;
        
        /** file types in input parameter */
        private enum FileType {OTHER, JAR, CLASS};
        
        /** 
         * getting FileType from given file name 
         * @param fileName The file name to get file type from
         * @return FileType found from extension of file name
         */
        private FileType getFileType(String fileName) {
            int i = fileName.lastIndexOf(".");
            if (i > 0) {
                String s = fileName.substring(i).toLowerCase();
                if (s.equals(".class")) {
                    return FileType.CLASS;
                } else {
                    if (s.equals(".jar")) {
                        return FileType.JAR;
                    } else {
                        return FileType.OTHER;
                    }
                }
            } else { 
                return FileType.OTHER;
            }
        }

        /** 
         * getting FileType from given file name 
         * @param fileName The file name to get file type from
         * @return FileType found from extension of file name
         */
        private boolean checkWriteFile(String fileName, String fileTag) {
            if (new File(fileName).exists()) {
                System.err.println(fileTag + " '" + fileName
                        + "' does already exist");
                return false;
            } else {
                try {
                    File tmpFile = new File(fileName);
                    tmpFile.createNewFile();
                    if (!tmpFile.canWrite()) {
                        System.err.println(fileTag + " '" + fileName
                                + "' cannot be written to");
                        return false;
                    } else {
                        return true;
                    }
                } catch (IOException e) {
                    System.err.println(fileTag + " '" + fileName
                            + "' cannot be created.");
                    return false;
                }
            }
        }
        
        /** 
         * Type to encapsulate parameters defined by options to batchjob 
         * based on apache.cli
         */
        private class BatchParameters {
            /**
             * Options object for parameters
             */
            Options options = new Options();      
            private CommandLineParser parser = new PosixParser();
            CommandLine cmd;
            //HelpFormatter only prints directly, thus this is not used at the moment
            //HelpFormatter formatter = new HelpFormatter();
            //Instead the method listArguments is defined
            
            /**
             * Initialize options by setting legal parameters for batch jobs
             */
            BatchParameters() {
                options.addOption(
                    "C", true, "Class file to be run"
//                  //TODO jar addition: + "from class file or from "
                    //           + "specified jar file (is required)"
                );
                //TODO jar addition 
                //options.addOption(
                //    "J", true, "Jar file to be run (required if class file " 
                //               + "is in jar file)");
                options.addOption(
                    "R", true, "Regular expression for files to be processed "
                               + "(default: '" + regexp + "')");
                options.addOption(
                    "B", true, "Bitarchive location where batch must be run "
                                + "(default: '" 
                                + CommonSettings.ENVIRONMENT_BATCH_LOCATION
                                + "')");
                options.addOption(
                    "O", true, "Output file to contain result (default is "
                               + "stdout)");
                options.addOption(
                    "E", true, "Error file to contain errors from run "
                               + "(default is stderr)");
            }
            
            String parseParameters(String[] args) {
                try {
                    // parse the command line arguments
                    cmd = parser.parse( options, args);
                }
                catch(ParseException exp) {
                    return "Parsing parameters failed.  Reason is: " + exp.getMessage();
                }
                return "";
            }
            
            String listArguments() {
                String s = "\nwith arguments:\n";
                // add options
                for (Object o: options.getOptions()) {
                    Option op = (Option)o;
                    s += "-" + op.getOpt() + " " + op.getDescription() + "\n";  
                }
                //delete last delimitter
                if (s.length() > 0) {
                    s = s.substring(0, s.length()-1);
                }
                return s;
            }
        }
        
        
        /** To contain parameters defined by options to batchjob */
        private BatchParameters parms = new BatchParameters();
        
        /**
         * Accept parameters and checks them for validity.
         * @param args the arguments
         * @return true, if given arguments are valid
         *  returns false otherwise
         */
        public boolean checkArgs(String... args) {
            //Parse arguments to check that the options are valid
            String msg = parms.parseParameters(args);
            if (msg.length() > 0) { 
                System.err.println(msg);
                return false;
            }
            
            //Check number of arguments
            if (args.length < 1) {
                System.err.println(
                        "Missing required argument: "
//                      //TODO jar addition + "jar and/or "
                        + "class file"
                );
                return false;
            } 
            if (args.length > parms.cmd.getOptions().length) {
                System.err.println("Too many arguments");
                return false;
            }

            //Check class file argument
            String jar = null; //TODO jar addition set to: parms.cmd.getOptionValue("J");
            String cl = parms.cmd.getOptionValue("C");
            if (cl == null) {
                msg = "Missing required class file argument ";
                if (jar == null) { msg += "(-C)"; }
                else { msg += " to run in jar-file (-C)"; }
                System.err.println(msg);
                return false;
            } 
            if (!getFileType(cl).equals(FileType.CLASS)) {
                System.err.println("Argument '"+ cl + "' is not denoting a class file");
                return false;
            }
            if (jar == null) {
                if (!new File(cl).canRead()) {
                    System.err.println("Cannot read class file: '" + cl + "'");
                    return false;
                }
            } //else class file is included in jar file
            
            //Check jar file argument
            if (jar != null) {
                if (!getFileType(jar).equals(FileType.JAR)) {
                    System.err.println("Argument '"+ jar 
                                       + "' is not denoting a jar file");
                    return false;
                }
                if (!new File(jar).canRead()) {
                    System.err.println("Cannot read jar file: '" + jar + "'");
                    return false;
                }
            } 

            //Check regular expression argument
            String reg = parms.cmd.getOptionValue("R");
            if (reg != null) {
                try {
                    Pattern.compile(reg);
                } catch (PatternSyntaxException e) {
                    System.err.println("Illegal pattern syntax: '"
                                       + reg + "'");
                    return false;
                }
            }
            
            //Check bitarchive location argument
            String loc = parms.cmd.getOptionValue("B");
            if (loc != null) {
                if (!Location.isKnownLocation(loc)) {
                    System.err.println("Unknown location '" + loc
                                       + "', known location are "
                                       + Location.getKnown());
                    return false;
                }
            }
            
            //Check output file argument
            String oFile = parms.cmd.getOptionValue("O");
            if (oFile != null) {
                if (!checkWriteFile(oFile, "Output file")) {
                    return false;
                }
            }

            //Check output file argument
            String eFile = parms.cmd.getOptionValue("E");
            if (eFile != null) {
                if (!checkWriteFile(eFile, "Error file")) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Create the ArcRepositoryClient instance here for reliable execution
         * of close method in tearDown.
         *
         * @param args the arguments (not used)
         */
        public void setUp(String... args) {
            arcrep = ArcRepositoryClientFactory.getViewerInstance();
        }

        /**
         * Ensure reliable execution of the ArcRepositoryClient.close() method.
         * Remember to check if arcrep was actually created. Also reliably
         * cleans up the JMSConnection.
         */
        public void tearDown() {
            if (arcrep != null) {
                arcrep.close();
            }
            JMSConnectionFactory.getInstance().cleanup();
        }

        /**
         * Perform the actual work. Procure the necessary information from
         * command line parameters and system settings required to run the
         * ViewerArcRepositoryClient.batch(), and perform the operation.
         * Creating and closing the ArcRepositoryClient (arcrep) is
         * done in the setUp and tearDown methods.
         *
         * @param args the arguments
         */
        public void run(String... args) {
            //Arguments are allready checked by checkArgs 
            String jarName = null; //TODO jar addition set to: parms.cmd.getOptionValue("J");
            String className = parms.cmd.getOptionValue("C");
            
            FileBatchJob job;

            if (jarName == null) {
                LoadableFileBatchJob classJob = new LoadableFileBatchJob(new File(className));
                job = classJob;
            } else {
                LoadableJarBatchJob jarJob = new LoadableJarBatchJob(new File(jarName), className);
                job = jarJob;
            }
            
            String reg = parms.cmd.getOptionValue("R");
            if (reg != null) { 
                regexp = reg;
                job.processOnlyFilesMatching(regexp);
            }
            
            String loc = parms.cmd.getOptionValue("B");
            if (loc != null) { 
                batchLocation = Location.get(loc);
            }
            
            //Note: if no filename is given, output will be written to stdout
            String oFile = parms.cmd.getOptionValue("O");
            if (oFile != null) { 
                outputFile = new File(oFile);
            }
            
            //Note: if no filename is given, errors will be written to stderr
            String eFile = parms.cmd.getOptionValue("E");
            if (eFile != null) {
                errorFile = new File(eFile);
            }

            System.out.println(
                "Running batch job '" + className + "' "
              //TODO jar addition: 
              //+ ((jarName == null) ? "" : "from jar-file '" + jarName + "' ")
                + "on files matching '" + regexp + "' "
                + "on location '" + batchLocation.getName() + "', " 
                + "output written to " 
                   + ((oFile == null) ? "stdout " : "file '" + oFile + "', ")
                + "errors written to " 
                   + ((eFile == null) ? "stderr " : "file '" + eFile + "' ")
            );
            BatchStatus status = arcrep.batch(job, batchLocation.getName());
            final Collection<File> failedFiles = status.getFilesFailed();
            Collection<ExceptionOccurrence> exceptions = status.getExceptions();
            
            System.out.println("Processed " + status.getNoOfFilesProcessed()
                               + " files with "
                               + failedFiles.size()
                               + " failures");
            
            //Write to output file or stdout
            if (outputFile == null) {
                status.appendResults(System.out);
            } else {
                status.copyResults(outputFile);
            }
            
            
            //Write to error file or stderr
            PrintStream errorOutput = System.err;
            if (errorFile != null) {
                try {
                    System.err.println("Writing errors to file: "
                            + errorFile.getAbsolutePath());
                    errorOutput = new PrintStream(errorFile);
                } catch (FileNotFoundException e) {
                    //Should not occur since argument is checked
                    System.err.println(
                            "Unable to to create errorfile for writing: " + e);
                    System.err.println(
                            "Writing errors to stdout instead!");                
                }
            }
            
            if (!failedFiles.isEmpty()) {
                errorOutput.println("Failed files:");
                for (File f : failedFiles) {
                    errorOutput.println(f.getName());
                }
            }
            
            if (!exceptions.isEmpty()) {
                errorOutput.println("Failed files that produced exceptions("
                        + exceptions.size() + "):");
                for (ExceptionOccurrence occurrence : exceptions) {
                    errorOutput.println("File: " + occurrence.getFileName());
                    errorOutput.println("Offset: " +  occurrence.getFileOffset());
                    errorOutput.println("OutputOffset: " +  occurrence.getOutputOffset());
                    errorOutput.println("Class name: " +  occurrence.getClass().getName());
                    errorOutput.println("Was exception during initialize: " + occurrence.isInitializeException());
                    errorOutput.println("Was exception during finish: " + occurrence.isFinishException());
                    errorOutput.println("Exception w/stacktrace: ");
                    occurrence.getException().printStackTrace(errorOutput);
                }
                
            }
            errorOutput.close();
        }

        /**
         * Return the list of parameters accepted by the RunBatchTool class.
         *
         * @return the list of parameters accepted.
         */
        public String listParameters() {
            return parms.listArguments();
        }

    }
}
