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

package dk.netarkivet.harvester.datamodel;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

public class ExtendedFieldValue {
	private Long extendedFieldValueID;
    private Long extendedFieldID;
    private String content;
    private Long instanceID;
    
    public Long getExtendedFieldValueID() {
		return extendedFieldValueID;
	}
	public void setExtendedFieldValueID(Long extendedFieldValueID) {
		this.extendedFieldValueID = extendedFieldValueID;
	}
	public Long getExtendedFieldID() {
		return extendedFieldID;
	}
	public void setExtendedFieldID(Long extendedFieldID) {
		this.extendedFieldID = extendedFieldID;
	}
	
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public Long getInstanceID() {
		return instanceID;
	}
	public void setInstanceID(Long instanceID) {
		this.instanceID = instanceID;
	}
	
	public ExtendedFieldValue() {
	    content = "";
	}
	
    public ExtendedFieldValue(Long aExtendedFieldValueID, Long aExtendedFieldID, Long aInstanceID, String aContent) throws ArgumentNotValid {
    	extendedFieldValueID = aExtendedFieldValueID;
    	extendedFieldID = aExtendedFieldID;
    	instanceID = aInstanceID;
    	content = aContent;
    }
    
	public boolean getBooleanValue() {
		String aValue = getContent();
		if (aValue == null) {
			return false;
		}
		
		aValue = aValue.toLowerCase();
		for (String val : ExtendedFieldDefaultValues.possibleTrueValues) {
			if (aValue.equals(val)) {
				return true;
			}
		}
		
		return false;
	}

}
