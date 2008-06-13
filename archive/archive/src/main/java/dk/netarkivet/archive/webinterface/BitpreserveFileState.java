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
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.archive.arcrepository.bitpreservation.ActiveBitPreservation;
import dk.netarkivet.archive.arcrepository.bitpreservation.FileBasedActiveBitPreservation;
import dk.netarkivet.archive.arcrepository.bitpreservation.FilePreservationState;
import dk.netarkivet.common.distribute.arcrepository.Location;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;

/**
 * Class encapsulating methods for handling web requests for
 * ActiveBitPreservation.
 */
public class BitpreserveFileState {
    /** Internationalisation object. */
    private static final I18n I18N
            = new I18n(dk.netarkivet.archive.Constants.TRANSLATIONS_BUNDLE);
    /** The logger for this class. */
    private static Log log = LogFactory.getLog(BitpreserveFileState.class);

    /**
     * Extract the name of the bitarchive
     * (parameter Constants.BITARCHIVE_NAME_PARAM) and whether to update missing
     * files (parameter Constants.FIND_MISSING_FILES_PARAM) or checksums
     * (parameter Constants.CHECKSUM_PARAM).
     *
     * Does nothing if parameter 'bitarchive' is not set.
     *
     * @param context the current JSP context
     *
     * @throws ForwardedToErrorPage if an unknown bitarchive is posted.
     */
    public static void processUpdateRequest(PageContext context) {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ServletRequest request = context.getRequest();
        String bitarchiveName
                = request.getParameter(Constants.BITARCHIVE_NAME_PARAM);
        if (bitarchiveName == null) { // parameter BITARCHIVE_NAME_PARAM not set
            return;
        }
        if (!Location.isKnownLocation(bitarchiveName)) {
            HTMLUtils.forwardWithErrorMessage(context, I18N,
                                              "errormsg;unknown.bitarchive.0",
                                              bitarchiveName);
            throw new ForwardedToErrorPage("Unknown bitarchive: "
                                           + bitarchiveName);
        }
        Location bitarchive = Location.get(bitarchiveName);

        String findmissingfiles =
                request.getParameter(Constants.FIND_MISSING_FILES_PARAM);
        String checksum = request.getParameter(Constants.CHECKSUM_PARAM);

        ActiveBitPreservation preserve
                = FileBasedActiveBitPreservation.getInstance();
        if (findmissingfiles != null) {
            preserve.findMissingFiles(bitarchive);
        }

        if (checksum != null) {
            preserve.findChangedFiles(bitarchive);
        }
    }

