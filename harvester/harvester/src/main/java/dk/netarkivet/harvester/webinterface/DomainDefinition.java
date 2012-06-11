/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.DomainUtils;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.Domain;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedField;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDBDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDataTypes;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDefaultValue;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldTypes;

/**
 * Utility class for handling update of domain from the domain jsp page.
 *
 */
public class DomainDefinition {
    
    
    private static Log log = LogFactory.getLog(DomainDefinition.class.getName());
    
    /** Private constructor to prevent public construction of this class.*/
    private DomainDefinition() {    
    }
    
    /**
     * Extracts all required parameters from the request, checks for any
     * inconsistencies, and passes the requisite data to the updateDomain method
     * for processing.
     *
     * For reference, the parameters for this page look something like
     * http://localhost:8076/HarvestDefinition/Definitions-edit-domain.jsp?
     * update=1&name=netarkivet.dk&default=defaultconfig&configName=&order_xml=&
     * load=&maxObjects=&urlListName=&seedList=+&passwordName=&passwordDomain=&
     * passwordRealm=&userName=&password=&
     * crawlertraps=%2Fcgi-bin%2F*%0D%0A%2Ftrap%2F*%0D%0A
     *
     * update: This method throws an exception if update is not set
     *
     * name: must be the name of a known domain
     *
     * comments: optional user-entered comments about the domain
     *
     * default: the defaultconfig is set to this value. Must be non-null and a
     * known configuration of this domain.
     *
     * crawlertraps: a newline-separated list of urls to be ignored. May be
     * empty or null
     *
     * alias: If set, this domain is an alias of the set domain
     * renewAlias: If set, the alias date should be renewed
     * @param context The context of this request
     * @param i18n I18n information
     * @throws IOFailure on updateerrors in the DAO
     * @throws ForwardedToErrorPage if domain is not found, if the edition is
     * out-of-date, or if parameters are missing or invalid
     */
    public static void processRequest(PageContext context, I18n i18n) {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");

        HTMLUtils.forwardOnEmptyParameter(context,
                Constants.DOMAIN_PARAM, Constants.DEFAULT_PARAM);
        ServletRequest request = context.getRequest();
        String name = request.getParameter(Constants.DOMAIN_PARAM).trim();

        if (!DomainDAO.getInstance().exists(name)) {
            HTMLUtils.forwardWithErrorMessage(context, i18n,
                    "errormsg;unknown.domain.0", name);
            throw new ForwardedToErrorPage("Unknown domain '" + name + "'");
        }
        Domain domain = DomainDAO.getInstance().read(name);

        // check the edition number before updating
        long edition = HTMLUtils.parseOptionalLong(context,
                Constants.EDITION_PARAM, -1L);

        if (domain.getEdition() != edition) {
            HTMLUtils.forwardWithRawErrorMessage(context, i18n,
                    "errormsg;domain.definition.changed.0.retry.1",
                    "<br/><a href=\"Definitions-edit-domain.jsp?"
                            + Constants.DOMAIN_PARAM + "="
                            + HTMLUtils.escapeHtmlValues(HTMLUtils.encode(name))
                            + "\">",
                    "</a>");
            throw new ForwardedToErrorPage("Domain '" + name + "' has changed");
        }

        // default configuration
        String defaultConf = request.getParameter(Constants.DEFAULT_PARAM);
        if (!domain.hasConfiguration(defaultConf)) {
            HTMLUtils.forwardWithErrorMessage(context, i18n,
                    "errormsg;unknown.default.configuration.0.for.1",
                    defaultConf, name);
            throw new ForwardedToErrorPage(
                    "Unknown default configuration '" + defaultConf + "'");
        }

        String crawlertraps = request.getParameter(
                Constants.CRAWLERTRAPS_PARAM);
        if (crawlertraps == null) {
            crawlertraps = "";
        }
        String comments = request.getParameter(Constants.COMMENTS_PARAM);
        if (comments == null) {
            comments = "";
        }
        String alias = request.getParameter(Constants.ALIAS_PARAM);
        if (alias == null) {
            alias = "";
        }

        String aliasRenew = request.getParameter(Constants.RENEW_ALIAS_PARAM);
        if (aliasRenew == null) {
            aliasRenew = "no";
        }

        boolean renewAlias = aliasRenew.equals("yes");
        
        ExtendedFieldDAO extdao = ExtendedFieldDBDAO.getInstance();
        Iterator<ExtendedField> it 
            = extdao.getAll(ExtendedFieldTypes.DOMAIN).iterator();
        
        while (it.hasNext()) {
            String value = "";

            ExtendedField ef = it.next();
            String parameterName = ef.getJspFieldname();
            switch (ef.getDatatype()) {
            case ExtendedFieldDataTypes.BOOLEAN:
                String[] parb = request.getParameterValues(parameterName);
                if (parb != null && parb.length > 0) {
                    value = "true";
                } else {
                    value = "false";
                }
                break;
            case ExtendedFieldDataTypes.SELECT:
                String[] pars = request.getParameterValues(parameterName);
                if (pars != null && pars.length > 0) {
                    value = pars[0];
                } else {
                    value = "";
                }

                break;
            default:
                value = request.getParameter(parameterName);
                if (ef.isMandatory()) {
                    if (value == null) {
                        value = ef.getDefaultValue();
                    }

                    if (value == null || value.length() == 0) {
                        HTMLUtils.forwardWithErrorMessage(
                                context,
                                i18n,
                                "errormsg;extendedfields.field.0.is.empty."
                                + "but.mandatory",
                                ef.getName());
                        throw new ForwardedToErrorPage("Mandatory field "
                                + ef.getName() + " is empty.");
                    }
                }

                ExtendedFieldDefaultValue def = new ExtendedFieldDefaultValue(
                        value, ef.getFormattingPattern(), ef.getDatatype());
                if (!def.isValid()) {
                    HTMLUtils.forwardWithRawErrorMessage(context, i18n,
                            "errormsg;extendedfields.value.invalid");
                    throw new ForwardedToErrorPage(
                            "errormsg;extendedfields.value.invalid");
                }
                break;
            }

            domain.updateExtendedFieldValue(ef.getExtendedFieldID(), value);
        }
        
        updateDomain(domain, defaultConf, crawlertraps, comments, alias,
                renewAlias);
    }

