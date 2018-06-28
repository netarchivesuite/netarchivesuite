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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.antiaction.raptor.dao.AttributeBase;
import com.antiaction.raptor.dao.AttributeTypeBase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.Domain;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.Password;
import dk.netarkivet.harvester.datamodel.SeedList;
import dk.netarkivet.harvester.datamodel.TemplateDAO;
import dk.netarkivet.harvester.datamodel.eav.EAV;
import dk.netarkivet.harvester.datamodel.eav.EAV.AttributeAndType;

/**
 * Utility class containing methods for processing a GUI-request to update the details of a domain-configuration.
 */

public class DomainConfigurationDefinition {
	
	private static final Logger log = LoggerFactory.getLogger(DomainConfigurationDefinition.class);
	
    /**
     * Extracts all required parameters from the request, checks for any inconsistencies, and passes the requisite data
     * to the updateDomain method for processing. The specified domain configuration is then updated and the result
     * stored in the database.
     * <p>
     * update: This method does nothing if update is not set
     * <p>
     * name: must be the name of a known domain
     * <p>
     * default: the defaultconfig is set to this value. Must be non-null and a known configuration of this domain.
     * <p>
     * edition: The edition number the config was originally read as, if any.
     * <p>
     * (configName, order_xml, maxRate, maxObjects, maxBytes, urlListList[], passwordList): group specifying a
     * configuration to update or add. If configName is non null then order_xml must be a known order-xml and
     * urlListList must contain only known seedlists, (and at least one such). load, maxObjects, maxBytes, edition must
     * be parsable as integers if present. passwordList is currently ignored.
     *
     * @param context The context of this request
     * @param i18n I18n information
     * @throws ForwardedToErrorPage if a user error has caused forwarding to the error page, in which case processing
     * should abort.
     */
    public static void processRequest(PageContext context, I18n i18n) {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");

        ServletRequest request = context.getRequest();
        String update = request.getParameter(Constants.UPDATE_PARAM);
        if (update == null) {
            return; // no need to continue
        }

        HTMLUtils.forwardOnEmptyParameter(context, Constants.DOMAIN_PARAM, Constants.CONFIG_NAME_PARAM, 
                Constants.ORDER_XML_NAME_PARAM, Constants.SEEDLIST_LIST_PARAM);
        String name = request.getParameter(Constants.DOMAIN_PARAM).trim();
        String configName = request.getParameter(Constants.CONFIG_NAME_PARAM).trim();
        String configOldName = request.getParameter(Constants.CONFIG_OLDNAME_PARAM);
        if (configOldName != null) {
            configOldName = configOldName.trim();
        } else {
            configOldName = "";
        }
        String order_xml = request.getParameter(Constants.ORDER_XML_NAME_PARAM).trim();
        String[] urlListList = request.getParameterValues(Constants.SEEDLIST_LIST_PARAM);

        if (!DomainDAO.getInstance().exists(name)) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;unknown.domain.0", name);
            throw new ForwardedToErrorPage("Domain " + name + " does not exist");
        }

        Domain domain = DomainDAO.getInstance().read(name);

        long edition = HTMLUtils.parseOptionalLong(context, Constants.EDITION_PARAM, -1L);

        // check the edition number before updating
        if (domain.getEdition() != edition) {
            HTMLUtils.forwardWithRawErrorMessage(
                    context,
                    i18n,
                    "errormsg;domain.definition.changed.0.retry.1",
                    "<br/><a href=\"Definitions-edit-domain.jsp?" + Constants.DOMAIN_PARAM + "="
                            + HTMLUtils.escapeHtmlValues(HTMLUtils.encode(name)) + "\">", "</a>");
            throw new ForwardedToErrorPage("Domain '" + name + "' has changed");
        }

        if (!TemplateDAO.getInstance().exists(order_xml)) {
            HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;harvest.template.0.does.not.exist", order_xml);
            throw new ForwardedToErrorPage("Unknown template " + order_xml);
        }

        for (String s : urlListList) {
            s = s.trim();
            if (s.length() == 0 || !domain.hasSeedList(s)) {
                HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;unknown.seed.list.0", s);
                throw new ForwardedToErrorPage("Unknown seed list " + s);
            }
        }

        int load = HTMLUtils.parseOptionalLong(context, Constants.MAX_RATE_PARAM,
                (long) dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_REQUEST_RATE).intValue();
        long maxObjects = HTMLUtils.parseOptionalLong(context, Constants.MAX_OBJECTS_PARAM,
                dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_OBJECTS);
        long maxBytes = HTMLUtils.parseOptionalLong(context, Constants.MAX_BYTES_PARAM,
                dk.netarkivet.harvester.datamodel.Constants.DEFAULT_MAX_BYTES);

        String comments = request.getParameter(Constants.COMMENTS_PARAM);

        DomainConfiguration domainConf = null;

