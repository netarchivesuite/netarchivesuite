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

import dk.netarkivet.archive.arcrepository.bitpreservation.FileBasedActiveBitPreservation;
import dk.netarkivet.archive.arcrepository.bitpreservation.FilePreservationStatus;
import dk.netarkivet.archive.arcrepository.bitpreservation.WorkFiles;
import dk.netarkivet.common.distribute.arcrepository.Location;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.FileUtils;
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

        FileBasedActiveBitPreservation preserve = FileBasedActiveBitPreservation.getInstance();
        if (findmissingfiles != null) {
            preserve.findMissingFiles(bitarchive);
        }

        if (checksum != null) {
            preserve.findWrongFiles(bitarchive);
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
        FileBasedActiveBitPreservation preserve = FileBasedActiveBitPreservation.getInstance();
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
                    res.append(HTMLUtils.escapeHtmlValues(I18N.getString(l,
                                                                         "file.0.has.been.restored.in.bitarchive.on.1",
                                                                         filename,
                                                                         ba.getName())));
                    res.append("<br/>");
                } catch (Exception e) {
                    res.append(I18N.getString(l, "errmsg;attempt.at.restoring.0.in.bitarchive.at.1.failed", filename, ba));
                    res.append("<br/>");
                    res.append(e.getMessage());
                    res.append("<br/>");
                    log.warn("Could not restore file '" + filename
                             + "' in bitarchive '" + ba + "'", e);
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
                preserve.setAdminData(filename, ba);
                res.append(HTMLUtils.escapeHtmlValues(I18N.getString(l,
                                                                     "file.0.is.now.marked.as.failed.in.bitarchive.1",
                                                                     filename,
                                                                     ba.getName())));
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
            HTMLUtils.forwardWithErrorMessage(context, I18N,
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
    public static FilePreservationStatus processChecksumRequest(ServletRequest request,
                                              StringBuilder res,
                                              PageContext context) {
        Locale l = context.getResponse().getLocale();
        HTMLUtils.forwardOnMissingParameter(context,
                Constants.BITARCHIVE_NAME_PARAM);
        HTMLUtils.forwardOnIllegalParameter(context,
                Constants.BITARCHIVE_NAME_PARAM, Location.getKnownNames());
        String bitarchiveName
                = request.getParameter(Constants.BITARCHIVE_NAME_PARAM);
        if (bitarchiveName == null) { // param BITARCHIVE_PARAMETER_NAME not set
            res.append(I18N.getString(l,
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
                res.append(I18N.getString(l,
                                          "errmsg;lack.name.for.file.to.be.corrected.in.0",
                                          bitarchiveName));
            }
            return null;
        }

        FileBasedActiveBitPreservation preserve
                = FileBasedActiveBitPreservation.getInstance();
        if (fixadminchecksum != null) {
            FilePreservationStatus fs =
                    preserve.getFilePreservationStatus(filename);
            if (fs == null) {
                res.append(I18N.getString(l,
                                          "no.info.on.file.0", filename));
                FileUtils.removeLineFromFile(filename,
                                             WorkFiles.getFile(bitarchive,
                                                               WorkFiles.WRONG_FILES));
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
        } else if (checksum != null || credentials != null) {
            // If FIX_ADMIN_CHECKSUM_PARAM is unset, the parameters
            // CHECKSUM_PARAM and CREDENTIALS_PARAM are used for removal
            // of a broken file.
            if (checksum == null) { // param CHECKSUM_PARAM not set
                res.append(I18N.getString(l,
                                          "errmsg;lack.checksum.for.corrupted.file.0",
                                          filename));
                return null;
            }

            if (credentials == null) { // param CREDENTIALS_PARAM not set
                res.append(I18N.getString(l,
                                          "errmsg;lacking.privileges.to.correct.in.bitarchive")
                );
                return null;
            }
            preserve.removeAndGetFile(filename, bitarchive,
                                      checksum, credentials);
            res.append(I18N.getString(l,
                                      "file.0.has.been.deleted.in.1.needs.copy",
                                      filename, bitarchive));

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
        out.println(I18n.getString(
                dk.netarkivet.archive.Constants.TRANSLATIONS_BUNDLE,
                locale, "filestatus.for") + "&nbsp;<b>" + HTMLUtils
                .escapeHtmlValues(location.getName()) + "</b>");
        out.println("<br/>");

        out.println(I18n.getString(
                dk.netarkivet.archive.Constants.TRANSLATIONS_BUNDLE,
                locale, "number.of.files") + "&nbsp;" + FileBasedActiveBitPreservation.getInstance().getNumberOfFiles(
                location));
        out.println("<br/>");
        out.println(I18n.getString(
                dk.netarkivet.archive.Constants.TRANSLATIONS_BUNDLE,
                locale, "missing.files") + "&nbsp;" + FileBasedActiveBitPreservation.getInstance().getNumberOfMissingFiles(
                location));

        if (FileBasedActiveBitPreservation.getInstance().getNumberOfMissingFiles(location
        ) > 0) {
            out.print("&nbsp;<a href=\"" + Constants.FILESTATUS_MISSING_PAGE
                      + "?" + (Constants.BITARCHIVE_NAME_PARAM
                               + "=" + HTMLUtils
                    .encodeAndEscapeHTML(location.getName())) + " \">");
            out.print(I18n.getString(
                    dk.netarkivet.archive.Constants.TRANSLATIONS_BUNDLE,
                    locale, "show.missing.files"));
            out.print("</a>");
        }
        out.println("<br/>");

        out.println(I18n.getString(
                dk.netarkivet.archive.Constants.TRANSLATIONS_BUNDLE,
                locale, "last.update.at.0",
                FileBasedActiveBitPreservation.getInstance().getDateForMissingFiles(location
                )));
        out.println("<br/>");

        out.println("<a href=\"" + Constants.FILESTATUS_PAGE + "?"
                    + Constants.FIND_MISSING_FILES_PARAM + "=1&amp;"
                    + (Constants.BITARCHIVE_NAME_PARAM
                       + "=" + HTMLUtils
                .encodeAndEscapeHTML(location.getName())) + "\">" + I18n
                .getString(dk.netarkivet.archive.Constants.TRANSLATIONS_BUNDLE,
                           locale, "update") + "</a>");
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
        out.println(I18n.getString(
                dk.netarkivet.archive.Constants.TRANSLATIONS_BUNDLE,
                locale, "checksum.status.for") + "&nbsp;<b>" + HTMLUtils
                .escapeHtmlValues(location.getName()) + "</b>");
        out.println("<br/>");

        out.println(I18n.getString(
                dk.netarkivet.archive.Constants.TRANSLATIONS_BUNDLE,
                locale, "number.of.files.with.error") + "&nbsp;"
                                                      + FileBasedActiveBitPreservation.getInstance().getNumberOfChangedFiles(
                location));

        if (FileBasedActiveBitPreservation.getInstance().getNumberOfChangedFiles(location
        ) > 0) {
            out.print("&nbsp;<a href=\"" + Constants.FILESTATUS_CHECKSUM_PAGE
                      + "?" + (Constants.BITARCHIVE_NAME_PARAM
                               + "=" + HTMLUtils
                    .encodeAndEscapeHTML(location.getName())) + " \">");
            out.print(I18n.getString(
                    dk.netarkivet.archive.Constants.TRANSLATIONS_BUNDLE,
                    locale, "show.files.with.error"));
            out.print("</a>");
        }
        out.println("<br/>");

        out.println(I18n.getString(
                dk.netarkivet.archive.Constants.TRANSLATIONS_BUNDLE,
                locale, "last.update.at.0",
                FileBasedActiveBitPreservation.getInstance().getDateForChangedFiles(location
                )));
        out.println("<br/>");

        out.println("<a href=\"" + Constants.FILESTATUS_PAGE + "?"
                    + Constants.CHECKSUM_PARAM + "=1&amp;"
                    + (Constants.BITARCHIVE_NAME_PARAM
                       + "=" + HTMLUtils
                .encodeAndEscapeHTML(location.getName())) + "\">" + I18n
                .getString(dk.netarkivet.archive.Constants.TRANSLATIONS_BUNDLE,
                           locale, "update") + "</a>");
        out.println("<br/><br/>");
    }

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

    private static void printFileStatusForAdminData(JspWriter out,
                                                   FilePreservationStatus fs,
                                                   Locale locale)
            throws IOException {
        out.println("<tr><td>"
                    + I18N.getString(locale, "admin.data")
                    + "</td>");
        out.println("<td>-</td>");
        out.println(HTMLUtils.makeTableElement(fs.getAdminChecksum()));
        out.println("</tr>");
    }

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

    public static void printToggleCheckboxes(JspWriter out, Locale locale,
                                             int numberOfMissingCheckboxes,
                                             int numberOfUploadableCheckboxes)
            throws IOException {
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

    /** Present a list of checksums in a human-readable form.
     *
     * @param csum List of checksum strings
     * @param locale
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
