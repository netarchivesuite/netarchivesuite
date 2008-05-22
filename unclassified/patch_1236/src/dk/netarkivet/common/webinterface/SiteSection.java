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

package dk.netarkivet.common.webinterface;

import javax.servlet.jsp.JspWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.I18n;

/**
 * This class holds information about one section of the site, including
 * information about what to put in the menu sidebar and how to determine
 * which page you're in.
 *
 */
public abstract class SiteSection {
    /** The overall human-readable name of the section. */
    private final String mainname;
    /** The number of pages that should be visible in the sidebar. */
    private final int visiblePages;
    /** The map of page names ("path" part of URL) to page titles */
    private final LinkedHashMap<String, String> pagesAndTitles
            = new LinkedHashMap<String, String>();
    /** The top level directory this section represents. */
    private String dirname;
    /** The resource bundle with translations of this sitesection. */
    private String bundle;
    /**
     * Extension used for JSP files, including '.' separator.
     */
    private static final String JSP_EXTENSION = ".jsp";
    /**
     * Loaded list of site sections.
     * @see #getSections()
     */
    private static List<SiteSection> sections;

    /** Create a new SiteSection object.
     *
     * @param mainname The name of the entire section used in the sidebar.
     * @param prefix The prefix that all the JSP pages will have.
     * @param visiblePages How many of the pages will be visible in the menu
     * (taken from the start of the list).
     * @param pagesAndTitles The actual pages and title-labels, without the
     * prefix and jsp extension, involved in the section.  They must be given as
     * an array of 2-element arrays.
     * @param dirname The top level directory this site section is deployed
     * under.
     * @param bundle The resource bundle with translations of this sitesection.
     * @throws ArgumentNotValid if any of the elements of pagesAndTitles are
     * not a 2-element array.
     */
    public SiteSection(String mainname, String prefix, int visiblePages,
                       String[][] pagesAndTitles, String dirname,
                       String bundle) {
        ArgumentNotValid.checkNotNullOrEmpty(mainname, "mainname");
        ArgumentNotValid.checkNotNullOrEmpty(prefix, "prefix");
        ArgumentNotValid.checkNotNegative(visiblePages, "visiblePages");
        ArgumentNotValid.checkNotNull(pagesAndTitles, "String[][] pagesAndTitles");
        ArgumentNotValid.checkNotNullOrEmpty(dirname, "dirname");
        ArgumentNotValid.checkNotNull(bundle, "String bundle");
        this.dirname = dirname;
        this.mainname = mainname;
        this.visiblePages = visiblePages;
        this.bundle = bundle;
        for (String[] pageAndTitle : pagesAndTitles) {
            if (pageAndTitle.length != 2) {
                throw new ArgumentNotValid("Must have exactly page and title in "
                        + prefix);
            }
            this.pagesAndTitles.put(prefix + "-" +
                    pageAndTitle[0] + JSP_EXTENSION, pageAndTitle[1]);
        }
    }

    /**
     * Given a URL, returns the corresponding page title.
     *
     * @param url a given URL.
     * @param locale the current locale.
     * @return the corresponding page title, or null if it is not in this
     * section, or is null.
     * @throws ArgumentNotValid on null locale.
     */
    public String getTitle(String url, Locale locale) {
        ArgumentNotValid.checkNotNull(locale, "Locale locale");
        String page = getPage(url);
        if (page == null) {
            return null;
        }
        String label = pagesAndTitles.get(page);
        if (label == null) {
            return null;
        } else {
            return I18n.getString(bundle, locale, label);
        }
    }

