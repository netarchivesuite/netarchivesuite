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

package dk.netarkivet.archive.webinterface;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dk.netarkivet.archive.arcrepository.bitpreservation.ActiveBitPreservation;
import dk.netarkivet.archive.arcrepository.bitpreservation.FilePreservationStatus;
import dk.netarkivet.archive.arcrepository.bitpreservation.WorkFiles;
import dk.netarkivet.common.distribute.arcrepository.BitArchiveStoreState;
import dk.netarkivet.common.distribute.arcrepository.Location;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;

/**
 * Class encapsulating methods for handling web requests for
 * ActiveBitPreservation.
 *
 */

public class BitpreserveFileStatus {
    public static final String ADD_COMMAND = "add";
    public static final String SET_FAILED_COMMAND = "setFailed";
    public static final String GET_INFO_COMMAND = "getInfo";

    /** Internationalisation object. */
    private static final I18n I18N
            = new I18n(dk.netarkivet.archive.Constants.TRANSLATIONS_BUNDLE);

    /**
     * Extract the name of the bitarchive (parameter 'bitarchive') and whether
     * to update missing files (parameter "findmissingfiles") or checksums
     * (parameter "checksum").
     * Does nothing if parameter 'bitarchive' is not set.
     *
     * @param request the given ServletRequest
     * @param context the current JSP context
     */
    public static void processUpdateRequest(ServletRequest request,
            PageContext context) {
        String bitarchiveName
                = request.getParameter(Constants.BITARCHIVE_NAME_PARAM);
        if (bitarchiveName == null) { // parameter BITARCHIVE_NAME_PARAM not set
            return;
        }
        if (!Location.isKnownLocation(bitarchiveName)) {
            HTMLUtils.forwardWithErrorMessage(context, I18N,
                    "errormsg;unknown.bitarchive.0", bitarchiveName);
            throw new ForwardedToErrorPage("Unknown bitarchive: "
                                           + bitarchiveName);
        }
        Location bitarchive = Location.get(bitarchiveName);

        String findmissingfiles =
                request.getParameter(Constants.FIND_MISSING_FILES_PARAM);
        String checksum = request.getParameter(Constants.CHECKSUM_PARAM);

        ActiveBitPreservation preserve = ActiveBitPreservation.getInstance();
        if (findmissingfiles != null) {
            preserve.runFileListJob(bitarchive);
            preserve.findMissingFiles(bitarchive);
        }

        if (checksum != null) {
            preserve.runChecksumJob(bitarchive);
            preserve.findWrongFiles(bitarchive);
        }
    }