    /**
     * This updates the given domain in the database.
     * @param domain the given domain
     * @param defaultConfig the name of the default configuration
     * @param crawlertraps the current crawlertraps stated for the domain
     * @param comments User-defined comments for the domain
     * @param alias if this is non-null, this domain is an alias of 'alias'.
     * @param renewAlias true, if alias is to be updated even if it is not 
     * changed
     */
    private static void updateDomain(Domain domain, String defaultConfig,
                                     String crawlertraps, String comments,
                                     String alias, boolean renewAlias) {
        // Set default configuration
        domain.setDefaultConfiguration(defaultConfig);
        domain.setComments(comments);

        //Update crawlertraps
        List<String> trapList = new ArrayList<String>();
        if (crawlertraps.length() > 0) {
            String[] traps = crawlertraps.split("[\\r\\n]+");
            for (String trap : traps) {
                if (trap.trim().length() > 0) {
                    trapList.add(trap);
                }
            }
            domain.setCrawlerTraps(trapList, true);
        }

        //Update alias information

        // If alias is empty string, do not regard this domain as an alias any
        // more.
        // If alias is not-empty, update only if alias is different from
        // oldAlias or
        // This only updates alias if it is required: See javadoc for
        // needToUpdateAlias()
        String oldAlias = null;
        if (domain.getAliasInfo() != null) {
            oldAlias = domain.getAliasInfo().getAliasOf();
        }
        String newAlias;
        // If alias is empty string, this domain is or should not be an alias.

        if (alias.trim().isEmpty()) {
            newAlias = null;
        } else {
            newAlias = alias.trim();
        }

        if (needToUpdateAlias(oldAlias, newAlias, renewAlias)){
            domain.updateAlias(newAlias);
        }

        DomainDAO.getInstance().update(domain);
    }

