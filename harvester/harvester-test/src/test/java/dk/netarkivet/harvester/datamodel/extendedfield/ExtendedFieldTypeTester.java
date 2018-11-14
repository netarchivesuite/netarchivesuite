/*
 * #%L
 * Netarchivesuite - harvester - test
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

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import dk.netarkivet.common.utils.SlowTest;
import dk.netarkivet.harvester.datamodel.DataModelTestCase;

public class ExtendedFieldTypeTester extends DataModelTestCase {
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Category(SlowTest.class)
    @Test
    public void testRead() {
        ExtendedFieldTypeDAO extDAO = ExtendedFieldTypeDBDAO.getInstance();
        ExtendedFieldType type = null;

        type = extDAO.read(Long.valueOf(ExtendedFieldTypes.DOMAIN));
        assertEquals(type.getName(), ExtendedFieldTypes.tableNames[ExtendedFieldTypes.DOMAIN]);

        type = extDAO.read(Long.valueOf(ExtendedFieldTypes.HARVESTDEFINITION));
        assertEquals(type.getName(), ExtendedFieldTypes.tableNames[ExtendedFieldTypes.HARVESTDEFINITION]);

        ExtendedFieldTypeDAO extDAO2 = ExtendedFieldTypeDBDAO.getInstance();
        List<ExtendedFieldType> list = extDAO2.getAll();

        assertEquals(list.size(), 2);
        assertEquals(list.get(0).getName(), ExtendedFieldTypes.tableNames[ExtendedFieldTypes.DOMAIN]);
        assertEquals(list.get(1).getName(), ExtendedFieldTypes.tableNames[ExtendedFieldTypes.HARVESTDEFINITION]);
    }
}