    /**
     * Processes a missingFiles request:
     * Parameters of the form add##<bitarchive>##<filename> causes the file
     *   to be added to that bitarchive.
     * Parameters of the form getInfo##<filename> causes checksums to be
     *   computed for the file in all bitarchives and the information to be
     *   shown in the next update (notice that this information disappears
     *   when the page is next reloaded).
     * Parameters of the form setFailed##<bitarchive>##<filename> updates the
     *   arcrepository to consider that file failed in that bitarchive.
     *
     * @param context the current JSP context
     * @param res     the result object
     * @param params  the given parameters
     * @throws ForwardedToErrorPage if the commands have wrong number of
     * arguments
     * @return A map of info gathered for files as requested.
     */
    public static Map<String, FilePreservationStatus>
            processMissingRequest(PageContext context, StringBuilder res,
                                  Map<String, String[]> params) {
        ActiveBitPreservation preserve = ActiveBitPreservation.getInstance();
        Locale l = context.getResponse().getLocale();
        if (params.containsKey(ADD_COMMAND)) {
            String[] adds = params.get(ADD_COMMAND);
            for (String s : adds) {
                String[] parts = s.split(dk.netarkivet.archive.arcrepository
                        .bitpreservation.Constants.STRING_FILENAME_SEPARATOR);
                checkArgs(context, parts, ADD_COMMAND, "bitarchive name",
                          "filename");
                final Location ba = Location.get(parts[0]);
                final String filename = parts[1];
                if (preserve.reestablishMissingFile(filename, ba, res, l)) {
                    removeFileFromMissingFilesList(ba, filename);
                    res.append("<br/>");
                    res.append(HTMLUtils.escapeHtmlValues(I18N.getString(l,
                              "file.0.has.been.restored.in.bitarchive.on.1",
                              filename, ba.getName())));
                    res.append("<br/>");
                }
            }
        }

        if (params.containsKey(SET_FAILED_COMMAND)) {
            String[] setFaileds = params.get(SET_FAILED_COMMAND);
            for (String s : setFaileds) {
                String[] parts = s.split(dk.netarkivet.archive.arcrepository
                        .bitpreservation.Constants.STRING_FILENAME_SEPARATOR);
                checkArgs(context, parts, SET_FAILED_COMMAND, "bitarchive name",
                          "filename");
                final Location ba = Location.get(parts[0]);
                final String filename = parts[1];
                preserve.setAdminData(filename, ba,
                        BitArchiveStoreState.UPLOAD_FAILED);
                res.append(HTMLUtils.escapeHtmlValues(I18N.getString(l,
                          "file.0.is.now.marked.as.failed.in.bitarchive.1",
                          filename, ba.getName())));
                res.append("<br/>");
            }
        }
        Map<String, FilePreservationStatus> infoMap =
            new HashMap<String, FilePreservationStatus>();
        // Do this at the end so that the info reflects reality!
        if (params.containsKey(GET_INFO_COMMAND)) {
            String[] getInfos = params.get(GET_INFO_COMMAND);
            for (String s : getInfos) {
                String[] parts = s.split(dk.netarkivet.archive.arcrepository
                        .bitpreservation.Constants.STRING_FILENAME_SEPARATOR);
                checkArgs(context, parts, GET_INFO_COMMAND, "filename");
                final String filename = parts[0];
                infoMap.put(filename,
                        preserve.getFilePreservationStatus(filename));
            }
        }

        return infoMap;
    }

    /** Check that an array of strings has the arguments corresponding to
     * a command.
     * @param context the JSP context to forward to error to.
     * @param parts Array of arguments given by user
     * @param cmd The command to match
     * @param argnames The names of the expected arguments.
     * @throws ForwardedToErrorPage if the parts are not exactly as many
     * as the arguments.
     */
    private static void checkArgs(PageContext context, String[] parts,
                                  String cmd, String... argnames) {
        if (argnames.length != parts.length) {
            HTMLUtils.forwardWithErrorMessage(context,I18N,
                    "errormsg;argument.mismatch.command.needs.arguments.0.but.got.1",
                    Arrays.asList(argnames), Arrays.asList(parts));

            throw new ForwardedToErrorPage("Command " + cmd
                    + " needs arguments " + Arrays.asList(argnames)
                    + ", but got '" + Arrays.asList(parts) + "'");
        }
    }

