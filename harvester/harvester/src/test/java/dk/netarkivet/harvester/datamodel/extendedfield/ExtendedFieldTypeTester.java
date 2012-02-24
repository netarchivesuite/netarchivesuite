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
package dk.netarkivet.harvester.datamodel.extendedfield;

import dk.netarkivet.harvester.datamodel.DataModelTestCase;

import java.util.List;


public class ExtendedFieldTypeTester extends DataModelTestCase {
    public ExtendedFieldTypeTester(String aTestName) {
        super(aTestName);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testRead() {
        ExtendedFieldTypeDAO extDAO = ExtendedFieldTypeDBDAO.getInstance();
        ExtendedFieldType type = null;

        type = extDAO.read(Long.valueOf(ExtendedFieldTypes.DOMAIN));
        assertEquals(type.getName(),
                ExtendedFieldTypes.tableNames[ExtendedFieldTypes.DOMAIN]);

        type = extDAO.read(Long.valueOf(ExtendedFieldTypes.HARVESTDEFINITION));
        assertEquals(
                type.getName(),
                ExtendedFieldTypes.tableNames[ExtendedFieldTypes.HARVESTDEFINITION]);

        ExtendedFieldTypeDAO extDAO2 = ExtendedFieldTypeDBDAO.getInstance();
        List<ExtendedFieldType> list = extDAO2.getAll();

        assertEquals(list.size(), 2);
        assertEquals(list.get(0).getName(),
                ExtendedFieldTypes.tableNames[ExtendedFieldTypes.DOMAIN]);
        assertEquals(
                list.get(1).getName(),
                ExtendedFieldTypes.tableNames[ExtendedFieldTypes.HARVESTDEFINITION]);
    }
}
