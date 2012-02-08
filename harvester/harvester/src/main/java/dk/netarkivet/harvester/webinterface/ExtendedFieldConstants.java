/* File:        $Id$Id$
 * Revision:    $Revision$Revision$
 * Author:      $Author$Author$
 * Date:        $Date$Date$
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

/** 
 * Constants primarily used by class ExtendedFieldDefinition and 
 * the jsp pages with extendedField functionality.
 * That is:
 * HarvestDefinition/Definitions-edit-extendedfield.jsp
 * HarvestDefinition/Definitions-list-extendedfields.jsp
 * HarvestDefinition/Definitions-edit-domain.jsp 
 * 
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
    public static final String EXTF_DATATYPE = "extf_datatype";
    public static final String EXTF_MANDATORY = "extf_mandatory";
    public static final String EXTF_SEQUENCENR = "extf_sequencenr";
    public static final String EXTF_DEFAULTVALUE = "extf_defaultvalue";
    public static final String EXTF_OPTIONS = "extf_options";
    
    public static final String EXTF_PREFIX = "extf_";
    public static final String EXTF_ALLFIELDIDS = "extf_allfieldids";
    
}