    /**
     * Processes a missingFiles request.
     *
     * Parameters of the form Constants.ADD_COMMAND=<bitarchive>##<filename>
     * causes the file to be added to that bitarchive, if it is missing.
     *
     * Parameters of the form Constants.GET_INFO_COMMAND=<filename> causes
     * checksums to be computed for the file in all bitarchives and the
     * information to be shown in the next update (notice that this information
     * disappears when the page is next reloaded).
     *
     * @param context the current JSP context.
     * @param res     the result object. This is updated with result
     *                information, and expected to be printed to the resulting
     *                page.
     *
     * @return A map of info gathered for files as requested.
     *
     * @throws ForwardedToErrorPage if the commands have the wrong number of
     *                              arguments.
     */
    public static Map<String, FilePreservationState>
    processMissingRequest(PageContext context, StringBuilder res) {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(res, "StringBuilder res");
        Map<String, String[]> params = context.getRequest().getParameterMap();
        HTMLUtils.forwardOnMissingParameter(context,
                                            Constants.BITARCHIVE_NAME_PARAM);
        String bitarchiveName = params.get(Constants.BITARCHIVE_NAME_PARAM)[0];
        if (!Location.isKnownLocation(bitarchiveName)) {
            HTMLUtils.forwardOnIllegalParameter(
                    context, Constants.BITARCHIVE_NAME_PARAM,
                    Location.getKnownNames()
            );
        }
        ActiveBitPreservation preserve
                = FileBasedActiveBitPreservation.getInstance();
        Locale l = context.getResponse().getLocale();
        if (params.containsKey(Constants.ADD_COMMAND)) {
            String[] adds = params.get(Constants.ADD_COMMAND);
            for (String s : adds) {
                String[] parts = s.split(Constants.STRING_FILENAME_SEPARATOR);
                checkArgs(context, parts, Constants.ADD_COMMAND, "bitarchive name",
                          "filename");
                final Location ba = Location.get(parts[0]);
                final String filename = parts[1];
                try {
                    preserve.uploadMissingFiles(ba, filename);
                    res.append(HTMLUtils.escapeHtmlValues(I18N.getString(
                            l,
                            "file.0.has.been.restored.in.bitarchive.on.1",
                            filename, ba.getName())));
                    res.append("<br/>");
                } catch (Exception e) {
                    res.append(I18N.getString(
                            l,
                            "errormsg;attempt.at.restoring.0.in.bitarchive.at.1.failed",
                            filename, ba));
                    res.append("<br/>");
                    res.append(e.getMessage());
                    res.append("<br/>");
                    log.warn("Could not restore file '" + filename
                             + "' in bitarchive '" + ba + "'", e);
                }
            }
        }
        // A map ([filename] -> [preservationstate]) to contain
        // preservationstates for all files retrieved from the
        // parameter Constants.GET_INFO_COMMAND.
        // This map is an empty map, if this parameter is undefined. 
        Map<String, FilePreservationState> infoMap;
        // Do this at the end so that the info reflects the current state.
        if (params.containsKey(Constants.GET_INFO_COMMAND)) {
            String[] getInfos = params.get(Constants.GET_INFO_COMMAND);
            infoMap = preserve.getFilePreservationStateMap(getInfos);
        } else {
            infoMap = new HashMap<String, FilePreservationState>();
        }

        return infoMap;
    }

    /**
     * Check that an array of strings has the arguments corresponding to a
     * command.
     *
     * @param context  the JSP context to forward to error to.
     * @param parts    Array of arguments given by user.
     * @param cmd      The command to match.
     * @param argnames The names of the expected arguments.
     *
     * @throws ForwardedToErrorPage if the parts are not exactly as many as the
     *                              arguments.
     */
    private static void checkArgs(PageContext context, String[] parts,
                                  String cmd, String... argnames) {
        if (argnames.length != parts.length) {
            HTMLUtils.forwardWithErrorMessage(
                    context, I18N,
                    "errormsg;argument.mismatch.command.needs.arguments.0.but.got.1",
                    Arrays.asList(argnames),
                    Arrays.asList(parts));

            throw new ForwardedToErrorPage("Command " + cmd
                                           + " needs arguments "
                                           + Arrays.asList(argnames)
                                           + ", but got '"
                                           + Arrays.asList(parts) + "'");
        }
    }

