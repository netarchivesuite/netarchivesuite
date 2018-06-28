/*
 * #%L
 * Netarchivesuite - common
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

package dk.netarkivet.common.webinterface;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.utils.Settings;

/**
 * This class holds information about one section of the site, including information about what to put in the menu
 * sidebar and how to determine which page you're in.
 */
public abstract class SiteSection {

    private static final Logger log = LoggerFactory.getLogger(SiteSection.class);

    /** The overall human-readable name of the section. */
    private final String mainname;
    /** The number of pages that should be visible in the sidebar. */
    private final int visiblePages;
    /** The map of page names ("path" part of URL) to page titles. */
    private final LinkedHashMap<String, String> pagesAndTitles = new LinkedHashMap<String, String>();
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
     *
     * @see #getSections()
     */
    private static List<SiteSection> sections;

    /**
     * Create a new SiteSection object.
     *
     * @param mainname The name of the entire section used in the sidebar.
     * @param prefix The prefix that all the JSP pages will have.
     * @param visiblePages How many of the pages will be visible in the menu (taken from the start of the list).
     * @param pagesAndTitles The actual pages and title-labels, without the prefix and jsp extension, involved in the
     * section. They must be given as an array of 2-element arrays.
     * @param dirname The top level directory this site section is deployed under.
     * @param bundle The resource bundle with translations of this sitesection.
     * @throws ArgumentNotValid if any of the elements of pagesAndTitles are not a 2-element array.
     */
    public SiteSection(String mainname, String prefix, int visiblePages, String[][] pagesAndTitles, String dirname,
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
                throw new ArgumentNotValid("Must have exactly page and title in " + prefix);
            }
            // Handle links outside webpages directory
            // Assume pageurl ending with / as external url requiring no prefix
            String pageurl= prefix + "-" + pageAndTitle[0] + JSP_EXTENSION;
            if (pageAndTitle[0].endsWith("/")) {
                pageurl = pageAndTitle[0]; // Add no prefix
            }
            String pagelabel = pageAndTitle[1];
            this.pagesAndTitles.put(pageurl, pagelabel);
        }
    }

    /**
     * Given a URL, returns the corresponding page title.
     *
     * @param req the HTTP request object to respond to
     * @param url a given URL.
     * @param locale the current locale.
     * @return the corresponding page title, or null if it is not in this section, or is null.
     * @throws ArgumentNotValid on null locale.
     */
    public String getTitle(HttpServletRequest req, String url, Locale locale) {
        ArgumentNotValid.checkNotNull(locale, "Locale locale");
        String page = getPage(req, url);
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

    /**
     * Returns the page name from a URL, if the page is in this hierarchy, null otherwise.
     *
     * @param req the HTTP request object to respond to
     * @param url Url to check
     * @return Page name, or null for not in this hierarchy.
     */
    private String getPage(HttpServletRequest req, String url) {
        String contextPath;
        URL parsed = null;
        try {
            parsed = new URL(url);
        } catch (MalformedURLException e) {
            return null;
        } catch (NullPointerException e) {
            return null;
        }
        String path = parsed.getPath();
        String page = null;
        int index = path.lastIndexOf(dirname + "/");
        if (index != -1) {
            page = path.substring(index + dirname.length() + 1);
        } else if (req != null) {
            contextPath = req.getContextPath();
            if (url.startsWith('/' + contextPath + '/')) {
                // Context path is only /path.
                page = url.substring(contextPath.length() + 2);
            }
        }
        if (page != null) {
            index = page.indexOf('/');
            if (index != -1) {
                page = page.substring(0, index + 1);
            }
        }
        return page;
    }

    /**
     * Given a URL, returns the path part without schema, context path and query string.
     * 
     * @param req the HTTP request object to respond to
     * @param url a given URL.
     * @return the path part of a URL without the schema, context path and query string
     */
    public String getPath(HttpServletRequest req, String url) {
        URL parsed;
        String tmpPath;
        int index;
        String contextPath;
        String path;
        try {
            parsed = new URL(url);
        } catch (MalformedURLException e) {
            return null;
        } catch (NullPointerException e) {
            return null;
        }
        tmpPath = parsed.getPath();
        index = tmpPath.indexOf('?');
        if (index != -1) {
            tmpPath = tmpPath.substring(0, index);
        }
        path = null;
        index = tmpPath.lastIndexOf(dirname + "/");
        if (index != -1) {
            path = tmpPath.substring(index + dirname.length() + 1);
        } else if (req != null) {
            contextPath = req.getContextPath();
            if (url.startsWith('/' + contextPath + '/')) {
                // Context path is only /path.
                path = url.substring(contextPath.length() + 2);
            }
        }
        return path;
    }

    /**
     * Returns the first sub path of a given path without schema, context path and query string.
     * 
     * @param page a processed path without schema, context path and query string
     * @return
     */
    public String getPageFromPage(String page) {
        int index;
        if (page != null) {
            index = page.indexOf('/');
            if (index != -1) {
                page = page.substring(0, index + 1);
            }
        }
        return page;
    }

    /**
     * Generate this section's part of the navigation tree (sidebar). This outputs balanced HTML to the JspWriter. It
     * uses a locale to generate the right titles.
     *
     * @param out A place to write our HTML
     * @param url The url of the page we're currently viewing. The list of subpages will only be displayed if the page
     * we're viewing is one that belongs to this section.
     * @param locale The locale to generate the navigation tree for.
     * @throws IOException If there is a problem writing to the page.
     */
    public void generateNavigationTree(StringBuilder sb, HttpServletRequest req, String url, String subMenu, Locale locale) throws IOException {
        String firstPage = pagesAndTitles.keySet().iterator().next();
        sb.append("<tr>");
        sb.append("<td><a href=\"/");
        sb.append(HTMLUtils.encode(dirname));
        sb.append("/");
        sb.append(HTMLUtils.encode(firstPage));
        sb.append("\">");
        sb.append(HTMLUtils.escapeHtmlValues(I18n.getString(bundle, locale, mainname)));
        sb.append("</a></td>\n");
        sb.append("</tr>");
        // If we are on the above page or one of its subpages, display the
        // next level down in the tree
        String path = getPath(req, url);
        String page = getPageFromPage(path);
        if (page == null) {
            return;
        }
        if (pagesAndTitles.containsKey(page)) {
            int i = 0;
            String link;
            for (Map.Entry<String, String> pageAndTitle : pagesAndTitles.entrySet()) {
                if (i == visiblePages) {
                    break;
                }
                link = pageAndTitle.getKey();
                sb.append("<tr>");
                sb.append("<td>&nbsp; &nbsp; <a href=\"/");
                sb.append(HTMLUtils.encode(dirname));
                sb.append("/");
                sb.append(link);
                sb.append("\"> ");
                if (path.equals(link)) {
                    sb.append("<b>");
                    sb.append(HTMLUtils.escapeHtmlValues(I18n.getString(bundle, locale, pageAndTitle.getValue())));
                    sb.append("</b>");
                } else {
                    sb.append(HTMLUtils.escapeHtmlValues(I18n.getString(bundle, locale, pageAndTitle.getValue())));
                }
                sb.append("</a></td>");
                sb.append("</tr>\n");
                if (subMenu != null && page != null && path.startsWith(link) && path.length() > link.length()) {
                    sb.append(subMenu);
                }
                ++i;
            }
        }
    }

    /**
     * Return the directory name of this site section.
     *
     * @return The dirname.
     */
    public String getDirname() {
        return dirname;
    }

    /**
     * Called when the site section is first deployed. Meant to be overridden by subclasses.
     */
    public abstract void initialize();

    /**
     * Called when webserver shuts down. Meant to be overridden by subclasses.
     */
    public abstract void close();

    /**
     * The list of sections of the website. Each section has a number of pages, as defined in the sitesection classes
     * read from settings. These handle outputting their HTML part of the sidebar, depending on where in the site we
     * are.
     *
     * @return A list of site sections instantiated from settings.
     * @throws IOFailure if site sections cannot be read from settings.
     */
    public static synchronized List<SiteSection> getSections() {
        if (sections == null) {
            sections = new ArrayList<>();
            String[] sitesections = Settings.getAll(CommonSettings.SITESECTION_CLASS);
            log.debug("Loading {} site section(s).", sitesections.length);
            for (String sitesection : sitesections) {
                log.debug("Loading site section {}.", sitesection.toString());
                try {
                    ClassLoader loader = SiteSection.class.getClassLoader();
                    sections.add((SiteSection) loader.loadClass(sitesection).newInstance());
                } catch (Exception e) {
                    log.warn("Error loading class {}.", sitesection, e);
                    throw new IOFailure("Cannot read site section " + sitesection + " from settings", e);
                }
            }
        }
        return sections;
    }

    /**
     * Clean up site sections. This method calls close on all deployed site sections, and resets the list of site
     * sections.
     */
    public static synchronized void cleanup() {
        if (sections != null) {
            for (SiteSection section : sections) {
                section.close();
            }
        }
        sections = null;

    }

    /**
     * Check whether a section with a given dirName is deployed.
     *
     * @param dirName The dirName to check for
     * @return True of deployed, false otherwise.
     * @throws ArgumentNotValid if dirName is null or empty.
     */
    public static boolean isDeployed(String dirName) {
        ArgumentNotValid.checkNotNullOrEmpty(dirName, "String dirName");
        boolean sectionDeployed = false;
        for (SiteSection section : getSections()) {
            if (section.getDirname().equals(dirName)) {
                sectionDeployed = true;
            }
        }
        return sectionDeployed;
    }
}
