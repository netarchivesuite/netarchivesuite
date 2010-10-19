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

package dk.netarkivet.archive.webinterface;

/**
 * Constants for the bitarchive webinterface.
 *
 */
public class Constants {
    /** Parameter name for the bitarchive to perform operation on. */
    public static final String BITARCHIVE_NAME_PARAM = "bitarchive";
    /** Option for the UPDATE_TYPE_PARAM parameter for the action of running 
     * a batch job for missing files. */
    public static final String FIND_MISSING_FILES_OPTION = "findmissingfiles";
    /** Option for the UPDATE_TYPE_PARAM parameter for the action of 
     * running a checksum batch job. */
    public static final String CHECKSUM_OPTION = "checksum";
    /** 
     * Parameter used by the BitpreserveFileState.processChecksumRequest
     * called from Bitpreservation-filestatus-checksum.jsp.
     */
    public static final String CHECKSUM_PARAM = "checksum";
    
    /** Parameter name for the file to perform checksum operations on. */
    public static final String FILENAME_PARAM = "file";
    /** Parameter name for request to fix checksum in admin data. */
    public static final String FIX_ADMIN_CHECKSUM_PARAM = "fixadminchecksum";
    /** Parameter name for credentials for removing a file with wrong checksum.
     */
    public static final String CREDENTIALS_PARAM = "credentials";
    /** Parameter name to select the type of update required. */
    public static final String UPDATE_TYPE_PARAM = "type";
    /** BitPreservation main Java server page that contains status information
     * about the bitarchives. */
    public static final String FILESTATUS_PAGE
            = "Bitpreservation-filestatus.jsp";
    /** BitPreservation page that checks if any files are missing in one of
     *  the bitarchives. */
    public static final String FILESTATUS_MISSING_PAGE
            = "Bitpreservation-filestatus-missing.jsp";
    /** BitPreservation page that checks files in archive for wrong checksum. */
    public static final String FILESTATUS_CHECKSUM_PAGE
            = "Bitpreservation-filestatus-checksum.jsp";
    /** BitPreservation page that initiates update of the filestatus 
     * information. */ 
    public static final String FILESTATUS_UPDATE_PAGE
            = "Bitpreservation-filestatus-update.jsp";

    /** Maximum number of files to toggle on one go. */
    public static final int MAX_TOGGLE_AMOUNT = 100;

    /** Parameter for adding missing files. */
    public static final String ADD_COMMAND = "add";
    /** Parameter for getting info for missing files. */
    public static final String GET_INFO_COMMAND = "getInfo";
    /** String to separate filename from checksum. */
    public static final String STRING_FILENAME_SEPARATOR = "##";
    
    /** The extension for the output files.*/
    public static final String OUTPUT_FILE_EXTENSION = ".out";
    /** The extension for the error files.*/
    public static final String ERROR_FILE_EXTENSION = ".err";
    /** The separator between the name and the timestamp for result files 
     * of batchjobs. */
    public static final String NAME_TIMSTAMP_SEPARATOR = "-";

    /** The url for the batchjob page.*/
    public static final String URL_BATCHJOB = 
    	"/BitPreservation/Bitpreservation-batchjob.jsp";
    /** The url for retrieval of batchjob result files.*/
    public static final String URL_RETRIEVE_RESULT_FILES = 
    	"/BitPreservation/Bitpreservation-batchjob-retrieve-resultfile.jsp";
    /** The url for the execution of the batchjobs.*/
    public static final String URL_BATCHJOB_EXECUTE = 
    	"/BitPreservation/Bitpreservation-batchjob-execute.jsp";

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
