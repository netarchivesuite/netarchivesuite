/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package dk.netarkivet.harvester.webinterface;

//import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapList;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDAO;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDBDAO;

/**
 * Class to read and return a global crawler trap list to a web request.
 */

public class TrapReadAction extends TrapAction {
    
    protected static final Logger log = LoggerFactory.getLogger(TrapReadAction.class);
    
    @Override
    protected void doAction(PageContext context, I18n i18n) {
        HttpServletRequest request = (HttpServletRequest) context.getRequest();
        int trapId = Integer.parseInt(request.getParameter(Constants.TRAP_ID));
        String contentType = request.getParameter(Constants.TRAP_CONTENT_TYPE);
        HttpServletResponse response = (HttpServletResponse) context.getResponse();
        GlobalCrawlerTrapListDAO dao = GlobalCrawlerTrapListDBDAO.getInstance();
        GlobalCrawlerTrapList trapList = dao.read(trapId);
        response.setHeader("Content-Type", contentType);
        if (contentType.startsWith("binary")) {
            response.setHeader("Content-Disposition", "Attachment; filename=" + trapList.getName());
        }
        OutputStream out = null;
        Set<String> traps= trapList.getTraps();
        int trapSize = traps.size();
        try {
            out = response.getOutputStream();
            int count=0;
            for (String trap : trapList.getTraps()) {
                count++;
                log.trace("Writing trap {}/{} to output destination", count, trapSize);
                out.write((trap + "\n").getBytes());
            }
            out.close();
        } catch (Throwable e) {
            log.warn("error occurred", e);
            HTMLUtils.forwardWithErrorMessage(context, i18n, e, "");
            throw new ForwardedToErrorPage("Error in retrieving trap list", e);
        }
        log.info("All {} traps in list {} written to output destination successfully", trapSize, trapList.getName());
    }
}
