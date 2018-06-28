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

import java.util.Iterator;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendableEntity;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedField;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDBDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDataTypes;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDefaultValue;

@SuppressWarnings({"unused"})
public class ExtendedFieldValueDefinition {
    //private static Log log = LogFactory.getLog(ExtendedFieldValueDefinition.class.getName());
    
	static final Logger log = LoggerFactory.getLogger(ExtendedFieldValueDefinition.class);
    /**
     * Subprocessing of ServletRequest for Extended Fields and update field content
     * 
     * @param context The context of this request
     * 
     * @param i18n I18n information
     * 
     * @param entity ExtendableEntity
     * 
     * @param type ExtendedFieldType
     */
    public static void processRequest(PageContext context, I18n i18n, ExtendableEntity entity, int type) {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");

        ExtendedFieldDAO extdao = ExtendedFieldDBDAO.getInstance();
        Iterator<ExtendedField> it = extdao.getAll(type).iterator();

        ServletRequest request = context.getRequest();

        while (it.hasNext()) {
            String value = "";

            ExtendedField ef = it.next();
            String parameterName = ef.getJspFieldname();
            switch (ef.getDatatype()) {
            case ExtendedFieldDataTypes.BOOLEAN:
                String[] parb = request.getParameterValues(parameterName);
                if (parb != null && parb.length > 0) {
                    value = ExtendedFieldConstants.TRUE;
                } else {
                    value = ExtendedFieldConstants.FALSE;
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
                    if (value == null || value.length() == 0) {
                        value = ef.getDefaultValue();
                    }

                    if (value == null || value.length() == 0) {
                        HTMLUtils.forwardWithErrorMessage(context, i18n, "errormsg;extendedfields.field.0.is.empty."
                                + "but.mandatory", ef.getName());
                        throw new ForwardedToErrorPage("Mandatory field " + ef.getName() + " is empty.");
                    }
                }

                ExtendedFieldDefaultValue def = new ExtendedFieldDefaultValue(value, ef.getFormattingPattern(),
                        ef.getDatatype());
                if (!def.isValid()) {
                    HTMLUtils.forwardWithRawErrorMessage(context, i18n, "errormsg;extendedfields.value.invalid");
                    throw new ForwardedToErrorPage("errormsg;extendedfields.value.invalid");
                }
                value = def.getDBValue();
                break;
            }

            entity.updateExtendedFieldValue(ef.getExtendedFieldID(), value);
        }
    }

}
