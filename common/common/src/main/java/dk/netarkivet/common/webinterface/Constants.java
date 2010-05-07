package dk.netarkivet.common.webinterface;

public final class Constants {
    /** The extension for the output files.*/
    public static final String OUTPUT_FILE_EXTENSION = ".out";
    /** The extension for the error files.*/
    public static final String ERROR_FILE_EXTENSION = ".err";
    /** The separator between the name and the timestamp for result files 
     * of batchjobs. */
    public static final String NAME_TIMSTAMP_SEPARATOR = "-";
    
    /** The url for the batchjob page.*/
    public static final String QA_BATCHJOB_URL = "/QA/QA-batchjob.jsp";
    /** The url for retrieval of batchjob result files.*/
    public static final String QA_RETRIEVE_RESULT_FILES = 
        "/QA/QA-batchjob-retrieve-resultfile.jsp";
    /** The url for the execution of the batchjobs.*/
    public static final String QA_BATCHJOB_EXECUTE = 
        "/QA/QA-batchjob-execute.jsp";

    /** The context parameter 'filetype'.*/
    public static final String FILETYPE_PARAMETER = "filetype";
    /** The context parameter 'jobId'.*/
    public static final String JOB_ID_PARAMETER = "jobId";
    /** The context parameter 'batchjob'.*/
    public static final String BATCHJOB_PARAMETER = "batchjob";
    /** The context parameter 'replica'.*/
    public static final String REPLICA_PARAMETER = "replica";
    
    /** The regular expression for all files.*/
    public static final String REGEX_ALL = ".*";
    /** The regular expression for metadata files.*/
    public static final String REGEX_METADATA = "metadata.*";
    /** The regular expression for content files.*/
    public static final String REGEX_CONTENT = "(.*[.]){2}.*";
    
}
