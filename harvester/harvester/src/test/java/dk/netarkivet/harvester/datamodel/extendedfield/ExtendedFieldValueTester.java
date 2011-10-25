/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

import dk.netarkivet.harvester.datamodel.DataModelTestCase;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedField;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDBDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldTypes;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldValue;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldValueDAO;
import dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldValueDBDAO;

public class ExtendedFieldValueTester  extends DataModelTestCase {

    public ExtendedFieldValueTester(String aTestName) {
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
        ExtendedField extField = new ExtendedField(null, 
                (long)ExtendedFieldTypes.DOMAIN, "Test", "12345", 1, true, 1, 
                "a", "b");
        extDAO.create(extField);

        ExtendedFieldValueDAO extValDAO = ExtendedFieldValueDBDAO.getInstance();
        
        ExtendedFieldValue extFieldVal = new ExtendedFieldValue(null, 
                extField.getExtendedFieldID(), Long.valueOf(100L), "foo");
        extValDAO.create(extFieldVal);

        ExtendedFieldValueDAO extValDAO2 = ExtendedFieldValueDBDAO.getInstance();
        extFieldVal = extValDAO2.read(1L, 100L);
        
        assertEquals(extFieldVal.getExtendedFieldValueID().longValue(), 1);
        assertEquals(extFieldVal.getExtendedFieldID().longValue(), 1);
        assertEquals(extFieldVal.getInstanceID(), Long.valueOf(100));
        assertEquals(extFieldVal.getContent(), "foo");
        
        extFieldVal.setContent("bar");
        
        ExtendedFieldValueDAO extValDAO3 = ExtendedFieldValueDBDAO.getInstance();
        extValDAO3.update(extFieldVal);

        assertEquals(extFieldVal.getExtendedFieldValueID().longValue(), 1);
        assertEquals(extFieldVal.getExtendedFieldID().longValue(), 1);
        assertEquals(extFieldVal.getInstanceID(), Long.valueOf(100));
        assertEquals(extFieldVal.getContent(), "bar");
    }   
}
