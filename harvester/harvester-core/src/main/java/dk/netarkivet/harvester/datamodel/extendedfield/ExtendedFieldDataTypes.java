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

package dk.netarkivet.harvester.datamodel.extendedfield;

/**
 * Constants for the available ExtendedFieldDatatypes. TODO change into an enum.
 */
public class ExtendedFieldDataTypes {

    /** The datatype STRING. */
    public static final int STRING = 1;
    /** The datatype BOOLEAN. */
    public static final int BOOLEAN = 2;
    /** The datatype NUMBER. */
    public static final int NUMBER = 3;
    /** The datatype TIMESTAMP. */
    public static final int TIMESTAMP = 4;
    /** The datatype NOTE. */
    public static final int NOTE = 5;
    /** The datatype SELECT. */
    public static final int SELECT = 6;
    /** The datatype JSCALENDAR. */
    public static final int JSCALENDAR = 7;
    /** Min datatype value. */
    public static final int MIN_DATATYPE_VALUE = 1;
    /** Max datatype value. */
    public static final int MAX_DATATYPE_VALUE = 7;

}
