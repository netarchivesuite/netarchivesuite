/* File:        $Id: SelectiveHarvest.java 2033 2011-09-20 15:48:19Z svc $
 * Revision:    $Revision: 2033 $
 * Author:      $Author: svc $
 * Date:        $Date: 2011-09-20 17:48:19 +0200 (Tue, 20 Sep 2011) $
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

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.DomainUtils;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.Domain;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.DomainConfigurationKey;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.PartialHarvest;
import dk.netarkivet.harvester.datamodel.Schedule;
import dk.netarkivet.harvester.datamodel.ScheduleDAO;

/**
 * This class contains the methods for updating data for selective harvests.
 * New version not yet finished.
 */
public class SelectiveHarvestNew {
    
    /** The logger. */
    private static final Log log = LogFactory.getLog(SelectiveHarvest.class);
    
    /**
     * Utility class. No instances.
     */
    private SelectiveHarvestNew() {
    }

    /**
     * Update or create a partial harvest definition.
     * @param context JSP context of this call.  Contains parameters as
     * described in Definitions-edit-selective-harvest.jsp
     * @param i18n Translation information.
     * @param unknownDomains List to which unknown legal domains are added.
     * @param illegalDomains List to which illegal domains are added,
     */
    public static void processRequest(PageContext context, I18n i18n,
                                      List<String> unknownDomains,
                                      List<String> illegalDomains) {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");
        ArgumentNotValid.checkNotNull(unknownDomains, "List unknownDomains");
        ArgumentNotValid.checkNotNull(illegalDomains, "List illegalDomains");

        ServletRequest request = context.getRequest();
        if (request.getParameter(Constants.UPDATE_PARAM) == null) {
            return; //nothing to do.
        }
        HarvestDefinitionDAO hdDao = HarvestDefinitionDAO.getInstance();
        PartialHarvest hdd = updateHarvestDefinition(context, i18n,
                unknownDomains, illegalDomains);

        //If the override date is set, parse it and set the override date.
        Date nextDate = HTMLUtils.parseOptionalDate(
                context, Constants.NEXTDATE_PARAM,
                I18n.getString(
                        dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE,
                        context.getResponse().getLocale(),
                        "harvestdefinition.schedule.edit.timeformat"),
                null);
        if (nextDate != null) {
            //hdd.setNextDate(nextDate);
            hdDao.updateNextdate(hdd, nextDate);
        }

        // Case where we are adding domains that didn't exist before
        // This uses two parameters because it has an input field and a submit
        // button.
        if (request.getParameter(Constants.ADDDOMAINS_PARAM) != null) {
            HTMLUtils.forwardOnMissingParameter(context,
                    Constants.UNKNOWN_DOMAINS_PARAM);
            List<DomainConfiguration> dcList = findNewDomainsToHarvest(hdd,
                    request.getParameter(Constants.UNKNOWN_DOMAINS_PARAM));
            log.debug("Adding " + dcList.size() + " configurations");
            for (DomainConfiguration dc: dcList) {
            	hdDao.addDomainConfiguration(hdd, new DomainConfigurationKey(dc));
            }
        }

        String deleteConfig
                = request.getParameter(Constants.DELETECONFIG_PARAM);

        //Case where we are removing a configuration
        if (deleteConfig != null) {
            HTMLUtils.forwardOnEmptyParameter(context,
                    Constants.DELETECONFIG_PARAM);
            deleteConfig(context, i18n, deleteConfig, hdd);
        }
    }

    /**
     * Updates the harvest definition with posted values.
     *
     * @param context The context that the web request processing happens in
     * @param i18n Translation information for this site section.
     * @param unknownDomains List to add unknown but legal domains to.
     * @param illegalDomains List to add illegal domains to.
     * @return The updated harvest definition.  This object holds an edition
     * that is legal to use for further updates (adding or deleting domains)
     */
    private static PartialHarvest updateHarvestDefinition(
    		PageContext context, I18n i18n,
             List<String> unknownDomains, List<String> illegalDomains) {
        ServletRequest request = context.getRequest();
        HTMLUtils.forwardOnEmptyParameter(context,
                Constants.HARVEST_PARAM, Constants.SCHEDULE_PARAM);
        String harvestName = request.getParameter(Constants.HARVEST_PARAM);

        HTMLUtils.forwardOnMissingParameter(context,
                Constants.COMMENTS_PARAM, Constants.DOMAINLIST_PARAM);

        String scheduleName
                = request.getParameter(Constants.SCHEDULE_PARAM);
        Schedule sched = ScheduleDAO.getInstance().read(scheduleName);
        if (sched == null) {
            HTMLUtils.forwardWithErrorMessage(context, i18n,
                    "errormsg;unknown.schedule.0", scheduleName);
            throw new ForwardedToErrorPage("Schedule '" + scheduleName
                    + "' not found");
        }

        String comments = request.getParameter(Constants.COMMENTS_PARAM);

        List<DomainConfiguration> dcList
                = getDomainConfigurations(request.getParameterMap());
        
        addDomainsToConfigurations(dcList,
                request.getParameter(Constants.DOMAINLIST_PARAM),
                unknownDomains, illegalDomains);
      
        // If necessary create harvest from scratch
        HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        if ((request.getParameter(Constants.CREATENEW_PARAM) != null)) {
            if (hddao.exists(harvestName)) {
                HTMLUtils.forwardWithErrorMessage(context, i18n,
                        "errormsg;harvest.definition.0.already.exists",
                        harvestName);
                throw new ForwardedToErrorPage("A harvest definition "
                        + "called '" + harvestName + "' already exists");
            }
            PartialHarvest hdd = new PartialHarvest(dcList, sched, harvestName, comments);
            hdd.setActive(false);
            hddao.create(hdd);
            return hdd;
        } else {
            long edition = HTMLUtils.parseOptionalLong(context,
                    Constants.EDITION_PARAM, Constants.NO_EDITION);

            PartialHarvest hdd = (PartialHarvest) hddao.getHarvestDefinition(harvestName);
            
            if (hdd.getEdition() != edition) {
                HTMLUtils.forwardWithRawErrorMessage(context, i18n,
                        "errormsg;harvest.definition.changed.0.retry.1",
                        "<br/><a href=\"Definitions-edit-selective-harvest.jsp?"
                                + Constants.HARVEST_PARAM + "="
                                + HTMLUtils.encodeAndEscapeHTML(harvestName)
                                + "\">",
                        "</a>");
                throw new ForwardedToErrorPage("Harvest definition '"
                        + harvestName + "' has changed in the meantime. Old Edition = "
                        + edition + ". Current edition = " + hdd.getEdition());
            }
            // update the harvest definition
            hdd.setSchedule(sched);
            hdd.setComments(comments);
            hddao.update(hdd);
            hddao.resetDomainConfigurations(hdd, dcList);
            return hdd;
        }
    }

