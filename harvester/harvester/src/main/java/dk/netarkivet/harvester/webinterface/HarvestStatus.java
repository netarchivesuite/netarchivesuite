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

import javax.servlet.jsp.PageContext;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.JobDAO;

/**
 * This page provides support for the HarvestStatus pages of the web interface.
 */

public class HarvestStatus {
    /**
     * Process a request from Harveststatus-alljobs.
     *
     * Will resubmit a job if requested, otherwise do nothing.
     *
     * @param context The web context used for processing
     * @param i18n The resource i18n context.
     * @throws ForwardedToErrorPage If an error occurs that stops processing
     * and forwards the user to an error page.
     */
    public static void processRequest(PageContext context, I18n i18n)
            throws ForwardedToErrorPage {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");

        Long jobID = HTMLUtils.parseOptionalLong(context,
                Constants.JOB_RESUBMIT_PARAM, null);
        if ((jobID != null)) {
            try {
                JobDAO.getInstance().rescheduleJob(jobID);
            } catch (UnknownID e) {
                HTMLUtils.forwardWithErrorMessage(context, i18n,
                        "errormsg;job.unknown.id.0", jobID);
                throw new ForwardedToErrorPage("Job " + jobID + " not found");
            } catch (IOFailure e) {
                HTMLUtils.forwardWithErrorMessage(context, i18n, e,
                        "errormsg;job.unable.to.resubmit.id.0", jobID);
                throw new ForwardedToErrorPage("Error resubmitting job "
                        + jobID);
            }
        }
    }

    /** Create a link to the harvest-run page for a given run
     *
     * @param harvestID The ID of the harvest
     * @param harvestRun The run # of the harvest (always 0 for snapshots)
     * @return A properly encoded HTML string with a link and the harvest run
     * as the text.
     */
    public static String makeHarvestRunLink(long harvestID, int harvestRun) {
        ArgumentNotValid.checkNotNegative(harvestID, "harvestID");
        ArgumentNotValid.checkNotNegative(harvestRun, "harvestRun");
        return "<a href=\"/History/Harveststatus-perharvestrun.jsp?harvestID="
                + harvestID + "&amp;harvestNum=" + harvestRun + "\">"
                + harvestRun + "</a>";
    }
}