    /**
     * Processes a checksum request.
     *
     * The name of a bitarchive must always be given in parameter
     * Constants.BITARCHIVE_NAME_PARAM.
     *
     * If parameter Constants.FILENAME_PARAM is given, file info for that file
     * will be returned, and all actions will work on that file.
     *
     * If parameter Constants.FIX_ADMIN_CHECKSUM_PARAM is given, the admin data
     * checksum will be fixed for the file.
     *
     * If parameter Constants.CREDENTIALS and Constants.CHECKSUM_PARAM is given,
     * removes and reuploads a file with that checksum in the given bitarchive,
     * using the credentials for authorisation.
     *
     * @param res     the result object. This is updated with result
     *                information, and expected to be printed to the resulting
     *                page.
     * @param context the current JSP pagecontext.
     *
     * @return The file preservation state for a file, if a filename is given
     * in the request. Null otherwise.
     */
    public static FilePreservationState processChecksumRequest(
            StringBuilder res, PageContext context) {
        ArgumentNotValid.checkNotNull(res, "StringBuilder res");
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ServletRequest request = context.getRequest();
        Locale l = context.getResponse().getLocale();
        HTMLUtils.forwardOnMissingParameter(context,
                                            Constants.BITARCHIVE_NAME_PARAM);
        HTMLUtils.forwardOnIllegalParameter(context,
                                            Constants.BITARCHIVE_NAME_PARAM,
                                            Location.getKnownNames());
        String bitarchiveName
                = request.getParameter(Constants.BITARCHIVE_NAME_PARAM);
        Location bitarchive = Location.get(bitarchiveName);
        String filename = request.getParameter(Constants.FILENAME_PARAM);
        String fixadminchecksum =
                request.getParameter(Constants.FIX_ADMIN_CHECKSUM_PARAM);
        String credentials =
                request.getParameter(Constants.CREDENTIALS_PARAM);
        String checksum = request.getParameter(Constants.CHECKSUM_PARAM);

        // Parameter validation. Get filename. Complain about missing filename
        // if we are trying to do actions.
        if (filename == null) { // param "file" not set - no action to take
            if (fixadminchecksum != null ||
                credentials != null ||
                checksum != null) {
                // Only if an action was intended do we complain about
                // a missing file.
                res.append(I18N.getString(
                        l, "errormsg;lack.name.for.file.to.be.corrected.in.0",
                        bitarchiveName));
            }
            return null;
        }

        //At this point we know that the parameter filename is given.
        //Now we check for actions.
        ActiveBitPreservation preserve
                = FileBasedActiveBitPreservation.getInstance();
        if (fixadminchecksum != null) {
            // Action to fix admin.data checksum.
            preserve.changeStateForAdminData(filename);
            res.append(I18N.getString(
                    l,
                    "file.0.now.has.correct.checksum.in.admin.data",
                    filename));
            res.append("<br/>");
        } else if (checksum != null || credentials != null) {
            // Action to replace a broken file with a correct file.
            // Both parameters must be given.
            if (checksum == null) { // param CHECKSUM_PARAM not set
                res.append(I18N.getString(
                        l,
                        "errormsg;lack.checksum.for.corrupted.file.0",
                        filename));
                res.append("<br/>");
            } else if (credentials == null) { // param CREDENTIALS_PARAM not set
                res.append(I18N.getString(
                        l,
                        "errormsg;lacking.privileges.to.correct.in.bitarchive")
                );
                res.append("<br/>");
            } else {
                // Parameters are correct. Fix the file and report result.
                try {
                    preserve.replaceChangedFile(bitarchive, filename,
                                                credentials, checksum);
                    res.append(I18N.getString(
                            l,
                            "file.0.has.been.replaced.in.1",
                            filename, bitarchive));
                    res.append("<br/>");
                } catch (Exception e) {
                    res.append(I18N.getString(
                            l,
                            "errormsg;attempt.at.restoring.0.in.bitarchive.at.1.failed",
                            filename, bitarchive));
                    res.append("<br/>");
                    res.append(e.getMessage());
                    res.append("<br/>");
                    log.warn("Attempt at restoring '" + filename
                             + "' in bitarchive on location '" + bitarchive
                             + "' failed", e);
                }
            }
        }
        
        return preserve.getFilePreservationState(filename);
    }

    /**
     * Create a generic checkbox as used by processMissingRequest.
     *
     * @param command The name of the command
     * @param args    Arguments to the command
     *
     * @return A checkbox with the command and arguments in correct format and
     *         with HTML stuff escaped.
     */
    public static String makeCheckbox(String command, String... args) {
        ArgumentNotValid.checkNotNull(command, "command");
        ArgumentNotValid.checkNotNull(args, "args");
        StringBuilder res = new StringBuilder();
        for (String arg : args) {
            if (res.length() == 0) {
                res.append(" value=\"");
            } else {
                res.append(Constants.STRING_FILENAME_SEPARATOR);
            }
            res.append(HTMLUtils.escapeHtmlValues(arg));
        }
        if (res.length() != 0) {
            res.append("\"");
        }
        return ("<input type=\"checkbox\" name=\"" + command + "\""
                + res.toString() + " />");
    }