    /**
     * Delete a domain configuration from a harvestdefinition.
     * @param context The web server context for the JSP page.
     * @param i18n Translation information for this site section.
     * @param deleteConfig the configuration to delete, in the form of a
     * domain name, a colon, a configuration name.
     * @param hdd The harvest definition to delete a configuration from
     */
    private static void deleteConfig(PageContext context, I18n i18n,
                                        String deleteConfig, PartialHarvest hdd) {
        String[] domainConfigPair = deleteConfig.split(":", 2);
        if (domainConfigPair.length < 2) {
            HTMLUtils.forwardWithErrorMessage(context, i18n,
                    "errormsg;malformed.domain.config.pair.0", deleteConfig);
            throw new ForwardedToErrorPage("Malformed domain-config pair "
                    + deleteConfig);
        }
        String domainName = domainConfigPair[0];
        String configName = domainConfigPair[1];
        DomainConfigurationKey dcKey = new DomainConfigurationKey(
                domainName, configName);
        hdd.removeDomainConfiguration(dcKey);
        HarvestDefinitionDAO.getInstance().removeDomainConfiguration(hdd.getOid(), dcKey);
    }

    /**
     * Extract domain configuration list from a map of parameters.
     * All keys that start with Constants.DOMAIN_IDENTIFIER are treated
     * as a concatenation of : DOMAIN_IDENTIFIER + domain name.
     * The corresponding value in the map is treated as the configuration name
     * Entries that do not match this pattern are ignored.
     * @param configurations  a mapping (domain to its configurations)
     * @return a list of domain configurations
     */
    private static List<DomainConfiguration> getDomainConfigurations
            (Map<String, String[]> configurations) {
        List<DomainConfiguration> dcList = new ArrayList<DomainConfiguration>();

        for (Map.Entry<String, String[]> param : configurations.entrySet()) {
            if (param.getKey().startsWith(Constants.DOMAIN_IDENTIFIER)) {
                String domainName = param.getKey().substring
                        (Constants.DOMAIN_IDENTIFIER.length());
                Domain domain = DomainDAO.getInstance().read(domainName);
                for (String configurationName : param.getValue()) {
                	System.out.println("configname (for domain '" 
                			+ domain.getName() + "'): " + configurationName);
                    dcList.add(domain.getConfiguration(configurationName));
                }
            }
        }
        return dcList;
    }

    /**
     * Given a list of domain configurations and a list of domains, add the
     * default configurations for the domains to the configuration list. If any
     * of the domains are unknown, their names are instead appended to the
     * argument unknownDomains (with newline separation)
     * @param dcList the initial list of configurations
     * @param extraDomains the domains to be added to dcList with default
     * configurations
     * @param unknownDomains a list to add unknown, legal domains to
     * @param illegalDomains a list to add illegal domains to
     */
    private static void addDomainsToConfigurations(
    		List<DomainConfiguration> dcList,
            String extraDomains,
            List<String> unknownDomains,
            List<String> illegalDomains) {
        String[] domains = extraDomains.split("\\s+");
        DomainDAO ddao = DomainDAO.getInstance();
        for (String domain : domains) {
            domain = domain.trim();
            if (domain.length() > 0) {
            	System.out.println("Handling domain:" + domain);
                if (ddao.exists(domain)) {
                    Domain d = ddao.read(domain);
                    if (!dcList.contains(d.getDefaultConfiguration())) {
                        dcList.add(d.getDefaultConfiguration());
                    }
                } else {
                    if (DomainUtils.isValidDomainName(domain)) {
                        unknownDomains.add(domain);
                    } else {
                        illegalDomains.add(domain);
                    }
                }
            }
        }
    }

    /**
     * Given a harvest and list of domains, this method creates all the
     * specified domains and adds them to the harvest with their default
     * configuration.
     * @param hdd The harvest definition to change.
     * @param domains a whitespace-separated list of domains to create and
     * add to harvest
     * @return a list of new configurations to add to harvest
     */
    private static List<DomainConfiguration> findNewDomainsToHarvest(PartialHarvest hdd,
                                               String domains) {
        String[] domainsS = domains.split("\\s");
        List<DomainConfiguration> configurations
                = new ArrayList<DomainConfiguration>();
        for (String domainName : domainsS) {
            if (DomainUtils.isValidDomainName(domainName)) {
                Domain domain = Domain.getDefaultDomain(domainName);
                DomainDAO.getInstance().create(domain);
                configurations.add(domain.getDefaultConfiguration());
            } else {
                log.debug("Ignoring invalid domainname '"
                        +  domainName + "'.");
            }
        }
        return configurations;
    }

}