    /**
     * Processes a checksum request: Either sets the checksum for a given
     * file ("file" parameter) in the arcrepository (if "fixadminchecksum"
     * parameter is given) or removes and reuploads a file in one bitarchive
     * ("bitarchive" parameter) checking with the checksum and credentials
     * given.
     *
     * @param request the request
     * @param res     the result object
     * @param context the current JSP pagecontext
     */
    public static void processChecksumRequest(ServletRequest request,
                                              StringBuilder res,
                                              PageContext context) {
        Locale l = context.getResponse().getLocale();
        String bitarchiveName
                = request.getParameter(Constants.BITARCHIVE_NAME_PARAM);
        if (bitarchiveName == null) { // param BITARCHIVE_PARAMETER_NAME not set
            res.append(I18N.getString(l,
                    "errmsg;lack.name.for.bitarchive.to.be.corrected"));
            return;
        }
        Location bitarchive = Location.get(bitarchiveName);
        String filename = request.getParameter(Constants.FILENAME_PARAM);
        String fixadminchecksum =
                request.getParameter(Constants.FIX_ADMIN_CHECKSUM_PARAM);
        String credentials =
                request.getParameter(Constants.CREDENTIALS_PARAM);
        String checksum = request.getParameter(Constants.CHECKSUM_PARAM);
        if (filename == null) { // param "file" not set - no action to take
            if (fixadminchecksum != null ||
                    credentials != null ||
                    checksum != null) {
                // Only if an action was intended do we complain about
                // a missing file.
                res.append(I18N.getString(l,
                    "errmsg;lack.name.for.file.to.be.corrected.in.0",
                    bitarchiveName));
            }
            return;
        }

        if (fixadminchecksum != null) {
            ActiveBitPreservation preserve =
                ActiveBitPreservation.getInstance();
            FilePreservationStatus fs =
                preserve.getFilePreservationStatus(filename);
            if (fs == null) {
                res.append(I18N.getString(l,
                        "no.info.on.file.{0}", filename));
                FileUtils.removeLineFromFile(filename,
                        WorkFiles.getFile(bitarchive, WorkFiles.WRONG_FILES));
            } else {
                String referenceChecksum = fs.getReferenceCheckSum();
                if (referenceChecksum != null 
                    && !"".equals(referenceChecksum)) {
                    // update admin.data with correct checksum for file
                    preserve.setAdminChecksum(filename, referenceChecksum);
                    res.append(I18N.getString(l,
                            "file.0.now.has.correct.checksum.in.admin.data",
                            filename));
                    FileUtils.removeLineFromFile(filename,
                            WorkFiles.getFile(bitarchive,
                                    WorkFiles.WRONG_FILES));
                }
            }
        } else {
            // If FIX_ADMIN_CHECKSUM_PARAM is unset, the parameters
            // CHECKSUM_PARAM and CREDENTIALS_PARAM are used for removal
            // of a broken file.
            if (checksum == null) { // param CHECKSUM_PARAM not set
                res.append(I18N.getString(l,
                        "errmsg;lack.checksum.for.corrupted.file.0",
                        filename));
                return;
            }

            if (credentials == null) { // param CREDENTIALS_PARAM not set
                res.append(I18N.getString(l,
                        "errmsg;lacking.privileges.to.correct.in.bitarchive")
                        );
                return;
            }

            ActiveBitPreservation preserve
                    = ActiveBitPreservation.getInstance();
            preserve.removeAndGetFile(filename, bitarchive,
                                      checksum, credentials);
            res.append(I18N.getString(l,
                    "file.0.has.been.deleted.in.1.needs.copy",
                    filename, bitarchive));

            FileUtils.removeLineFromFile(filename,
                    WorkFiles.getFile(bitarchive, WorkFiles.MISSING_FILES_BA));
        }
    }

    /**
     * Return the number of files found in the bitarchive.
     * If no information found about the bitarchive -1 is returned
     *
     * @param bitarchive the bitarchive to check
     * @return the number of files found in the bitarchive
     */
    public static long getBACountFiles(Location bitarchive) {
        ArgumentNotValid.checkNotNull(bitarchive, "bitarchive");
        File unsortedOutput = WorkFiles.getFile(bitarchive, WorkFiles.FILES_ON_BA);

        if (!unsortedOutput.exists()) {
            return -1;
        }

        return FileUtils.countLines(unsortedOutput);
    }

    /**
     * Get the number of wrong files for a bitarchive.
     *
     * @param bitarchive a bitarchive
     * @return the number of wrong files for the bitarchive.
     */
    public static long getCountWrongFiles(Location bitarchive) {
        ArgumentNotValid.checkNotNull(bitarchive, "bitarchive");
        File wrongFileOutput = WorkFiles.getFile(bitarchive, WorkFiles.WRONG_FILES);

        if (!wrongFileOutput.exists()) {
            return -1;
        }

        return FileUtils.countLines(wrongFileOutput);
    }