    /**
     * Print HTML formatted state for missing files on a given location in a
     * given locale.
     *
     * @param out      The writer to write state to.
     * @param location The location to write state for.
     * @param locale   The locale to write state in.
     *
     * @throws IOException On IO trouble writing state to the writer.
     */
    public static void printMissingFileStateForLocation(JspWriter out,
                                                         Location location,
                                                         Locale locale)
            throws IOException {
        ArgumentNotValid.checkNotNull(out, "JspWriter out");
        ArgumentNotValid.checkNotNull(location, "Location location");
        ArgumentNotValid.checkNotNull(locale, "Locale locale");
        ActiveBitPreservation activeBitPreservation
                = FileBasedActiveBitPreservation.getInstance();

        //Header
        out.println(I18N.getString(
                locale, "filestatus.for") + "&nbsp;<b>" + HTMLUtils
                .escapeHtmlValues(location.getName()) + "</b>");
        out.println("<br/>");

        //Number of files, and number of files missing
        out.println(I18N.getString(
                locale,
                "number.of.files")
                    + "&nbsp;"
                    + activeBitPreservation.getNumberOfFiles(
                location));
        out.println("<br/>");
        out.println(I18N.getString(
                locale,
                "missing.files")
                    + "&nbsp;"
                    + activeBitPreservation.getNumberOfMissingFiles(
                location));

        if (activeBitPreservation.getNumberOfMissingFiles(location) > 0) {
            out.print("&nbsp;<a href=\"" + Constants.FILESTATUS_MISSING_PAGE
                      + "?" + (Constants.BITARCHIVE_NAME_PARAM
                               + "=" + HTMLUtils
                    .encodeAndEscapeHTML(location.getName())) + " \">");
            out.print(I18N.getString(
                    locale, "show.missing.files"));
            out.print("</a>");
        }
        out.println("<br/>");

        out.println(I18N.getString(
                locale, "last.update.at.0",
                activeBitPreservation.getDateForMissingFiles(location
                )));
        out.println("<br/>");

        out.println("<a href=\"" + Constants.FILESTATUS_PAGE + "?"
                    + Constants.FIND_MISSING_FILES_PARAM + "=1&amp;"
                    + (Constants.BITARCHIVE_NAME_PARAM
                       + "=" + HTMLUtils
                .encodeAndEscapeHTML(location.getName())) + "\">" + I18N
                .getString(locale, "update") + "</a>");
        out.println("<br/><br/>");
    }

    /**
     * Print HTML formatted state for checksum errors on a given location in a
     * given locale.
     *
     * @param out      The writer to write state to.
     * @param location The location to write state for.
     * @param locale   The locale to write state in.
     *
     * @throws IOException On IO trouble writing state to the writer.
     */
    public static void printChecksumErrorStateForLocation(JspWriter out,
                                                           Location location,
                                                           Locale locale)
            throws IOException {
        ArgumentNotValid.checkNotNull(out, "JspWriter out");
        ArgumentNotValid.checkNotNull(location, "Location location");
        ArgumentNotValid.checkNotNull(locale, "Locale locale");
        ActiveBitPreservation bitPreservation
                = FileBasedActiveBitPreservation.getInstance();

        //Header
        out.println(I18N.getString(locale, "checksum.status.for")
                    + "&nbsp;<b>"
                    + HTMLUtils.escapeHtmlValues(location.getName()) + "</b>");
        out.println("<br/>");

        //Number of changed files
        out.println(I18N.getString(locale, "number.of.files.with.error")
                    + "&nbsp;"
                    + bitPreservation.getNumberOfChangedFiles(location));

        //Link to fix-page
        if (bitPreservation.getNumberOfChangedFiles(location) > 0) {
            out.print("&nbsp;<a href=\"" + Constants.FILESTATUS_CHECKSUM_PAGE
                      + "?" + (Constants.BITARCHIVE_NAME_PARAM
                               + "=" + HTMLUtils
                    .encodeAndEscapeHTML(location.getName())) + " \">");
            out.print(I18N.getString(
                    locale, "show.files.with.error"));
            out.print("</a>");
        }
        out.println("<br/>");

        //Time for last update
        out.println(I18N.getString(locale, "last.update.at.0",
                                   bitPreservation.getDateForChangedFiles(
                                           location)));
        out.println("<br/>");

        //Link for running a new job
        out.println("<a href=\"" + Constants.FILESTATUS_PAGE + "?"
                    + Constants.CHECKSUM_PARAM + "=1&amp;"
                    + (Constants.BITARCHIVE_NAME_PARAM
                       + "=" + HTMLUtils
                .encodeAndEscapeHTML(location.getName())) + "\">" + I18N
                .getString(locale, "update") + "</a>");

        //Separator
        out.println("<br/><br/>");
    }

