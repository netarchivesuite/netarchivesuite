package dk.netarkivet.harvester.tools;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ViewerArcRepositoryClient;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;

public class GetDataResolver {

	/**
	 * Logger for this class.
	 */
	private static final Logger log = LoggerFactory.getLogger(GetDataResolver.class);

	/**
	 * The client for the arc repository.
	 */
	private static ViewerArcRepositoryClient client;

	/**
	 * Command for getting a single file from the bitarchive.
	 */
	public static final String GET_FILE_COMMAND = "getFile";
	/**
	 * Command for getting a specific record (file+offset) from an (W)ARC file in the bitarchive.
	 */
	public static final String GET_RECORD_COMMAND = "getRecord";

	/** Parameter defining the file to return the getting files or records. */
	public static final String ARCFILE_PARAMETER = "arcFile";
	/** Parameter defining the offset into an ARC file for getting a record. */
	public static final String FILE_OFFSET_PARAMETER = "arcOffset";
	/** Parameter for ids of jobs to get metadata for. */
	public static final String JOB_ID_PARAMETER = "jobID";
	/** Parameter defining the command to execute */
	public static final String COMMAND_PARAMETER = "command";
	/** Parameter defining the name of the file to return */
	public static final String FILENAME_PARAMETER = "filename";

	/** HTTP response code for OK. */
	private static final int OK_RESPONSE_CODE = 200;

	/** HTTP response code for failed. */
	private static final int FAILED_RESPONSE_CODE = 500;

	static {
		client = ArcRepositoryClientFactory.getViewerInstance();
	}

	public static void setClient(ViewerArcRepositoryClient c) {
		client = c;
	}

	/**
	 * Handles parsing of the URL and delegating to relevant methods for known commands. Commands are:
	 * getFile - params: arcfile - effect: get the full file specified by the parameter from the bitarchive.
	 * getRecord - params: arcfile, offset, filename - effect: get a single ARC record from the bitarchive.
	 *
	 * @param request The request to check
	 * @param response The response to give command results to if it is a command
	 * @throws IOFailure in any trouble.
	 */
	public static void executeCommand(HttpServletRequest request, HttpServletResponse response) {
		String command = getParameterOrThrowException(request, COMMAND_PARAMETER);
		log.debug("Executing command {}", command);
		switch (command) {
		case GET_FILE_COMMAND: {
			doGetFile(request, response);
			break;
		}
		case GET_RECORD_COMMAND: {
			doGetRecord(request, response);
			break;
		}
		default:
			throw new IOFailure("Invalid command: " + command);
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
	private static void doGetRecord(HttpServletRequest request, HttpServletResponse response) {
		String arcfile = getParameterOrThrowException(request, ARCFILE_PARAMETER);
		String offsetString = getParameterOrThrowException(request, FILE_OFFSET_PARAMETER);
		String filename = getParameterOrThrowException(request, FILENAME_PARAMETER);
		Long offset = 0L;
		try {
			offset = Long.parseLong(offsetString);
		} catch (NumberFormatException e) {
			String errMsg = "Unable to parse offsetstring '" + offsetString + "' as long";
			log.warn(errMsg, e);
			throw new IOFailure(errMsg, e);
		}
		BitarchiveRecord bRecord = client.get(arcfile, offset);
		if (bRecord == null) {
			throw new IOFailure("Null record returned by " + "ViewerArcRepositoryClient.get(" + arcfile + "," + offset + ")");
		}
		long maxSize = Settings.getLong(HarvesterSettings.MAXIMUM_OBJECT_IN_BROWSER);
		// TODO: what happens if the record already has these headers defined?
		if (bRecord.getLength() > maxSize) {
			response.setHeader("Content-Disposition", "Attachment; filename=record.txt");
			response.setHeader("Content-Type", "application/octet-stream");
		} else {
			// Content-type
			String contentType = "text/plain";
			if (filename.contains("xml")) {
				contentType = "application/xml";
			}
			response.setHeader("Content-type", contentType);
		}
		try (OutputStream outputStream = response.getOutputStream()) {
			bRecord.getData(outputStream);
			response.setStatus(OK_RESPONSE_CODE);
		} catch (IOException e) {
			response.setStatus(FAILED_RESPONSE_CODE);
			String errMsg = "";
			log.warn(errMsg);
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
	private static void doGetFile(HttpServletRequest request, HttpServletResponse response) {
		String arcfile = getParameterOrThrowException(request, ARCFILE_PARAMETER);
		long maxSize = Settings.getLong(HarvesterSettings.MAXIMUM_OBJECT_IN_BROWSER);
		try {
			File tempFile = null;
			try {
				tempFile = File.createTempFile(arcfile, "download", FileUtils.getTempDir());
				client.getFile(arcfile, Replica.getReplicaFromId(Settings.get(CommonSettings.USE_REPLICA_ID)), tempFile);
				long size = tempFile.length();
				response.setHeader("Content-Disposition", "Attachment; filename=" + arcfile);
				if (size > maxSize) {
					log.info("Requested file {} of size {} is larger than maximum object in browser. Forcing browser to save file to disk", arcfile, size);
					response.setHeader("Content-Type", "application/octet-stream");
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
			String errMsg = "Failure to getFile '" + arcfile + "': ";
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
	private static String getParameterOrThrowException(HttpServletRequest request, String name) {
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
	 * Extract filename from the url of a CDXRecord
	 * 
	 * @param urlString the url
	 * @return the name of the file
	 */
	public static String getFilename(String urlString) {
		int lastSlash = urlString.lastIndexOf("/");
		String filename = urlString.substring(lastSlash + 1);
		int paramStart = filename.indexOf("?");
		if (paramStart != -1) {
			filename = filename.substring(0, paramStart);
		}
		return filename;
	}

}
