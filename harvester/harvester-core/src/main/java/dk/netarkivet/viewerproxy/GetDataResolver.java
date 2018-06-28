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

package dk.netarkivet.viewerproxy;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.Constants;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ViewerArcRepositoryClient;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.harvester.HarvesterSettings;


/**
 * Wrapper for an URIResolver, which retrieves raw data on given specific URLs, and forwards all others to the wrapped
 * handler. This allows you to get metadata, individual files, and individual records.
 */
@SuppressWarnings({"serial", "unused"})
public class GetDataResolver extends CommandResolver {
    /** Logger for this class. */
	 private static final Logger log = LoggerFactory.getLogger(GetDataResolver.class);

    /** The client for the arc repository. */
    ViewerArcRepositoryClient client;

    /** Command for getting a single file from the bitarchive. */
    public static final String GET_FILE_COMMAND = "/getFile";
    /**
     * Command for getting a specific record (file+offset) from an (W)ARC file in the bitarchive.
     */
    public static final String GET_RECORD_COMMAND = "/getRecord";
    /** Command for getting all metadata for a single job. */
    public static final String GET_METADATA_COMMAND = "/getMetadata";

    /** Parameter defining the file to return the getting files or records. */
    public static final String FILE_NAME_PARAMETER = "arcFile";
    /** Parameter defining the offset into an ARC file for getting a record. */
    public static final String FILE_OFFSET_PARAMETER = "arcOffset";
    /** Parameter for ids of jobs to get metadata for. */
    public static final String JOB_ID_PARAMETER = "jobID";

    /** HTTP response code for OK. */
    private static final int OK_RESPONSE_CODE = 200;

    /** HTTP response code for failed. */
    private static final int FAILED_RESPONSE_CODE = 500;

    /**
     * Make a new GetDataResolver, which calls commands on the arcrepository, and forwards all other requests to the
     * given URIResolver.
     *
     * @param ur The URIResolver to handle all other uris.
     * @param client the arcrepository client
     * @throws ArgumentNotValid if either argument is null.
     */
    public GetDataResolver(URIResolver ur, ViewerArcRepositoryClient client) {
        super(ur);
        ArgumentNotValid.checkNotNull(client, "ArcRepositoryClient client");
        this.client = client;
    }

    /**
     * Handles parsing of the URL and delegating to relevant methods for known commands. Commands are: getFile - params:
     * fileName - effect: get the full file specified by the parameter from the bitarchive. getRecord - params:
     * fileName,offset - effect: get a single ARC record from the bitarchive. getMetadata - params: jobID - effect: get
     * all metadata for a single job from the bitarchive.
     *
     * @param request The request to check
     * @param response The response to give command results to if it is a command
     * @return Whether this was a command URL
     * @throws IOFailure in any trouble.
     */
    protected boolean executeCommand(Request request, Response response) {
        // If the url is for this host (potential command)
        if (isCommandHostRequest(request)) {
            log.debug("Executing command " + request.getURI());
            // get path
            String path = request.getURI().getPath();
            if (path.equals(GetDataResolver.GET_FILE_COMMAND)) {
                doGetFile(request, response);
                return true;
            }
            if (path.equals(GetDataResolver.GET_RECORD_COMMAND)) {
                doGetRecord(request, response);
                return true;
            }
            if (path.equals(GetDataResolver.GET_METADATA_COMMAND)) {
                doGetMetadata(request, response);
                return true;
            }
        }
        return false;
    }

    /**
     * Get all metadata for a given job id, and write it to response. Multiple metadata files will be concatenated.
     *
     * @param request A get metadata request; a parameter jobID is expected to be set.
     * @param response Metadata will be written to this response.
     * @throws IOFailure in case of missing or bad parameters.
     */
    private void doGetMetadata(Request request, Response response) {
    	String idString = getParameterOrThrowException(request, JOB_ID_PARAMETER);
    	try {
    		Long id = Long.parseLong(idString);
    		FileBatchJob job = new GetFileBatchJob();
    		job.processOnlyFilesMatching(".*" + id + ".*" + Settings.get(CommonSettings.METADATAFILE_REGEX_SUFFIX));
    		BatchStatus b = client.batch(job, Settings.get(CommonSettings.USE_REPLICA_ID));
    		if (b.getNoOfFilesProcessed() > b.getFilesFailed().size() && b.hasResultFile()) {
    			b.appendResults(response.getOutputStream());
    			response.setStatus(OK_RESPONSE_CODE);
    		} else {
    			if (b.getNoOfFilesProcessed() > 0) {
    				throw new IOFailure("Error finding metadata for job " + id + ": Processed "
    						+ b.getNoOfFilesProcessed() + ", failed on files " + b.getFilesFailed());
    			} else {
    				throw new IOFailure("No metadata found for job " + id + " or error while fetching metadata");
    			}
    		}
    	} catch (NumberFormatException e) {
    		String errMsg = "The value '" + idString + "' of Parameter jobID is not a parsable job id";
    		log.warn(errMsg, e);
    		throw new IOFailure(errMsg, e);
    	}
    }

