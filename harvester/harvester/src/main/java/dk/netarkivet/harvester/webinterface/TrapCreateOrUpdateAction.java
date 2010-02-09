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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapList;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDAO;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDBDAO;

/**
 * This action processes multipart uploads to either create or update
 * a global crawler trap list. The choice of which action to carry out is
 * determined by whether the TRAP_ID is specified in the request.
 *
 */

public class TrapCreateOrUpdateAction extends TrapAction {

    /**
     * The logger for this class.
     */
    private static final Log log =
            LogFactory.getLog(TrapCreateOrUpdateAction.class) ;

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
            HTMLUtils.forwardWithErrorMessage(context, i18n, e,
                                          "errormsg;crawlertrap.upload.error");
            throw new ForwardedToErrorPage("Error on multipart post", e);
        }
        for (FileItem item: items) {
            if (item.isFormField()) {
                if (item.getFieldName().equals(Constants.TRAP_NAME)) {
                    name = item.getString();
                } else if (item.getFieldName()
                        .equals(Constants.TRAP_IS_ACTIVE)){
                    isActive = Boolean.parseBoolean(item.getString());
                } else if (item.getFieldName()
                        .equals(Constants.TRAP_DESCRIPTION)) {
                    description = item.getString();
                } else if (item.getFieldName().equals(Constants.TRAP_ID)) {
                    id = item.getString();
                }
            } else {
                try {
                    fileName = item.getName();
                    is = item.getInputStream();
                } catch (IOException e) {
                    HTMLUtils.forwardWithErrorMessage(context, i18n, e,
                                        "errormsg;crawlertrap.upload.error");
                    throw new
                            ForwardedToErrorPage("Error on multipart post", e);
                }
            }
        }
        GlobalCrawlerTrapListDAO dao = GlobalCrawlerTrapListDBDAO.getInstance();
        if (id != null) {   //update existing trap list
            int trapId = Integer.parseInt(id);
            GlobalCrawlerTrapList trap = dao.read(trapId);
            trap.setActive(isActive);
            trap.setDescription(description);
            trap.setName(name);
            if (fileName != null && !"".equals(fileName)) {
                log.debug("Reading global crawler trap list from '" +
                          fileName + "'");
                trap.setTrapsFromInputStream(is);
            }
            dao.update(trap);
        } else {  //create new trap list
            GlobalCrawlerTrapList trap = new GlobalCrawlerTrapList(is, name,
                                                       description, isActive);
            dao.create(trap);
        }


    }
}