    /** Generate this section's part of the navigation tree (sidebar).  This
     * outputs balanced HTML to the JspWriter. It uses a locale to generate the
     * right titles.
     *
     * @param out A place to write our HTML
     * @param url The url of the page we're currently viewing.  The
     * list of subpages will only be displayed if the page we're viewing is one
     * that belongs to this section.
     * @param locale The locale to generate the navigation tree for.
     * @throws IOException If there is a problem writing to the page.
     */
    public void generateNavigationTree(JspWriter out,
                                       String url, Locale locale)
            throws IOException {
        String firstPage = pagesAndTitles.keySet().iterator().next();
        out.print("<tr>");
        out.print("<td><a href=\"/" + HTMLUtils.encode(dirname)
                  + "/" + HTMLUtils.encode(firstPage) + "\">"
                  + HTMLUtils.escapeHtmlValues(
                I18n.getString(bundle, locale, mainname))
                  + "</a></td>\n");
        out.print("</tr>");
        // If we are on the above page or one of its subpages, display the
        // next level down in the tree
        String page = getPage(url);
        if (page == null) {
            return;
        }
        if (pagesAndTitles.containsKey(page)) {
            int i = 0;
            for (Map.Entry<String, String> pageAndTitle :
                    pagesAndTitles.entrySet()) {
                if (i == visiblePages) {
                    break;
                }
                out.print("<tr>");
                out.print("<td>&nbsp; &nbsp; <a href=\"/"
                          + HTMLUtils.encode(dirname) + "/"
                          + HTMLUtils.encode(pageAndTitle.getKey()) + "\"> "
                          + HTMLUtils.escapeHtmlValues(
                        I18n.getString(bundle, locale, pageAndTitle.getValue()))
                          + "</a></td>\n");
                out.print("</tr>");
                i++;
            }
        }
    }

    /** Returns the page name from a URL, if the page is in this hierarchy, null
     * otherwise.
     * @param url Url to check
     * @return Page name, or null for not in this hierarchy.
     */
    private String getPage(String url) {
        URL parsed = null;
        try {
            parsed = new URL(url);
        } catch (MalformedURLException e) {
            return null;
        } catch (NullPointerException e) {
            return null;
        }
        String path = parsed.getPath();
        String page;
        int index = path.lastIndexOf(dirname + "/");
        if (index == -1) {
            return null;
        }
        page = path.substring(index + dirname.length() + 1);
        return page;
    }

    /**
     * Return the directory name of this site section.
     * @return The dirname.
     */
    public String getDirname() {
        return dirname;
    }

    /** Called when the site section is first deployed.
     * Meant to be overridden by subclasses. */
    public abstract void initialize();

    /** Called when webserver shuts down.
     * Meant to be overridden by subclasses. */
    public abstract void close();

    /**
     * The list of sections of the website.  Each section has a number of pages,
     * as defined in the sitesection classes read from settings. These handle
     * outputting their HTML part of the sidebar, depending on where in the site
     * we are.
     *
     * @return A list of site sections instantiated from settings.
     *
     * @throws IOFailure if site sections cannot be read from settings.
     */
    public static synchronized List<SiteSection> getSections() {
        if (sections == null) {
            sections = new ArrayList<SiteSection>();
            String[] sitesections = Settings.getAll(Settings.SITESECTION_CLASS);
            for (String sitesection : sitesections) {
                try {
                    ClassLoader loader
                            = SiteSection.class.getClassLoader();
                    sections.add((SiteSection) loader.loadClass(
                            sitesection).newInstance());
                } catch (Exception e) {
                    throw new IOFailure(
                            "Cannot read site section from settings", e);
                }
            }
        }
        return sections;
    }

    /**
     * Clean up sitesections.
     * This method calls close on all deployed site sections, and resets the
     * list of site sections.
     */
    public static synchronized void cleanup() {
        if (sections != null) {
            for (SiteSection section : sections) {
                section.close();
            }
        }
        sections = null;

    }

    /** Check whether a section with a given dirName is deployed.
     *
     * @param dirName The dirName to check for
     * @return True of deployed, false otherwise.
     * @throws ArgumentNotValid if dirName is null or empty.
     */
    public static boolean isDeployed(String dirName) {
        ArgumentNotValid.checkNotNullOrEmpty(dirName, "String dirName");
        boolean sectionDeployed = false;
        List<SiteSection> sections = getSections();
        for (SiteSection section : sections) {
            if (section.getDirname().equals(dirName)) {
                sectionDeployed = true;
            }
        }
        return sectionDeployed;
    }
}
