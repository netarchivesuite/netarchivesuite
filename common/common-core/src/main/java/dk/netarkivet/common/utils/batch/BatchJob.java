package dk.netarkivet.common.utils.batch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringUtils;

public interface BatchJob {

    /** Regexp that matches everything. */
    static final String EVERYTHING_REGEXP = ".*";

    /**
     * Mark the job to process only the specified files. This will override any previous setting of which files to
     * process.
     *
     * @param specifiedFilenames A list of filenamess to process (without paths). If null, all files will be processed.
     */
    public default void processOnlyFilesNamed(List<String> specifiedFilenames) {
        if (specifiedFilenames != null) {
            List<String> quoted = new ArrayList<String>();
            for (String name : specifiedFilenames) {
                quoted.add(Pattern.quote(name));
            }
            processOnlyFilesMatching(quoted);
        } else {
            processOnlyFilesMatching(EVERYTHING_REGEXP);
        }
    }

    /**
     * Helper method for only processing one file. This will override any previous setting of which files to process.
     *
     * @param specifiedFilename The name of the single file that should be processed. Should not include any path
     * information.
     */
    public default void processOnlyFileNamed(String specifiedFilename) {
        ArgumentNotValid.checkNotNullOrEmpty(specifiedFilename, "specificedFilename");
        processOnlyFilesMatching(Pattern.quote(specifiedFilename));
    }

    /**
     * Set this job to match only a certain set of patterns. This will override any previous setting of which files to
     * process.
     *
     * @param specifiedPatterns The patterns of file names that this job will operate on. These should not include any
     * path information, but should match the entire filename (e.g. .*foo.* for any file with foo in the name).
     */
    public default void processOnlyFilesMatching(List<String> specifiedPatterns) {
        ArgumentNotValid.checkNotNull(specifiedPatterns, "specifiedPatterns");
        processOnlyFilesMatching("(" + StringUtils.conjoin("|", specifiedPatterns) + ")");
    }

    /**
     * Set this job to match only a certain pattern. This will override any previous setting of which files to process.
     *
     * @param specifiedPattern Regular expression of file names that this job will operate on. This should not include
     * any path information, but should match the entire filename (e.g. .*foo.* for any file with foo in the name).
     */
    public default void processOnlyFilesMatching(String specifiedPattern) {
        ArgumentNotValid.checkNotNullOrEmpty(specifiedPattern, "specificedPattern");
        setFilesToProcess(Pattern.compile(specifiedPattern));
    }

    void setFilesToProcess(Pattern compile);

    /**
     * Get the pattern for files that should be processed.
     *
     * @return A pattern for files to process.
     */
    public Pattern getFilenamePattern();


    /**
     * Return the number of files processed in this job.
     *
     * @return the number of files processed in this job
     */
    public int getNoOfFilesProcessed();

    void setNoOfFilesProcessed(int i);


    /**
     * Return the list of names of files where processing failed. An empty list is returned, if none failed.
     *
     * @return the possibly empty list of names of files where processing failed
     */
    public Collection<File> getFilesFailed();

    void setFilesFailed(Collection<File> files);



    /**
     * Initialize the job before runnning. This is called before the processFile() calls. If this throws an exception,
     * processFile() will not be called, but finish() will,
     *
     * @param os the OutputStream to which output should be written
     */
    public default void initialize(OutputStream os) {};

    /**
     * Process one file stored in the bit archive.
     *
     * @param file the file to be processed.
     * @param os the OutputStream to which output should be written
     * @return true if the file was successfully processed, false otherwise
     */
    public default boolean processFile(File file, OutputStream os) {
        return false;
    };

    /**
     * Finish up the job. This is called after the last process() call. If the initialize() call throws an exception,
     * this will still be called so that any resources allocated can be cleaned up. Implementations should make sure
     * that this method can handle a partial initialization
     *
     * @param os the OutputStream to which output should be written
     */
    public default void finish(OutputStream os) {};

    /**
     * Get the list of exceptions that have occurred during processing.
     *
     * @return List of exceptions together with information on where they happened.
     */
    public List<ExceptionOccurrence> getExceptions();

    /**
     * Processes the concatenated result files. This is intended to be overridden by batchjobs, who they wants a
     * different post-processing process than concatenation.
     *
     * @param input The inputstream to the file containing the concatenated results.
     * @param output The outputstream where the resulting data should be written.
     * @return Whether it actually does any post processing. If false is returned then the default concatenated result
     * file is returned.
     * @throws ArgumentNotValid If the concatenated file is null.
     */
    public default boolean postProcess(InputStream input, OutputStream output) {
        // Do not post process. Override in inherited classes to post process.
        return false;
    }


    /**
     * Getter for batchJobTimeout. If the batchjob has not defined a maximum time (thus set the value to -1) then the
     * default value from settings are used.
     *
     * @return timeout in miliseconds.
     */
    public long getBatchJobTimeout();

