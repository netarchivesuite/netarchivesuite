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

import java.io.Serializable;

/**
 * This class represents one Extended Field Type.
 */
@SuppressWarnings({"serial"})
public class ExtendedFieldType implements Serializable {

    /** The id of this ExtendedFieldType. */
    private Long extendedFieldTypeID;
    /** The name of this ExtendedFieldType. */
    private String name;

    /**
     * Constructor. TODO Add validation
     *
     * @param aExtendedFieldTypeID The id of this ExtendedFieldType.
     * @param aName The name of this ExtendedFieldType.
     */
    ExtendedFieldType(Long aExtendedFieldTypeID, String aName) {
        extendedFieldTypeID = aExtendedFieldTypeID;
        name = aName;
    }

    /**
     * @return id of this ExtendedFieldType.
     */
    public Long getExtendedFieldTypeID() {
        return extendedFieldTypeID;
    }

    /**
     * Set the id of this ExtendedFieldType.
     *
     * @param extendedFieldTypeID the id of this ExtendedFieldType
     */
    public void setExtendedFieldTypeID(Long extendedFieldTypeID) {
        this.extendedFieldTypeID = extendedFieldTypeID;
    }

    /**
     * @return the name of this ExtendedFieldType.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this ExtendedFieldType.
     *
     * @param name the name of this ExtendedFieldType.
     */
    public void setName(String name) {
        this.name = name;
    }

}
