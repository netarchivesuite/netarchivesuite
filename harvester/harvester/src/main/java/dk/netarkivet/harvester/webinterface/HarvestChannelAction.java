/* File:        $Id: HarvestChannelAction.java 2116 2011-10-21 18:04:53Z svc $
 * Revision:    $Revision: 2116 $
 * Author:      $Author: svc $
 * Date:        $Date: 2011-10-21 20:04:53 +0200 (Fri, 21 Oct 2011) $
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.datamodel.HarvestChannelDAO;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;

/**
 * Abstract class representing an action to take on the collection of global
 * crawler traps.
 *
 */

public abstract class HarvestChannelAction {
	
	public static enum ActionType {
		createHarvestChannel,
		mapHarvestToChannel
	}
	
	public final static String ACTION = "channelAction";
	public final static String CHANNEL_NAME = "channelName";
	public final static String HARVEST_ID = "harvestId";
	public final static String CHANNEL_ID = "channelId";
	public final static String SNAPSHOT = "channelIsSnapshot";
	public final static String COMMENTS = "channelComments";

    private static final Log log =
            LogFactory.getLog(HarvestChannelAction.class);

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
    public static void processRequest(PageContext context, I18n i18n) 
    		throws ForwardedToErrorPage {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");
        log.debug("Processing request");
        HttpServletRequest request = (HttpServletRequest) context.getRequest();
        try {
        	String action = request.getParameter(ACTION);
        	if (action == null || action.isEmpty()) {
            	return;
            }            
            switch (ActionType.valueOf(action)) {
            	case createHarvestChannel:
            		String name = request.getParameter(CHANNEL_NAME);
            		ArgumentNotValid.checkNotNullOrEmpty(name, "harvest channel name");
            		HarvestChannelDAO dao = HarvestChannelDAO.getInstance();
            		dao.create(new HarvestChannel(name, request.getParameter(COMMENTS), false));
            		break;
            	case mapHarvestToChannel:
            		long harvestId = Long.parseLong(request.getParameter(HARVEST_ID));
            		long channelId = Long.parseLong(request.getParameter(CHANNEL_ID));
            		HarvestChannelDAO hcDao = HarvestChannelDAO.getInstance();
            		HarvestDefinitionDAO hdDao = HarvestDefinitionDAO.getInstance();
            		hdDao.mapToHarvestChannel(harvestId, hcDao.getById(channelId));
            		break;
            	default: 
            }
        } catch (Throwable e) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, e,
                                         "errormsg;harvest.channel.create.error");
            throw new ForwardedToErrorPage("Error in Harvest Channels", e);
        }
    }

}
