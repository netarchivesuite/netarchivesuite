/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

package dk.netarkivet.harvester.webinterface;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.Domain;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.SeedList;

/**
 * Contains utility methods for updating seedlists from the GUI.
 *
 */

public class DomainSeedsDefinition {
    /**
     * Private constructor. No sense in initialising this class.
     */
    private DomainSeedsDefinition() {

    }

    /**
     * Utility class gathering together data relating to the editing
     * of a seed list.
     */
    public static class UrlInfo {
        private String urlListName;
        private String seedList;

        public UrlInfo(String urlListName, String seedList) {
            this.urlListName = urlListName;
            this.seedList = seedList;
        }

        public String getUrlListName() {
            return urlListName;
        }
        public String getSeedList() {
            return seedList;
        }
    }

    /**
     * Extracts information from a servlet request to update seedlists in a
     * domain
     *
     * editUrlList: if not null, we are editing, not updating so return
     *
     * (urlListName, seedlist) The name of a seedlist and the actual seedlist
     * for a seedlist to be updated.
     * If urlListName is present and non-empty, seedlist must also
     * be non-empty.
     *
     * @param context
     * @param i18n
     */
    public static void processRequest(PageContext context, I18n i18n) {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");

        ServletRequest request = context.getRequest();
        if (request.getParameter(Constants.UPDATE_PARAM) == null) {
            return;
        }

        HTMLUtils.forwardOnEmptyParameter(context,
                Constants.DOMAIN_PARAM, Constants.URLLIST_NAME_PARAM,
                Constants.SEED_LIST_PARAMETER);

        String name = request.getParameter(Constants.DOMAIN_PARAM).trim();
        String urlListName
                = request.getParameter(Constants.URLLIST_NAME_PARAM).trim();
        String seedList
                = request.getParameter(Constants.SEED_LIST_PARAMETER).trim();

        // check the edition number before updating
        long edition = HTMLUtils.parseOptionalLong(context,
                Constants.EDITION_PARAM, -1L);

        if (!DomainDAO.getInstance().exists(name)) {
            HTMLUtils.forwardWithErrorMessage(context, i18n,
                    "errormsg;unknown.domain.0", name);
            throw new ForwardedToErrorPage("Domain '" + name
                    + "' does not exist");
        }

        Domain domain = DomainDAO.getInstance().read(name);

        if (domain.getEdition() != edition) {
            HTMLUtils.forwardWithRawErrorMessage(context, i18n,
                    "errormsg;domain.definition.changed.0.retry.1",
                    "<br/><a href=\"Definitions-edit-domain.jsp?"
                            + Constants.DOMAIN_PARAM + "="
                            + HTMLUtils.escapeHtmlValues(HTMLUtils.encode(name))
                            + "\">",
                    "</a>");
            throw new ForwardedToErrorPage("Domain '" + name + "' has changed");
        }

        UrlInfo urlInfo = new UrlInfo(urlListName, seedList);
        String comments = request.getParameter(Constants.COMMENTS_PARAM);
        updateDomain(domain, urlInfo, comments);
    }

    /** Update a domain from given (checked) seedlist data.
     *
     * @param domain The domain to update.
     * @param urlInfo The seedlist to update
     * @param comments Any comments for this seedlist.
     */
    private static void updateDomain(Domain domain, UrlInfo urlInfo,
                                     String comments) {
        //Update/create seedlist
        String seedlistName = urlInfo.getUrlListName();
        SeedList sl = new SeedList(seedlistName, urlInfo.getSeedList());
        if (comments != null) {
            sl.setComments(comments);
        }
        if (domain.hasSeedList(seedlistName)) {
            domain.updateSeedList(sl);
        } else {
            domain.addSeedList(sl);
        }
        DomainDAO.getInstance().update(domain);
    }
}
