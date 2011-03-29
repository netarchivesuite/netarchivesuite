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
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import java.io.IOException;
import java.io.OutputStream;

import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapList;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDAO;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDBDAO;

/**
 * Class to read and return a global crawler trap list to a web request.
 *
 */

public class TrapReadAction extends TrapAction {
    @Override
    protected void doAction(PageContext context, I18n i18n) {
        HttpServletRequest request = (HttpServletRequest) context.getRequest();
        int trapId = Integer.parseInt(request.getParameter(Constants.TRAP_ID));
        String contentType = request.getParameter(Constants.TRAP_CONTENT_TYPE);
        HttpServletResponse response = (HttpServletResponse)
                context.getResponse();
        GlobalCrawlerTrapListDAO dao = GlobalCrawlerTrapListDBDAO.getInstance();
        GlobalCrawlerTrapList trapList = dao.read(trapId);
        response.setHeader("Content-Type", contentType);
        if (contentType.startsWith("binary")) {
            response.setHeader("Content-Disposition", "Attachment; filename="
                                                      +  trapList.getName());
        }
        OutputStream out = null;
        try {
            out = response.getOutputStream();
            for (String trap : trapList.getTraps()) {
                out.write((trap + "\n").getBytes());
            }
            out.close();
        } catch (IOException e) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, e, "");
            throw new ForwardedToErrorPage("Error in retrieving trap list", e);
        }
    }
}
