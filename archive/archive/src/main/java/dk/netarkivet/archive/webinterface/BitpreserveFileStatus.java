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
import dk.netarkivet.archive.arcrepository.bitpreservation.FilePreservationStatus;
import dk.netarkivet.common.distribute.arcrepository.Location;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;

/**
 * Class encapsulating methods for handling web requests for
 * ActiveBitPreservation.
 */

public class BitpreserveFileStatus {
    public static final String ADD_COMMAND = "add";
    public static final String SET_FAILED_COMMAND = "setFailed";
    public static final String GET_INFO_COMMAND = "getInfo";

    /** Internationalisation object. */
    private static final I18n I18N
            = new I18n(dk.netarkivet.archive.Constants.TRANSLATIONS_BUNDLE);
    private static Log log = LogFactory.getLog(BitpreserveFileStatus.class);

    /**
     * Extract the name of the bitarchive (parameter 'bitarchive') and whether
     * to update missing files (parameter "findmissingfiles") or checksums
     * (parameter "checksum"). Does nothing if parameter 'bitarchive' is not
     * set.
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
     * Processes a missingFiles request: Parameters of the form
     * add##<bitarchive>##<filename> causes the file to be added to that
     * bitarchive. Parameters of the form getInfo##<filename> causes checksums
     * to be computed for the file in all bitarchives and the information to be
     * shown in the next update (notice that this information disappears when
     * the page is next reloaded). Parameters of the form
     * setFailed##<bitarchive>##<filename> updates the arcrepository to consider
     * that file failed in that bitarchive.
     *
     * @param context the current JSP context
     * @param res     the result object
     *
     * @return A map of info gathered for files as requested.
     *
     * @throws ForwardedToErrorPage if the commands have wrong number of
     *                              arguments
     */
    public static Map<String, FilePreservationStatus>
    processMissingRequest(PageContext context, StringBuilder res) {
        Map<String, String[]> params = context.getRequest().getParameterMap();
        HTMLUtils.forwardOnMissingParameter(context,
                                            Constants.BITARCHIVE_NAME_PARAM);
        String bitarchiveName = params.get(Constants.BITARCHIVE_NAME_PARAM)[0];
        if (!Location.isKnownLocation(bitarchiveName)) {
            HTMLUtils.forwardOnIllegalParameter(
                    context,
                    Constants.BITARCHIVE_NAME_PARAM,
                    Location.getKnownNames()
            );
        }
        ActiveBitPreservation preserve
                = FileBasedActiveBitPreservation.getInstance();
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
                try {
                    preserve.reuploadMissingFiles(ba, filename);
                    res.append("<br/>");
                    res.append(HTMLUtils.escapeHtmlValues(I18N.getString(
                            l,
                            "file.0.has.been.restored.in.bitarchive.on.1",
                            filename, ba.getName())));
                    res.append("<br/>");
                } catch (Exception e) {
                    res.append(I18N.getString(
                            l,
                            "errmsg;attempt.at.restoring.0.in.bitarchive.at.1.failed",
                            filename, ba));
                    res.append("<br/>");
                    res.append(e.getMessage());
                    res.append("<br/>");
                    log.warn("Could not restore file '" + filename
                             + "' in bitarchive '" + ba + "'", e);
                }
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

    /**
     * Check that an array of strings has the arguments corresponding to a
     * command.
     *
     * @param context  the JSP context to forward to error to.
     * @param parts    Array of arguments given by user
     * @param cmd      The command to match
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
     * Processes a checksum request: Either sets the checksum for a given file
     * ("file" parameter) in the arcrepository (if "fixadminchecksum" parameter
     * is given) or removes and reuploads a file in one bitarchive ("bitarchive"
     * parameter) checking with the checksum and credentials given.
     *
     * @param request the request
     * @param res     the result object
     * @param context the current JSP pagecontext
     *
     * @return The file preservation status for a file, if that was requested.
     */
    public static FilePreservationStatus processChecksumRequest(
            ServletRequest request,
            StringBuilder res,
            PageContext context) {
        Locale l = context.getResponse().getLocale();
        HTMLUtils.forwardOnMissingParameter(context,
                                            Constants.BITARCHIVE_NAME_PARAM);
        HTMLUtils.forwardOnIllegalParameter(context,
                                            Constants.BITARCHIVE_NAME_PARAM,
                                            Location.getKnownNames());
        String bitarchiveName
                = request.getParameter(Constants.BITARCHIVE_NAME_PARAM);
        if (bitarchiveName == null) { // param BITARCHIVE_PARAMETER_NAME not set
            res.append(I18N.getString(
                    l,
                    "errmsg;lack.name.for.bitarchive.to.be.corrected"));
            return null;
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
                res.append(I18N.getString(
                        l,
                        "errmsg;lack.name.for.file.to.be.corrected.in.0",
                        bitarchiveName));
            }
            return null;
        }

        ActiveBitPreservation preserve
                = FileBasedActiveBitPreservation.getInstance();
        if (fixadminchecksum != null) {
            preserve.changeStatusForAdminData(filename);
        } else if (checksum != null || credentials != null) {
            // If FIX_ADMIN_CHECKSUM_PARAM is unset, the parameters
            // CHECKSUM_PARAM and CREDENTIALS_PARAM are used for removal
            // of a broken file.
            if (checksum == null) { // param CHECKSUM_PARAM not set
                res.append(I18N.getString(
                        l,
                        "errmsg;lack.checksum.for.corrupted.file.0",
                        filename));
                res.append("<br/>");
                return null;
            }

            if (credentials == null) { // param CREDENTIALS_PARAM not set
                res.append(I18N.getString(
                        l,
                        "errmsg;lacking.privileges.to.correct.in.bitarchive")
                );
                res.append("<br/>");
                return null;
            }
            try {
                preserve.replaceChangedFile(bitarchive, filename, credentials,
                                            checksum);
                res.append(I18N.getString(
                        l,
                        "file.0.has.been.replaced.in.1",
                        filename, bitarchive));
                res.append("<br/>");
            } catch (Exception e) {
                res.append(I18N.getString(
                        l,
                        "errmsg;attempt.at.restoring.0.in.bitarchive.at.1.failed",
                        filename, bitarchive));
                res.append("<br/>");
                res.append(e.getMessage());
                res.append("<br/>");
                log.warn("Attempt at restoring '" + filename
                         + "' in bitarchive on location '" + bitarchive
                         + "' failed", e);
            }
        }
        if (filename != null) {
            return preserve.getFilePreservationStatus(filename);
        } else {
            return null;
        }
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
                res.append(dk.netarkivet.archive.arcrepository
                        .bitpreservation.Constants.STRING_FILENAME_SEPARATOR);
            }
            res.append(HTMLUtils.escapeHtmlValues(arg));
        }
        if (res.length() != 0) {
            res.append("\"");
        }
        return ("<input type=\"checkbox\" name=\"" + command + "\""
                + res.toString() + ">");
    }

    /**
     * Print HTML formatted status for missing files on a given location in a
     * given locale.
     *
     * @param out      The writer to write status to.
     * @param location The location to write status for.
     * @param locale   The locale to write status in.
     *
     * @throws IOException On IO trouble writing status to the writer.
     */
    public static void printMissingFileStatusForLocation(JspWriter out,
                                                         Location location,
                                                         Locale locale)
            throws IOException {
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
     * Print HTML formatted status for checksum errors on a given location in a
     * given locale.
     *
     * @param out      The writer to write status to.
     * @param location The location to write status for.
     * @param locale   The locale to write status in.
     *
     * @throws IOException On IO trouble writing status to the writer.
     */
    public static void printChecksumErrorStatusForLocation(JspWriter out,
                                                           Location location,
                                                           Locale locale)
            throws IOException {
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
        out.println("<tr class=\"" + HTMLUtils.getRowClass(rowCount) + "\">");
        out.println(HTMLUtils.makeTableElement(filename));
        out.print("<td>");
        out.print(makeCheckbox(GET_INFO_COMMAND, filename));
        out.print(I18N.getString(locale, "get.info"));
        out.println("</td>");
        out.println("</tr>");
    }

    /**
     * Print a file status table for a file. This will present the state of the
     * file in admin data and all bitarchives.
     *
     * @param out    The stream to print to.
     * @param fs     The file status for the file.
     * @param locale The locale to print labels in.
     *
     * @throws IOException On trouble printing to a stream.
     */
    public static void printFileStatus(JspWriter out,
                                       FilePreservationStatus fs,
                                       Locale locale
    )
            throws IOException {
        out.println(I18N.getString(locale, "status"));

        //Table headers for info table
        out.println("<table>");
        out.println("<tr><th>&nbsp;</th>");
        out.println("<th>" + I18N.getString(locale, "state") + "</th>");
        out.println("<th>" + I18N.getString(locale, "checksum") + "</th>");
        out.println("</tr>");

        //Admin data info
        printFileStatusForAdminData(out, fs, locale);

        // Info for all bitarchives
        for (Location l : Location.getKnown()) {
            printFileStatusForBitarchive(out, l, fs, locale);
        }
        out.println("</table>");
    }

    /**
     * Print a table row with current status of a file in admin data.
     *
     * @param out    The stream to print status to.
     * @param fs     The file preservation status for that file.
     * @param locale Locale of the labels.
     *
     * @throws IOException on trouble printing the status.
     */
    private static void printFileStatusForAdminData(JspWriter out,
                                                    FilePreservationStatus fs,
                                                    Locale locale)
            throws IOException {
        out.println("<tr>");
        out.println(HTMLUtils.makeTableElement(
                I18N.getString(locale, "admin.data")));
        out.println(HTMLUtils.makeTableElement("-"));
        out.println(HTMLUtils.makeTableElement(fs.getAdminChecksum()));
        out.println("</tr>");
    }

    /**
     * Print a table row with current status of a file in a given bitarchive.
     *
     * @param out    The stream to print status to.
     * @param l      The location of the files.
     * @param fs     The file preservation status for that file.
     * @param locale Locale of the labels.
     *
     * @throws IOException
     */
    private static void printFileStatusForBitarchive(JspWriter out, Location l,
                                                     FilePreservationStatus fs,
                                                     Locale locale)
            throws IOException {
        String baLocation = l.getName();
        out.println("<tr>");
        out.println(HTMLUtils.makeTableElement(baLocation)
                    + HTMLUtils.makeTableElement(fs.getAdminBitarchiveState(l))
                    + HTMLUtils.makeTableElement(presentChecksum(
                fs.getBitarchiveChecksum(l), locale)));
        out.println("</tr>");
    }

    /**
     * Print checkboxes for changing status for files. This will print two
     * checkboxes for changing a number of checkboxes, one for getting more
     * info, one for reestablishing missing files.
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
        out.print("<script type=\"text/javascript\" language=\"javascript\">\n"
                  + "    /** Toggles the status of all checkboxes with a given class */\n"
                  + "    function toggleCheckboxes(command) {\n"
                  + "        var toggler = document.getElementById(\"toggle\" + command);\n"
                  + "        if (toggler.checked) {\n"
                  + "            var setOn = true;\n"
                  + "        } else {\n"
                  + "            var setOn = false;\n"
                  + "        }\n"
                  + "        var elements = document.getElementsByName(command);\n"
                  + "        var maxToggle = document.getElementById(\"toggleAmount\" + command).value;\n"
                  + "        if (maxToggle <= 0) {\n"
                  + "            maxToggle = elements.length;\n"
                  + "        }\n"
                  + "        for (var i = 0; i < elements.length && i < maxToggle; i++) {\n"
                  + "            elements[i].checked = setOn;\n"
                  + "        }\n"
                  + "    }\n"
                  + "</script>");
        // Add checkbox to toggle multiple "fileinfo" checkboxes
        printMultipleToggler(
                out, GET_INFO_COMMAND,
                numberOfMissingCheckboxes, "change.infobox.for.0.files",
                locale);
        // Add checkbox to toggle multiple "reupload" checkboxes
        if (numberOfUploadableCheckboxes > 0) {
            printMultipleToggler(
                    out, ADD_COMMAND,
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
     * @param label              The I18N label for the describing text, an
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
                                 + "\">"));
        out.println("<br/> ");
    }

    /**
     * Present a list of checksums in a human-readable form.
     *
     * @param csum   List of checksum strings
     * @param locale
     *
     * @return String presenting the checksums.
     */
    public static String presentChecksum(List<String> csum, Locale locale) {
        String csumString = csum.toString();
        if (csum.isEmpty()) {
            csumString = I18N.getString(locale, "no.checksum");
        } else if (csum.size() == 1) {
            csumString = csum.get(0);
        }
        return csumString;
    }
}
