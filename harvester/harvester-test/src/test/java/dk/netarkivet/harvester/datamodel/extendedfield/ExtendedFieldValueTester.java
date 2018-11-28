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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import dk.netarkivet.common.utils.SlowTest;
import dk.netarkivet.harvester.datamodel.DataModelTestCase;
import dk.netarkivet.harvester.webinterface.ExtendedFieldConstants;

public class ExtendedFieldValueTester extends DataModelTestCase {

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
    public void testCreateReadUpdateDelete() {
        ExtendedFieldDAO extDAO = ExtendedFieldDBDAO.getInstance();
        ExtendedField extField = new ExtendedField(null, (long) ExtendedFieldTypes.DOMAIN, "Test", "12345", 1, true, 1,
                "a", "b", ExtendedFieldConstants.MAXLEN_EXTF_NAME);
        extDAO.create(extField);

        ExtendedFieldValueDAO extValDAO = ExtendedFieldValueDBDAO.getInstance();

        ExtendedFieldValue extFieldVal = new ExtendedFieldValue(null, extField.getExtendedFieldID(),
                Long.valueOf(100L), "foo");
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
