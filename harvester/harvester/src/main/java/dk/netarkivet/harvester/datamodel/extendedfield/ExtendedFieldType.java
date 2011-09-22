/* File:        $Id$Id$
 * Revision:    $Revision$Revision$
 * Author:      $Author$Author$
 * Date:        $Date$Date$
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

package dk.netarkivet.harvester.datamodel.extendedfield;

import java.io.Serializable;

import dk.netarkivet.common.exceptions.ArgumentNotValid;


/**
 * This class represents one Extended Field Type.
 */
public class ExtendedFieldType implements Serializable {
    private Long extendedFieldTypeID;

    public Long getExtendedFieldTypeID() {
        return extendedFieldTypeID;
    }

    public void setExtendedFieldTypeID(Long extendedFieldTypeID) {
        this.extendedFieldTypeID = extendedFieldTypeID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String name;

    ExtendedFieldType(Long aExtendedFieldTypeID, String aName)
            throws ArgumentNotValid {
        extendedFieldTypeID = aExtendedFieldTypeID;
        name = aName;
    }

}
