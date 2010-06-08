/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */
package dk.netarkivet.common.webinterface;

/**
 * Class containing constants.
 */
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
    /** The regular expression for content files.
     * This ensures that there is 2 dots in the filename, which is only the 
     * case for the content-files (due to the harvester machine name).
     * Alternatively the following has been suggested: 
     * .*(?<!metadata-[0-9]+).arc 
     */
    public static final String REGEX_CONTENT = "(.*[.]){2}.*";
    
    /** The size of the &lt;input&gt; HTML code.*/
    public static final int HTML_INPUT_SIZE = 50;
}
