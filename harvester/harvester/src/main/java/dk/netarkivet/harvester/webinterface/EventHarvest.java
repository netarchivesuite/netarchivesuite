/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.util.Locale;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.PartialHarvest;
import dk.netarkivet.harvester.datamodel.TemplateDAO;

/**
 * Contains utility methods for supporting event harvest GUI.
 *
 */
public class EventHarvest {
    
    final static Log log = LogFactory.getLog(EventHarvest.class.getName());
    
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
    
    
    
    /**
     * Add configurations to an existing selective harvest.
     * @param context The current JSP context
     * @param i18n The translation information to use in this context
     * @param eventHarvest The partial harvest to which these
     * seeds are to be added
     * @param seeds The seeds as a String
     * @param maxbytesString The given maxbytes as a string
     * @param maxobjectsString The given maxobjects as a string (currently not used)
     * @param maxrateString The given maxrate as a string (currently not used)
     * @param ordertemplate The name of the ordertemplate to use
     */
    public static void addConfigurationsFromSeedsFile(PageContext context, I18n i18n,
            PartialHarvest eventHarvest, String seeds, String maxbytesString, 
            String maxobjectsString, String maxrateString, String ordertemplate) {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");
        ArgumentNotValid.checkNotNull(eventHarvest, "PartialHarvest eventHarvest");
        ArgumentNotValid.checkNotNull(seeds, "String seeds");
        ArgumentNotValid.checkNotNull(ordertemplate, "String ordertemplate");
        
        long maxBytes = 0L;
        try {
            if (maxbytesString == null){
                maxBytes = dk.netarkivet.harvester.datamodel
                    .Constants.DEFAULT_MAX_BYTES;
            } else {
                Locale loc = HTMLUtils.getLocaleObject(context);
                maxBytes = HTMLUtils.parseLong(
                        loc, maxbytesString,  Constants.MAX_BYTES_PARAM, 
                        dk.netarkivet.harvester.datamodel
                        .Constants.DEFAULT_MAX_BYTES);
            }
        } catch (Exception e) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, 
                    "Exception.thrown.when.adding.seeds", e);
            return;
        }
        // Check that order template exists
        if (!TemplateDAO.getInstance().exists(ordertemplate)) {
            HTMLUtils.forwardWithErrorMessage(context, i18n,
                    "errormsg;harvest.template.0.does.not.exist",
                    ordertemplate);
            throw new ForwardedToErrorPage("The orderTemplate with name '"
                    + ordertemplate + "' does not exist!");
        }

        // All parameters are valid, so call method
        try {
            eventHarvest.addSeeds(seeds, ordertemplate, maxBytes);
        } catch (Exception e) {
            HTMLUtils.forwardWithErrorMessage(context, i18n,
                    "errormsg;error.adding.seeds.to.0", e, eventHarvest.getName(),
                    e);
            throw new ForwardedToErrorPage("Error while adding seeds", e);
        }
    }
}
