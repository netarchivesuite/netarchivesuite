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

package dk.netarkivet.viewerproxy.webinterface;

import javax.servlet.http.HttpServletRequest;

import dk.netarkivet.common.webinterface.SiteSection;
import dk.netarkivet.viewerproxy.Constants;

/**
 * Site section that creates the menu for QA.
 *
 */
public class QASiteSection extends SiteSection {
    /**
     * Create a QA SiteSection object.
     *
     * This initialises the SiteSection object with the page(!) that exists in
     * QA.
     */
    public QASiteSection() {
        super("sitesection;qa", "QA", 1,
              new String[][]{
                      {"status", "pagetitle;qa.status"}
              }, "QA",
                 Constants.TRANSLATIONS_BUNDLE);
    }

    /** Create a return-url for the QA pages that takes one.
     *
     * The current implementation is hokey, but trying to go through URL
     * objects is a mess.
     *
     * @param request The request that we have been called with.
     * @return A URL object that leads to the QA-status page on the same
     * machine as the request came from.
     */
    public static String createQAReturnURL(HttpServletRequest request) {
        return request.getRequestURL().toString().replaceAll(
                "/[^/]*\\.jsp.*$", "/QA-status.jsp");
    }

    /** No initialisation necessary in this site section. */
    public void initialize() {
    }

    /** No cleanup necessary in this site section. */
    public void close() {
    }
}
