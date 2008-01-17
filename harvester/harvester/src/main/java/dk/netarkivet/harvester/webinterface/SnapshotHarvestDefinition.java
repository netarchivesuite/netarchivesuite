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

package dk.netarkivet.harvester.webinterface;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.FullHarvest;
import dk.netarkivet.harvester.datamodel.HarvestDefinition;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;

/**
 * Contains utility methods for supporting GUI for updating snapshot harvests.
 */
public class SnapshotHarvestDefinition {
    /** Default private constructor to avoid class being instantiated. */
    private SnapshotHarvestDefinition() {

    }

    /**
     * Extracts all required parameters from the request, checks for
     * any inconsistencies, and passes the requisite data to the
     * updateHarvestDefinition method for processing.  If the "update"
     * parameter is not set, this method does nothing.
     *
     * The parameters in the request are defined in
     * Definitions-edit-snapshot-harvest.jsp.
     *
     * @param context The context of the web request.
     * @param i18n Translation information
     * @throws ForwardedToErrorPage if an error happened that caused a forward
     * to the standard error page, in which case further JSP processing should
     * be aborted.
     */
    public static void processRequest(PageContext context, I18n i18n) {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");

        ServletRequest request = context.getRequest();
        if (request.getParameter(Constants.UPDATE_PARAM) == null) {
            return;
        }

        HTMLUtils.forwardOnEmptyParameter(context, Constants.HARVEST_PARAM);
        HTMLUtils.forwardOnMissingParameter(context,
                Constants.DOMAIN_LIMIT_PARAM, Constants.DOMAIN_BYTELIMIT_PARAM);

        String name = request.getParameter(Constants.HARVEST_PARAM);
        String comments = request.getParameter(Constants.COMMENTS_PARAM);

        long objectLimit = HTMLUtils.parseOptionalLong(context,
                Constants.DOMAIN_LIMIT_PARAM, dk.netarkivet.harvester
                .datamodel.Constants.DEFAULT_MAX_OBJECTS);
        long byteLimit = HTMLUtils.parseOptionalLong(context,
                Constants.DOMAIN_BYTELIMIT_PARAM,
            dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_BYTES);

        Long oldHarvestId = HTMLUtils.parseOptionalLong(context,
                Constants.OLDSNAPSHOT_PARAM, null);

        HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        if (oldHarvestId != null && !hddao.exists(oldHarvestId)) {
            HTMLUtils.forwardWithErrorMessage(context, i18n,
                    "errormsg;harvestdefinition.0.does.not.exist",
                    oldHarvestId);
            throw new ForwardedToErrorPage("Old harvestdefinition "
                    + oldHarvestId + " does not exist");
        }

        FullHarvest hd;
        if ((request.getParameter(Constants.CREATENEW_PARAM) != null)) {
            if (hddao.getHarvestDefinition(name) != null) {
                HTMLUtils.forwardWithErrorMessage(context, i18n,
                        "errormsg;harvest.definition.0.already.exists", name);
                throw new ForwardedToErrorPage("Harvest definition '" + name
                        + "' already exists");
            }
            // Note, object/bytelimit set to default values, if not set
            hd = new FullHarvest(name, comments, oldHarvestId, objectLimit,
                                 byteLimit);
            hd.setActive(false);
            hddao.create(hd);
        } else {
            hd = (FullHarvest) hddao.getHarvestDefinition(name);
            if (hd == null) {
                HTMLUtils.forwardWithErrorMessage(context, i18n,
                        "errormsg;harvest.0.does.not.exist", name);
                throw new UnknownID("Harvest definition '" + name
                        + "' doesn't exist!");
            }
            long edition = HTMLUtils.parseOptionalLong(context,
                    Constants.EDITION_PARAM, Constants.NO_EDITION);

            if (hd.getEdition() != edition) {
                HTMLUtils.forwardWithRawErrorMessage(context, i18n,
                        "errormsg;harvest.definition.changed.0.retry.1",
                        "<br/><a href=\"Definitions-edit-snapshot-harvest.jsp?"
                                + Constants.HARVEST_PARAM + "="
                                + HTMLUtils.encodeAndEscapeHTML(name)
                                + "\">",
                        "</a>");

                throw new ForwardedToErrorPage("Harvest definition '" + name
                        + "' has changed");
            }

            // MaxBytes is set to
            // dk.netarkivet.harvestdefinition.Constants.DEFAULT_MAX_BYTES
            // if parameter byteLimit is not defined
            hd.setMaxBytes(byteLimit);

            // MaxCountObjects is set to
            // dk.netarkivet.harvestdefinition.Constants.DEFAULT_MAX_OBJECTS
            // if parameter objectLimit is not defined
            hd.setMaxCountObjects(objectLimit);
            hd.setPreviousHarvestDefinition(oldHarvestId);
            hd.setComments(comments);
            hddao.update(hd);
        }
    }

    /** Flip the active status of a harvestdefinition named in the
     * "flipactive" parameter.
     *
     * @param context The context of the web servlet
     * @param i18n Translation information
     * @return True if a harvest definition changed state.
     */
    public static boolean flipActive(PageContext context, I18n i18n) {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");

        ServletRequest request = context.getRequest();
        String flipactive = request.getParameter(Constants.FLIPACTIVE_PARAM);
        // Change activation if requested
        if (flipactive != null) {
            HarvestDefinitionDAO dao = HarvestDefinitionDAO.getInstance();
            HarvestDefinition hd = dao.getHarvestDefinition(flipactive);
            if (hd != null) {
                hd.setActive(!hd.getActive());
                dao.update(hd);
                return true;
            } else {
                HTMLUtils.forwardWithErrorMessage(context, i18n,
                        "errormsg;harvestdefinition.0.does.not.exist",
                        flipactive);
                throw new ForwardedToErrorPage("Harvest definition "
                        + flipactive + " doesn't exist");
            }
        }
        return false;
    }
}
