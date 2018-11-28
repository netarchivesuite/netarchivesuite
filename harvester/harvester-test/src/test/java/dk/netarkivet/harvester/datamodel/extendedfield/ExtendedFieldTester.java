/*
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

import org.junit.Test;
import org.junit.experimental.categories.Category;

import dk.netarkivet.common.utils.SlowTest;
import dk.netarkivet.harvester.datamodel.DataModelTestCase;

public class ExtendedFieldTester extends DataModelTestCase {
    @Category(SlowTest.class)
    @Test
    public void testCreateReadUpdateDelete() {
        ExtendedFieldDAO extDAO = ExtendedFieldDBDAO.getInstance();
        ExtendedField extField = new ExtendedField(null, (long) ExtendedFieldTypes.DOMAIN, "Test", "12345", 1, true, 1,
                "a", "b", 50);
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
        assertEquals(extField.getMaxlen(), 50);

        ExtendedFieldDAO extDAO3 = ExtendedFieldDBDAO.getInstance();
        extField.setExtendedFieldTypeID((long) ExtendedFieldTypes.HARVESTDEFINITION);
        extField.setName("Test2");
        extField.setFormattingPattern("67890");
        extField.setDatatype(2);
        extField.setMandatory(false);
        extField.setSequencenr(2);
        extField.setDefaultValue("c");
        extField.setOptions("d");
        extField.setMaxlen(55);

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
        assertEquals(extField.getMaxlen(), 55);

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
        assertEquals(list.get(0).getMaxlen(), extField.getMaxlen());

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
