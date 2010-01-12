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

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapList;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDAO;
import dk.netarkivet.harvester.datamodel.GlobalCrawlerTrapListDBDAO;

/**
 * csr forgot to comment this!
 *
 * @author csr
 * @since Jan 5, 2010
 */

public class GlobalCrawlerTrapDefinition {

    private static final Log log =
            LogFactory.getLog(GlobalCrawlerTrapDefinition.class);

    /**
     * Process requests to alter global crawler trap definitions.
     * The behaviour is controlled by the TRAP_ACTION parameter,
     * which can have values:
     * CREATE: implies parameters TRAP_FILE and IS_ACTIVE, the file to read and
     * whether the trap is to be created in an active state.
     *
     * READ: implies parameters TRAP_ID and REQUESTED_CONTENT_TYPE, the id of
     * the trap to download and the form in which to display it
     *
     * UPDATE: implies parameters TRAP_ID and TRAP_FILE, the id of the trap to
     * be replaced and the file from which to replace it.
     *
     * DELETE: implies parameter TRAP_ID, the trap to delete.
     *
     * ACTIVATE: implies parameter TRAP_ID, the trap to activate.
     *
     * DEACTIVATE: implies parameter TRAP_ID, the trap to deactivate.
     *
     *
     * The TRAP_ACTION parameter must be the first item posted in the
     * calling form.
     *
     * @param context the servlet context from which this method was called.
     * @param i18n the i18n specifying the locale of the GUI.
     */
    public static void processRequest(PageContext context, I18n i18n) {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");
        log.debug("Processing request");
        HttpServletRequest request = (HttpServletRequest) context.getRequest();
        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        if (!ServletFileUpload.isMultipartContent(request)) {
            //This situation is expected if the webpage is displayed with
            //no parameters.
            log.debug("Nothing to process");
            return;
        }
        List<FileItem> items = null;
        FileItem firstItem = null;
        try {
            items = upload.parseRequest(request);
            if (items.isEmpty()) {
                return;
            }
            firstItem = items.get(0);
        } catch (FileUploadException e) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, e,
                                         "errormsg;crawlertrap.upload.error");
            throw new ForwardedToErrorPage("Error on multipart post", e);
        }
        
        if (!firstItem.getFieldName().equals(Constants.TRAP_ACTION)) {
             HTMLUtils.forwardOnEmptyParameter(context, Constants.TRAP_ACTION);
        } else if (firstItem.getString().equals(Constants.TRAP_CREATE)) {
             log.debug("Processing request to create new list of "
                       + "global crawler traps");
              processCreateRequest(context, items, i18n);
        } else if (firstItem.getString().equals(Constants.TRAP_DOWNLOAD)) {
              processDownloadRequest(context, items, i18n);
        } else if (firstItem.getString().equals(Constants.TRAP_ACTIVATE)) {
              setActivation(true, context, items, i18n);
        } else if (firstItem.getString().equals(Constants.TRAP_INACTIVATE)) {
            setActivation(false, context, items, i18n);
        }
        else {
            HTMLUtils.
                 forwardWithErrorMessage(context, i18n, "Not yet implemented");
        }
    }


    private static void setActivation(boolean activation, PageContext context,
                                      List<FileItem> items, I18n i18n) {
        int trapId = 0;
        for (FileItem item:items) {
            if (item.getFieldName().equals(Constants.TRAP_ID)) {
                trapId = Integer.parseInt(item.getString());
            }
        }
        GlobalCrawlerTrapListDAO dao = GlobalCrawlerTrapListDBDAO.getInstance();
        GlobalCrawlerTrapList trapList = dao.read(trapId);
        trapList.setActive(activation);
        dao.update(trapList);
    }

    private static void processDownloadRequest(PageContext context,
                                               List<FileItem> items, I18n i18n) {
        String contentType = null;
        int trapId = 0;
        for (FileItem item:items) {
            if (item.getFieldName().equals(Constants.TRAP_ID)) {
                trapId = Integer.parseInt(item.getString());
            } else if (item.getFieldName().equals(Constants.TRAP_CONTENT_TYPE)){
                contentType = item.getString();
            }
        }
        HttpServletResponse response = (HttpServletResponse)
                context.getResponse();
        GlobalCrawlerTrapListDAO dao = GlobalCrawlerTrapListDBDAO.getInstance();
        GlobalCrawlerTrapList trapList = dao.read(trapId);
        response.setHeader("Content-Type", contentType);
        if (contentType.startsWith("binary")) {
            response.setHeader("Content-Disposition", "Attachment; filename=" +
                                                      trapList.getName());
        }
        OutputStream out = null;
        try {
            out = response.getOutputStream();
            for (String trap:trapList.getTraps()) {
                out.write((trap+"\n").getBytes());
            }
            out.close();
        } catch (IOException e) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, e, "");
            throw new ForwardedToErrorPage("Error in retrieving trap list", e);
        }
    }

    private static void processCreateRequest(PageContext context,

                                             List<FileItem> items, I18n i18n) {
        String name = null;
        boolean isActive = true;
        String description = null;
        InputStream is = null;
        for (FileItem item: items) {
            if (item.isFormField()) {
                if (item.getFieldName().equals(Constants.TRAP_NAME)) {
                    name = item.getString();
                } else if (item.getFieldName().equals(Constants.TRAP_IS_ACTIVE)){
                    isActive = Boolean.parseBoolean(item.getString());
                } else if (item.getFieldName().equals(Constants.TRAP_DESCRIPTION)) {
                    description = item.getString();
                }
            } else {
                try {
                    is = item.getInputStream();
                } catch (IOException e) {
                    HTMLUtils.forwardWithErrorMessage(context, i18n, e,
                                           "errormsg;crawlertrap.upload.error");
                    throw new
                            ForwardedToErrorPage("Error on multipart post", e);
                }
            }
        }
        log.debug("Creating trap list '" + name + "'");
        GlobalCrawlerTrapList list =
                new GlobalCrawlerTrapList(is, name, description, isActive);
        GlobalCrawlerTrapListDAO dao = GlobalCrawlerTrapListDBDAO.getInstance();
        dao.create(list);
    }

}
