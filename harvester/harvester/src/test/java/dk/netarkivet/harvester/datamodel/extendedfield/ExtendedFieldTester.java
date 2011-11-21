/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2011 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.util.List;

import dk.netarkivet.harvester.datamodel.DataModelTestCase;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedField;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDBDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldTypes;



public class ExtendedFieldTester extends DataModelTestCase {
    public ExtendedFieldTester(String aTestName) {
        super(aTestName);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testCreateReadUpdateDelete() {
        ExtendedFieldDAO extDAO = ExtendedFieldDBDAO.getInstance();
        ExtendedField extField = new ExtendedField(null, (long)ExtendedFieldTypes.DOMAIN, "Test", "12345", 1, true, 1, "a", "b");
        extDAO.create(extField);

        ExtendedFieldDAO extDAO2 = ExtendedFieldDBDAO.getInstance();
        extField = extDAO2.read(Long.valueOf(1L));
        
        assertEquals(extField.getExtendedFieldID().longValue(), 1L);
        assertEquals(extField.getExtendedFieldTypeID().longValue(), ExtendedFieldTypes.DOMAIN);
        assertEquals(extField.getName(), "Test");
        assertEquals(extField.getFormattingPattern(), "12345");
        assertEquals(extField.getDatatype(), 1);
        assertEquals(extField.isMandatory(), true);
        assertEquals(extField.getSequencenr(), 1);
        assertEquals(extField.getDefaultValue(), "a");
        assertEquals(extField.getOptions(), "b");
        
        ExtendedFieldDAO extDAO3 = ExtendedFieldDBDAO.getInstance();
        extField.setExtendedFieldTypeID((long)ExtendedFieldTypes.HARVESTDEFINITION);
        extField.setName("Test2");
        extField.setFormattingPattern("67890");
        extField.setDatatype(2);
        extField.setMandatory(false);
        extField.setSequencenr(2);
        extField.setDefaultValue("c");
        extField.setOptions("d");
        
        
        extDAO3.update(extField);

        ExtendedFieldDAO extDAO4 = ExtendedFieldDBDAO.getInstance();
        extField = extDAO4.read(Long.valueOf(1L));
        
        assertEquals(extField.getExtendedFieldID().longValue(), 1L);
        assertEquals(extField.getExtendedFieldTypeID().longValue(), ExtendedFieldTypes.HARVESTDEFINITION);
        assertEquals(extField.getName(), "Test2");
        assertEquals(extField.getFormattingPattern(), "67890");
        assertEquals(extField.getDatatype(), 2);
        assertEquals(extField.isMandatory(), false);
        assertEquals(extField.getSequencenr(), 2);
        assertEquals(extField.getDefaultValue(), "c");
        assertEquals(extField.getOptions(), "d");

        ExtendedFieldDAO extDAO5 = ExtendedFieldDBDAO.getInstance();
        List<ExtendedField> list = extDAO5.getAll(2);
        
        assertEquals(list.size(), 1);
        assertEquals(list.get(0).getExtendedFieldID(), extField.getExtendedFieldID());
        assertEquals(list.get(0).getExtendedFieldTypeID(), extField.getExtendedFieldTypeID());
        assertEquals(list.get(0).getName(), extField.getName());
        assertEquals(list.get(0).getFormattingPattern(), extField.getFormattingPattern());
        assertEquals(list.get(0).getDatatype(), extField.getDatatype());
        assertEquals(list.get(0).isMandatory(), extField.isMandatory());
        assertEquals(list.get(0).getSequencenr(), extField.getSequencenr());
        assertEquals(list.get(0).getDefaultValue(), extField.getDefaultValue());
        assertEquals(list.get(0).getOptions(), extField.getOptions());
        
        ExtendedFieldDAO extDAO6 = ExtendedFieldDBDAO.getInstance();
        assertEquals(extDAO6.exists(extField.getExtendedFieldID()), true);
        extDAO6.delete(extField.getExtendedFieldID());
        
        ExtendedFieldDAO extDAO7 = ExtendedFieldDBDAO.getInstance();
        assertEquals(extDAO7.exists(extField.getExtendedFieldID()), false);
        
        ExtendedFieldDAO extDAO8 = ExtendedFieldDBDAO.getInstance();
        List<ExtendedField> list2 = extDAO8.getAll(ExtendedFieldTypes.HARVESTDEFINITION);
        assertEquals(list2.size(), 0);
    }
}
