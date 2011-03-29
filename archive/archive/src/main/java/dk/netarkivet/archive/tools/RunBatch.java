/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.archive.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
import dk.netarkivet.common.distribute.arcrepository.ViewerArcRepositoryClient;
import dk.netarkivet.common.tools.SimpleCmdlineTool;
import dk.netarkivet.common.tools.ToolRunnerBase;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.batch.LoadableFileBatchJob;
import dk.netarkivet.common.utils.batch.LoadableJarBatchJob;
import dk.netarkivet.common.utils.batch.FileBatchJob.ExceptionOccurrence;


/**
 * A command-line tool to run batch jobs in the bitarchive.
 *
 * Usage:
 *  java dk.netarkivet.archive.tools.RunBatch 
 *       with arguments as defined in local class BatchParameters 
 *
 * where:
 * <br/>-J&lt;jarfile&gt; is a file containing all the classes needed by a 
 * BatchJob
 * <br/>-C&lt;classfile&gt; is a file containing a FileBatchJob implementation
 * <br/>-R&lt;regexp&gt; is a regular expression that will be matched against
 * file names in the archive, by default .*
 * <br/>-B&lt;replica&gt; is the name of the bitarchive replica this should be 
 * run on, by default taken from settings.
 * <br/>-O&lt;outputfile&lt; is a file where the output from the batch job 
 * will be written.  By default, it goes to stdout.
 * <br/>-E&lt;errorFile&gt; is a file where the errors from the batch job 
 * will be written. By default, it goes to stderr.
 * <br/>-N&lt;className&gt; is the name of the primary class to be loaded 
 * when doing a LoadableJarBatchJob
 * <br/>-A&lt;Arguments&gt; The arguments for the batchjob, separated by '##', 
 * e.g. -Aarg1##arg2##...
 * <br/>
 * Examples:
 * <br/>
 * java dk.netarkivet.archive.tools.RunBatch -CFindMime.class \ 
 *                          -R10-*.arc -BReplicaOne -Omimes
 * <br/>
 * java dk.netarkivet.archive.tools.RunBatch -JFindMime.jar -NFindMime \ 
 *                          -R10-*.arc -BReplicaOne -Omimes
 * <br/>
 * Note that you probably want to set the application instance id setting
 * ({@literal CommonSettings#APPLICATION_INSTANCE_ID}) to something other than
 * its default value to avoid clashing with other channel listeners.
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
        
        /** Character to separate jarfiles with option J. */
        private static final String JARFILELIST_SEPARATOR = ",";
        
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
        
        /** The list of arguments for the batchjob. */
        private List<String> argumentList = new ArrayList<String>();
        
        /** File types in input parameter. */
        private enum FileType {OTHER, JAR, CLASS};
        
        /** File suffix denoting FileType.CLASS. */
        private static final String CLASS_FILE_SUFFIX = ".class";
        
        /** File suffix denoting FileType.JAR. */
        private static final String JAR_FILE_SUFFIX = ".jar";
        
        /** The jarfile option key. */
        private static final String JARFILE_OPTION_KEY = "J";
        /** The classfile option key. */
        private static final String CLASSFILE_OPTION_KEY = "C";
        /** The regexp option key. */
        private static final String REGEXP_OPTION_KEY = "R";
        /** The replica option key. */
        private static final String REPLICA_OPTION_KEY = "B";
        /** The outputfile option key. */
        private static final String OUTPUTFILE_OPTION_KEY = "O";
        /** The errorfile option key. */
        private static final String ERRORFILE_OPTION_KEY = "E";
        /** The classname option key. */
        private static final String CLASSNAME_OPTION_KEY = "N";
        /** The arguments option key. */
        private static final String ARGUMENTS_OPTION_KEY = "A";

        /** To contain parameters defined by options to batchjob. */
        private BatchParameters parms = new BatchParameters();
        
        /** String to separate the arguments for the batchjob.
         * TODO make into global constant.
         * */
        private static final String ARGUMENT_SEPARATOR = "##";
 
        /** 
         * Getting FileType from given file name. 
         * @param fileName The file name to get file type from
         * @return FileType found from extension of file name
         */
        private FileType getFileType(String fileName) {
            int i = fileName.lastIndexOf(".");
            if (i > 0) { // Does fileName have a suffix?
                String s = fileName.substring(i).toLowerCase();
                if (s.equals(CLASS_FILE_SUFFIX)) {
                    return FileType.CLASS;
                } else {
                    if (s.equals(JAR_FILE_SUFFIX)) {
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
         * Check, if you can write a file named fileName to current working
         * directory.
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
            protected Options options = new Options();
            /** The parser.*/
            private CommandLineParser parser = new PosixParser();
            /** The command line. */
            protected CommandLine cmd;
            
            /**
             * Initialize options by setting legal parameters for batch jobs.
             * Note that all our options has arguments.
             */
            public BatchParameters() {
                final boolean hasArg = true;
                options.addOption(CLASSFILE_OPTION_KEY, hasArg,
                        "Class file to be run");
                options.addOption(JARFILE_OPTION_KEY, hasArg,
                        "Jar file to be run (required if class file "
                                + "is in jar file)");
                options.addOption(CLASSNAME_OPTION_KEY, hasArg,
                        "Name of the primary class to be run. Only "
                                + "needed when using the Jar-file option");

                options.addOption(REGEXP_OPTION_KEY, hasArg,
                        "Regular expression for files to be processed "
                                + "(default: '" + regexp + "')");
                options.addOption(REPLICA_OPTION_KEY, hasArg,
                        "Name of bitarchive replica where batch must "
                                 + "be run "
                                 + "(default: '"
                                 + Replica.getReplicaFromId(
                                         Settings.get(
                                                 CommonSettings.USE_REPLICA_ID))
                                                .getName() + "')");
                options.addOption(OUTPUTFILE_OPTION_KEY, hasArg,
                        "Output file to contain result (default is "
                                + "stdout)");
                options.addOption(ERRORFILE_OPTION_KEY, hasArg,
                        "Error file to contain errors from run "
                                + "(default is stderr)");
                options.addOption(ARGUMENTS_OPTION_KEY, hasArg,
                        "Arguments for the batchjob. If several arguments, "
                        + "then separate with '##'. Default no arguments.");
            }
            
            /**
             * Method for parsing the arguments.
             * 
             * @param args The arguments.
             * @return The empty string, or an error message.
             */
            public String parseParameters(String[] args) {
                try {
                    // parse the command line arguments
                    cmd = parser.parse(options, args);
                } catch(ParseException exp) {
                    return "Parsing parameters failed.  Reason is: "
                        + exp.getMessage();
                }
                return "";
            }
            
            /**
             * Method for retrieving the arguments of this instance.
             * 
             * @return The list of arguments, ready to be printed to system out.
             */
            public String listArguments() {
                String s = "\nwith arguments:\n";
                // add options
                for (Object o: options.getOptions()) {
                    Option op = (Option) o;
                    s += "-" + op.getOpt() + " " + op.getDescription() + "\n";  
                }
                //delete last delimiter
                if (s.length() > 0) {
                    s = s.substring(0, s.length()-1);
                }
                return s;
            }
        }
        
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
                System.err.println("Missing required argument: jar or class "
                        + "file");
                return false;
            } 
            if(args.length > parms.options.getOptions().size()) {
                System.err.println("Too many arguments");
                return false;
            }

            //Check class file argument
            String jars = parms.cmd.getOptionValue(JARFILE_OPTION_KEY);
            String className = parms.cmd.getOptionValue(CLASSNAME_OPTION_KEY);
            String classFileName = parms.cmd.getOptionValue(
                    CLASSFILE_OPTION_KEY);
          
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
            
            // Validate the situation where -C is used and not -J
            if (classFileName != null && jars == null) {
                if (!getFileType(classFileName).equals(FileType.CLASS)) {
                    System.err.println("Argument '" + classFileName 
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
                
                String[] jarList = jars.split(JARFILELIST_SEPARATOR);
                File[] jarFiles = new File[jarList.length];
                for (int i = 0; i < jarList.length; i++) {
                    String jar = jarList[i];
                    
                    // check extension
                    if (!getFileType(jar).equals(FileType.JAR)) {
                        System.err.println("Argument '" + jar
                                + "' is not denoting a jar file");
                        return false;
                    }
                    
                    File jarFile = new File(jar);
                    jarFiles[i] = jarFile;
                    
                    // Check if file is readable.
                    if (!jarFile.canRead()) {
                        System.err.println("Cannot read jar file: '" + jar
                                + "'");
                        return false;
                    }
                }

                // Try to load the jar batch job.
                try {
                    new LoadableJarBatchJob(className, argumentList, jarFiles);
                } catch(Throwable e) {
                    System.err.println("Cannot create batchjob '" + className 
                            + "' from the jarfiles '" + jars + "'");
                    e.printStackTrace();
                    return false;
                }
            }

            //Check regular expression argument
            String reg = parms.cmd.getOptionValue(REGEXP_OPTION_KEY);
            if (reg != null) {
                try {
                    Pattern.compile(reg);
                } catch (PatternSyntaxException e) {
                    System.err.println("Illegal pattern syntax: '"
                                       + reg + "'");
                    e.printStackTrace();
                    return false;
                }
            }
            
            //Check replica argument
            String repName = parms.cmd.getOptionValue(REPLICA_OPTION_KEY);
            if (repName != null) {
                // Is the replica known
                if (!Replica.isKnownReplicaName(repName)) {
                    System.err.println("Unknown replica name '" + repName
                            + "', known replicas are "
                            + Replica.getKnownNamesAsSet());
                    return false;
                }
                // Is it a bitarchive replica.
                if(!Replica.getReplicaFromName(repName).getType()
                        .equals(ReplicaType.BITARCHIVE)) {
                    System.err.println("Can only send a batchjob to a "
                            + "bitarchive replica, and '" 
                            + Replica.getReplicaFromName(repName) 
                            + "' is of the type '" 
                            + Replica.getReplicaFromName(repName).getType() 
                            + "'"); 
                    return false;
                }
            }
            
            //Check output file argument
            String oFile = parms.cmd.getOptionValue(OUTPUTFILE_OPTION_KEY);
            if (oFile != null && !checkWriteFile(oFile, "Output file")) {
                return false;
            }

            //Check output file argument
            String eFile = parms.cmd.getOptionValue(ERRORFILE_OPTION_KEY);
            if (eFile != null && !checkWriteFile(eFile, "Error file")) {
                return false;
            }
            
            // check arguments for the batchjob.
            String arguments = parms.cmd.getOptionValue(ARGUMENTS_OPTION_KEY);
            if(arguments != null) {
                // go through all the arguments and put them into the list.
                for(String arg : arguments.split(ARGUMENT_SEPARATOR)) {
                    argumentList.add(arg);
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
            String jarArgs = parms.cmd.getOptionValue(JARFILE_OPTION_KEY);
            String classFileName = parms.cmd.getOptionValue(
                    CLASSFILE_OPTION_KEY);
            String className = parms.cmd.getOptionValue(CLASSNAME_OPTION_KEY);

            FileBatchJob job;

            if (jarArgs == null) {
                LoadableFileBatchJob classJob = new LoadableFileBatchJob(
                        new File(classFileName), argumentList);
                job = classJob;
            } else {
                // split jar argument into jar file names
                String[] jarNames = jarArgs.split(",");

                // get jar files and put them into an array
                File[] jarFiles = new File[jarNames.length];
                for (int i = 0; i < jarNames.length; i++) {
                    jarFiles[i] = new File(jarNames[i]);
                }

                job = new LoadableJarBatchJob(className, 
                        argumentList, jarFiles);
            }
            
            String reg = parms.cmd.getOptionValue(REGEXP_OPTION_KEY);
            if (reg != null) { 
                regexp = reg;
                job.processOnlyFilesMatching(regexp);
            }
            
            String repName = parms.cmd.getOptionValue(REPLICA_OPTION_KEY);
            if (repName != null) { 
                batchReplica = Replica.getReplicaFromName(repName);
            }
            
            //Note: if no filename is given, output will be written to stdout
            String oFile = parms.cmd.getOptionValue(OUTPUTFILE_OPTION_KEY);
            if (oFile != null) { 
                outputFile = new File(oFile);
            }
            
            //Note: if no filename is given, errors will be written to stderr
            String eFile = parms.cmd.getOptionValue(ERRORFILE_OPTION_KEY);
            if (eFile != null) {
                errorFile = new File(eFile);
            }

            System.out.println(
                "Running batch job '" 
               + ((classFileName == null)? "" : classFileName + "' ")
               + ((jarArgs == null) ? "" : className + "' from jar-file '"
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
