/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package dk.netarkivet.harvester.webinterface;

import java.io.File;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.PartialHarvest;
import dk.netarkivet.harvester.datamodel.TemplateDAO;

/**
 * Contains utility methods for supporting event harvest GUI.
 *
 */
public class EventHarvest {
    /**
     * Private Constructor. Instances are not meaningful.
     */
    private EventHarvest() {
    }

    /**
     * Adds a bunch of configurations to a given PartialHarvest.  For full
     * definitions of the parameters, see Definitions-add-event-seeds.jsp.
     * For each seed in the list, the following steps are taken:
     * 1) The domain is parsed out of the seed. If no such domain is
     *    known, it is created with the usual defaults.
     * 2) For each domain, a configuration with the name
     *    &lt;harvestDefinition&gt;_&lt;orderTemplate&gt;_&lt;maxBytes&gt;Bytes
     *    is created unless it already exists. The configuration uses
     *    orderTemplate, and the specified maxBytes. If maxBytes is unspecified,
     *    its default value is used. The configuration is added to the harvest 
     *    specified by the harvestDefinition argument.
     * 3) For each domain, a seedlist with the name
     *    &lt;harvestDefinition&gt;_&lt;orderTemplate&gt;_&lt;maxBytes&gt;Bytes
     *    is created if it does not already exist and the given url is added
     *    to it. This seedlist is the only seedlist associated with the
     *    configuration of the same name.
     *
     * @param context the current JSP context
     * @param i18n the translation information to use in this context
     * @param eventHarvest the partial harvest to which these
     * seeds are to be added
     * @throws ForwardedToErrorPage If maxBytes is not a number,
     *   or if any of the seeds is badly formatted such that no domain name can
     *   be parsed from it, or if orderTemplate is not given or unknown.
     */
    public static void addConfigurations(PageContext context, I18n i18n,
                                         PartialHarvest eventHarvest) {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");
        ArgumentNotValid.checkNotNull(eventHarvest,
                "PartialHarvest eventHarvest");

        HTMLUtils.forwardOnMissingParameter(context, Constants.SEEDS_PARAM);
        ServletRequest request = context.getRequest();

        // If no seeds are specified, just return
        String seeds = request.getParameter(Constants.SEEDS_PARAM);
        if (seeds == null || seeds.trim().length() == 0) {
            return;
        }

        HTMLUtils.forwardOnEmptyParameter(context,
                Constants.ORDER_TEMPLATE_PARAM);
        String orderTemplate
                = request.getParameter(Constants.ORDER_TEMPLATE_PARAM);
        // Check that order template exists
        if (!TemplateDAO.getInstance().exists(orderTemplate)) {
            HTMLUtils.forwardWithErrorMessage(context, i18n,
                    "errormsg;harvest.template.0.does.not.exist",
                    orderTemplate);
            throw new ForwardedToErrorPage("The orderTemplate with name '"
                    + orderTemplate + "' does not exist!");
        }

        // Check that numerical parameters are meaningful and replace null or
        // empty with default values
        long maxBytes = HTMLUtils.parseOptionalLong(context,
                Constants.MAX_BYTES_PARAM, dk.netarkivet.harvester
                    .datamodel.Constants.DEFAULT_MAX_BYTES);
        // All parameters are valid, so call method
        try {
            eventHarvest.addSeeds(seeds, orderTemplate, maxBytes);
        } catch (Exception e) {
            HTMLUtils.forwardWithErrorMessage(context, i18n,
                    "errormsg;error.adding.seeds.to.0", eventHarvest.getName(),
                    e);
            throw new ForwardedToErrorPage("Error while adding seeds", e);
        }
    }
    
    /** addConfigurations with support for multipart data. */
    public static void addConfigurations(PageContext context, boolean isMultipart, I18n i18n,
            PartialHarvest eventHarvest) {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");
        ArgumentNotValid.checkNotNull(eventHarvest, "PartialHarvest eventHarvest");
        if (!isMultipart) {
            addConfigurations(context, i18n, eventHarvest);
            return;
        }
        HttpServletRequest request = (HttpServletRequest) context.getRequest();
        String seeds = null;
        long maxBytes = 0L;
        String orderTemplate = null;
        //The seeds are found in a file 
        try {
            String maxbytesString = null;           
            // Create a factory for disk-based file items
            FileItemFactory factory = new DiskFileItemFactory();

           // Create a new file upload handler
           ServletFileUpload upload = new ServletFileUpload(factory);

           File seedsFile = File.createTempFile("seeds", ".txt", 
                    FileUtils.getTempDir());
            String seedsFileName = "";
            List items = upload.parseRequest(request);
            for (Object o : items) {
                FileItem item = (FileItem) o;
                if (!item.isFormField()) {
                    item.write(seedsFile);
                    seedsFileName = item.getName();
                } else {
                    String fieldName = item.getFieldName();
                    if (fieldName.equals(Constants.MAX_BYTES_PARAM)) {
                        maxbytesString = item.getString();
                    } else if (fieldName.equals(Constants.ORDER_TEMPLATE_PARAM)) {
                        orderTemplate = item.getString();
                }               
                }
            }
            if (!seedsFileName.isEmpty()) { // A file was found
                seeds = FileUtils.readFile(seedsFile);
            } else {
                seeds = "www.defaultdomain.dk";
            }
            if (maxbytesString == null){
                maxBytes = dk.netarkivet.harvester
                .datamodel.Constants.DEFAULT_MAX_BYTES;
            } else {
                maxBytes = parseLong(context, maxbytesString, dk.netarkivet.harvester
            .datamodel.Constants.DEFAULT_MAX_BYTES);
            }
            //maxBytes = HTMLUtils.parseOptionalLong(context,
            //        Constants.MAX_BYTES_PARAM, dk.netarkivet.harvester
            //            .datamodel.Constants.DEFAULT_MAX_BYTES);
        } catch (Exception e) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, 
                    "Exception.thrown.when.adding.seeds", e);
            return;
        }
        
        if (orderTemplate == null) {
            orderTemplate = "default_orderxml";
        }
        
        // Check that order template exists
        if (!TemplateDAO.getInstance().exists(orderTemplate)) {
            HTMLUtils.forwardWithErrorMessage(context, i18n,
                    "errormsg;harvest.template.0.does.not.exist",
                    orderTemplate);
            throw new ForwardedToErrorPage("The orderTemplate with name '"
                    + orderTemplate + "' does not exist!");
        }

        // All parameters are valid, so call method
        try {
            eventHarvest.addSeeds(seeds, orderTemplate, maxBytes);
        } catch (Exception e) {
            HTMLUtils.forwardWithErrorMessage(context, i18n,
                    "errormsg;error.adding.seeds.to.0", e, eventHarvest.getName(),
                    e);
            throw new ForwardedToErrorPage("Error while adding seeds", e);
        }
    }

    private static long parseLong(PageContext context, String maxbytesString, long defaultMaxBytes) {
        Locale loc = HTMLUtils.getLocaleObject(context);
        String paramValue = maxbytesString;
        if (paramValue != null && paramValue.trim().length() > 0) {
            paramValue = paramValue.trim();
            try {
                return NumberFormat.getInstance(loc).parse(paramValue).longValue();
            } catch (ParseException e) {
                throw new ForwardedToErrorPage("Invalid value " + paramValue
                        + " for integer parameter '" + Constants.MAX_BYTES_PARAM + "'", e);
            }
        } else {
            return defaultMaxBytes;
        }
    }
}
