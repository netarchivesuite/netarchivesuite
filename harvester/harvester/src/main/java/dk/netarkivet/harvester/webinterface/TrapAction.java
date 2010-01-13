/* File:        $Id: License.txt,v $
 * Revision:    $Revision: 1.4 $
 * Author:      $Author: csr $
 * Date:        $Date: 2005/04/11 16:29:16 $
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

import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.I18n;

/**
 * csr forgot to comment this!
 *
 * @author csr
 * @since Jan 13, 2010
 */

public abstract class TrapAction {

    private static final Log log =
            LogFactory.getLog(TrapAction.class);

    /**
     * This method processes the request to determine which action it
     * corresponds to and pass the request along accordingly. If it
     * is a multipart post then it is passed along to a create-or-update
     * instance. Otherwise if no action is specified, none is taken.
     * Otherwise the request is passed on to a specific concrete instance
     * of this class for further processing.
     *
     * @param context
     * @param i18n
     */
    public static void processRequest(PageContext context, I18n i18n) {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");
        log.debug("Processing request");
        HttpServletRequest request = (HttpServletRequest) context.getRequest();
        if (ServletFileUpload.isMultipartContent(request)) {
            TrapActionEnum.CREATE_OR_UPDATE.getTrapAction().doAction(context, i18n);
        } else {
            String requestType = request.getParameter(Constants.TRAP_ACTION);
            if (requestType == null || "".equals(requestType)) {
                TrapActionEnum.NULL_ACTION.getTrapAction().doAction(context, i18n);
            } else {
                TrapActionEnum actionType = TrapActionEnum.valueOf(requestType);
                actionType.getTrapAction().doAction(context, i18n);
            }
        }
    }

    protected abstract void doAction(PageContext context, I18n i18n);

}