    /**
     * Override predefined timeout period for batchjob.
     *
     * @param batchJobTimeout timout period
     */
    public void setBatchJobTimeout(long batchJobTimeout);

    /**
     * This class holds the information about exceptions that occurred in a batchjob.
     */
    public static class ExceptionOccurrence implements Serializable {

        /**
         * The maximum number of exceptions we will accumulate before aborting processing.
         */
        protected static final int MAX_EXCEPTIONS = Settings.getInt(CommonSettings.MAX_NUM_BATCH_EXCEPTIONS);

        /**
         * Marker for the case when we couldn't find an offset for the outputstream.
         */
        public static final long UNKNOWN_OFFSET = -1;

        /**
         * The name of the file we were processing when the exception occurred, or null.
         */
        private final String fileName;

        /**
         * The offset in the file we were processing when the exception occurred.
         */
        private final long fileOffset;
        /**
         * How much we had written to the output stream when the exception occurred.
         */
        private final long outputOffset;
        /** The exception that was thrown. */
        private final Exception exception;
        /** True if this exception was thrown during initialize(). */
        private final boolean inInitialize;
        /** True if this exception was thrown during finish(). */
        private final boolean inFinish;

        /**
         * Standard Constructor for ExceptionOccurrence.
         *
         * @param file The file that caused the exception.
         * @param fileOffset The relevant offset into the file when the exception happened (e.g. the start of an ARC
         * record).
         * @param outputOffset The offset we were at in the outputstream when the exception happened.
         * @param exception The exception thrown. This exception must be serializable.
         * @see FileBatchJob#addException(File, long, long, Exception) for details on the parameters.
         */
        public ExceptionOccurrence(File file, long fileOffset, long outputOffset, Exception exception) {
            ArgumentNotValid.checkNotNull(file, "File file");
            ArgumentNotValid.checkNotNegative(fileOffset, "long fileOffset");
            ArgumentNotValid.checkTrue(outputOffset >= 0 || outputOffset == UNKNOWN_OFFSET,
                    "outputOffset must be either non-negative or UNKNOWN_OFFSET");
            ArgumentNotValid.checkNotNull(exception, "Exception exception");
            this.fileName = file.getName();
            this.fileOffset = fileOffset;
            this.inFinish = false;
            this.inInitialize = false;
            this.outputOffset = outputOffset;
            this.exception = exception;
        }

        /**
         * Constructor for ExceptionOccurrence when an exception happened during initialize() or finish().
         *
         * @param inInitialize True if the exception happened in initialize()
         * @param outputOffset Current offset in the output stream, or UNKNOWN_OFFSET if the offset cannot be found.
         * @param exception The exception that was thrown.
         */
        public ExceptionOccurrence(boolean inInitialize, long outputOffset, Exception exception) {
            ArgumentNotValid.checkTrue(outputOffset >= 0 || outputOffset == UNKNOWN_OFFSET,
                    "outputOffset must be either non-negative or UNKNOWN_OFFSET");
            ArgumentNotValid.checkNotNull(exception, "Exception exception");
            this.fileName = null;
            this.fileOffset = UNKNOWN_OFFSET;
            this.inFinish = !inInitialize;
            this.inInitialize = inInitialize;
            this.outputOffset = outputOffset;
            this.exception = exception;
        }

        /**
         * Get the name of the file that this exception occurred in.
         *
         * @return Name of the file that this exception occurred in, or null if it happened during initialize() or
         * finish().
         */
        public String getFileName() {
            return fileName;
        }

        /**
         * Get the offset into the file that this exception occurred at. This location may not be exactly where the
         * problem that caused the exception occurred, but may be e.g. at the start of a corrupt record.
         *
         * @return Offset into the file that this exception occurred at, or UNKNOWN_OFFSET if it happened during
         * initialize() or finish().
         */
        public long getFileOffset() {
            return fileOffset;
        }

        /**
         * Offset of the output stream when this exception occurred.
         *
         * @return Offset in output stream, or UNKNOWN_OFFSET if the offset could not be determined.
         */
        public long getOutputOffset() {
            return outputOffset;
        }

        /**
         * The exception that was thrown.
         *
         * @return An exception.
         */
        public Exception getException() {
            return exception;
        }

        /**
         * Returns true if the exception was thrown during initialize(). In that case, no processing has taken place,
         * but finish() has been called.
         *
         * @return true if the exception was thrown during initialize()
         */
        public boolean isInitializeException() {
            return inInitialize;
        }

        /**
         * Returns true if the exception was thrown during finish().
         *
         * @return true if the exception was thrown during finish().
         */
        public boolean isFinishException() {
            return inFinish;
        }

        /**
         * @return a Human readable representation of this ExceptionOccurence object.
         */
        public String toString() {
            return "ExceptionOccurrence: (filename, fileoffset, outputoffset, " + "exception, inInitialize, inFinish)"
                    + " = (" + fileName + ", " + fileOffset + ", " + outputOffset + ", " + exception + ", "
                    + inInitialize + ", " + inFinish + "). ";
        }

    }

}
