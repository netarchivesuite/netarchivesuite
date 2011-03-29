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

import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapList;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDAO;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDBDAO;

/**
 * Action class for changing the activation status of a crawler trap list
 *
 */

public class TrapActivationAction extends TrapAction {

    /**
     * The new activation state for the trap list.
     */
    private boolean newActivationState;

    /**
     * Constructor specifying whether this actions activates or deactivates
     * a trap list.
     * @param newActivationState the new activation state.
     */
    public TrapActivationAction(boolean newActivationState) {
        this.newActivationState = newActivationState;
    }

    @Override
    protected void doAction(PageContext context, I18n i18n) {
        HttpServletRequest request = (HttpServletRequest) context.getRequest();
        int trapId = Integer.parseInt(request.getParameter(Constants.TRAP_ID));
        GlobalCrawlerTrapListDAO dao = GlobalCrawlerTrapListDBDAO.getInstance();
        GlobalCrawlerTrapList trapList = dao.read(trapId);
        trapList.setActive(newActivationState);
        dao.update(trapList);
    }
}