    /**
     * Get a record from an ARC file, and write it to response. If the record has size greater than
     * settings.viewerproxy.maxSizeInBrowser then a header is added to turn the response into a file-download.
     *
     * @param request A getRecord request; parameters arcFile and arcOffset are expected to be set.
     * @param response Metadata will be written to this response.
     * @throws IOFailure in case of missing or bad parameters.
     */
    private void doGetRecord(Request request, Response response) {
    	String fileName = getParameterOrThrowException(request, FILE_NAME_PARAMETER);
    	String offsetString = getParameterOrThrowException(request, FILE_OFFSET_PARAMETER);
    	try {
    		Long offset = Long.parseLong(offsetString);
    		BitarchiveRecord record = client.get(fileName, offset);
    		if (record == null) {
    			throw new IOFailure("Null record returned by " + "ViewerArcRepositoryClient.get(" + fileName + ","
    					+ offset + "),");
    		}
    		long maxSize = Settings.getLong(HarvesterSettings.MAXIMUM_OBJECT_IN_BROWSER);
    		// TODO: what happens if the record already has these headers defined?
    		if (record.getLength() > maxSize) {
    			response.addHeaderField("Content-Disposition", "Attachment; filename=record.txt");
    			response.addHeaderField("Content-Type", "application/octet-stream");
    		}
    		record.getData(response.getOutputStream());
    		response.setStatus(OK_RESPONSE_CODE);
    	} catch (NumberFormatException e) {
    		String errMsg = "Unable to parse offsetstring '" + offsetString + "' as long";
    		log.warn(errMsg, e);
    		throw new IOFailure(errMsg, e);
    	}
    }

    /**
     * Get a file from bitarchive, and write it to response.
     *
     * @param request A getFile request; parameter arcFile is expected to be set.
     * @param response File will be written to this response.
     * @throws IOFailure in any trouble.
     */
    private void doGetFile(Request request, Response response) {
    	String fileName = getParameterOrThrowException(request, FILE_NAME_PARAMETER);
    	long maxSize = Settings.getLong(HarvesterSettings.MAXIMUM_OBJECT_IN_BROWSER);
    	try {
    		File tempFile = null;
    		try {
    			tempFile = File.createTempFile(fileName, "download", FileUtils.getTempDir());
    			client.getFile(fileName, Replica.getReplicaFromId(Settings.get(CommonSettings.USE_REPLICA_ID)),
    					tempFile);
    			long size = tempFile.length();
                response.addHeaderField("Content-Disposition", "Attachment; filename=" + fileName);
    			if (size > maxSize) {
    				log.info("Requested file {} of size {} is larger than maximum object in browser. Forcing browser to save file to disk");
    				response.addHeaderField("Content-Type", "application/octet-stream");
    			}
    			response.setStatus(OK_RESPONSE_CODE);
                response.getOutputStream().flush();
                FileUtils.writeFileToStream(tempFile, response.getOutputStream());
    		} finally {
    			if (tempFile != null) {
    				FileUtils.remove(tempFile);
    			}
    		}
    	} catch (IOException e) {
    		String errMsg = "Failure to getFile '" + fileName + "': ";
    		log.warn(errMsg, e);
    		throw new IOFailure(errMsg, e);
    	}
    }

    /**
     * Get a single parameter out of a parameter-map, checking for errors, including empty string parameter value.
     *
     * @param request The request with the parameters
     * @param name The name of the parameter
     * @return The single value found trimmed
     * @throws IOFailure if an error was encountered.
     */
    private String getParameterOrThrowException(Request request, String name) {
        String[] values = request.getParameterMap().get(name);
        if (values == null || values.length == 0) {
            throw new IOFailure("Missing parameter '" + name + "'");
        }
        if (values.length > 1) {
            throw new IOFailure("Multiple parameters for '" + name + "': " + Arrays.asList(values));
        }
        // Check that trimmed value is not empty string
        String returnValue = values[0].trim();
        if (returnValue.isEmpty()) {
        	throw new IOFailure("Trimmed value of parameter '" + name + "' is empty string!");
        }
        return returnValue;
    }

    /**
     * The trivial batch job: simply concatenate batched files to output.
     */
    private static class GetFileBatchJob extends FileBatchJob implements Serializable {

        public GetFileBatchJob() {
            batchJobTimeout = 10 * Constants.ONE_MIN_IN_MILLIES;
        }

        /** Does nothing. */
        public void initialize(OutputStream os) {
        }

        /**
         * Simply write file to output.
         *
         * @param file File to write to output.
         * @param os Outputstream to write to.
         * @return true always
         */
        public boolean processFile(File file, OutputStream os) {
            FileUtils.writeFileToStream(file, os);
            return true;
        }

        /** does nothing. */
        public void finish(OutputStream os) {
        }
    }
}
