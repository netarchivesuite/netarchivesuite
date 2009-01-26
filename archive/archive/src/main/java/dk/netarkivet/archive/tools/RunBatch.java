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
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ViewerArcRepositoryClient;
import dk.netarkivet.common.tools.SimpleCmdlineTool;
import dk.netarkivet.common.tools.ToolRunnerBase;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.batch.LoadableFileBatchJob;
import dk.netarkivet.common.utils.batch.LoadableJarBatchJob;
import dk.netarkivet.common.utils.batch.FileBatchJob.ExceptionOccurrence;

import org.apache.commons.cli.*;

/**
 * A command-line tool to run batch jobs in the bitarchive.
 *
 * Usage:
 *  java dk.netarkivet.archive.tools.RunBatch 
 *       with arguments as defined in local class BatchParameters 
 *
 * where -J<jarfile> is a file containing all the classes needed by a BatchJob
 *       -C<classfile> is a file containing a FileBatchJob implementation
 *       -R<regexp> is a regular expression that will be matched against
 *              file names in the archive, by default .*
 *       -B<replica> is the name of the bitarchive replica this should be run
 *               on, by default taken from settings.
 *       -O<outputfile> is a file where the output from the batch job will be
 *              written.  By default, it goes to stdout.
 *       -E<errorFile> is a file where the errors from the batch job will be
 *              written. By default, it goes to stderr.
 *       -N<className> is the name of the primary class to be loaded when doing 
 *              a LoadableJarBatchJob       
 * Examples:
 *
 * java dk.netarkivet.archive.tools.RunBatch -CFindMime.class \ 
 *                          -R10-*.arc -BReplicaOne -Omimes
 * 
 * java dk.netarkivet.archive.tools.RunBatch -JFindMime.jar -NFindMime \ 
 *                          -R10-*.arc -BReplicaOne -Omimes
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
     *   The name of a class-file containing an implementation of FileBatchJob
     *   Name of jar file which includes the class file, and the className
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
            file names in the archive, by default ".*".
        */
        private String regexp = DEFAULT_REGEXP;
        
        /** Bitarchive replica where batchjob is to be run. Set to setting 
         *  use replica is as default */
        private Replica batchReplica = Replica.getReplicaFromId(
            Settings.get(
                CommonSettings.USE_REPLICA_ID
            ));

        /**
         * The outputfile, if any was given.
         */
        private File outputFile;
        
        /** The errorfile, if any was given. */
        private File errorFile;
        
        /** file types in input parameter. */
        private enum FileType {OTHER, JAR, CLASS};
        
        /** 
         * Getting FileType from given file name. 
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
         * Check, if you can write a file named fileName to CWD.
         * @param fileName The file name
         * @param fileTag a tag for the fileName
         * @return true, if you can write such a file;
         * False, if the file already exists, or you cannot create the file
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
         * based on apache.commons.cli.
         */
        private class BatchParameters {
            /**
             * Options object for parameters.
             */
            Options options = new Options();
            private CommandLineParser parser = new PosixParser();
            CommandLine cmd;
            //HelpFormatter only prints directly, thus this is not used at
            //the moment
            //HelpFormatter formatter = new HelpFormatter();
            //Instead the method listArguments is defined
            
            /**
             * Initialize options by setting legal parameters for batch jobs.
             */
            BatchParameters() {
                options.addOption(
                    "C", true, "Class file to be run");
                options.addOption(
                    "J", true, "Jar file to be run (required if class file " 
                               + "is in jar file)");
                options.addOption(
                        "N", true, "Name of the primary class to be run. Only " 
                                   + "needed when using the Jar-file option");
                    
                options.addOption(
                    "R", true, "Regular expression for files to be processed "
                               + "(default: '" + regexp + "')");
                options.addOption(
                    "B", true, "Name of bitarchive replica where batch must " +
                               "be run "
                                + "(default: '" 
                                + Replica.getReplicaFromId(
                                     CommonSettings.USE_REPLICA_ID
                                  ).getName()
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
                    cmd = parser.parse(options, args);
                } catch(ParseException exp) {
                    return "Parsing parameters failed.  Reason is: "
                        + exp.getMessage();
                }
                return "";
            }
            
            String listArguments() {
                String s = "\nwith arguments:\n";
                // add options
                for (Object o: options.getOptions()) {
                    Option op = (Option) o;
                    s += "-" + op.getOpt() + " " + op.getDescription() + "\n";  
                }
                //delete last delimitter
                if (s.length() > 0) {
                    s = s.substring(0, s.length()-1);
                }
                return s;
            }
        }
        
        
        /** To contain parameters defined by options to batchjob. */
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
                        + "jar or "
                        + "class file"
                );
                return false;
            } 
            if (args.length > parms.cmd.getOptions().length) {
                System.err.println("Too many arguments");
                return false;
            }

            //Check class file argument
            String jars = parms.cmd.getOptionValue("J");
            String className = parms.cmd.getOptionValue("N");
            String classFileName = parms.cmd.getOptionValue("C");
          
            if (classFileName == null && jars == null) {
                msg = "Missing required class file argument ";
                msg += "(-C) or Jarfile argument (-J)"; 
                System.err.println(msg);
                return false;
            } 
            // Check, that option -C and -J is not used simultaneously
            if (classFileName != null && jars != null) {
                msg = "Cannot use option -J and -C at the same time";
                System.err.println(msg);
                return false;
            }
            
            if (classFileName != null && jars == null) { // -C is used and not -J
                if (!getFileType(classFileName).equals(FileType.CLASS)) {
                    System.err.println("Argument '"+ classFileName 
                            + "' is not denoting a class file");
                    return false;
                }
                if (!new File(classFileName).canRead()) {
                    System.err.println("Cannot read class file: '"
                            + classFileName + "'");
                    return false;
                }
            }
            
            //Check jar file arguments
            if (jars != null) {
                if (className == null) {
                    msg = "Using option -J also requires"
                        + "option -N (the name of the class).";
                    System.err.println(msg);
                    return false;
                }

                String[] jarList = jars.split(",");
                for(String jar : jarList) {
                	if (!getFileType(jar).equals(FileType.JAR)) {
                		System.err.println("Argument '"+ jar 
                				+ "' is not denoting a jar file");
                		return false;
                	}

                	if(!new File(jar).canRead()) {
                		System.err.println("Cannot read jar file: '" + jar + "'");
                		return false;
                	}
                }
                
                //TODO Validate class name
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
            
            //Check bitarchive replica argument
            String rep = parms.cmd.getOptionValue("B");
            if (rep != null) {
                if (!Replica.isKnownReplicaName(rep)) {
                    System.err.println("Unknown replica name '" + rep
                                       + "', known replicas are "
                                       + Replica.getKnownNames());
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
            String jarArgs = parms.cmd.getOptionValue("J");
            String classFileName = parms.cmd.getOptionValue("C");
            String className = parms.cmd.getOptionValue("N");

            
            FileBatchJob job;

            if (jarArgs == null) {
                LoadableFileBatchJob classJob = new LoadableFileBatchJob(
                        new File(classFileName));
                job = classJob;
            } else {
            	// split jar argument into jar file names 
            	String[] jarNames = jarArgs.split(",");
//            	System.out.println("Number of jar files: " + jarNames.length);
            	
            	// get jar files an put them into an array
            	File[] jarFiles = new File[jarNames.length];
            	for(int i=0; i<jarNames.length; i++) {
            		jarFiles[i] = new File(jarNames[i]);
            	}

            	LoadableJarBatchJob jarJob = new LoadableJarBatchJob(className,
                        jarFiles);
                job = jarJob;
            }
            
            String reg = parms.cmd.getOptionValue("R");
            if (reg != null) { 
                regexp = reg;
                job.processOnlyFilesMatching(regexp);
            }
            
            String repName = parms.cmd.getOptionValue("B");
            if (repName != null) { 
                batchReplica = Replica.getReplicaFromName(repName);
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
                "Running batch job '" 
               + ((classFileName == null)? "" : classFileName + "' ")
               + ((jarArgs == null) ? "" : className + " from jar-file '"
                       + jarArgs + "' ")
                + "on files matching '" + regexp + "' "
                + "on replica '" + batchReplica.getName() + "', " 
                + "output written to " 
                   + ((oFile == null) ? "stdout " : "file '" + oFile + "', ")
                + "errors written to " 
                   + ((eFile == null) ? "stderr " : "file '" + eFile + "' ")
            );
            BatchStatus status = arcrep.batch(job, batchReplica.getId());
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
                    errorOutput.println("File: "
                            + occurrence.getFileName());
                    errorOutput.println("Offset: "
                            + occurrence.getFileOffset());
                    errorOutput.println("OutputOffset: "
                            + occurrence.getOutputOffset());
                    errorOutput.println("Class name: "
                            + occurrence.getClass().getName());
                    errorOutput.println("Was exception during initialize: "
                            + occurrence.isInitializeException());
                    errorOutput.println("Was exception during finish: "
                            + occurrence.isFinishException());
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
