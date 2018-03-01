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

import java.text.DecimalFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedField;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDBDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDataTypes;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDefaultValue;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldOptions;

/**
 * Contains utility methods for creating and editing schedule definitions for harvests.
 */
public final class ExtendedFieldDefinition {

    /**
     * Private constructor. No instances.
     */
    private ExtendedFieldDefinition() {
    }

    /**
     * Process an request from the jsp-pages. HarvestDefinition/Definitions-edit-extendedfield.jsp
     * HarvestDefinition/Definitions-list-extendedfields.jsp HarvestDefinition/Definitions-edit-domain.jsp
     *
     * @param context the given JSP-context
     * @param i18n the given I18n object.
     * @return the extendedfield resulting from the processing.
     */
    public static ExtendedField processRequest(PageContext context, I18n i18n) {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        ArgumentNotValid.checkNotNull(i18n, "I18n i18n");

        ServletRequest request = context.getRequest();

        Long extendedFieldID = HTMLUtils.parseOptionalLong(context, ExtendedFieldConstants.EXTF_ID, null);
        Long extendedFieldTypeID = HTMLUtils.parseOptionalLong(context, ExtendedFieldConstants.EXTF_TYPE_ID, null);

        HTMLUtils.forwardOnEmptyParameter(context, ExtendedFieldConstants.EXTF_NAME);
        String name = request.getParameter(ExtendedFieldConstants.EXTF_NAME).trim();
        int datatype = HTMLUtils.parseAndCheckInteger(context, ExtendedFieldConstants.EXTF_DATATYPE,
                ExtendedFieldDataTypes.MIN_DATATYPE_VALUE, ExtendedFieldDataTypes.MAX_DATATYPE_VALUE);

        boolean mandatory = false;

        String[] checkboxValues = null;
        checkboxValues = request.getParameterValues(ExtendedFieldConstants.EXTF_MANDATORY);
        if (checkboxValues != null) {
            mandatory = true;
        }

        int maxlen = HTMLUtils.parseAndCheckInteger(context, ExtendedFieldConstants.EXTF_MAXLEN,
                ExtendedFieldConstants.MAXLEN_EXTF_BOOLEAN, ExtendedFieldConstants.MAXLEN_EXTF_CONTENT);
        int sequencenr = HTMLUtils.parseAndCheckInteger(context, ExtendedFieldConstants.EXTF_SEQUENCENR, 1,
                Integer.MAX_VALUE);

        String options = "";
        ExtendedFieldOptions efo = null;

        if (datatype == ExtendedFieldDataTypes.SELECT) {
            options = request.getParameter(ExtendedFieldConstants.EXTF_OPTIONS);
            if (options == null) {
                options = "";
            } else {
                efo = new ExtendedFieldOptions(options);
                if (efo.isValid()) {
                    options = efo.getOptionsString();
                } else {
                    throw new ForwardedToErrorPage("errormsg;extendedfields.options.invalid");
                }
            }
        }

        String defaultvalue = "";
        if (datatype == ExtendedFieldDataTypes.NOTE) {
            defaultvalue = request.getParameter(ExtendedFieldConstants.EXTF_DEFAULTVALUE_TEXTAREA);
        } else if (datatype == ExtendedFieldDataTypes.BOOLEAN) {
            checkboxValues = request.getParameterValues(ExtendedFieldConstants.EXTF_DEFAULTVALUE_CHECKBOX);
            if (checkboxValues != null) {
                defaultvalue = ExtendedFieldConstants.TRUE;
            }
        } else {
            defaultvalue = request.getParameter(ExtendedFieldConstants.EXTF_DEFAULTVALUE_TEXTFIELD);
        }

        if (defaultvalue == null || defaultvalue.length() == 0) {
            defaultvalue = "";
        }

        if (mandatory && defaultvalue.length() == 0) {
            throw new ForwardedToErrorPage("errormsg;extendedfields.defaultvalue.empty");
        }

        String format = "";
        if (datatype == ExtendedFieldDataTypes.NUMBER || datatype == ExtendedFieldDataTypes.TIMESTAMP
                || datatype == ExtendedFieldDataTypes.JSCALENDAR) {
            if (datatype == ExtendedFieldDataTypes.JSCALENDAR) {
                format = request.getParameter(ExtendedFieldConstants.EXTF_FORMAT_JSCALENDAR);
            } else {
                format = request.getParameter(ExtendedFieldConstants.EXTF_FORMAT);
            }
            if (format == null || format.length() == 0) {
                format = "";
            } else {
                format = format.trim();

                Format aFormat = null;
                try {
                    if (datatype == ExtendedFieldDataTypes.NUMBER) {
                        aFormat = new DecimalFormat(format);
                    } else {
                        aFormat = new SimpleDateFormat(format);
                    }
                } catch (IllegalArgumentException e) {
                    throw new ForwardedToErrorPage("errormsg;extendedfields.pattern.invalid");
                }

                if (defaultvalue != null && defaultvalue.length() > 0) {
                    try {
                        aFormat.parseObject(defaultvalue);
                    } catch (ParseException e) {
                        throw new ForwardedToErrorPage("errormsg;extendedfields.value." + "does.not.match.pattern");
                    }
                }
            }
        }

        if (defaultvalue.length() > 0) {
            ExtendedFieldDefaultValue efd = new ExtendedFieldDefaultValue(defaultvalue, format, datatype);
            if (!efd.isValid()) {
                throw new ForwardedToErrorPage("errormsg;extendedfields.defaultvalue.invalid");
            }

            if (datatype == ExtendedFieldDataTypes.SELECT && efo != null && !efo.isKeyValid(defaultvalue)) {
                throw new ForwardedToErrorPage("errormsg;extendedfields.defaultvalue.invalid");
            }
        }

        String maxlenStr = "";
        maxlenStr = request.getParameter(ExtendedFieldConstants.EXTF_MAXLEN);
        if (maxlenStr == null || maxlenStr.length() == 0) {
            try {
                maxlen = Integer.valueOf(maxlenStr);
            } catch (NumberFormatException e) {
                throw new ForwardedToErrorPage("errormsg;extendedfields.maxlen.invalid");
            }
        }

        if (mandatory && defaultvalue.length() == 0) {
            throw new ForwardedToErrorPage("errormsg;extendedfields.defaultvalue.empty");
        }

        ExtendedField extendedField = new ExtendedField(extendedFieldID, extendedFieldTypeID, name, format, datatype,
                mandatory, sequencenr, defaultvalue, options, maxlen);
        updateExtendedField(extendedField);

        return extendedField;
    }

    /**
     * Create or update the extendedField in the database.
     *
     * @param aExtendedField The given extendedfield
     */
    private static void updateExtendedField(ExtendedField aExtendedField) {
        ExtendedFieldDAO extdao = ExtendedFieldDBDAO.getInstance();

        if (aExtendedField.getExtendedFieldID() == null) {
            extdao.create(aExtendedField);
        } else {
            extdao.update(aExtendedField);
        }
    }

    /**
     * Read and return the Extendedfield for the given id.
     *
     * @param aId An Id for a specific ExtendedField
     * @return the Extendedfield for the given id.
     */
    public static ExtendedField readExtendedField(String aId) {
        ExtendedFieldDAO extdao = ExtendedFieldDBDAO.getInstance();
        if (aId == null) {
            return null;
        }
        return extdao.read(Long.parseLong(aId));
    }
}
