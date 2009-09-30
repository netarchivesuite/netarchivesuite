/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 USA
 */

package dk.netarkivet.common.webinterface;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringTree;

/**
 * This is a utility class containing methods for use in the GUI for
 * netarkivet.
 */
public class HTMLUtils {
    /** Web page title placeholder. */
    private static String TITLE_PLACEHOLDER = "STRING_1";
    /** Web page header template. */
    private static String WEBPAGE_HEADER_TEMPLATE = "<!DOCTYPE html "
            + "PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \n "
            + "  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n"
            + "<html xmlns=\"http://www.w3.org/1999/xhtml\""
            + " xml:lang=\"en\" lang=\"en\">\n" 
            + "<head>\n"
            + "<meta content=\"text/html; charset=UTF-8\" "
            + "http-equiv= \"content-type\" />"
            + "<meta http-equiv=\"Expires\" content=\"0\"/>\n"
            + "<meta http-equiv=\"Cache-Control\" content=\"no-cache\"/>\n"
            + "<meta http-equiv=\"Pragma\" content=\"no-cache\"/> \n"
            + "<title>" + TITLE_PLACEHOLDER + "</title>\n"
            + "<script type=\"text/javascript\">\n"
            + "<!--\n"
            + "function giveFocus() {\n"
            + "    var e = document.getElementById('focusElement');\n"
            + "    if (e != null) {\n"
            + "        var elms = e.getElementsByTagName('*');\n"
            + "        if (elms != null && elms.length != null "
            + "            && elms.item != null && elms.length > 0) {\n"
            + "            var e2 = elms.item(0);\n"
            + "                if (e2 != null && e2.focus != null) {\n"
            + "            }\n"
            + "            e2.focus();\n"
            + "        }\n"
            + "    }\n"
            + "}\n"
            + "-->\n"
            + "</script>\n"
            + "<link rel=\"stylesheet\" href=\"./netarkivet.css\" "
            + "type=\"text/css\" />\n"
            + "<link rel=\"stylesheet\" type=\"text/css\" media=\"all\" "
            + "href=\"./jscalendar/calendar-win2k-cold-1.css\" "
            + "title=\"./jscalendar/win2k-cold-1\" />\n"
            + "</head> <body onload=\"giveFocus()\">\n";

    /** Logger for this class. */
    private static Log log = LogFactory.getLog(HTMLUtils.class.getName());
    /** Translations for this module. */
    private static final I18n I18N = new I18n(Constants.TRANSLATIONS_BUNDLE);

    /**
     * Private constructor. There is no reason to instantiate this class.
     */
    private HTMLUtils() {
        // Nothing to initialize
    }


