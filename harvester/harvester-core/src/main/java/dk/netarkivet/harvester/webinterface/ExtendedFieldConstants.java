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

/**
 * Constants primarily used by class ExtendedFieldDefinition and the jsp pages with extendedField functionality. That
 * is: HarvestDefinition/Definitions-edit-extendedfield.jsp HarvestDefinition/Definitions-list-extendedfields.jsp
 * HarvestDefinition/Definitions-edit-domain.jsp
 */
public class ExtendedFieldConstants {

    public static final String EXTF_ACTION = "extf_action";
    public static final String EXTF_ACTION_CREATE = "create";
    public static final String EXTF_ACTION_READ = "read";
    public static final String EXTF_ACTION_DELETE = "delete";
    public static final String EXTF_ACTION_SUBMIT = "submit";

    public static final String EXTF_ID = "extf_id";
    public static final String EXTF_TYPE_ID = "exf_type_id";
    public static final String EXTF_NAME = "extf_name";
    public static final String EXTF_FORMAT = "extf_format";
    public static final String EXTF_FORMAT_JSCALENDAR = "extf_format_jscalendar";
    public static final String EXTF_DATATYPE = "extf_datatype";
    public static final String EXTF_MANDATORY = "extf_mandatory";
    public static final String EXTF_SEQUENCENR = "extf_sequencenr";
    public static final String EXTF_DEFAULTVALUE_TEXTFIELD = "extf_defaultvalue_textfield";
    public static final String EXTF_DEFAULTVALUE_TEXTAREA = "extf_defaultvalue_textarea";
    public static final String EXTF_DEFAULTVALUE_CHECKBOX = "extf_defaultvalue_checkbox";
    public static final String EXTF_OPTIONS = "extf_options";
    public static final String EXTF_MAXLEN = "extf_maxlen";

    public static final String EXTF_PREFIX = "extf_";
    public static final String EXTF_ALLFIELDIDS = "extf_allfieldids";

    public static final String TRUE = "1";
    public static final String FALSE = "0";

    public static final int MAXLEN_EXTF_BOOLEAN = 1;
    public static final int MAXLEN_EXTF_NAME = 50;
    public static final int MAXLEN_EXTF_FORMAT = 50;
    public static final int MAXLEN_EXTF_DEFAULTVALUE = 50;
    public static final int MAXLEN_EXTF_OPTIONS = 1000;

    public static final int MAXLEN_EXTF_CONTENT = 30000;

}