    /**
     * Print a table row with a file name and a checkbox to request more info.
     *
     * @param out      The stream to print to.
     * @param filename The name of the file.
     * @param rowCount The rowcount, used for styling rows.
     * @param locale   The current locale for labels.
     *
     * @throws IOException On trouble writing to stream.
     */
    public static void printFileName(JspWriter out, String filename,
                                     int rowCount,
                                     Locale locale) throws IOException {
        ArgumentNotValid.checkNotNull(out, "JspWriter out");
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        ArgumentNotValid.checkNotNull(locale, "Locale locale");
        out.println("<tr class=\"" + HTMLUtils.getRowClass(rowCount) + "\">");
        out.println(HTMLUtils.makeTableElement(filename));
        out.print("<td>");
        out.print(makeCheckbox(Constants.GET_INFO_COMMAND, filename));
        out.print(I18N.getString(locale, "get.info"));
        out.println("</td>");
        out.println("</tr>");
    }

    /**
     * Print a file state table for a file. This will present the state of the
     * file in admin data and all bitarchives.
     *
     * @param out    The stream to print to.
     * @param fs     The file state for the file.
     * @param locale The locale to print labels in.
     *
     * @throws IOException On trouble printing to a stream.
     */
    public static void printFileState(JspWriter out,
                                       FilePreservationState fs,
                                       Locale locale
    )
            throws IOException {
        ArgumentNotValid.checkNotNull(out, "JspWriter out");
        ArgumentNotValid.checkNotNull(fs, "FilePreservationState fs");
        ArgumentNotValid.checkNotNull(locale, "Locale locale");
        out.println(I18N.getString(locale, "status"));

        //Table headers for info table
        out.println("<table>");
        out.print(HTMLUtils.makeTableRow(
                "<th>&nbsp;</th>",
                HTMLUtils.makeTableHeader(I18N.getString(locale, "state")),
                HTMLUtils.makeTableHeader(I18N.getString(locale, "checksum"))));

        //Admin data info
        printFileStateForAdminData(out, fs, locale);

        // Info for all bitarchives
        for (Location l : Location.getKnown()) {
            printFileStateForBitarchive(out, l, fs, locale);
        }
        out.println("</table>");
    }

    /**
     * Print a table row with current state of a file in admin data.
     *
     * @param out    The stream to print state to.
     * @param fs     The file preservation state for that file.
     * @param locale Locale of the labels.
     *
     * @throws IOException on trouble printing the state.
     */
    private static void printFileStateForAdminData(JspWriter out,
                                                    FilePreservationState fs,
                                                    Locale locale)
            throws IOException {
        out.print(HTMLUtils.makeTableRow(
                HTMLUtils.makeTableElement(I18N.getString(locale,
                                                          "admin.data")),
                HTMLUtils.makeTableElement("-"),
                HTMLUtils.makeTableElement(fs.getAdminChecksum())));
    }

    /**
     * Print a table row with current state of a file in a given bitarchive.
     *
     * @param out    The stream to print state to.
     * @param baLocation The location of the files.
     * @param fs     The file preservation state for that file.
     * @param locale Locale of the labels.
     *
     * @throws IOException
     */
    private static void printFileStateForBitarchive(
            JspWriter out, Location baLocation,
            FilePreservationState fs, Locale locale) throws IOException {
        log.debug("Printing filestate for bitarchive '"
                +  baLocation.getName() + "'");
        out.print(HTMLUtils.makeTableRow(
                HTMLUtils.makeTableElement(baLocation.getName()),
                HTMLUtils.makeTableElement(fs.getAdminBitarchiveState(baLocation)),
                HTMLUtils.makeTableElement(presentChecksum(
                    fs.getBitarchiveChecksum(baLocation), locale))));
    }