    /**
     * Url encodes a string in UTF-8. This encodes _all_ non-letter non-number
     * characters except '-', '_' and '.'.
     * The characters '/' and ':' are encoded.
     * @param s the string to encode
     * @return the encoded string
     */
    public static String encode(String s) {
        ArgumentNotValid.checkNotNull(s, "s");
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ArgumentNotValid(URLEncoder.class.getName()
                    + " does not support UTF-8", e);
        }
    }

    /**
     * Url decodes a string encoded in UTF-8.
     * @param s the string to decode
     * @return the decoded string
     */
    public static String decode(String s) {
        ArgumentNotValid.checkNotNull(s, "s");
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ArgumentNotValid(
                    URLDecoder.class.getName()
                    + " does not support UTF-8", e);
        }
    }

    /**
     * Prints the header information for the webpages in the GUI. This includes
     * the navigation menu, and links for changing the language.
     * The title of the page is generated internationalised from sitesections.
     * If you want to specify it, use the overloaded method.
     * @param context The context of the web page request.
     * @throws IOException if an error occurs during writing of output.
     */
    public static void generateHeader(PageContext context)
            throws IOException {
        ArgumentNotValid.checkNotNull(context, "context");
        String url = ((HttpServletRequest) context.getRequest())
                .getRequestURL().toString();
        Locale locale = context.getResponse().getLocale();
        String title = getTitle(url, locale);
        generateHeader(title, context);
    }

    /**
     * Prints the header information for the webpages in the GUI. This includes
     * the navigation menu, and links for changing the language.
     * @param title An internationalised title of the page.
     * @param context The context of the web page request.
     * @throws IOException if an error occurs during writing to output.
     */
    public static void generateHeader(String title, PageContext context)
            throws IOException {
        ArgumentNotValid.checkNotNull(title, "title");
        ArgumentNotValid.checkNotNull(context, "context");
     
        JspWriter out = context.getOut();
        String url = ((HttpServletRequest) context.getRequest())
                .getRequestURL().toString();
        Locale locale = context.getResponse().getLocale();
        title = escapeHtmlValues(title);
        log.debug("Loaded URL '" + url + "' with title '" + title + "'");
        out.print(WEBPAGE_HEADER_TEMPLATE.replace(TITLE_PLACEHOLDER, title));
        // Start the two column / one row table which fills the page
        out.print("<table id =\"main_table\"><tr>\n");
        // fill in data in the left column
        generateNavigationTree(out, url, locale);
        // The right column contains the active form content for this page
        out.print("<td valign = \"top\" >\n");
        // Language links
        generateLanguageLinks(out);
    }

    /**
     * Get the locale according to header context information.
     * @param context The context of the web page request.
     * @return The locale given in the the page response.
     */
    public static Locale getLocaleObject(PageContext context) {
        ArgumentNotValid.checkNotNull(context, "context");
        return context.getResponse().getLocale();
    }

    /**
     * Prints out links to change languages. Will read locales and names of
     * languages from settings, and write them as links to the page "lang.jsp".
     * The locale will be given to this page as a parameter, the name will be
     * shown as the text of the link
     * @param out the writer to which the links are written.
     * @throws IOException if an error occurs during writing of output.
     */
    private static void generateLanguageLinks(JspWriter out) throws
                                                             IOException {
        out.print("<div class=\"languagelinks\">");
        StringTree<String> webinterfaceSettings = Settings.getTree(
                CommonSettings.WEBINTERFACE_SETTINGS);

        for (StringTree<String> language
                : webinterfaceSettings.getSubTrees(
                        CommonSettings.WEBINTERFACE_LANGUAGE)) {
            out.print(String.format(
                    "<a href=\"lang.jsp?locale=%s&amp;name=%s\">%s</a>&nbsp;",
                    escapeHtmlValues(encode(language.getValue(
                            CommonSettings.WEBINTERFACE_LANGUAGE_LOCALE))),
                    escapeHtmlValues(encode(language.getValue(
                            CommonSettings.WEBINTERFACE_LANGUAGE_NAME))),
                    escapeHtmlValues(language.getValue(
                            CommonSettings.WEBINTERFACE_LANGUAGE_NAME)))
            );
        }
        out.print("</div>");
    }

    /**
     * Prints out the navigation tree appearing as a <td> in the left column of
     * the "main_table" table. Subpages are shown only for the currently-active
     * main-heading of the sections defined in settings.
     *
     * @param out the writer to which the output must be written.
     * @param url the url of the page.
     * @param locale The locale selecting the language.
     * @throws IOException if the output cannot be written.
     */
    private static void generateNavigationTree(JspWriter out,
                                               String url, Locale locale)
            throws IOException {
        out.print("<td valign=\"top\" id=\"menu\">\n");
        // The list of menu items is presented as a 1-column table
        out.print("<table id=\"menu_table\">\n");
        String s = I18N.getString(locale, "sidebar.title.menu");
        out.print("<tr><td><a class=\"sidebarHeader\" href=\"index.jsp\">"
                 + "<img src=\"transparent_menu_logo.png\" alt=\"" + s + "\"/> "
                 + s
                 + "</a></td></tr>\n");

        for (SiteSection section : SiteSection.getSections()) {
            section.generateNavigationTree(out, url, locale);
        }
        out.print("</table>\n");
        out.print("</td>\n");
    }

    /**
     * Writes out footer information to close the page.
     * @param out the writer to which the information is written
     * @throws IOException if the output cannot be written
     */
    public static void generateFooter(JspWriter out) throws IOException {
        ArgumentNotValid.checkNotNull(out, "out");
        // Close the element containing the page content
        out.print("</td>\n");
        // Close the single row in the table
        out.print("</tr>\n");
        // Close the table
        out.print("</table>\n");
        // Add information about the running system
        out.print("<div class='systeminfo'>");
        out.print("NetarchiveSuite " + Constants.getVersionString()
                  + ", " + Settings.get(CommonSettings.ENVIRONMENT_NAME));
        out.print("</div>");
        // Close the page
        out.print("</body></html>");


    }

    /** Create a table element containing the given string, escaping HTML
     * values in the process.
     *
     * @param s An unescaped string.  Any HTML tags in this string will end up
     * escaped away.
     * @return The same string escaped and enclosed in td tags.
     */
    public static String makeTableElement(String s) {
        ArgumentNotValid.checkNotNull(s, "s");
        return "<td>" + escapeHtmlValues(s) + "</td>";
    }

    /** Create a table header element containing the given string, escaping HTML
     * values in the process.
     *
     * @param contents An unescaped string.  Any HTML tags in this string will
     * end up escaped away.
     * @return The same string escaped and enclosed in th tags.
     */
    public static String makeTableHeader(String contents) {
        ArgumentNotValid.checkNotNull(contents, "contents");
        return "<th>" + escapeHtmlValues(contents) + "</th>";
    }

    /** Create a table row. Note that in contrast to createTableElement and
     * createTableHeader, the contents are not escaped. They are expected to
     * contain table elements.
     *
     * @param contents The contents to put into the table row.
     * The entries will be delimited by newline characters.
     * @return The same string escaped and enclosed in td tags.
     */
    public static String makeTableRow(String... contents) {
        ArgumentNotValid.checkNotNull(contents, "contents");
        StringBuilder sb = new StringBuilder("<tr>");
        for (String element: contents) {
            sb.append(element);
            sb.append("\n");
        }
        sb.append("</tr>\n");
        return sb.toString();
    }

    /** Get an HTML representation of the date given.
     *
     * @param d A date
     * @return A representation of the date that can be directly inserted
     * into an HTML document, or the empty string if d is null.
     * @deprecated Please use <fmt:date> from taglib instead.
     */
    public static String makeDate(Date d) {
        if (d == null) {
            return "";
        } else {
            return escapeHtmlValues(d.toString());
        }
    }

    /**
     * Returns the toString() value of an object or a hyphen if the
     * argument is null.
     * @param o the given object
     * @return o.toString() or "-" if o is null
     */
    public static String nullToHyphen(Object o) {
        if (o == null) {
            return "-";
        } else {
            return o.toString();
        }
    }

    /**
     * Escapes HTML special characters ", &, < and > (but not ').
     *
     * @param input a string
     * @return The string with values escaped. If input is null, the empty
     *         string is returned.
     */
    public static String escapeHtmlValues(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("&", "&amp;").replaceAll("\\\"", "&quot;")
                .replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    }

    /** Encode a string for use in a URL, then escape characters that must be
     * escaped in HTML.  This must be used whenever unknown strings are used
     * in URLs that are placed in HTML.
     *
     * @param input A string
     * @return The same string, encoded to be safely placed in a URL in HTML.
     */
    public static String encodeAndEscapeHTML(String input) {
        ArgumentNotValid.checkNotNull(input, "input");
        return escapeHtmlValues(encode(input));
    }

    /**
     * Escapes a string for use in javascript. Replaces " with \"
     * and ' with \', so e.g. escapeJavascriptQuotes("\"").equals("\\\"")
     * Also, \ and any non-printable character is escaped for use in javascript
     *
     * @param input a string
     * @return The string with values escaped. If input is null, the empty
     *         string is returned.
     */
    public static String escapeJavascriptQuotes(String input) {
        if (input == null) {
            return "";
        }
        input = input.replaceAll("\\\\", "\\\\\\\\");
        input = input.replaceAll("\\\"", "\\\\\\\"");
        input = input.replaceAll("\\\'", "\\\\\\\'");
        input = input.replaceAll("\\\u0000", "\\\\u0000");
        input = input.replaceAll("\\\u0001", "\\\\u0001");
        input = input.replaceAll("\\\u0002", "\\\\u0002");
        input = input.replaceAll("\\\u0003", "\\\\u0003");
        input = input.replaceAll("\\\u0004", "\\\\u0004");
        input = input.replaceAll("\\\u0005", "\\\\u0005");
        input = input.replaceAll("\\\u0006", "\\\\u0006");
        input = input.replaceAll("\\\u0007", "\\\\u0007");
        input = input.replaceAll("\\\b", "\\\\b");
        input = input.replaceAll("\\\t", "\\\\t");
        input = input.replaceAll("\\\n", "\\\\n");
        //Note: \v is an escape for vertical tab that exists
        //in javascript but not in java
        input = input.replaceAll("\\\u000B", "\\\\v");
        input = input.replaceAll("\\\f", "\\\\f");
        input = input.replaceAll("\\\r", "\\\\r");
        input = input.replaceAll("\\\u000E", "\\\\u000E");
        input = input.replaceAll("\\\u000F", "\\\\u000F");
        input = input.replaceAll("\\\u0010", "\\\\u0010");
        input = input.replaceAll("\\\u0011", "\\\\u0011");
        input = input.replaceAll("\\\u0012", "\\\\u0012");
        input = input.replaceAll("\\\u0013", "\\\\u0013");
        input = input.replaceAll("\\\u0014", "\\\\u0014");
        input = input.replaceAll("\\\u0015", "\\\\u0015");
        input = input.replaceAll("\\\u0016", "\\\\u0016");
        input = input.replaceAll("\\\u0017", "\\\\u0017");
        input = input.replaceAll("\\\u0018", "\\\\u0018");
        input = input.replaceAll("\\\u0019", "\\\\u0019");
        input = input.replaceAll("\\\u001A", "\\\\u001A");
        input = input.replaceAll("\\\u001B", "\\\\u001B");
        input = input.replaceAll("\\\u001C", "\\\\u001C");
        input = input.replaceAll("\\\u001D", "\\\\u001D");
        input = input.replaceAll("\\\u001E", "\\\\u001E");
        input = input.replaceAll("\\\u001F", "\\\\u001F");
        return input;
    }

    /**
     * Sets the character encoding for reading parameters and content from a
     * request in a JSP page.
     * @param request The servlet request object
     */
    public static void setUTF8(HttpServletRequest request) {
        ArgumentNotValid.checkNotNull(request, "request");
        // Why is this in an if block? Suppose we forward from a page where
        // we read file input from the request. Trying to set the character
        // encoding again here will throw an exception!
        // This is a bit of a hack - we know that _if_ we have set it,
        // we have set it to UTF-8, so this way we won't set it twice...
        if (request.getCharacterEncoding() == null
            || !request.getCharacterEncoding().equals("UTF-8")) {
            try {
                request.setCharacterEncoding("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new ArgumentNotValid(
                        "Should never happen! UTF-8 not supported", e);
            }
        }
    }

    /**
     * Given a URL in the sitesection hierarchy, returns the corresponding page
     * title.
     *
     * @param url a given URL
     * @param locale the current locale
     * @return the corresponding page title, or string about "(no title)" if no
     * title can be found
     * @throws ArgumentNotValid if the given url or locale is null or
     * url is empty.
     */
    public static String getTitle(String url, Locale locale) {
        ArgumentNotValid.checkNotNull(locale, "Locale locale");
        ArgumentNotValid.checkNotNullOrEmpty(url, "String url");
        for (SiteSection section : SiteSection.getSections()) {
            String title = section.getTitle(url, locale);
            if (title != null) {
                return title;
            }
        }
        log.warn("Could not find page title for page '" + url + "'");
        return I18N.getString(locale, "pagetitle.unknown");
    }

    /** Get the (CSS) class name for a row in a table.  The row count should
     * start at 0.
     *
     * @param rowCount The number of the row
     * @return A CSS class name that should be the class of the TR element.
     */
    public static String getRowClass(int rowCount) {
        if (rowCount % 6 < 3) {
            return "row0";
        } else {
            return "row1";
        }
    }

    /** Get a locale from cookie, if present. The default request locale
     * otherwise.
     * @param request The request to get the locale for.
     * @return The cookie locale, if present. The default request locale
     * otherwise.
     */
    public static String getLocale(HttpServletRequest request) {
        ArgumentNotValid.checkNotNull(request, "request");
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (c.getName().equals("locale")) {
                    return c.getValue();
                }
            }
        }
        return request.getLocale().toString();
    }

    /** Forward to our standard error message page with an internationalized
     * message.  Note that this <em>doesn't</em> throw ForwardedToErrorPage,
     * it is the job of whoever calls this to do that if not within a JSP page
     * (a JSP page can just return immediately).
     * All text involved will be HTML-escaped.
     *
     * @param context The context that the error happened in (the JSP-defined
     * pageContext, typically)
     * @param I18N The i18n information
     * @param label An i18n label for the error.  This label should begin with
     * "errormsg;".
     * @param args Any extra args for i18n
     * @throws IOFailure If the forward fails
     */
    public static void forwardWithErrorMessage(PageContext context,
                                               I18n I18N, String label,
                                               Object... args) {
        // Note that we may not want to be to strict here
        // as otherwise information could be lost.
        ArgumentNotValid.checkNotNull(context, "context");
        ArgumentNotValid.checkNotNull(I18N, "I18N");
        ArgumentNotValid.checkNotNull(label, "label");
        ArgumentNotValid.checkNotNull(args, "args");

        String msg = HTMLUtils.escapeHtmlValues(I18N.getString(
                context.getResponse().getLocale(),
                label, args));
        context.getRequest().setAttribute("message", msg);
        RequestDispatcher rd
                = context.getServletContext().getRequestDispatcher(
                "/message.jsp");
        final String errormsg = "Failed to forward on error " + msg;        
        try {
            rd.forward(context.getRequest(), context.getResponse());
        } catch (IOException e) {
            log.warn(errormsg, e);
            throw new IOFailure(errormsg, e);
        } catch (ServletException e) {
            log.warn(errormsg, e);
            throw new IOFailure(errormsg, e);
        }
    }

    /** Forward to our standard error message page with an internationalized
     * message.  Note that this <em>doesn't</em> throw ForwardedToErrorPage,
     * it is the job of whoever calls this to do that if not within a JSP page
     * (a JSP page can just return immediately).
     * The text involved must be HTML-escaped before passing to this method.
     *
     * @param context The context that the error happened in (the JSP-defined
     * pageContext, typically)
     * @param i18n The i18n information
     * @param label An i18n label for the error.  This label should begin with
     * "errormsg;".
     * @param args Any extra args for i18n.  These must be valid HTML.
     * @throws IOFailure If the forward fails.
     */
    public static void forwardWithRawErrorMessage(PageContext context,
                                                  I18n i18n, String label,
                                                  Object... args) {        
        // Note that we may not want to be to strict here
        // as otherwise information could be lost.
        ArgumentNotValid.checkNotNull(context, "context");
        ArgumentNotValid.checkNotNull(I18N, "I18N");
        ArgumentNotValid.checkNotNull(label, "label");
        ArgumentNotValid.checkNotNull(args, "args");
        
        String msg = i18n.getString(context.getResponse().getLocale(),
                label, args);
        context.getRequest().setAttribute("message", msg);
        RequestDispatcher rd
                = context.getServletContext().getRequestDispatcher(
                "/message.jsp");
        try {
            rd.forward(context.getRequest(), context.getResponse());
        } catch (IOException e) {
            final String errormsg = "Failed to forward on error " + msg;
            log.warn(errormsg, e);
            throw new IOFailure(errormsg, e);
        } catch (ServletException e) {
            final String errormsg = "Failed to forward on error " + msg;
            log.warn(errormsg, e);
            throw new IOFailure(errormsg, e);
        }
    }

    /** Forward to our standard error message page with an internationalized
     * message, in case of exception. Note that this <em>doesn't</em> throw
     * ForwardedToErrorPage, it is the job of whoever calls this to do that if
     * not within a JSP page (a JSP page can just return immediately).
     * All text involved will be HTML-escaped.
     *
     * @param context The context that the error happened in (the JSP-defined
     * pageContext, typically)
     * @param i18n The i18n information
     * @param e The exception that is being handled.
     * @param label An i18n label for the error.  This label should begin with
     * "errormsg;".
     * @param args Any extra args for i18n
     * @throws IOFailure If the forward fails
     */
    public static void forwardWithErrorMessage(PageContext context,
                                               I18n i18n, Throwable e,
                                               String label, Object... args) {
        // Note that we may not want to be to strict here
        // as otherwise information could be lost.
        ArgumentNotValid.checkNotNull(context, "context");
        ArgumentNotValid.checkNotNull(I18N, "I18N");
        ArgumentNotValid.checkNotNull(label, "label");
        ArgumentNotValid.checkNotNull(args, "args");
        
        String msg = HTMLUtils.escapeHtmlValues(i18n.getString(
                context.getResponse().getLocale(),
                label, args));
        context.getRequest().setAttribute("message",
                msg + "\n" + e.getLocalizedMessage());
        RequestDispatcher rd
                = context.getServletContext().getRequestDispatcher(
                "/message.jsp");
        final String errormsg = "Failed to forward on error " + msg;        
        try {
            rd.forward(context.getRequest(), context.getResponse());
        } catch (IOException e1) {
            log.warn(errormsg, e1);
            throw new IOFailure(errormsg, e1);
        } catch (ServletException e1) {
            log.warn(errormsg, e1);
            throw new IOFailure(errormsg, e1);
        }
    }

    /** Checks that the given parameters exist.  If any of
     * them do not exist, forwards to the error page and throws
     * ForwardedToErrorPage.
     *
     * @param context The context of the current JSP page
     * @param parameters List of parameters that must exist
     * @throws IOFailure If the forward fails
     * @throws ForwardedToErrorPage If a parameter is missing
     */
    public static void forwardOnMissingParameter(PageContext context,
                                                 String... parameters)
            throws ForwardedToErrorPage {
        // Note that we may not want to be to strict here
        // as otherwise information could be lost.
        ArgumentNotValid.checkNotNull(context, "context");
        ArgumentNotValid.checkNotNull(parameters, "parameters");
        
        ServletRequest request = context.getRequest();
        for (String parameter : parameters) {
            String value = request.getParameter(parameter);
            if (value == null) {
                forwardWithErrorMessage(context, I18N,
                        "errormsg;missing.parameter.0", parameter);
                throw new ForwardedToErrorPage("Missing parameter '"
                        + parameter + "'");
            }
        }

    }

    /** Checks that the given parameters exist and are not empty.  If any of
     * them are missing or empty, forwards to the error page and throws
     * ForwardedToErrorPage.  A parameter with only whitespace is considered
     * empty.
     *
     * @param context The context of the current JSP page
     * @param parameters List of parameters that must exist and be non-empty
     * @throws IOFailure If the forward fails
     * @throws ForwardedToErrorPage if a parameter was missing or empty
     */
    public static void forwardOnEmptyParameter(PageContext context,
                                               String... parameters) {
        // Note that we may not want to be to strict here
        // as otherwise information could be lost.
        ArgumentNotValid.checkNotNull(context, "context");
        ArgumentNotValid.checkNotNull(parameters, "parameters");
        
        forwardOnMissingParameter(context, parameters);
        ServletRequest request = context.getRequest();
        for (String parameter : parameters) {
            String value = request.getParameter(parameter);
            if (value.trim().length() == 0) {
                forwardWithErrorMessage(context, I18N,
                        "errormsg;empty.parameter.0", parameter);
                throw new ForwardedToErrorPage("Empty parameter '"
                        + parameter + "'");
            }
        }
    }

    /** Checks that the given parameter exists and is one of a set of values.
     * If is is missing or doesn't equal one of the given values, forwards to
     * the error page and throws ForwardedToErrorPage.
     *
     * @param context The context of the current JSP page
     * @param parameter parameter that must exist
     * @param legalValues legal values for the parameter
     * @throws IOFailure If the forward fails
     * @throws ForwardedToErrorPage if the parameter is none of the given values
     */
    public static void forwardOnIllegalParameter(PageContext context,
                                                 String parameter,
                                                 String... legalValues)
            throws ForwardedToErrorPage {
        // Note that we may not want to be to strict here
        // as otherwise information could be lost.
        ArgumentNotValid.checkNotNull(context, "context");
        ArgumentNotValid.checkNotNull(parameter, "parameter");
        ArgumentNotValid.checkNotNull(legalValues, "legalValues");

        forwardOnMissingParameter(context, parameter);
        String value = context.getRequest().getParameter(parameter);
        for (String legalValue : legalValues) {
            if (value.equals(legalValue)) {
                return;
            }
        }
        forwardWithErrorMessage(context, I18N,
                "errormsg;illegal.value.0.for.parameter.1", value, parameter);
        throw new ForwardedToErrorPage("Illegal value '" + value
                + "' for parameter '" + parameter + "'");
    }

    /** Parses a integer request parameter and checks that it lies within
     * a given interval.  If it doesn't, forwards to an error page and
     * throws ForwardedToErrorPage.
     *
     * @param context The context this call happens in
     * @param param A parameter to parse.
     * @param minValue The minimum allowed value
     * @param maxValue The maximum allowed value
     * @return The value x parsed from the string, if minValue <= x <= maxValue
     * @throws ForwardedToErrorPage if  the parameter doesn't exist, is not a
     * parseable integer, or doesn't lie within the limits.
     */
    public static int parseAndCheckInteger(PageContext context,
                                           String param,
                                           int minValue, int maxValue)
            throws ForwardedToErrorPage {
        // Note that we may not want to be to strict here
        // as otherwise information could be lost.
        ArgumentNotValid.checkNotNull(context, "context");
        ArgumentNotValid.checkNotNull(param, "param");

        Locale loc = HTMLUtils.getLocaleObject(context);
        forwardOnEmptyParameter(context, param);
        int value;
        String paramValue = context.getRequest().getParameter(param);
        try {
            value = NumberFormat.getInstance(loc).parse(paramValue).intValue();
            if (value < minValue || value > maxValue) {
                forwardWithErrorMessage(context, I18N,
                        "errormsg;parameter.0.outside.range.1.to.2.3",
                        param, paramValue, minValue, maxValue);
                throw new ForwardedToErrorPage("Parameter '" + param
                        + "' should be between " + minValue + " and " + maxValue
                        + " but is " + paramValue);
            }
            return value;
        } catch (ParseException e) {
            forwardWithErrorMessage(context, I18N,
                    "errormsg;parameter.0.not.an.integer.1", param,
                    paramValue);
            throw new ForwardedToErrorPage("Invalid value " + paramValue
                    + " for integer parameter '" + param + "'", e);
        }
    }

    /** Parse an optionally present long-value from a request parameter.
     *
     * @param context The context of the web request.
     * @param param The name of the parameter to parse.
     * @param defaultValue A value to return if the parameter is not present
     * (may be null).
     * @return Parsed value or default value if the parameter is missing
     * or empty. Null will only be returned if passed as the default value.
     * @throws ForwardedToErrorPage if the parameter is present but not
     * parseable as a long value.
     */
    public static Long parseOptionalLong(PageContext context,
                                         String param, Long defaultValue) {
        // Note that we may not want to be to strict here
        // as otherwise information could be lost.
        ArgumentNotValid.checkNotNull(context, "context");
        ArgumentNotValid.checkNotNullOrEmpty(param, "String param");

        Locale loc = HTMLUtils.getLocaleObject(context);
        String paramValue = context.getRequest().getParameter(param);
        if (paramValue != null && paramValue.trim().length() > 0) {
            paramValue = paramValue.trim();
            try {
                return NumberFormat.getInstance(loc).parse(paramValue).longValue();
            } catch (ParseException e) {
                forwardWithErrorMessage(context, I18N,
                        "errormsg;parameter.0.not.an.integer.1", param,
                        paramValue);
                throw new ForwardedToErrorPage("Invalid value " + paramValue
                        + " for integer parameter '" + param + "'", e);
            }
        } else {
            return defaultValue;
        }
    }

    /** Parse an optionally present date-value from a request parameter.
     *
     * @param context The context of the web request.
     * @param param The name of the parameter to parse
     * @param format The format of the date, in
     *               the format defined by SimpleDateFormat
     * @param defaultValue A value to return if the parameter is not present
     * (may be null)
     * @return Parsed value or default value if the parameter is missing
     * or empty. Null will only be returned if passed as the default value.
     * @throws ForwardedToErrorPage if the parameter is present but not
     * parseable as a date
     */
    public static Date parseOptionalDate(PageContext context, String param,
                                         String format, Date defaultValue) {
        ArgumentNotValid.checkNotNullOrEmpty(param, "String param");
        ArgumentNotValid.checkNotNullOrEmpty(format, "String format");
        String paramValue = context.getRequest().getParameter(param);
        if (paramValue != null && paramValue.trim().length() > 0) {
            paramValue = paramValue.trim();
            try {
                return new SimpleDateFormat(format).parse(paramValue);
            } catch (ParseException e) {
                forwardWithErrorMessage(context, I18N,
                            "errormsg;parameter.0.not.a.date.with.format.1.2",
                            param, format, paramValue);
                throw new ForwardedToErrorPage("Invalid value " + paramValue
                        + " for date parameter '" + param + "' with format '"
                        + format + "'", e);
            }
        } else {
            return defaultValue;
        }
    }

    public static String localiseLong(long i, PageContext context) {
        NumberFormat nf = NumberFormat.getInstance(
                HTMLUtils.getLocaleObject(context));
        return nf.format(i);
    }

    public static String localiseLong(long i, Locale locale) {
        NumberFormat nf = NumberFormat.getInstance(locale);
        return nf.format(i);
    }
}