    /**
     * Get the number of missing files in a given bitarchive.
     * @param bitarchive a given bitarchive
     * @return the number of missing files in the given bitarchive.
     */
    public static long getBACountMissingFiles(Location bitarchive) {
        ArgumentNotValid.checkNotNull(bitarchive, "bitarchive");

        File missingOutput = WorkFiles.getFile(bitarchive, WorkFiles.MISSING_FILES_BA);
        if (!missingOutput.exists()) {
            return -1;
        }

        return FileUtils.countLines(missingOutput);
    }

    /**
     * Get a list of missing files in a given bitarchive.
     * @param bitarchive a given bitarchive
     * @param context the current JSP pagecontext
     * @throws ForwardedToErrorPage if the file with the list
     * cannot be found.
     * @return a list of missing files in a given bitarchive.
     */
    public static List<String> getMissingFilesList(Location bitarchive,
                                                   PageContext context) {
        File missingOutput = WorkFiles.getFile(bitarchive, WorkFiles.MISSING_FILES_BA);

        if (!missingOutput.exists()) {
            HTMLUtils.forwardWithErrorMessage(context, I18N,
                    "errormsg;could.not.find.file.0", missingOutput.getAbsolutePath());
            throw new ForwardedToErrorPage("Could not find the file: "
                    + missingOutput.getAbsolutePath());
        }

        return FileUtils.readListFromFile(missingOutput);
    }

    /**
     * Get a list of wrong files in a given bitarchive.
     * @param bitarchive a bitarchive
     * @param context the current JSP pagecontext
     * @throws ForwardedToErrorPage if the file with the list
     * cannot be found.
     * @return a list of wrong files in a given bitarchive.
     */
    public static List<String> getWrongFilesList(Location bitarchive,
                                                 PageContext context) {
        File wrongFilesOutput = WorkFiles.getFile(bitarchive, WorkFiles.WRONG_FILES);

        if (!wrongFilesOutput.exists()) {
            HTMLUtils.forwardWithErrorMessage(context, I18N,
                    "errormsg;could.not.find.file.0", wrongFilesOutput.getAbsolutePath());
            throw new ForwardedToErrorPage("Could not find the file: "
                    + wrongFilesOutput.getAbsolutePath());
        }

        // Create set of file names from bitarchive data
        return FileUtils.readListFromFile(wrongFilesOutput);
    }

    /**
     * Remove given filename from list of files missing on a given bitarchive.
     *
     * @param bitarchive a bitarchive
     * @param fileName   a filename
     */
    public static void removeFileFromMissingFilesList(Location bitarchive,
                                                      String fileName) {
        ArgumentNotValid.checkNotNull(bitarchive, "bitarchive");
        ArgumentNotValid.checkNotNull(fileName, "fileName");

        File missingOutput = WorkFiles.getFile(bitarchive, WorkFiles.MISSING_FILES_BA);
        FileUtils.removeLineFromFile(fileName, missingOutput);
    }

    /** Create a generic checkbox as used by processMissingRequest.
     *
     * @param command The name of the command
     * @param args Arguments to the command
     * @return A checkbox with the command and arguments in correct format and
     * with HTML stuff escaped.
     */
    public static String makeCheckbox(String command, String... args) {
        ArgumentNotValid.checkNotNull(command, "command");
        ArgumentNotValid.checkNotNull(args, "args");
        StringBuilder res = new StringBuilder();
        for (String arg : args) {
            if (res.length() == 0) {
                res.append(" value=\"");
            } else {
                res.append(dk.netarkivet.archive.arcrepository
                        .bitpreservation.Constants.STRING_FILENAME_SEPARATOR);
            }
            res.append(HTMLUtils.escapeHtmlValues(arg));
        }
        if (res.length() != 0) {
            res.append("\"");
        }
        return("<input type=\"checkbox\" name=\"" + command + "\""
                + res.toString() + ">");
    }
}
