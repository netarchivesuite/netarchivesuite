/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * Copyright Det Kongelige Bibliotek og Statsbiblioteket, Danmark
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package dk.netarkivet.harvester.webinterface;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;

/**
 * Abstract class representing an action to take on the collection of global
 * crawler traps.
 *
 */

public abstract class TrapAction {

    private static final Log log =
            LogFactory.getLog(TrapAction.class);

    /**
     * This method processes the request to determine which action it
     * corresponds to and passes the request along accordingly. If it
     * is a multipart post then it is passed along to a create-or-update
     * instance. Otherwise if no action is specified, none is taken.
     * Otherwise the request is passed on to a specific concrete instance
     * of this class for further processing.
     *
     * @param context the original servlet context of the request.
     * @param i18n the internationalisation to be used.
     * @throws ForwardedToErrorPage if an exception is thrown while carrying
     * out the action.
     */
    public static void processRequest(PageContext context, I18n i18n) throws
                                                       ForwardedToErrorPage {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");
        log.debug("Processing request");
        HttpServletRequest request = (HttpServletRequest) context.getRequest();
        try {
            if (ServletFileUpload.isMultipartContent(request)) {
                TrapActionEnum.CREATE_OR_UPDATE.getTrapAction()
                        .doAction(context, i18n);
            } else {
                String requestType = request.getParameter(Constants.TRAP_ACTION);
                if (requestType == null || requestType.isEmpty()) {
                    TrapActionEnum.NULL_ACTION.getTrapAction()
                            .doAction(context, i18n);
                } else {
                    TrapActionEnum actionType =
                            TrapActionEnum.valueOf(requestType);
                    actionType.getTrapAction().doAction(context, i18n);
                }
            }
        } catch (Throwable e) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, e,
                                         "errormsg;crawlertrap.action.error");
            throw new ForwardedToErrorPage("Error in Global Crawler Traps", e);
        }
    }

    /**
     * Method implementing the specific action to take
     * @param context the context of the servlet request triggering this action.
     * @param i18n the internationalisation to use for presenting the results.
     */
    protected abstract void doAction(PageContext context, I18n i18n);

}