        if (!configOldName.isEmpty() && !configOldName.equals(configName)){
            // Are we allowed to rename to the new name? or does it already exist?
            if (domain.hasConfiguration(configName)) {
                HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;configuration.exists.0", configName);
                throw new ForwardedToErrorPage("Configuration " + configName + " already exist");
            } else {
                domainConf = domain.getConfiguration(configOldName);
                String defaultConfigName = DomainDAO.getInstance().getDefaultDomainConfigurationName(domain.getName());
                if (defaultConfigName.equals(configName)){	
                    HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;cannot.rename.defaultconfiguration.0", configOldName);
                    throw new ForwardedToErrorPage("Configuration " + configOldName + " cannot be renamed. It is the defaultconfiguration");
                } else {
                    List<SeedList> seedlistList = new ArrayList<SeedList>();
                    for (String seedlistName : urlListList) {
                        seedlistList.add(domain.getSeedList(seedlistName));
                    }
                    domainConf.setName(configName);
                    domainConf.setOrderXmlName(order_xml);
                    domainConf.setMaxObjects(maxObjects);
                    domainConf.setMaxBytes(maxBytes);
                    domainConf.setMaxRequestRate(load);
                    domainConf.setSeedLists(domain, seedlistList);
                    if (comments != null) {
                        domainConf.setComments(comments);
                    }
                    DomainDAO.getInstance().renameAndUpdateConfig(domain, domainConf, configOldName);
                }
            }
        } else {
        	domainConf = updateDomainConfig(domain, configName, order_xml, load, maxObjects, maxBytes, urlListList, comments);
        }

        // EAV
        try {
        	long entity_id = domainConf.getID();
        	log.info("Saving attributes for domain config id {} and name {}", entity_id, domainConf.getName());
            EAV eav = EAV.getInstance();
            List<AttributeAndType> attributeTypes = eav.getAttributesAndTypes(EAV.DOMAIN_TREE_ID, (int)entity_id);
            AttributeAndType attributeAndType;
            AttributeTypeBase attributeType;
            AttributeBase attribute;
            for (int i=0; i<attributeTypes.size(); ++i) {
            	attributeAndType = attributeTypes.get(i);
            	attributeType = attributeAndType.attributeType;
            	attribute = attributeAndType.attribute;
            	if (attribute == null) {
                	attribute = attributeType.instanceOf();
                	attribute.entity_id = (int)entity_id;
            	}
            	switch (attributeType.viewtype) {
            	case 1:
                	long longValue = HTMLUtils.parseOptionalLong(context, attributeType.name, (long)attributeType.def_int);
                	log.info("Setting attribute {} to value {}", attributeType.name, longValue);
                	attribute.setInteger((int)longValue);
            		break;
            	case 5:
            	case 6:
                    String paramValue = context.getRequest().getParameter(attributeType.name);
                    int intVal = 0;
                    if (paramValue != null && !"0".equals(paramValue)) {
                        log.debug("Set intVal = 1 for attribute {} when receiving paramValue={}", attributeType.name, paramValue);
                    	intVal = 1;
                    }
                	attribute.setInteger(intVal);
            		break;
            	}
            	eav.saveAttribute(attribute);
            }
        } catch (SQLException e) {
        	throw new RuntimeException("Unable to store EAV data!", e);
        }
    }

    /**
     * Given the parsed values, update or create a configuration in the domain.
     *
     * @param domain The domain
     * @param configName Name of config - if this exists we update, otherwise we create a new. 
     * @param orderXml Order-template name
     * @param load Request rate
     * @param maxObjects Max objects
     * @param maxBytes Max bytes
     * @param urlListList List of url list names
     * @param comments Comments, or null for none.
     */
    private static DomainConfiguration updateDomainConfig(Domain domain, String configName, String orderXml, int load, long maxObjects,
            long maxBytes, String[] urlListList, String comments) {
    	// Update/create new configuration	
    	List<SeedList> seedlistList = new ArrayList<SeedList>();
    	for (String seedlistName : urlListList) {
    		seedlistList.add(domain.getSeedList(seedlistName));
    	}
    	DomainConfiguration domainConf;
    	if (domain.hasConfiguration(configName)) {
    		domainConf = domain.getConfiguration(configName);
    	} else { // new DomainConfiguration
    		domainConf = new DomainConfiguration(configName, domain, seedlistList, new ArrayList<Password>());
    		domain.addConfiguration(domainConf);
    	}
    	domainConf.setOrderXmlName(orderXml);
    	domainConf.setMaxObjects(maxObjects);
    	domainConf.setMaxBytes(maxBytes);
    	domainConf.setMaxRequestRate(load);
    	domainConf.setSeedLists(domain, seedlistList);
    	if (comments != null) {
    		domainConf.setComments(comments);
    	}
    	DomainDAO.getInstance().update(domain);
        return domainConf;
    }
}
