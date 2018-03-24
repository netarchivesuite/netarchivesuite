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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapList;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDAO;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDBDAO;

/**
 * This action processes multipart uploads to either create or update a global crawler trap list. The choice of which
 * action to carry out is determined by whether the TRAP_ID is specified in the request.
 */
@SuppressWarnings({"unchecked"})
public class TrapCreateOrUpdateAction extends TrapAction {

    /**
     * The logger for this class.
     */
    protected static final Logger log = LoggerFactory.getLogger(TrapCreateOrUpdateAction.class);
 
    @Override
    protected void doAction(PageContext context, I18n i18n) {
        String name = null;
        boolean isActive = true;
        String description = null;
        InputStream is = null;
        String id = null;
        String fileName = null;
        HttpServletRequest request = (HttpServletRequest) context.getRequest();
        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        List<FileItem> items = null;
        try {
            items = upload.parseRequest(request);
        } catch (FileUploadException e) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, e, "errormsg;crawlertrap.upload.error");
            throw new ForwardedToErrorPage("Error on multipart post", e);
        }
        for (FileItem item : items) {
            if (item.isFormField()) {
                if (item.getFieldName().equals(Constants.TRAP_NAME)) {
                    name = item.getString();
                } else if (item.getFieldName().equals(Constants.TRAP_IS_ACTIVE)) {
                    isActive = Boolean.parseBoolean(item.getString());
                } else if (item.getFieldName().equals(Constants.TRAP_DESCRIPTION)) {
                    description = item.getString();
                } else if (item.getFieldName().equals(Constants.TRAP_ID)) {
                    id = item.getString();
                }
            } else {
                try {
                    fileName = item.getName();
                    is = item.getInputStream();
                } catch (IOException e) {
                    HTMLUtils.forwardWithErrorMessage(context, i18n, e, "errormsg;crawlertrap.upload.error");
                    throw new ForwardedToErrorPage("Error on multipart post", e);
                }
            }
        }
        GlobalCrawlerTrapListDAO dao = GlobalCrawlerTrapListDBDAO.getInstance();
        if (id != null) { // update existing trap list
            int trapId = Integer.parseInt(id);
            GlobalCrawlerTrapList trap = dao.read(trapId);
            trap.setActive(isActive);
            trap.setDescription(description);
            trap.setName(name);
            if (fileName != null && !fileName.isEmpty()) {
                log.debug("Reading global crawler trap list from '" + fileName + "'");
                try {
                    trap.setTrapsFromInputStream(is, name);
                } catch (ArgumentNotValid argumentNotValid) {
                    HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;crawlertrap.regexp.error");
                    throw new ForwardedToErrorPage(argumentNotValid.getMessage());
                }
            }
            dao.update(trap);
        } else { // create new trap list
            log.debug("Reading global crawler trap list from '" + fileName + "'");
            GlobalCrawlerTrapList trap = new GlobalCrawlerTrapList(is, name, description, isActive);
            if (!dao.exists(name)) {
                dao.create(trap);
            } else {
                // crawlertrap named like this already exists.
                HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;crawlertrap.0.exists.error", name);
                throw new ForwardedToErrorPage("Crawlertrap with name '" + name + "' exists already");
            }
        }
    }
}
