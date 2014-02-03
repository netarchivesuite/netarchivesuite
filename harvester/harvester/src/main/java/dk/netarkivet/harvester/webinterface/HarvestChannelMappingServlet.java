/* File:        $Id: HarvestChannelMappingServlet.java 2116 2011-10-21 18:04:53Z svc $
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

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hp.gagawa.java.elements.A;
import com.hp.gagawa.java.elements.Form;
import com.hp.gagawa.java.elements.Input;
import com.hp.gagawa.java.elements.Option;
import com.hp.gagawa.java.elements.Select;

import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.harvester.dao.HarvestChannelDAO;
import dk.netarkivet.harvester.datamodel.HarvestChannel;
import dk.netarkivet.harvester.webinterface.HarvestChannelAction.ActionType;

/**
 * This class process an Ajax call from the UI to generate a form that allows to map
 * a harvest to a channel.
 *
 */
public class HarvestChannelMappingServlet extends HttpServlet {
	
	private static final I18n I18N = new I18n(
			dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
	
	public static enum Param {
		harvestId,
		snapshot,
		currentChannelId
	}

	@Override
	protected void doPost(
			HttpServletRequest req, 
			HttpServletResponse resp)
			throws ServletException, IOException {
		
		long harvestId = Long.parseLong(req.getParameter(Param.harvestId.name()));
		boolean snapshot = Boolean.parseBoolean(req.getParameter(Param.snapshot.name()));
		
		Form selectChannelForm = new Form("./HarvestChannel-edit-harvest-mappings.jsp");
    	selectChannelForm.setMethod("post");
    	
    	Input hiddenAction = new Input();
    	hiddenAction.setType("hidden");
    	hiddenAction.setId(HarvestChannelAction.ACTION);
    	hiddenAction.setName(HarvestChannelAction.ACTION);
    	hiddenAction.setValue(ActionType.mapHarvestToChannel.name());
    	selectChannelForm.appendChild(hiddenAction);
    	
    	Input hiddenHarvestId = new Input();
    	hiddenHarvestId.setType("hidden");
    	hiddenHarvestId.setId(HarvestChannelAction.HARVEST_ID);
    	hiddenHarvestId.setName(HarvestChannelAction.HARVEST_ID);
    	hiddenHarvestId.setValue(Long.toString(harvestId));
    	selectChannelForm.appendChild(hiddenHarvestId);
    	
    	Select selectChannel = new Select();
    	selectChannel.setId(HarvestChannelAction.CHANNEL_ID);
    	selectChannel.setName(HarvestChannelAction.CHANNEL_ID);
    	
    	HarvestChannelDAO dao = HarvestChannelDAO.getInstance();
    	
    	HarvestChannel mappedChan = dao.getChannelForHarvestDefinition(harvestId);
    	if (mappedChan == null) {
    		mappedChan = dao.getDefaultChannel(snapshot);
    	}
    	long mappedChanId = mappedChan.getId();
    	
    	Iterator<HarvestChannel> chans = dao.getAll(snapshot);
    	while (chans.hasNext()) {
    		HarvestChannel chan = chans.next();
    		Option opt = new Option();
    		long id = chan.getId();
    		opt.setValue(Long.toString(id));
    		opt.appendText(chan.getName());
    		if (id == mappedChanId) {
    			opt.setSelected("selected");
    		}
    		selectChannel.appendChild(opt);
    	}    	
    	selectChannelForm.appendChild(selectChannel);
    	
    	Input submit = new Input();
    	submit.setType("submit");
    	submit.setValue(I18N.getString(
    			resp.getLocale(), 
    			"edit.harvest.mappings.dialog.submit"));
    	selectChannelForm.appendChild(submit);
    	
    	A cancelLink = new A();
    	cancelLink.setHref("#");
    	cancelLink.appendText(I18N.getString(
    			resp.getLocale(), 
    			"edit.harvest.mappings.dialog.cancel"));
    	cancelLink.setAttribute("onClick", "onClickCancelEditChannel()");
    	selectChannelForm.appendChild(cancelLink);
    	
    	resp.setContentType("text/html");
        resp.setHeader("Cache-Control", "no-cache");
    	resp.getWriter().write(selectChannelForm.write());		
	}

	

}