    /** Define the cases where we want to update the alias information.
     *  1. The alias information is updated, if the new alias is null, and the
     *  old alias is different from null
     *  2. The alias information is updated, if the new alias is different from
     *  null, and old alias is null
     *  3. The alias information is updated,
     *      if the new alias is different from null,
     *      and the old alias is different from null,
     *      and they are not either not equal, or renewAlias is true
     * @param oldAlias the old alias (could be null)
     * @param newAlias the new alias (could be null)
     * @param renewAlias should we renew alias, if the alias is unchanged?
     * @return true, if we want to update the alias information, false otherwise
     */
    private static boolean needToUpdateAlias(String oldAlias, String newAlias,
            boolean renewAlias) {
        boolean needToUpdate = false;
        // If new alias is null: update if old alias is different from null
        if (newAlias == null) {
            if (oldAlias != null) {
                needToUpdate = true;
            }
        } else { // newAlias is not null
            if (oldAlias == null) {
                needToUpdate = true;
            } else {
                if (oldAlias.equals(newAlias)) {
                    if (renewAlias) {
                        needToUpdate = true;
                    }
                } else {
                    needToUpdate = true;
                }
            }
        }
        return needToUpdate;
    }

    /**
     * Creates domains with default attributes.
     *
     * @param domains a list of domain names
     * @return List of the non-empty domain names that were not legal domain
     * names or already exist.
     */
    public static List<String> createDomains(String... domains) {
        DomainDAO ddao = DomainDAO.getInstance();
        List<String> illegals = new ArrayList<String>();
        List<Domain> domainsToCreate = new ArrayList<Domain>();
        for (String domain : domains) {
            if (DomainUtils.isValidDomainName(domain) && !ddao.exists(domain)) {
                domainsToCreate.add(Domain.getDefaultDomain(domain));
            } else {
                if (domain.trim().length() > 0) {
                    illegals.add(domain);
                }
            }
        }

        ddao.create(domainsToCreate);

        return illegals;
    }

    /**
     * Creates a link to the domain edit page.
     *
     * @param domain The domain to show with a link
     * @return HTML code with the link and the domain name shown
     */
    public static String makeDomainLink(String domain) {
        ArgumentNotValid.checkNotNullOrEmpty(domain, "domain");
        String url = "/HarvestDefinition/Definitions-edit-domain.jsp?"
                + Constants.DOMAIN_PARAM + "="
                + HTMLUtils.encode(domain);
        return "<a href=\"" + url + "\">"
                + HTMLUtils.escapeHtmlValues(domain)
                + "</a>";
    }
    
    /**
     * Search for domains matching the following criteria.
     * Should we allow more than one criteria?
     * @param context the context of the JSP page calling
     * @param i18n The translation properties file used
     * @param searchQuery The given searchQuery for searching for among the domains.
     * @param searchType The given searchCriteria (TODO use Enum instead)
     * @return the set of domain-names matching the given criteria.
     */
    public static List<String> getDomains(PageContext context, I18n i18n,
            String searchQuery, String searchType) {
        List<String> resultSet = new ArrayList<String>();
        ArgumentNotValid.checkNotNullOrEmpty(searchQuery, "String searchQuery");
        ArgumentNotValid.checkNotNullOrEmpty(searchType, "String searchType");
        
        if (!getSearchTypes().contains(searchType)) {
            HTMLUtils.forwardWithErrorMessage(context, i18n,
                    "errormsg;invalid.domain.search.criteria.0", searchType);
            throw new ForwardedToErrorPage("Unknown domain search criteria '" 
                    + searchType + "'");
        }
        log.info("SearchQuery '" + searchQuery + "', searchType: " +  searchType);
        resultSet = DomainDAO.getInstance().getDomains(searchQuery, searchType);
        return resultSet;
    }
    
    /**
     * The list of available domain search types. Seeds and configuration search
     * are not implemented currently, so they are exempted from the list 
     * @return the list of available domain search types.
     */
    public static Set<String> getSearchTypes() {
        Set<String> types = new HashSet<String>();
        //types.add(Constants.SEEDS_DOMAIN_SEARCH);
        //types.add(Constants.CONFIGS_DOMAIN_SEARCH);
        types.add(Constants.TRAPS_DOMAIN_SEARCH);
        types.add(Constants.NAME_DOMAIN_SEARCH);
        types.add(Constants.COMMENTS_DOMAIN_SEARCH);
        return types;
    }
}