    /**
     * Print checkboxes for changing state for files. This will print two
     * checkboxes for changing a number of checkboxes, one for getting more
     * info, one for reestablishing missing files.
     * This method assumes the file toggleCheckboxes.js to be available in the
     * directory with the page this method is called from.
     *
     * @param out                          The stream to print the checkboxes
     *                                     to.
     * @param locale                       The locale of the labels.
     * @param numberOfMissingCheckboxes    The total possible number of missing
     *                                     checkboxes.
     * @param numberOfUploadableCheckboxes The total possible number of
     *                                     reestablish checkboxes.
     *
     * @throws IOException On trouble printing the checkboxes.
     */
    public static void printToggleCheckboxes(JspWriter out, Locale locale,
                                             int numberOfMissingCheckboxes,
                                             int numberOfUploadableCheckboxes)
            throws IOException {
        // Print the javascript needed.
        ArgumentNotValid.checkNotNull(out, "JspWriter out");
        ArgumentNotValid.checkNotNull(locale, "Locale locale");
        out.println("<script type=\"text/javascript\" language=\"javascript\""
                    + " src=\"toggleCheckboxes.js\"></script>");
        // Add checkbox to toggle multiple "fileinfo" checkboxes
        printMultipleToggler(
                out, Constants.GET_INFO_COMMAND,
                numberOfMissingCheckboxes, "change.infobox.for.0.files",
                locale);
        // Add checkbox to toggle multiple "reupload" checkboxes
        if (numberOfUploadableCheckboxes > 0) {
            printMultipleToggler(
                    out, Constants.ADD_COMMAND,
                    numberOfUploadableCheckboxes, "change.0.may.be.added",
                    locale);
        }
    }

    /**
     * Print a checkbox that on click will turn a number of checkboxes of a
     * certain type on or off.
     *
     * @param out                The stream to print the checkbox to.
     * @param command            The type of checkbox.
     * @param numberOfCheckboxes The total number of checksboxes possible to
     *                           turn on or off.
     * @param label              The I18N label for the describing text. An
     *                           input box with the number to change will be
     *                           added as parameter {0} in this label.
     * @param locale             The locale for the checkbox.
     *
     * @throws IOException On trouble printing the checkbox.
     */
    private static void printMultipleToggler(JspWriter out, String command,
                                             int numberOfCheckboxes,
                                             String label, Locale locale)
            throws IOException {
        out.print("<input type=\"checkbox\" id=\"toggle" + command
                  + "\" onclick=\"toggleCheckboxes('" + command
                  + "')\"/>");
        out.print(I18N.getString(locale, label,
                                 "<input id=\"toggleAmount" + command
                                 + "\" value=\"" + Math.min(
                                         numberOfCheckboxes,
                                         Constants.MAX_TOGGLE_AMOUNT)
                                 + "\" />"));
        out.println("<br/> ");
    }

    /**
     * Present a list of checksums in a human-readable form.
     * If size of list is 0, it returns "No checksum".
     * If size of list is 1, it returns the one available checksum.
     * Otherwise, it returns toString of the list. 
     * @param csum   List of checksum strings
     * @param locale The given locale.
     *
     * @return String presenting the checksums.
     */
    public static String presentChecksum(List<String> csum, Locale locale) {
        ArgumentNotValid.checkNotNull(csum, "List<String> csum");
        ArgumentNotValid.checkNotNull(locale, "Locale locale");
        String csumString = csum.toString();
        if (csum.isEmpty()) {
            csumString = I18N.getString(locale, "no.checksum");
        } else if (csum.size() == 1) {
            csumString = csum.get(0);
        }
        return csumString;
    }
}
